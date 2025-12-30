package com.safeguard.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safeguard.app.R
import com.safeguard.app.SafeGuardApplication
import com.safeguard.app.core.LocationManager
import com.safeguard.app.data.models.LocationUpdate
import com.safeguard.app.data.repository.SafeGuardRepository
import com.safeguard.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.safeguard.app.START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.safeguard.app.STOP_LOCATION_TRACKING"
        const val EXTRA_SOS_EVENT_ID = "sos_event_id"
        
        const val NOTIFICATION_ID = 1003
        const val CHANNEL_ID = "location_tracking_channel"
        const val CHANNEL_NAME = "Location Tracking"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private lateinit var locationManager: LocationManager
    private lateinit var repository: SafeGuardRepository
    
    private var sosEventId: Long = 0

    override fun onCreate() {
        super.onCreate()
        val app = application as SafeGuardApplication
        locationManager = app.locationManager
        repository = app.repository
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                sosEventId = intent.getLongExtra(EXTRA_SOS_EVENT_ID, 0)
                startTracking()
            }
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
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

        // Start continuous location tracking
        locationManager.startContinuousTracking { location ->
            scope.launch {
                saveLocationUpdate(location)
            }
        }
    }

    private suspend fun saveLocationUpdate(location: Location) {
        if (sosEventId <= 0) {
            sosEventId = repository.getActiveSOSEventId().first()
        }
        
        if (sosEventId > 0) {
            val update = LocationUpdate(
                sosEventId = sosEventId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                speed = location.speed,
                bearing = location.bearing
            )
            repository.insertLocationUpdate(update)
        }
    }

    private fun stopTracking() {
        locationManager.stopLocationUpdates()
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
                description = "Tracks location during SOS mode"
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
            .setContentTitle("📍 Location Sharing Active")
            .setContentText("Your location is being shared with emergency contacts")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationManager.stopLocationUpdates()
        scope.cancel()
        super.onDestroy()
    }
}
