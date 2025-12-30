package com.safeguard.app.core

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Journey Monitor - Track trips and auto-alert if user doesn't reach destination
 * Premium safety feature for travel monitoring
 */
class JourneyMonitor(
    private val context: Context,
    private val locationManager: LocationManager,
    private val sosManager: SOSManager
) {
    companion object {
        private const val TAG = "JourneyMonitor"
        private const val CHECK_INTERVAL_MS = 30_000L // Check every 30 seconds
        private const val DESTINATION_RADIUS_METERS = 200f // Consider arrived within 200m
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class Journey(
        val id: String = UUID.randomUUID().toString(),
        val destination: LatLng,
        val destinationName: String,
        val expectedArrivalTime: Long,
        val startTime: Long = System.currentTimeMillis(),
        val startLocation: LatLng? = null,
        val graceMinutes: Int = 15,
        val notifyContacts: Boolean = true,
        val autoSOS: Boolean = false,
        val status: JourneyStatus = JourneyStatus.ACTIVE
    )
    
    enum class JourneyStatus {
        ACTIVE,
        ARRIVED,
        OVERDUE,
        CANCELLED,
        SOS_TRIGGERED
    }
    
    private val _activeJourney = MutableStateFlow<Journey?>(null)
    val activeJourney: StateFlow<Journey?> = _activeJourney.asStateFlow()
    
    private val _journeyHistory = MutableStateFlow<List<Journey>>(emptyList())
    val journeyHistory: StateFlow<List<Journey>> = _journeyHistory.asStateFlow()
    
    private var monitoringJob: Job? = null
    
    /**
     * Start a new journey with expected arrival time
     */
    fun startJourney(
        destination: LatLng,
        destinationName: String,
        expectedArrivalMinutes: Int,
        graceMinutes: Int = 15,
        notifyContacts: Boolean = true,
        autoSOS: Boolean = false
    ): Journey {
        // Cancel any existing journey
        cancelJourney()
        
        val journey = Journey(
            destination = destination,
            destinationName = destinationName,
            expectedArrivalTime = System.currentTimeMillis() + (expectedArrivalMinutes * 60 * 1000L),
            startLocation = null, // Will be updated when monitoring starts
            graceMinutes = graceMinutes,
            notifyContacts = notifyContacts,
            autoSOS = autoSOS
        )
        
        _activeJourney.value = journey
        startMonitoring()
        
        Log.d(TAG, "Journey started to $destinationName, ETA: $expectedArrivalMinutes minutes")
        return journey
    }
    
    /**
     * Mark journey as arrived (user confirms safe arrival)
     */
    fun confirmArrival() {
        _activeJourney.value?.let { journey ->
            val completedJourney = journey.copy(status = JourneyStatus.ARRIVED)
            addToHistory(completedJourney)
            _activeJourney.value = null
            stopMonitoring()
            Log.d(TAG, "Journey completed - arrived safely")
        }
    }
    
    /**
     * Cancel the current journey
     */
    fun cancelJourney() {
        _activeJourney.value?.let { journey ->
            val cancelledJourney = journey.copy(status = JourneyStatus.CANCELLED)
            addToHistory(cancelledJourney)
            _activeJourney.value = null
            stopMonitoring()
            Log.d(TAG, "Journey cancelled")
        }
    }
    
    /**
     * Extend the expected arrival time
     */
    fun extendTime(additionalMinutes: Int) {
        _activeJourney.value?.let { journey ->
            _activeJourney.value = journey.copy(
                expectedArrivalTime = journey.expectedArrivalTime + (additionalMinutes * 60 * 1000L)
            )
            Log.d(TAG, "Journey extended by $additionalMinutes minutes")
        }
    }
    
    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                checkJourneyStatus()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }
    
    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    private suspend fun checkJourneyStatus() {
        val journey = _activeJourney.value ?: return
        
        // Check if user has arrived at destination
        val currentLocation = locationManager.getCurrentLocation()
        if (currentLocation != null) {
            val distanceToDestination = calculateDistance(
                currentLocation.latitude, currentLocation.longitude,
                journey.destination.latitude, journey.destination.longitude
            )
            
            if (distanceToDestination <= DESTINATION_RADIUS_METERS) {
                Log.d(TAG, "User arrived at destination")
                confirmArrival()
                return
            }
        }
        
        // Check if journey is overdue
        val now = System.currentTimeMillis()
        val overdueTime = journey.expectedArrivalTime + (journey.graceMinutes * 60 * 1000L)
        
        if (now > overdueTime) {
            Log.w(TAG, "Journey is OVERDUE!")
            handleOverdueJourney(journey)
        } else if (now > journey.expectedArrivalTime) {
            // In grace period - update status
            _activeJourney.value = journey.copy(status = JourneyStatus.OVERDUE)
        }
    }
    
    private suspend fun handleOverdueJourney(journey: Journey) {
        if (journey.autoSOS) {
            Log.w(TAG, "Auto-triggering SOS due to overdue journey")
            _activeJourney.value = journey.copy(status = JourneyStatus.SOS_TRIGGERED)
            addToHistory(_activeJourney.value!!)
            _activeJourney.value = null
            stopMonitoring()
            
            // Trigger SOS
            withContext(Dispatchers.Main) {
                sosManager.triggerSOS(com.safeguard.app.data.models.TriggerType.SCHEDULED_CHECKIN_MISSED)
            }
        } else {
            // Just mark as overdue - contacts will be notified
            _activeJourney.value = journey.copy(status = JourneyStatus.OVERDUE)
        }
    }
    
    private fun addToHistory(journey: Journey) {
        _journeyHistory.value = listOf(journey) + _journeyHistory.value.take(49) // Keep last 50
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    fun getFormattedETA(): String? {
        val journey = _activeJourney.value ?: return null
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Date(journey.expectedArrivalTime))
    }
    
    fun getRemainingMinutes(): Int? {
        val journey = _activeJourney.value ?: return null
        val remaining = journey.expectedArrivalTime - System.currentTimeMillis()
        return (remaining / 60000).toInt().coerceAtLeast(0)
    }
    
    fun isOverdue(): Boolean {
        val journey = _activeJourney.value ?: return false
        return System.currentTimeMillis() > journey.expectedArrivalTime
    }
    
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
    }
}
