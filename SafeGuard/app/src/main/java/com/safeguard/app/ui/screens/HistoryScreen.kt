package com.safeguard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeguard.app.data.models.SOSEvent
import com.safeguard.app.data.models.SOSStatus
import com.safeguard.app.data.models.TriggerType
import com.safeguard.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    val events by viewModel.sosEvents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No SOS events yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Your emergency history will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: SOSEvent,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (event.status) {
                        SOSStatus.ACTIVE -> Icons.Default.Warning
                        SOSStatus.CANCELLED -> Icons.Default.Cancel
                        SOSStatus.COMPLETED -> Icons.Default.CheckCircle
                        SOSStatus.FAILED -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (event.status) {
                        SOSStatus.ACTIVE -> MaterialTheme.colorScheme.error
                        SOSStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        SOSStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        SOSStatus.FAILED -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Triggered by: ${getTriggerName(event.triggerType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(event.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (event.status) {
                            SOSStatus.ACTIVE -> MaterialTheme.colorScheme.errorContainer
                            SOSStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                            SOSStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            SOSStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${event.smsSentCount} SMS sent",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${event.callsMadeCount} calls",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Battery5Bar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${event.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (event.latitude != null && event.longitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        event.address ?: "Lat: ${event.latitude}, Lng: ${event.longitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun getTriggerName(type: TriggerType): String {
    return when (type) {
        TriggerType.VOLUME_BUTTON_SEQUENCE -> "Volume Buttons"
        TriggerType.POWER_BUTTON_PATTERN -> "Power Button"
        TriggerType.SHAKE_DETECTION -> "Shake"
        TriggerType.WIDGET_BUTTON -> "Widget"
        TriggerType.NOTIFICATION_ACTION -> "Notification"
        TriggerType.LOCK_SCREEN_WIDGET -> "Lock Screen"
        TriggerType.MANUAL_BUTTON -> "Manual"
        TriggerType.VOICE_ACTIVATION -> "Voice"
        TriggerType.SCHEDULED_CHECKIN_MISSED -> "Missed Check-in"
        TriggerType.DANGER_ZONE_ENTRY -> "Danger Zone"
    }
}
