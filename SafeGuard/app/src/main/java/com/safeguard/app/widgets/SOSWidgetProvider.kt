package com.safeguard.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.safeguard.app.R
import com.safeguard.app.services.SOSForegroundService

class SOSWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SOS_WIDGET = "com.safeguard.app.ACTION_SOS_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_SOS_WIDGET) {
            // Trigger SOS
            val sosIntent = Intent(context, SOSForegroundService::class.java).apply {
                action = SOSForegroundService.ACTION_START_SOS
            }
            context.startForegroundService(sosIntent)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_sos)

        // Set up click intent
        val intent = Intent(context, SOSWidgetProvider::class.java).apply {
            action = ACTION_SOS_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_sos_button, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
