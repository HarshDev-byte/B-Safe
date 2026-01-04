package com.safeguard.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wearable Device Manager
 * Supports smartwatches, fitness bands, and Bluetooth panic buttons
 */
class WearableManager(private val context: Context) {

    companion object {
        private const val TAG = "WearableManager"
    }

    data class WearableDevice(
        val id: String,
        val name: String,
        val type: DeviceType,
        val isConnected: Boolean,
        val batteryLevel: Int?,
        val lastSeen: Long,
        val capabilities: List<Capability>
    )

    enum class DeviceType(val icon: String) {
        SMARTWATCH("⌚"),
        FITNESS_BAND("📿"),
        PANIC_BUTTON("🔴"),
        SMART_RING("💍"),
        SMART_JEWELRY("📿"),
        BLUETOOTH_TAG("📍")
    }

    enum class Capability {
        SOS_TRIGGER,
        HEART_RATE,
        FALL_DETECTION,
        LOCATION,
        VIBRATION_ALERT,
        AUDIO_ALERT,
        TWO_WAY_COMMUNICATION
    }

    data class WearableTrigger(
        val deviceId: String,
        val triggerType: TriggerType,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class TriggerType {
        BUTTON_PRESS,
        DOUBLE_TAP,
        LONG_PRESS,
        SHAKE,
        FALL_DETECTED,
        HEART_RATE_ANOMALY,
        GESTURE
    }

    private val _connectedDevices = MutableStateFlow<List<WearableDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WearableDevice>> = _connectedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var onSOSTrigger: (() -> Unit)? = null

    /**
     * Set callback for SOS trigger from wearable
     */
    fun setOnSOSTrigger(callback: () -> Unit) {
        onSOSTrigger = callback
    }

    /**
     * Start scanning for wearable devices
     */
    fun startScanning() {
        _isScanning.value = true
        Log.d(TAG, "Started scanning for wearable devices")
        
        // In production, this would use Bluetooth LE scanning
        // For demo, we'll simulate finding devices
        simulateDeviceDiscovery()
    }

    /**
     * Stop scanning
     */
    fun stopScanning() {
        _isScanning.value = false
        Log.d(TAG, "Stopped scanning")
    }

    /**
     * Connect to a wearable device
     */
    fun connectDevice(deviceId: String): Boolean {
        val devices = _connectedDevices.value.toMutableList()
        val index = devices.indexOfFirst { it.id == deviceId }
        
        if (index >= 0) {
            devices[index] = devices[index].copy(isConnected = true)
            _connectedDevices.value = devices
            Log.d(TAG, "Connected to device: $deviceId")
            return true
        }
        return false
    }

    /**
     * Disconnect from a wearable device
     */
    fun disconnectDevice(deviceId: String) {
        val devices = _connectedDevices.value.toMutableList()
        val index = devices.indexOfFirst { it.id == deviceId }
        
        if (index >= 0) {
            devices[index] = devices[index].copy(isConnected = false)
            _connectedDevices.value = devices
            Log.d(TAG, "Disconnected from device: $deviceId")
        }
    }

    /**
     * Send alert to all connected wearables
     */
    fun sendAlertToWearables(message: String) {
        _connectedDevices.value
            .filter { it.isConnected && Capability.VIBRATION_ALERT in it.capabilities }
            .forEach { device ->
                sendVibrationAlert(device.id)
                Log.d(TAG, "Sent alert to ${device.name}")
            }
    }

    /**
     * Send vibration alert to specific device
     */
    private fun sendVibrationAlert(deviceId: String) {
        // In production, this would send BLE command to device
        Log.d(TAG, "Vibration alert sent to $deviceId")
    }

    /**
     * Handle trigger from wearable device
     */
    fun handleWearableTrigger(trigger: WearableTrigger) {
        Log.d(TAG, "Received trigger from ${trigger.deviceId}: ${trigger.triggerType}")
        
        when (trigger.triggerType) {
            TriggerType.BUTTON_PRESS,
            TriggerType.DOUBLE_TAP,
            TriggerType.LONG_PRESS,
            TriggerType.FALL_DETECTED -> {
                onSOSTrigger?.invoke()
            }
            TriggerType.HEART_RATE_ANOMALY -> {
                // Could trigger a check-in or alert
                Log.d(TAG, "Heart rate anomaly detected")
            }
            else -> {}
        }
    }

    /**
     * Get supported wearable types
     */
    fun getSupportedDeviceTypes(): List<DeviceType> {
        return DeviceType.values().toList()
    }

    /**
     * Check if any device with SOS capability is connected
     */
    fun hasSOSCapableDevice(): Boolean {
        return _connectedDevices.value.any { 
            it.isConnected && Capability.SOS_TRIGGER in it.capabilities 
        }
    }

    private fun simulateDeviceDiscovery() {
        // Simulate finding devices for demo
        val simulatedDevices = listOf(
            WearableDevice(
                id = "watch_001",
                name = "Galaxy Watch 5",
                type = DeviceType.SMARTWATCH,
                isConnected = false,
                batteryLevel = 85,
                lastSeen = System.currentTimeMillis(),
                capabilities = listOf(
                    Capability.SOS_TRIGGER,
                    Capability.HEART_RATE,
                    Capability.FALL_DETECTION,
                    Capability.VIBRATION_ALERT
                )
            ),
            WearableDevice(
                id = "band_001",
                name = "Mi Band 8",
                type = DeviceType.FITNESS_BAND,
                isConnected = false,
                batteryLevel = 72,
                lastSeen = System.currentTimeMillis(),
                capabilities = listOf(
                    Capability.SOS_TRIGGER,
                    Capability.HEART_RATE,
                    Capability.VIBRATION_ALERT
                )
            ),
            WearableDevice(
                id = "panic_001",
                name = "SafetyPro Panic Button",
                type = DeviceType.PANIC_BUTTON,
                isConnected = false,
                batteryLevel = 100,
                lastSeen = System.currentTimeMillis(),
                capabilities = listOf(
                    Capability.SOS_TRIGGER,
                    Capability.LOCATION
                )
            )
        )
        
        _connectedDevices.value = simulatedDevices
        _isScanning.value = false
    }

    fun cleanup() {
        stopScanning()
    }
}
