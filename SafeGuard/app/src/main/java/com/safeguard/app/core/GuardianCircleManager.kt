package com.safeguard.app.core

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Guardian Circle Manager
 * Family/friend safety network with real-time location sharing and check-ins
 */
class GuardianCircleManager(private val context: Context) {

    companion object {
        private const val TAG = "GuardianCircle"
    }

    data class Guardian(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val phoneNumber: String,
        val email: String?,
        val relationship: String,
        val role: GuardianRole,
        val isActive: Boolean = true,
        val lastLocation: LatLng? = null,
        val lastLocationTime: Long? = null,
        val batteryLevel: Int? = null,
        val isInSOS: Boolean = false,
        val profileImageUrl: String? = null
    )

    enum class GuardianRole(val permissions: List<Permission>) {
        ADMIN(listOf(Permission.VIEW_LOCATION, Permission.RECEIVE_SOS, Permission.MANAGE_CIRCLE, Permission.VIEW_HISTORY)),
        GUARDIAN(listOf(Permission.VIEW_LOCATION, Permission.RECEIVE_SOS, Permission.VIEW_HISTORY)),
        MEMBER(listOf(Permission.VIEW_LOCATION, Permission.RECEIVE_SOS)),
        VIEWER(listOf(Permission.VIEW_LOCATION))
    }

    enum class Permission {
        VIEW_LOCATION,
        RECEIVE_SOS,
        MANAGE_CIRCLE,
        VIEW_HISTORY,
        TRIGGER_CHECK_IN
    }

    data class Circle(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val members: List<Guardian>,
        val createdAt: Long = System.currentTimeMillis(),
        val inviteCode: String = generateInviteCode()
    )

    data class CheckInRequest(
        val id: String = UUID.randomUUID().toString(),
        val fromGuardianId: String,
        val toGuardianId: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val status: CheckInStatus = CheckInStatus.PENDING,
        val responseTime: Long? = null
    )

    enum class CheckInStatus {
        PENDING,
        RESPONDED_SAFE,
        RESPONDED_HELP,
        EXPIRED,
        ESCALATED
    }

    data class LocationUpdate(
        val guardianId: String,
        val location: LatLng,
        val accuracy: Float,
        val speed: Float,
        val batteryLevel: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val activityType: ActivityType
    )

    enum class ActivityType {
        STILL,
        WALKING,
        RUNNING,
        DRIVING,
        CYCLING,
        UNKNOWN
    }

    private val _circles = MutableStateFlow<List<Circle>>(emptyList())
    val circles: StateFlow<List<Circle>> = _circles.asStateFlow()

    private val _activeCircle = MutableStateFlow<Circle?>(null)
    val activeCircle: StateFlow<Circle?> = _activeCircle.asStateFlow()

    private val _pendingCheckIns = MutableStateFlow<List<CheckInRequest>>(emptyList())
    val pendingCheckIns: StateFlow<List<CheckInRequest>> = _pendingCheckIns.asStateFlow()

    private val _memberLocations = MutableStateFlow<Map<String, LocationUpdate>>(emptyMap())
    val memberLocations: StateFlow<Map<String, LocationUpdate>> = _memberLocations.asStateFlow()

    /**
     * Create a new guardian circle
     */
    fun createCircle(name: String): Circle {
        val circle = Circle(name = name, members = emptyList())
        _circles.value = _circles.value + circle
        _activeCircle.value = circle
        return circle
    }

    /**
     * Join a circle using invite code
     */
    fun joinCircle(inviteCode: String): Boolean {
        val circle = _circles.value.find { it.inviteCode == inviteCode }
        if (circle != null) {
            _activeCircle.value = circle
            return true
        }
        return false
    }

    /**
     * Add a guardian to the active circle
     */
    fun addGuardian(
        name: String,
        phoneNumber: String,
        email: String?,
        relationship: String,
        role: GuardianRole = GuardianRole.GUARDIAN
    ): Guardian? {
        val activeCircle = _activeCircle.value ?: return null
        
        val guardian = Guardian(
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            relationship = relationship,
            role = role
        )
        
        val updatedCircle = activeCircle.copy(
            members = activeCircle.members + guardian
        )
        
        _circles.value = _circles.value.map { 
            if (it.id == activeCircle.id) updatedCircle else it 
        }
        _activeCircle.value = updatedCircle
        
        return guardian
    }

    /**
     * Remove a guardian from the circle
     */
    fun removeGuardian(guardianId: String) {
        val activeCircle = _activeCircle.value ?: return
        
        val updatedCircle = activeCircle.copy(
            members = activeCircle.members.filter { it.id != guardianId }
        )
        
        _circles.value = _circles.value.map { 
            if (it.id == activeCircle.id) updatedCircle else it 
        }
        _activeCircle.value = updatedCircle
    }

    /**
     * Send check-in request to a guardian
     */
    fun requestCheckIn(toGuardianId: String, message: String = "Are you safe?"): CheckInRequest {
        val request = CheckInRequest(
            fromGuardianId = "self", // Current user
            toGuardianId = toGuardianId,
            message = message
        )
        _pendingCheckIns.value = _pendingCheckIns.value + request
        return request
    }

    /**
     * Respond to a check-in request
     */
    fun respondToCheckIn(requestId: String, isSafe: Boolean) {
        _pendingCheckIns.value = _pendingCheckIns.value.map { request ->
            if (request.id == requestId) {
                request.copy(
                    status = if (isSafe) CheckInStatus.RESPONDED_SAFE else CheckInStatus.RESPONDED_HELP,
                    responseTime = System.currentTimeMillis()
                )
            } else request
        }
    }

    /**
     * Update location for a guardian
     */
    fun updateMemberLocation(update: LocationUpdate) {
        _memberLocations.value = _memberLocations.value + (update.guardianId to update)
        
        // Update guardian's last known location
        _activeCircle.value?.let { circle ->
            val updatedMembers = circle.members.map { guardian ->
                if (guardian.id == update.guardianId) {
                    guardian.copy(
                        lastLocation = update.location,
                        lastLocationTime = update.timestamp,
                        batteryLevel = update.batteryLevel
                    )
                } else guardian
            }
            _activeCircle.value = circle.copy(members = updatedMembers)
        }
    }

    /**
     * Broadcast SOS to all guardians in circle
     */
    fun broadcastSOS(location: Location, message: String) {
        _activeCircle.value?.members?.forEach { guardian ->
            if (Permission.RECEIVE_SOS in guardian.role.permissions) {
                // Send SOS notification to guardian
                // In production, this would use FCM or SMS
            }
        }
    }

    /**
     * Get guardians who haven't been seen recently
     */
    fun getInactiveGuardians(thresholdMinutes: Int = 60): List<Guardian> {
        val threshold = System.currentTimeMillis() - (thresholdMinutes * 60 * 1000L)
        return _activeCircle.value?.members?.filter { guardian ->
            guardian.lastLocationTime?.let { it < threshold } ?: true
        } ?: emptyList()
    }

    /**
     * Get guardians currently in SOS
     */
    fun getGuardiansInSOS(): List<Guardian> {
        return _activeCircle.value?.members?.filter { it.isInSOS } ?: emptyList()
    }

    /**
     * Generate shareable invite link
     */
    fun getInviteLink(): String {
        val code = _activeCircle.value?.inviteCode ?: return ""
        return "https://bsafe-app.com/join/$code"
    }

    companion object {
        private fun generateInviteCode(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..6).map { chars.random() }.joinToString("")
        }
    }
}
