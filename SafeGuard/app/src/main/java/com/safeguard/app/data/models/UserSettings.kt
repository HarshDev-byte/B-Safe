package com.safeguard.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    // User Profile (Optional)
    val userName: String = "",
    val userPhone: String = "",
    val bloodGroup: String = "",
    val medicalNotes: String = "",
    val allergies: String = "",
    val emergencyMedicalInfo: String = "",
    val genderIdentity: String = "", // Optional, user-defined
    val includePersonalInfoInSOS: Boolean = false,

    // Trigger Settings
    val enableVolumeButtonTrigger: Boolean = true,
    val volumeButtonSequence: String = "UP,UP,DOWN,DOWN", // Pattern
    val volumeButtonTimeoutMs: Long = 3000,
    
    val enablePowerButtonTrigger: Boolean = false,
    val powerButtonPressCount: Int = 5,
    val powerButtonTimeoutMs: Long = 3000,
    
    val enableShakeTrigger: Boolean = true,
    val shakeThreshold: Float = 15f,
    val shakeCount: Int = 3,
    val shakeTimeoutMs: Long = 2000,
    
    val enableWidgetTrigger: Boolean = true,
    val enableLockScreenWidget: Boolean = true,
    val enableNotificationTrigger: Boolean = true,

    // SOS Behavior
    val sosCountdownSeconds: Int = 5, // Countdown before SOS activates
    val enableCountdownVibration: Boolean = true,
    val enableSilentMode: Boolean = false, // No siren/flash
    
    val enableSirenOnSOS: Boolean = true,
    val sirenDurationSeconds: Int = 30,
    val enableFlashlightOnSOS: Boolean = true,
    val flashlightPattern: String = "SOS", // SOS morse code pattern
    
    val enableAutoCall: Boolean = false,
    val autoCallDelay: Int = 10, // Seconds after SOS before auto-call
    val primaryEmergencyNumber: String = "",
    val regionalEmergencyNumber: String = "911",
    
    val enablePeriodicLocationUpdates: Boolean = true,
    val locationUpdateIntervalSeconds: Int = 15, // 5, 10, 15, 30, 60 seconds options
    val maxLocationUpdates: Int = 100, // Stop after this many updates (increased for frequent updates)

    // SMS Settings
    val smsTemplate: String = "🆘 EMERGENCY! I need help!\n\n" +
            "Location: {LOCATION}\n" +
            "Maps: {MAPS_LINK}\n" +
            "Time: {TIMESTAMP}\n" +
            "Battery: {BATTERY}%\n" +
            "{PERSONAL_INFO}\n\n" +
            "This is an automated emergency alert from SafeGuard.",
    val includeMapLink: Boolean = true,
    val includeTimestamp: Boolean = true,
    val includeBatteryLevel: Boolean = true,

    // Stealth Mode
    val enableStealthMode: Boolean = false,
    val stealthAppName: String = "Calculator",
    val stealthPinCode: String = "", // PIN to access real app in stealth mode

    // Fake Call Settings
    val enableFakeCall: Boolean = true,
    val fakeCallerName: String = "Mom",
    val fakeCallerNumber: String = "+1234567890",
    val fakeCallDelaySeconds: Int = 5,
    val fakeCallRingtone: String = "default",

    // Privacy & Security
    val enableBiometricLock: Boolean = false,
    val enablePinLock: Boolean = false,
    val appPinCode: String = "",
    val autoDeleteLogsAfterDays: Int = 30,
    val encryptLocalData: Boolean = true,

    // Geofencing
    val enableDangerZoneAlerts: Boolean = false,
    val dangerZoneAlertRadius: Float = 500f,

    // Check-in Settings
    val enableScheduledCheckIns: Boolean = false,
    val missedCheckInGraceMinutes: Int = 15,
    val autoSOSOnMissedCheckIn: Boolean = false,

    // Network & Sync
    val enableCloudBackup: Boolean = false,
    val enableLiveLocationSharing: Boolean = false,
    val liveLocationShareDurationMinutes: Int = 60,

    // App State
    val isOnboardingComplete: Boolean = false,
    val lastSOSEventId: Long = 0,
    val appLaunchCount: Int = 0,
    val lastAppOpenTimestamp: Long = 0
)

@Serializable
data class RegionalSettings(
    val countryCode: String = "US",
    val emergencyNumber: String = "911",
    val policeNumber: String = "911",
    val ambulanceNumber: String = "911",
    val fireNumber: String = "911",
    val womenHelplineNumber: String = "",
    val childHelplineNumber: String = "",
    val customHelplineNumbers: Map<String, String> = emptyMap()
)
