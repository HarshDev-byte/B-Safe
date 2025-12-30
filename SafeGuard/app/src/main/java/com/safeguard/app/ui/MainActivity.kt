package com.safeguard.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.safeguard.app.SafeGuardApplication
import com.safeguard.app.services.TriggerDetectionService
import com.safeguard.app.ui.navigation.SafeGuardNavHost
import com.safeguard.app.ui.theme.SafeGuardTheme

class MainActivity : ComponentActivity() {

    private val app by lazy { application as SafeGuardApplication }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startTriggerDetectionService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            SafeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeGuardNavHost()
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startTriggerDetectionService()
        }
    }

    private fun startTriggerDetectionService() {
        val intent = Intent(this, TriggerDetectionService::class.java).apply {
            action = TriggerDetectionService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
                              keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            app.triggerDetector.onVolumeKeyEvent(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }
}
