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
import com.safeguard.app.core.TriggerDetector
import com.safeguard.app.data.repository.SafeGuardRepository
import com.safeguard.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class TriggerDetectionService : Service() {

    companion object {
        const val ACTION_START = "com.safeguard.app.START_TRIGGER_DETECTION"
        const val ACTION_STOP = "com.safeguard.app.STOP_TRIGGER_DETECTION"
        
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "trigger_detection_channel"
        const val CHANNEL_NAME = "Background Protection"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var triggerDetector: TriggerDetector
    private lateinit var sosManager: SOSManager
    private lateinit var repository: SafeGuardRepository

    override fun onCreate() {
        super.onCreate()
        val app = application as SafeGuardApplication
        triggerDetector = app.triggerDetector
        sosManager = app.sosManager
        repository = app.repository
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startDetection()
            ACTION_STOP -> stopDetection()
        }
        return START_STICKY
    }

    private fun startDetection() {
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Initialize trigger detector with current settings
        scope.launch {
            val settings = repository.getUserSettings().first()
            triggerDetector.initialize(settings)
        }

        // Listen for trigger events
        scope.launch {
            triggerDetector.triggerEvents.collectLatest { triggerType ->
                sosManager.triggerSOS(triggerType)
            }
        }

        // Update settings when they change
        scope.launch {
            repository.getUserSettings().collectLatest { settings ->
                triggerDetector.updateSettings(settings)
            }
        }
    }

    private fun stopDetection() {
        triggerDetector.cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SafeGuard ready to detect emergency triggers"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeGuard Active")
            .setContentText("Protection is running in background")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        triggerDetector.cleanup()
        scope.cancel()
        super.onDestroy()
    }
}
