package com.safeguard.app.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.safeguard.app.R

/**
 * Quick Settings Tile for SOS
 * Allows triggering SOS directly from the notification shade / lock screen
 * 
 * Users can add this tile to their Quick Settings panel for instant access
 */
@RequiresApi(Build.VERSION_CODES.N)
class SOSQuickSettingsTile : TileService() {

    companion object {
        private const val TAG = "SOSQuickSettingsTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        // Trigger SOS
        val sosIntent = Intent(this, SOSForegroundService::class.java).apply {
            action = SOSForegroundService.ACTION_START_SOS
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sosIntent)
        } else {
            startService(sosIntent)
        }

        // Update tile to show active state
        qsTile?.let { tile ->
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.sos_active)
            tile.updateTile()
        }

        // Collapse the notification shade
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires different handling
            try {
                val statusBarManager = getSystemService("statusbar")
                statusBarManager?.javaClass?.getMethod("collapsePanels")?.invoke(statusBarManager)
            } catch (e: Exception) {
                // Ignore if we can't collapse
            }
        }
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.sos_tile_label)
            tile.contentDescription = getString(R.string.sos_tile_description)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tap_for_emergency)
            }
            
            tile.updateTile()
        }
    }
}
