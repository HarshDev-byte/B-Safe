package com.safeguard.app.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.KeyEvent
import com.safeguard.app.data.models.TriggerType
import com.safeguard.app.data.models.UserSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

class TriggerDetector(private val context: Context) : SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _triggerEvents = MutableSharedFlow<TriggerType>()
    val triggerEvents: SharedFlow<TriggerType> = _triggerEvents.asSharedFlow()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Volume button tracking
    private val volumeButtonPresses = mutableListOf<VolumePress>()
    private var volumeSequenceJob: Job? = null

    // Power button tracking
    private val powerButtonPresses = mutableListOf<Long>()
    private var powerSequenceJob: Job? = null

    // Shake detection
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var acceleration = 0f

    // Settings
    private var settings: UserSettings = UserSettings()

    data class VolumePress(
        val isVolumeUp: Boolean,
        val timestamp: Long
    )

    fun initialize(userSettings: UserSettings) {
        settings = userSettings
        
        if (settings.enableShakeTrigger) {
            initializeShakeDetection()
        }
    }

    fun updateSettings(userSettings: UserSettings) {
        settings = userSettings
        
        if (settings.enableShakeTrigger) {
            initializeShakeDetection()
        } else {
            stopShakeDetection()
        }
    }

    private fun initializeShakeDetection() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopShakeDetection() {
        sensorManager?.unregisterListener(this)
    }

    // Volume button handling
    fun onVolumeKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (!settings.enableVolumeButtonTrigger) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

        if (!isVolumeUp && !isVolumeDown) return false

        val press = VolumePress(isVolumeUp, System.currentTimeMillis())
        volumeButtonPresses.add(press)

        // Reset timeout
        volumeSequenceJob?.cancel()
        volumeSequenceJob = scope.launch {
            delay(settings.volumeButtonTimeoutMs)
            volumeButtonPresses.clear()
        }

        // Check if sequence matches
        checkVolumeSequence()

        return false // Don't consume the event
    }

    private fun checkVolumeSequence() {
        val expectedSequence = settings.volumeButtonSequence
            .split(",")
            .map { it.trim().uppercase() }

        if (volumeButtonPresses.size < expectedSequence.size) return

        val recentPresses = volumeButtonPresses.takeLast(expectedSequence.size)
        val actualSequence = recentPresses.map { if (it.isVolumeUp) "UP" else "DOWN" }

        if (actualSequence == expectedSequence) {
            // Check timing - all presses should be within timeout
            val firstTime = recentPresses.first().timestamp
            val lastTime = recentPresses.last().timestamp
            
            if (lastTime - firstTime <= settings.volumeButtonTimeoutMs) {
                volumeButtonPresses.clear()
                scope.launch {
                    _triggerEvents.emit(TriggerType.VOLUME_BUTTON_SEQUENCE)
                }
            }
        }
    }

    // Power button handling
    fun onPowerButtonPress() {
        if (!settings.enablePowerButtonTrigger) return

        powerButtonPresses.add(System.currentTimeMillis())

        // Reset timeout
        powerSequenceJob?.cancel()
        powerSequenceJob = scope.launch {
            delay(settings.powerButtonTimeoutMs)
            powerButtonPresses.clear()
        }

        // Check if we have enough presses
        if (powerButtonPresses.size >= settings.powerButtonPressCount) {
            val recentPresses = powerButtonPresses.takeLast(settings.powerButtonPressCount)
            val firstTime = recentPresses.first()
            val lastTime = recentPresses.last()

            if (lastTime - firstTime <= settings.powerButtonTimeoutMs) {
                powerButtonPresses.clear()
                scope.launch {
                    _triggerEvents.emit(TriggerType.POWER_BUTTON_PATTERN)
                }
            }
        }
    }

    // Shake detection
    override fun onSensorChanged(event: SensorEvent?) {
        if (!settings.enableShakeTrigger) return
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt(x * x + y * y + z * z)
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta

        if (acceleration > settings.shakeThreshold) {
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastShakeTime > 500) { // Debounce
                shakeCount++
                lastShakeTime = currentTime

                // Reset shake count after timeout
                scope.launch {
                    delay(settings.shakeTimeoutMs)
                    if (System.currentTimeMillis() - lastShakeTime >= settings.shakeTimeoutMs) {
                        shakeCount = 0
                    }
                }

                if (shakeCount >= settings.shakeCount) {
                    shakeCount = 0
                    scope.launch {
                        _triggerEvents.emit(TriggerType.SHAKE_DETECTION)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    // Manual triggers
    fun triggerManualSOS() {
        scope.launch {
            _triggerEvents.emit(TriggerType.MANUAL_BUTTON)
        }
    }

    fun triggerWidgetSOS() {
        scope.launch {
            _triggerEvents.emit(TriggerType.WIDGET_BUTTON)
        }
    }

    fun triggerNotificationSOS() {
        scope.launch {
            _triggerEvents.emit(TriggerType.NOTIFICATION_ACTION)
        }
    }

    fun triggerLockScreenSOS() {
        scope.launch {
            _triggerEvents.emit(TriggerType.LOCK_SCREEN_WIDGET)
        }
    }

    fun cleanup() {
        stopShakeDetection()
        scope.cancel()
    }
}
