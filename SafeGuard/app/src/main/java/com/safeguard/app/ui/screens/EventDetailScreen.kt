package com.safeguard.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safeguard.app.data.models.SOSEvent
import com.safeguard.app.data.models.SOSStatus
import com.safeguard.app.data.models.TriggerType
import com.safeguard.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: MainViewModel,
    eventId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val events by viewModel.sosEvents.collectAsState()
    val event = events.find { it.id == eventId }
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
    val shortDateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    var showFullMap by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (event?.latitude != null && event.longitude != null) {
                        IconButton(onClick = {
                            openInMaps(context, event.latitude!!, event.longitude!!)
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open in Maps")
                        }
                    }
                    IconButton(onClick = {
                        event?.let { shareEventDetails(context, it) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = if (event?.status == SOSStatus.ACTIVE) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFE53935),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { padding ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Event not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Map section (if location available)
                if (event.latitude != null && event.longitude != null) {
                    EventMapSection(
                        event = event,
                        isExpanded = showFullMap,
                        onExpandToggle = { showFullMap = !showFullMap },
                        onOpenInMaps = { openInMaps(context, event.latitude!!, event.longitude!!) }
                    )
                }

                // Details section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Status Card
                    EventStatusCard(event = event, dateFormat = dateFormat)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Stats Row
                    QuickStatsRow(event = event)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trigger Info
                    DetailSection(
                        title = "Trigger Information",
                        icon = Icons.Default.TouchApp
                    ) {
                        DetailRow(
                            icon = getTriggerIcon(event.triggerType),
                            label = "Trigger Type",
                            value = getTriggerDisplayName(event.triggerType)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Info
                    if (event.latitude != null && event.longitude != null) {
                        DetailSection(
                            title = "Location",
                            icon = Icons.Default.LocationOn
                        ) {
                            DetailRow(
                                icon = Icons.Default.MyLocation,
                                label = "Coordinates",
                                value = "${String.format("%.6f", event.latitude)}, ${String.format("%.6f", event.longitude)}"
                            )
                            if (event.accuracy != null) {
                                DetailRow(
                                    icon = Icons.Default.GpsFixed,
                                    label = "Accuracy",
                                    value = "${event.accuracy!!.toInt()}m"
                                )
                            }
                            if (!event.address.isNullOrEmpty()) {
                                DetailRow(
                                    icon = Icons.Default.Place,
                                    label = "Address",
                                    value = event.address!!
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Device Info
                    DetailSection(
                        title = "Device Status",
                        icon = Icons.Default.PhoneAndroid
                    ) {
                        DetailRow(
                            icon = getBatteryIcon(event.batteryLevel, event.isCharging),
                            label = "Battery",
                            value = "${event.batteryLevel}%${if (event.isCharging) " (Charging)" else ""}"
                        )
                        if (!event.networkType.isNullOrEmpty()) {
                            DetailRow(
                                icon = Icons.Default.SignalCellularAlt,
                                label = "Network",
                                value = event.networkType!!
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions Taken
                    DetailSection(
                        title = "Actions Taken",
                        icon = Icons.Default.Notifications
                    ) {
                        ActionRow(
                            icon = Icons.Default.Sms,
                            label = "SMS Alerts",
                            count = event.smsSentCount,
                            color = Color(0xFF4CAF50)
                        )
                        ActionRow(
                            icon = Icons.Default.Call,
                            label = "Emergency Calls",
                            count = event.callsMadeCount,
                            color = Color(0xFF2196F3)
                        )
                    }

                    // Duration (if ended)
                    if (event.endTimestamp != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailSection(
                            title = "Timeline",
                            icon = Icons.Default.Timeline
                        ) {
                            TimelineItem(
                                icon = Icons.Default.PlayArrow,
                                label = "Started",
                                time = shortDateFormat.format(Date(event.timestamp)),
                                isFirst = true
                            )
                            TimelineItem(
                                icon = Icons.Default.Stop,
                                label = "Ended",
                                time = shortDateFormat.format(Date(event.endTimestamp!!)),
                                isLast = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val duration = event.endTimestamp!! - event.timestamp
                            DurationChip(durationMs = duration)
                        }
                    }

                    // Notes
                    if (!event.notes.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailSection(
                            title = "Notes",
                            icon = Icons.Default.Notes
                        ) {
                            Text(
                                text = event.notes!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EventMapSection(
    event: SOSEvent,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onOpenInMaps: () -> Unit
) {
    val location = LatLng(event.latitude!!, event.longitude!!)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 16f)
    }

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            expandVertically() + fadeIn() togetherWith shrinkVertically() + fadeOut()
        },
        label = "map_expand"
    ) { expanded ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (expanded) 350.dp else 200.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = expanded,
                    zoomGesturesEnabled = expanded
                )
            ) {
                // SOS location marker
                Marker(
                    state = MarkerState(position = location),
                    title = "SOS Location",
                    snippet = event.address ?: "Emergency triggered here"
                )
                
                // Accuracy circle
                event.accuracy?.let { accuracy ->
                    Circle(
                        center = location,
                        radius = accuracy.toDouble(),
                        fillColor = Color(0x33E53935),
                        strokeColor = Color(0xFFE53935),
                        strokeWidth = 2f
                    )
                }
            }

            // Status badge overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = event.status.toColor().copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = event.status.toIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.status.toDisplayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Control buttons
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expand/collapse button
                SmallFloatingActionButton(
                    onClick = onExpandToggle,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse map" else "Expand map"
                    )
                }
                
                // Open in maps button
                SmallFloatingActionButton(
                    onClick = onOpenInMaps,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open in Google Maps"
                    )
                }
            }
        }
    }
}

@Composable
private fun EventStatusCard(
    event: SOSEvent,
    dateFormat: SimpleDateFormat
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = event.status.toColor().copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(event.status.toColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.status.toIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SOS ${event.status.toDisplayName()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = event.status.toColor()
                )
                Text(
                    text = dateFormat.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(event: SOSEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            icon = Icons.Default.Sms,
            value = "${event.smsSentCount}",
            label = "SMS Sent",
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Default.Call,
            value = "${event.callsMadeCount}",
            label = "Calls Made",
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            icon = Icons.Default.Battery5Bar,
            value = "${event.batteryLevel}%",
            label = "Battery",
            color = getBatteryColor(event.batteryLevel),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TimelineItem(
    icon: ImageVector,
    label: String,
    time: String,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DurationChip(durationMs: Long) {
    val minutes = durationMs / 60000
    val seconds = (durationMs % 60000) / 1000
    val durationText = when {
        minutes > 60 -> "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Duration: $durationText",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Helper functions
private fun openInMaps(context: android.content.Context, lat: Double, lng: Double) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(SOS Location)")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val browserUri = Uri.parse("https://www.google.com/maps?q=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

private fun shareEventDetails(context: android.content.Context, event: SOSEvent) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val locationText = if (event.latitude != null && event.longitude != null) {
        "\n📍 Location: https://www.google.com/maps?q=${event.latitude},${event.longitude}"
    } else ""
    
    val shareText = """
        🆘 SOS Event Report
        
        Status: ${event.status.toDisplayName()}
        Time: ${dateFormat.format(Date(event.timestamp))}
        Trigger: ${getTriggerDisplayName(event.triggerType)}
        $locationText
        ${event.address?.let { "\n📌 Address: $it" } ?: ""}
        
        📱 Device: ${event.batteryLevel}% battery
        📨 ${event.smsSentCount} SMS sent, ${event.callsMadeCount} calls made
        
        Shared via B-Safe
    """.trimIndent()
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share event details"))
}

private fun getTriggerDisplayName(type: TriggerType): String = when (type) {
    TriggerType.VOLUME_BUTTON_SEQUENCE -> "Volume Button Sequence"
    TriggerType.POWER_BUTTON_PATTERN -> "Power Button Pattern"
    TriggerType.SHAKE_DETECTION -> "Shake Detection"
    TriggerType.WIDGET_BUTTON -> "Home Screen Widget"
    TriggerType.NOTIFICATION_ACTION -> "Notification Action"
    TriggerType.LOCK_SCREEN_WIDGET -> "Lock Screen Widget"
    TriggerType.MANUAL_BUTTON -> "Manual SOS Button"
    TriggerType.VOICE_ACTIVATION -> "Voice Activation"
    TriggerType.SCHEDULED_CHECKIN_MISSED -> "Missed Check-in"
    TriggerType.DANGER_ZONE_ENTRY -> "Danger Zone Entry"
}

private fun getTriggerIcon(type: TriggerType): ImageVector = when (type) {
    TriggerType.VOLUME_BUTTON_SEQUENCE -> Icons.Default.VolumeUp
    TriggerType.POWER_BUTTON_PATTERN -> Icons.Default.Power
    TriggerType.SHAKE_DETECTION -> Icons.Default.Vibration
    TriggerType.WIDGET_BUTTON -> Icons.Default.Widgets
    TriggerType.NOTIFICATION_ACTION -> Icons.Default.Notifications
    TriggerType.LOCK_SCREEN_WIDGET -> Icons.Default.Lock
    TriggerType.MANUAL_BUTTON -> Icons.Default.TouchApp
    TriggerType.VOICE_ACTIVATION -> Icons.Default.Mic
    TriggerType.SCHEDULED_CHECKIN_MISSED -> Icons.Default.Schedule
    TriggerType.DANGER_ZONE_ENTRY -> Icons.Default.Warning
}

private fun getBatteryIcon(level: Int, isCharging: Boolean): ImageVector = when {
    isCharging -> Icons.Default.BatteryChargingFull
    level > 80 -> Icons.Default.BatteryFull
    level > 50 -> Icons.Default.Battery5Bar
    level > 20 -> Icons.Default.Battery3Bar
    else -> Icons.Default.Battery1Bar
}

private fun getBatteryColor(level: Int): Color = when {
    level > 50 -> Color(0xFF4CAF50)
    level > 20 -> Color(0xFFFFC107)
    else -> Color(0xFFE53935)
}

// Extension functions for SOSStatus
private fun SOSStatus.toColor(): Color = when (this) {
    SOSStatus.ACTIVE -> Color(0xFFE53935)
    SOSStatus.CANCELLED -> Color(0xFF9E9E9E)
    SOSStatus.COMPLETED -> Color(0xFF4CAF50)
    SOSStatus.FAILED -> Color(0xFFFF5722)
}

private fun SOSStatus.toIcon(): ImageVector = when (this) {
    SOSStatus.ACTIVE -> Icons.Default.Warning
    SOSStatus.CANCELLED -> Icons.Default.Cancel
    SOSStatus.COMPLETED -> Icons.Default.CheckCircle
    SOSStatus.FAILED -> Icons.Default.Error
}

private fun SOSStatus.toDisplayName(): String = when (this) {
    SOSStatus.ACTIVE -> "Active"
    SOSStatus.CANCELLED -> "Cancelled"
    SOSStatus.COMPLETED -> "Completed"
    SOSStatus.FAILED -> "Failed"
}
