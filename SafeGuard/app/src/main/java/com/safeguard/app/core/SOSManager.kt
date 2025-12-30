package com.safeguard.app.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.safeguard.app.data.models.*
import com.safeguard.app.data.repository.SafeGuardRepository
import com.safeguard.app.services.SOSForegroundService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class SOSManager(
    private val context: Context,
    private val repository: SafeGuardRepository,
    private val locationManager: LocationManager,
    private val alertManager: AlertManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sosState = MutableStateFlow<SOSState>(SOSState.Idle)
    val sosState: StateFlow<SOSState> = _sosState.asStateFlow()

    private val _currentEvent = MutableStateFlow<SOSEvent?>(null)
    val currentEvent: StateFlow<SOSEvent?> = _currentEvent.asStateFlow()

    private var countdownJob: Job? = null
    private var periodicUpdateJob: Job? = null
    private var currentEventId: Long = 0

    sealed class SOSState {
        object Idle : SOSState()
        data class Countdown(val secondsRemaining: Int) : SOSState()
        object Active : SOSState()
        object Cancelling : SOSState()
    }

    suspend fun triggerSOS(triggerType: TriggerType) {
        if (_sosState.value is SOSState.Active || _sosState.value is SOSState.Countdown) {
            return // Already active
        }

        val settings = repository.getUserSettingsOnce()
        
        if (settings.sosCountdownSeconds > 0 && !settings.enableSilentMode) {
            startCountdown(settings.sosCountdownSeconds, triggerType)
        } else {
            activateSOS(triggerType)
        }
    }

    private fun startCountdown(seconds: Int, triggerType: TriggerType) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in seconds downTo 1) {
                _sosState.value = SOSState.Countdown(i)
                
                val settings = repository.getUserSettingsOnce()
                if (settings.enableCountdownVibration) {
                    alertManager.vibrateCountdown()
                }
                
                delay(1000)
            }
            activateSOS(triggerType)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _sosState.value = SOSState.Idle
    }

    private suspend fun activateSOS(triggerType: TriggerType) {
        _sosState.value = SOSState.Active

        // Get current location
        val location = locationManager.getCurrentLocation()
        
        // Get battery info
        val batteryInfo = getBatteryInfo()

        // Create SOS event
        val event = SOSEvent(
            triggerType = triggerType,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracy = location?.accuracy,
            address = location?.let { locationManager.getAddressFromLocation(it) },
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            networkType = getNetworkType(),
            status = SOSStatus.ACTIVE
        )

        currentEventId = repository.insertSOSEvent(event)
        _currentEvent.value = event.copy(id = currentEventId)
        repository.setSOSActive(true, currentEventId)

        // Start foreground service
        startSOSService()

        // Execute emergency protocol
        executeEmergencyProtocol(location)
    }

    private suspend fun executeEmergencyProtocol(location: Location?) {
        val settings = repository.getUserSettingsOnce()
        val contacts = repository.getSMSEnabledContacts()
        val allContacts = repository.getAllContactsList()
        val batteryInfo = getBatteryInfo()

        android.util.Log.d("SOSManager", "=== EXECUTING EMERGENCY PROTOCOL ===")
        android.util.Log.d("SOSManager", "Location: ${location?.latitude}, ${location?.longitude}")
        android.util.Log.d("SOSManager", "SMS-enabled contacts: ${contacts.size}")
        android.util.Log.d("SOSManager", "All contacts: ${allContacts.size}")
        android.util.Log.d("SOSManager", "Periodic updates enabled: ${settings.enablePeriodicLocationUpdates}")
        android.util.Log.d("SOSManager", "Update interval: ${settings.locationUpdateIntervalSeconds}s")

        // Initialize internet alert manager
        val internetAlertManager = InternetAlertManager(context)

        // 1. Try SMS first (requires SIM card)
        if (contacts.isNotEmpty()) {
            android.util.Log.d("SOSManager", "Sending initial emergency SMS...")
            sendEmergencySMS(contacts, location, settings)
        } else {
            android.util.Log.w("SOSManager", "No SMS-enabled contacts found!")
        }

        // 2. Send internet-based alerts (works WITHOUT SIM card!)
        if (internetAlertManager.isInternetAvailable()) {
            scope.launch {
                try {
                    // Send via Firebase (email + push notifications)
                    val message = internetAlertManager.buildEmergencyMessage(
                        userName = settings.userName.ifEmpty { null },
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        batteryLevel = batteryInfo.first
                    )
                    
                    internetAlertManager.sendInternetAlert(
                        contacts = allContacts,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        message = message,
                        batteryLevel = batteryInfo.first
                    )

                    // Send email alerts to contacts with email
                    val emailContacts = allContacts.filter { it.email.isNotEmpty() && it.enableEmail }
                    if (emailContacts.isNotEmpty()) {
                        val emailHtml = internetAlertManager.buildEmailHtml(
                            userName = settings.userName.ifEmpty { null },
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            batteryLevel = batteryInfo.first
                        )
                        internetAlertManager.sendEmailAlert(
                            recipientEmails = emailContacts.map { it.email },
                            subject = "🆘 EMERGENCY: ${settings.userName.ifEmpty { "Someone" }} needs help!",
                            htmlBody = emailHtml
                        )
                    }

                    android.util.Log.d("SOSManager", "Internet alerts sent successfully")
                } catch (e: Exception) {
                    android.util.Log.e("SOSManager", "Failed to send internet alerts", e)
                }
            }
        }

        // 3. Activate siren if enabled
        if (settings.enableSirenOnSOS && !settings.enableSilentMode) {
            alertManager.startSiren(settings.sirenDurationSeconds)
        }

        // 4. Activate flashlight if enabled
        if (settings.enableFlashlightOnSOS) {
            alertManager.startFlashlight(settings.flashlightPattern)
        }

        // 5. Auto-call if enabled
        if (settings.enableAutoCall) {
            scope.launch {
                delay(settings.autoCallDelay * 1000L)
                if (_sosState.value is SOSState.Active) {
                    makeEmergencyCall(settings)
                }
            }
        }

        // 6. Start periodic location updates
        if (settings.enablePeriodicLocationUpdates) {
            android.util.Log.d("SOSManager", "Starting periodic location updates (every ${settings.locationUpdateIntervalSeconds}s)")
            startPeriodicLocationUpdates(settings)
        } else {
            android.util.Log.d("SOSManager", "Periodic location updates DISABLED in settings")
        }
    }

    private suspend fun sendEmergencySMS(
        contacts: List<EmergencyContact>,
        location: Location?,
        settings: UserSettings
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSManager", "SMS permission not granted!")
            return
        }

        android.util.Log.d("SOSManager", "Sending emergency SMS to ${contacts.size} contacts")
        val message = buildSMSMessage(location, settings)
        android.util.Log.d("SOSManager", "SMS message: $message")
        
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        contacts.forEach { contact ->
            try {
                android.util.Log.d("SOSManager", "Sending SMS to ${contact.name} (${contact.phoneNumber})")
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
                repository.incrementSMSCount(currentEventId)
                android.util.Log.d("SOSManager", "SMS sent successfully to ${contact.name}")
            } catch (e: Exception) {
                android.util.Log.e("SOSManager", "Failed to send SMS to ${contact.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun buildSMSMessage(location: Location?, settings: UserSettings): String {
        var message = settings.smsTemplate

        // Replace placeholders
        val locationText = if (location != null) {
            "Lat: ${location.latitude}, Lng: ${location.longitude}"
        } else {
            "Location unavailable"
        }
        message = message.replace("{LOCATION}", locationText)

        val mapsLink = if (location != null && settings.includeMapLink) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            ""
        }
        message = message.replace("{MAPS_LINK}", mapsLink)

        val timestamp = if (settings.includeTimestamp) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
        } else {
            ""
        }
        message = message.replace("{TIMESTAMP}", timestamp)

        val batteryInfo = getBatteryInfo()
        val batteryText = if (settings.includeBatteryLevel) {
            "${batteryInfo.first}"
        } else {
            ""
        }
        message = message.replace("{BATTERY}", batteryText)

        val personalInfo = if (settings.includePersonalInfoInSOS) {
            buildString {
                if (settings.bloodGroup.isNotEmpty()) append("Blood: ${settings.bloodGroup}\n")
                if (settings.medicalNotes.isNotEmpty()) append("Medical: ${settings.medicalNotes}\n")
                if (settings.allergies.isNotEmpty()) append("Allergies: ${settings.allergies}")
            }
        } else {
            ""
        }
        message = message.replace("{PERSONAL_INFO}", personalInfo)

        return message.trim()
    }

    private fun makeEmergencyCall(settings: UserSettings) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val numberToCall = settings.primaryEmergencyNumber.ifEmpty { 
            settings.regionalEmergencyNumber 
        }

        if (numberToCall.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$numberToCall")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                scope.launch {
                    repository.incrementCallCount(currentEventId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPeriodicLocationUpdates(settings: UserSettings) {
        periodicUpdateJob?.cancel()
        periodicUpdateJob = scope.launch {
            var updateCount = 0
            val intervalMs = settings.locationUpdateIntervalSeconds * 1000L
            
            android.util.Log.d("SOSManager", "Starting periodic updates every ${settings.locationUpdateIntervalSeconds}s")
            
            while (isActive && updateCount < settings.maxLocationUpdates) {
                delay(intervalMs)
                
                if (_sosState.value !is SOSState.Active) {
                    android.util.Log.d("SOSManager", "SOS no longer active, stopping updates")
                    break
                }

                val location = locationManager.getFreshLocation() ?: locationManager.getCurrentLocation()
                if (location != null) {
                    updateCount++
                    android.util.Log.d("SOSManager", "Sending location update #$updateCount")
                    
                    // Save location update
                    val update = LocationUpdate(
                        sosEventId = currentEventId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        speed = location.speed,
                        bearing = location.bearing
                    )
                    repository.insertLocationUpdate(update)

                    // Send update SMS to all SMS-enabled contacts
                    val contacts = repository.getSMSEnabledContacts()
                    if (contacts.isNotEmpty()) {
                        sendLocationUpdateSMS(contacts, location, settings, updateCount)
                    } else {
                        android.util.Log.w("SOSManager", "No SMS-enabled contacts found")
                    }
                } else {
                    android.util.Log.w("SOSManager", "Could not get location for update #${updateCount + 1}")
                }
            }
            android.util.Log.d("SOSManager", "Periodic updates completed after $updateCount updates")
        }
    }

    private suspend fun sendLocationUpdateSMS(
        contacts: List<EmergencyContact>,
        location: Location,
        settings: UserSettings,
        updateNumber: Int
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSManager", "SMS permission not granted")
            return
        }

        val message = """
🆘 LIVE LOCATION UPDATE #$updateNumber

📍 ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}
🗺️ https://maps.google.com/?q=${location.latitude},${location.longitude}
⏰ ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}
🔋 ${getBatteryInfo().first}%
📡 Accuracy: ${location.accuracy.toInt()}m
${if (location.speed > 0) "🚗 Speed: ${(location.speed * 3.6).toInt()} km/h" else ""}
        """.trimIndent()

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
                android.util.Log.d("SOSManager", "SMS update #$updateNumber sent to ${contact.name}")
                repository.incrementSMSCount(currentEventId)
            } catch (e: Exception) {
                android.util.Log.e("SOSManager", "Failed to send SMS to ${contact.name}: ${e.message}")
            }
        }
    }

    suspend fun cancelSOS() {
        _sosState.value = SOSState.Cancelling
        
        countdownJob?.cancel()
        periodicUpdateJob?.cancel()
        
        alertManager.stopSiren()
        alertManager.stopFlashlight()

        if (currentEventId > 0) {
            repository.updateSOSEventStatus(currentEventId, SOSStatus.CANCELLED)
        }
        
        repository.setSOSActive(false, 0)
        _currentEvent.value = null
        currentEventId = 0

        stopSOSService()
        
        _sosState.value = SOSState.Idle
    }

    private fun startSOSService() {
        try {
            // Check if we can start foreground service (Android 14+ requires POST_NOTIFICATIONS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.w("SOSManager", "POST_NOTIFICATIONS permission not granted, skipping foreground service")
                    return
                }
            }
            
            val intent = Intent(context, SOSForegroundService::class.java).apply {
                action = SOSForegroundService.ACTION_START_SOS
            }
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            android.util.Log.e("SOSManager", "Failed to start foreground service: ${e.message}")
        }
    }

    private fun stopSOSService() {
        val intent = Intent(context, SOSForegroundService::class.java).apply {
            action = SOSForegroundService.ACTION_STOP_SOS
        }
        context.startService(intent)
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        return Pair(percentage, isCharging)
    }

    private fun getNetworkType(): String {
        return "Unknown" // Simplified - would need ConnectivityManager for full implementation
    }

    fun cleanup() {
        scope.cancel()
    }
}
