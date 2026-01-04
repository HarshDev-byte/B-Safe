package com.safeguard.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "emergency_contacts")
@Serializable
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "", // For internet-based alerts without SIM
    val relationship: String = "",
    val isPrimary: Boolean = false,
    val enableSMS: Boolean = true,
    val enableCall: Boolean = false,
    val enableEmail: Boolean = true, // Email alerts (works without SIM)
    val enablePushNotification: Boolean = true, // Push notification (works without SIM)
    val enableLiveLocation: Boolean = false,
    val telegramChatId: String = "", // Optional Telegram integration
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sos_events")
@Serializable
data class SOSEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerType: TriggerType,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val address: String?,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val status: SOSStatus = SOSStatus.ACTIVE,
    val smsSentCount: Int = 0,
    val callsMadeCount: Int = 0,
    val notes: String? = null
)

@Entity(tableName = "location_updates")
@Serializable
data class LocationUpdate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sosEventId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val speed: Float?,
    val bearing: Float?,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "danger_zones")
@Serializable
data class DangerZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 500f,
    val isEnabled: Boolean = true,
    val alertOnEntry: Boolean = true,
    val alertOnExit: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scheduled_checkins")
@Serializable
data class ScheduledCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scheduledTime: Long,
    val graceMinutes: Int = 15,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val recurringDays: String = "", // Comma-separated day numbers (1-7)
    val contactIds: String = "", // Comma-separated contact IDs
    val status: CheckInStatus = CheckInStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class TriggerType {
    VOLUME_BUTTON_SEQUENCE,
    POWER_BUTTON_PATTERN,
    SHAKE_DETECTION,
    WIDGET_BUTTON,
    NOTIFICATION_ACTION,
    LOCK_SCREEN_WIDGET,
    MANUAL_BUTTON,
    VOICE_ACTIVATION,
    SCHEDULED_CHECKIN_MISSED,
    DANGER_ZONE_ENTRY,
    SAFE_WALK_EMERGENCY,
    JOURNEY_OVERDUE,
    THREAT_DETECTED,
    WEARABLE_TRIGGER,
    GUARDIAN_ALERT
}

@Serializable
enum class SOSStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED,
    FAILED
}

@Serializable
enum class CheckInStatus {
    PENDING,
    CHECKED_IN,
    MISSED,
    SOS_TRIGGERED
}
