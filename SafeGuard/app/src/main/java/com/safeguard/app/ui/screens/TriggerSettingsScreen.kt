package com.safeguard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trigger Methods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Volume Button Trigger
            TriggerCard(
                icon = Icons.Default.VolumeUp,
                title = "Volume Button Sequence",
                description = "Press volume buttons in a specific pattern",
                enabled = settings.enableVolumeButtonTrigger,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enableVolumeButtonTrigger = it))
                }
            ) {
                if (settings.enableVolumeButtonTrigger) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Current Pattern: ${settings.volumeButtonSequence}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("UP,UP,DOWN,DOWN", "UP,DOWN,UP,DOWN", "DOWN,DOWN,DOWN").forEach { pattern ->
                            FilterChip(
                                selected = settings.volumeButtonSequence == pattern,
                                onClick = {
                                    viewModel.updateUserSettings(settings.copy(volumeButtonSequence = pattern))
                                },
                                label = { Text(pattern.replace(",", "→")) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shake Detection
            TriggerCard(
                icon = Icons.Default.Vibration,
                title = "Shake Detection",
                description = "Shake your phone vigorously to trigger SOS",
                enabled = settings.enableShakeTrigger,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enableShakeTrigger = it))
                }
            ) {
                if (settings.enableShakeTrigger) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Shake Count: ${settings.shakeCount}")
                    Slider(
                        value = settings.shakeCount.toFloat(),
                        onValueChange = {
                            viewModel.updateUserSettings(settings.copy(shakeCount = it.toInt()))
                        },
                        valueRange = 2f..5f,
                        steps = 2
                    )
                    
                    Text("Sensitivity: ${if (settings.shakeThreshold < 12) "High" else if (settings.shakeThreshold < 18) "Medium" else "Low"}")
                    Slider(
                        value = settings.shakeThreshold,
                        onValueChange = {
                            viewModel.updateUserSettings(settings.copy(shakeThreshold = it))
                        },
                        valueRange = 8f..25f
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Power Button
            TriggerCard(
                icon = Icons.Default.Power,
                title = "Power Button Pattern",
                description = "Press power button multiple times quickly",
                enabled = settings.enablePowerButtonTrigger,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enablePowerButtonTrigger = it))
                }
            ) {
                if (settings.enablePowerButtonTrigger) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Press Count: ${settings.powerButtonPressCount}")
                    Slider(
                        value = settings.powerButtonPressCount.toFloat(),
                        onValueChange = {
                            viewModel.updateUserSettings(settings.copy(powerButtonPressCount = it.toInt()))
                        },
                        valueRange = 3f..7f,
                        steps = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget Trigger
            TriggerCard(
                icon = Icons.Default.Widgets,
                title = "Home Screen Widget",
                description = "Quick SOS button on your home screen",
                enabled = settings.enableWidgetTrigger,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enableWidgetTrigger = it))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lock Screen Widget
            TriggerCard(
                icon = Icons.Default.LockOpen,
                title = "Lock Screen Access",
                description = "Trigger SOS from lock screen",
                enabled = settings.enableLockScreenWidget,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enableLockScreenWidget = it))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Trigger
            TriggerCard(
                icon = Icons.Default.Notifications,
                title = "Notification Action",
                description = "Quick action from persistent notification",
                enabled = settings.enableNotificationTrigger,
                onToggle = {
                    viewModel.updateUserSettings(settings.copy(enableNotificationTrigger = it))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown Settings
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Countdown Before SOS",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Time to cancel accidental triggers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("${settings.sosCountdownSeconds} seconds")
                    Slider(
                        value = settings.sosCountdownSeconds.toFloat(),
                        onValueChange = {
                            viewModel.updateUserSettings(settings.copy(sosCountdownSeconds = it.toInt()))
                        },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibrate during countdown")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.enableCountdownVibration,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enableCountdownVibration = it))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
            content()
        }
    }
}
