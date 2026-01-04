package com.safeguard.app.core

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Safe Walk Manager
 * Virtual companion feature - someone "walks" with you virtually
 * Auto-alerts if you don't reach destination or stop responding
 */
class SafeWalkManager(
    private val context: Context,
    private val locationManager: LocationManager,
    private val sosManager: SOSManager
) {

    companion object {
        private const val TAG = "SafeWalkManager"
        private const val CHECK_IN_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val RESPONSE_TIMEOUT_MS = 2 * 60 * 1000L // 2 minutes to respond
        private const val ARRIVAL_RADIUS_METERS = 100f
    }

    data class SafeWalkSession(
        val id: String = UUID.randomUUID().toString(),
        val companionName: String,
        val companionPhone: String,
        val destination: LatLng,
        val destinationName: String,
        val startTime: Long = System.currentTimeMillis(),
        val expectedArrivalTime: Long,
        val status: SessionStatus = SessionStatus.ACTIVE,
        val checkIns: List<CheckIn> = emptyList(),
        val currentLocation: LatLng? = null,
        val distanceRemaining: Float? = null,
        val etaMinutes: Int? = null
    )

    data class CheckIn(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val location: LatLng?,
        val status: CheckInStatus,
        val responseTime: Long? = null
    )

    enum class SessionStatus {
        ACTIVE,
        PAUSED,
        ARRIVED,
        CANCELLED,
        EMERGENCY_TRIGGERED,
        COMPANION_ALERTED
    }

    enum class CheckInStatus {
        PENDING,
        RESPONDED_OK,
        RESPONDED_HELP,
        MISSED,
        AUTO_CONFIRMED // Location confirmed arrival
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _activeSession = MutableStateFlow<SafeWalkSession?>(null)
    val activeSession: StateFlow<SafeWalkSession?> = _activeSession.asStateFlow()

    private val _pendingCheckIn = MutableStateFlow<CheckIn?>(null)
    val pendingCheckIn: StateFlow<CheckIn?> = _pendingCheckIn.asStateFlow()

    private var monitoringJob: Job? = null
    private var checkInJob: Job? = null

    /**
     * Start a safe walk session
     */
    fun startSafeWalk(
        companionName: String,
        companionPhone: String,
        destination: LatLng,
        destinationName: String,
        expectedMinutes: Int
    ): SafeWalkSession {
        // Cancel any existing session
        endSession()

        val session = SafeWalkSession(
            companionName = companionName,
            companionPhone = companionPhone,
            destination = destination,
            destinationName = destinationName,
            expectedArrivalTime = System.currentTimeMillis() + (expectedMinutes * 60 * 1000L)
        )

        _activeSession.value = session
        startMonitoring()
        notifyCompanionStart(session)

        Log.d(TAG, "Safe walk started to $destinationName with ${companionName}")
        return session
    }

    /**
     * Respond to check-in
     */
    fun respondToCheckIn(isOk: Boolean) {
        val checkIn = _pendingCheckIn.value ?: return
        val session = _activeSession.value ?: return

        val updatedCheckIn = checkIn.copy(
            status = if (isOk) CheckInStatus.RESPONDED_OK else CheckInStatus.RESPONDED_HELP,
            responseTime = System.currentTimeMillis()
        )

        val updatedSession = session.copy(
            checkIns = session.checkIns + updatedCheckIn
        )

        _activeSession.value = updatedSession
        _pendingCheckIn.value = null

        if (!isOk) {
            // User needs help
            triggerEmergency("User responded HELP during safe walk")
        }
    }

    /**
     * Confirm arrival at destination
     */
    fun confirmArrival() {
        val session = _activeSession.value ?: return
        
        _activeSession.value = session.copy(status = SessionStatus.ARRIVED)
        notifyCompanionArrival(session)
        endSession()
        
        Log.d(TAG, "Safe walk completed - arrived at ${session.destinationName}")
    }

    /**
     * Pause the session (e.g., stopped for coffee)
     */
    fun pauseSession(reason: String) {
        val session = _activeSession.value ?: return
        _activeSession.value = session.copy(status = SessionStatus.PAUSED)
        monitoringJob?.cancel()
        
        Log.d(TAG, "Safe walk paused: $reason")
    }

    /**
     * Resume paused session
     */
    fun resumeSession() {
        val session = _activeSession.value ?: return
        if (session.status == SessionStatus.PAUSED) {
            _activeSession.value = session.copy(status = SessionStatus.ACTIVE)
            startMonitoring()
        }
    }

    /**
     * Cancel the session
     */
    fun cancelSession() {
        val session = _activeSession.value ?: return
        _activeSession.value = session.copy(status = SessionStatus.CANCELLED)
        notifyCompanionCancelled(session)
        endSession()
    }

    /**
     * Extend expected arrival time
     */
    fun extendTime(additionalMinutes: Int) {
        val session = _activeSession.value ?: return
        _activeSession.value = session.copy(
            expectedArrivalTime = session.expectedArrivalTime + (additionalMinutes * 60 * 1000L)
        )
        Log.d(TAG, "Extended arrival time by $additionalMinutes minutes")
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                val session = _activeSession.value ?: break
                if (session.status != SessionStatus.ACTIVE) break

                try {
                    // Get current location
                    val location = locationManager.getCurrentLocation()
                    location?.let { updateLocation(it) }

                    // Check if arrived
                    if (location != null && isNearDestination(location, session.destination)) {
                        confirmArrival()
                        break
                    }

                    // Check if overdue
                    if (System.currentTimeMillis() > session.expectedArrivalTime) {
                        handleOverdue(session)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Monitoring error", e)
                }

                delay(30_000) // Check every 30 seconds
            }
        }

        // Start periodic check-ins
        startCheckInTimer()
    }

    private fun startCheckInTimer() {
        checkInJob?.cancel()
        checkInJob = scope.launch {
            while (isActive) {
                delay(CHECK_IN_INTERVAL_MS)
                
                val session = _activeSession.value ?: break
                if (session.status != SessionStatus.ACTIVE) break

                // Request check-in
                requestCheckIn()

                // Wait for response
                delay(RESPONSE_TIMEOUT_MS)

                // Check if responded
                val pendingCheckIn = _pendingCheckIn.value
                if (pendingCheckIn?.status == CheckInStatus.PENDING) {
                    handleMissedCheckIn(pendingCheckIn)
                }
            }
        }
    }

    private fun requestCheckIn() {
        val session = _activeSession.value ?: return
        
        val checkIn = CheckIn(
            location = session.currentLocation,
            status = CheckInStatus.PENDING
        )
        
        _pendingCheckIn.value = checkIn
        
        // Send notification to user
        // In production, show a notification asking "Are you OK?"
        Log.d(TAG, "Check-in requested")
    }

    private fun handleMissedCheckIn(checkIn: CheckIn) {
        val session = _activeSession.value ?: return
        
        val missedCheckIn = checkIn.copy(status = CheckInStatus.MISSED)
        val updatedSession = session.copy(
            checkIns = session.checkIns + missedCheckIn,
            status = SessionStatus.COMPANION_ALERTED
        )
        
        _activeSession.value = updatedSession
        _pendingCheckIn.value = null
        
        // Alert companion
        notifyCompanionMissedCheckIn(session)
        
        Log.w(TAG, "Check-in missed - companion alerted")
    }

    private fun handleOverdue(session: SafeWalkSession) {
        val overdueMinutes = (System.currentTimeMillis() - session.expectedArrivalTime) / 60000
        
        if (overdueMinutes > 15) {
            // Significantly overdue - alert companion
            _activeSession.value = session.copy(status = SessionStatus.COMPANION_ALERTED)
            notifyCompanionOverdue(session)
        }
    }

    private fun updateLocation(location: Location) {
        val session = _activeSession.value ?: return
        
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val distance = calculateDistance(currentLatLng, session.destination)
        val etaMinutes = if (location.speed > 0) {
            (distance / location.speed / 60).toInt()
        } else null
        
        _activeSession.value = session.copy(
            currentLocation = currentLatLng,
            distanceRemaining = distance,
            etaMinutes = etaMinutes
        )
    }

    private fun isNearDestination(location: Location, destination: LatLng): Boolean {
        val distance = calculateDistance(
            LatLng(location.latitude, location.longitude),
            destination
        )
        return distance <= ARRIVAL_RADIUS_METERS
    }

    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }

    private fun triggerEmergency(reason: String) {
        val session = _activeSession.value ?: return
        _activeSession.value = session.copy(status = SessionStatus.EMERGENCY_TRIGGERED)
        
        // Trigger SOS
        scope.launch {
            sosManager.triggerSOS(com.safeguard.app.data.models.TriggerType.SAFE_WALK_EMERGENCY)
        }
        
        Log.e(TAG, "Emergency triggered: $reason")
    }

    private fun notifyCompanionStart(session: SafeWalkSession) {
        // Send SMS to companion
        val message = """
            🚶 B-Safe Walk Started
            ${session.companionName}, I'm walking to ${session.destinationName}.
            Expected arrival: ${formatTime(session.expectedArrivalTime)}
            You'll be notified when I arrive safely.
            
            Track me: https://bsafe-app.com/track/${session.id}
        """.trimIndent()
        
        // In production, send SMS
        Log.d(TAG, "Notified companion: $message")
    }

    private fun notifyCompanionArrival(session: SafeWalkSession) {
        val message = "✅ B-Safe: I've arrived safely at ${session.destinationName}!"
        Log.d(TAG, "Notified companion of arrival")
    }

    private fun notifyCompanionCancelled(session: SafeWalkSession) {
        val message = "ℹ️ B-Safe: Safe walk to ${session.destinationName} was cancelled."
        Log.d(TAG, "Notified companion of cancellation")
    }

    private fun notifyCompanionMissedCheckIn(session: SafeWalkSession) {
        val message = """
            ⚠️ B-Safe ALERT
            ${session.companionName}, I missed a check-in during my walk to ${session.destinationName}.
            Last known location: ${session.currentLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "Unknown"}
            Please try to contact me.
        """.trimIndent()
        
        Log.w(TAG, "Notified companion of missed check-in")
    }

    private fun notifyCompanionOverdue(session: SafeWalkSession) {
        val message = """
            🚨 B-Safe ALERT
            I haven't arrived at ${session.destinationName} and I'm overdue.
            Last known location: ${session.currentLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "Unknown"}
            Please check on me immediately.
        """.trimIndent()
        
        Log.w(TAG, "Notified companion - overdue")
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun endSession() {
        monitoringJob?.cancel()
        checkInJob?.cancel()
        _pendingCheckIn.value = null
    }

    fun cleanup() {
        endSession()
        scope.cancel()
    }
}
