package com.safeguard.app.core

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import kotlinx.coroutines.*

class AlertManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    
    private var sirenJob: Job? = null
    private var flashlightJob: Job? = null
    private var isFlashlightOn = false

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startSiren(durationSeconds: Int) {
        sirenJob?.cancel()
        sirenJob = scope.launch {
            try {
                // Set volume to max
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                // Play alarm sound
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }

                // Also vibrate
                startVibration()

                // Stop after duration
                delay(durationSeconds * 1000L)
                stopSiren()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopSiren() {
        sirenJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            stopVibration()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500) // Vibration pattern
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0) // 0 = repeat from index 0
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    fun vibrateCountdown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }

    fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }

    fun startFlashlight(pattern: String = "SOS") {
        flashlightJob?.cancel()
        flashlightJob = scope.launch {
            try {
                android.util.Log.d("AlertManager", "Starting flashlight with pattern: $pattern")
                when (pattern.uppercase()) {
                    "SOS" -> flashSOSPattern()
                    "CONTINUOUS" -> {
                        setFlashlight(true)
                        // Keep it on until cancelled
                        while (isActive) {
                            delay(1000)
                        }
                    }
                    "STROBE" -> flashStrobePattern()
                    else -> flashSOSPattern()
                }
            } catch (e: CancellationException) {
                android.util.Log.d("AlertManager", "Flashlight job cancelled")
                setFlashlight(false)
            } catch (e: Exception) {
                android.util.Log.e("AlertManager", "Flashlight error: ${e.message}")
                e.printStackTrace()
            } finally {
                setFlashlight(false)
            }
        }
    }

    private suspend fun flashSOSPattern() {
        // SOS in Morse code: ... --- ...
        // . = short (200ms), - = long (600ms), gap between = 200ms
        val dot = 200L
        val dash = 600L
        val gap = 200L
        val letterGap = 600L

        while (currentCoroutineContext().isActive) {
            // S: ...
            repeat(3) {
                if (!currentCoroutineContext().isActive) return
                setFlashlight(true)
                delay(dot)
                setFlashlight(false)
                delay(gap)
            }
            delay(letterGap)

            // O: ---
            repeat(3) {
                if (!currentCoroutineContext().isActive) return
                setFlashlight(true)
                delay(dash)
                setFlashlight(false)
                delay(gap)
            }
            delay(letterGap)

            // S: ...
            repeat(3) {
                if (!currentCoroutineContext().isActive) return
                setFlashlight(true)
                delay(dot)
                setFlashlight(false)
                delay(gap)
            }
            delay(letterGap * 2)
        }
    }

    private suspend fun flashStrobePattern() {
        while (currentCoroutineContext().isActive) {
            setFlashlight(true)
            delay(100)
            setFlashlight(false)
            delay(100)
        }
    }

    private fun setFlashlight(on: Boolean) {
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, on)
                isFlashlightOn = on
                android.util.Log.d("AlertManager", "Flashlight ${if (on) "ON" else "OFF"}")
            } ?: run {
                android.util.Log.w("AlertManager", "No camera ID available for flashlight")
            }
        } catch (e: CameraAccessException) {
            android.util.Log.e("AlertManager", "Camera access error: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("AlertManager", "Flashlight error: ${e.message}")
        }
    }

    fun stopFlashlight() {
        android.util.Log.d("AlertManager", "Stopping flashlight")
        flashlightJob?.cancel()
        flashlightJob = null
        try {
            if (isFlashlightOn) {
                setFlashlight(false)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlertManager", "Error stopping flashlight: ${e.message}")
        }
    }

    fun toggleFlashlight(): Boolean {
        isFlashlightOn = !isFlashlightOn
        setFlashlight(isFlashlightOn)
        return isFlashlightOn
    }

    fun cleanup() {
        stopSiren()
        stopFlashlight()
        scope.cancel()
    }
}
