package com.safeguard.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.safeguard.app.SafeGuardApplication

class VolumeButtonReceiver : BroadcastReceiver() {

    private var previousVolume = -1

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            if (previousVolume != -1) {
                val app = context.applicationContext as? SafeGuardApplication
                app?.let {
                    when {
                        currentVolume > previousVolume -> {
                            // Volume up pressed
                            it.triggerDetector.onVolumeKeyEvent(
                                android.view.KeyEvent.KEYCODE_VOLUME_UP,
                                android.view.KeyEvent(
                                    android.view.KeyEvent.ACTION_DOWN,
                                    android.view.KeyEvent.KEYCODE_VOLUME_UP
                                )
                            )
                        }
                        currentVolume < previousVolume -> {
                            // Volume down pressed
                            it.triggerDetector.onVolumeKeyEvent(
                                android.view.KeyEvent.KEYCODE_VOLUME_DOWN,
                                android.view.KeyEvent(
                                    android.view.KeyEvent.ACTION_DOWN,
                                    android.view.KeyEvent.KEYCODE_VOLUME_DOWN
                                )
                            )
                        }
                        else -> { /* Volume unchanged, do nothing */ }
                    }
                }
            }
            previousVolume = currentVolume
        }
    }
}
