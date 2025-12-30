package com.safeguard.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safeguard.app.R
import com.safeguard.app.SafeGuardApplication
import com.safeguard.app.core.SOSManager
import com.safeguard.app.data.models.SOSStatus
import com.safeguard.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SOSForegroundService : Service() {

    companion object {
        const val ACTION_START_SOS = "com.safeguard.app.START_SOS"
        const val ACTION_STOP_SOS = "com.safeguard.app.STOP_SOS"
        const val ACTION_CANCEL_SOS = "com.safeguard.app.CANCEL_SOS"
        
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sos_channel"
        const val CHANNEL_NAME = "SOS Active"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var sosManager: SOSManager

    override fun onCreate() {
        super.onCreate()
        sosManager = (application as SafeGuardApplication).sosManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SOS -> startSOSMode()
            ACTION_STOP_SOS, ACTION_CANCEL_SOS -> stopSOSMode()
        }
        return START_STICKY
    }

    private fun startSOSMode() {
        val notification = createSOSNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Monitor SOS state
        scope.launch {
            sosManager.sosState.collectLatest { state ->
                when (state) {
                    is SOSManager.SOSState.Active -> {
                        updateNotification("🆘 SOS ACTIVE - Help is being notified")
                    }
                    is SOSManager.SOSState.Countdown -> {
                        updateNotification("⏱️ SOS in ${state.secondsRemaining}s - Tap to cancel")
                    }
                    is SOSManager.SOSState.Idle -> {
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopSOSMode() {
        scope.launch {
            sosManager.cancelSOS()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when SOS mode is active"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSOSNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, SOSForegroundService::class.java).apply {
            action = ACTION_CANCEL_SOS
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🆘 SOS Active")
            .setContentText("Emergency contacts are being notified")
            .setSmallIcon(R.drawable.ic_sos)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_cancel,
                "Cancel SOS",
                cancelPendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🆘 SafeGuard")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sos)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
