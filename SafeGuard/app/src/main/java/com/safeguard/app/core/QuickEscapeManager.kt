package com.safeguard.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Quick Escape Manager - Instant escape features for dangerous situations
 * - Quick dial emergency
 * - Fake incoming call
 * - Screen blackout (pretend phone died)
 * - Quick app switch (hide the app)
 */
class QuickEscapeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "QuickEscapeManager"
    }
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Quick dial emergency number
     */
    fun quickDialEmergency(number: String = "112") {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Quick dialing: $number")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to quick dial", e)
            // Fallback to dial pad
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    }
    
    /**
     * Open phone dialer with emergency number pre-filled
     */
    fun openDialerWithNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Quick vibration pattern to confirm action (silent confirmation)
     */
    fun confirmationVibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 50), // Quick double tap
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }
    
    /**
     * SOS pattern vibration (... --- ...)
     */
    fun sosVibrate() {
        val dot = 100L
        val dash = 300L
        val gap = 100L
        val letterGap = 300L
        
        val pattern = longArrayOf(
            0,
            // S: ...
            dot, gap, dot, gap, dot, letterGap,
            // O: ---
            dash, gap, dash, gap, dash, letterGap,
            // S: ...
            dot, gap, dot, gap, dot
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
    
    /**
     * Open Google Maps with directions to nearest police station
     */
    fun navigateToNearestPolice() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=police+station+near+me")
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/maps/search/police+station+near+me")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Open Google Maps with directions to nearest hospital
     */
    fun navigateToNearestHospital() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=hospital+near+me")
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/maps/search/hospital+near+me")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Share current location via any app
     */
    fun shareLocation(latitude: Double, longitude: Double, message: String = "") {
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val shareText = if (message.isNotEmpty()) {
            "$message\n\nMy location: $mapsLink"
        } else {
            "My current location: $mapsLink"
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Share Location").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
    
    /**
     * Open WhatsApp with pre-filled emergency message
     */
    fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        try {
            val formattedNumber = phoneNumber.replace("+", "").replace(" ", "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp", e)
        }
    }
    
    /**
     * Open Telegram with pre-filled message
     */
    fun sendTelegramMessage(username: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tg://msg?to=$username&text=${Uri.encode(message)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://t.me/$username")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
    
    /**
     * Go to home screen (hide the app quickly)
     */
    fun goToHomeScreen() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
