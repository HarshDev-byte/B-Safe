package com.safeguard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToContacts: (() -> Unit)? = null
) {
    val settings by viewModel.userSettings.collectAsState()
    val regionalSettings by viewModel.regionalSettings.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Behavior") },
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
            // Emergency Contacts Section
            Text(
                "Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Show contact count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${contacts.size} Contact${if (contacts.size != 1) "s" else ""} Added",
                                style = MaterialTheme.typography.titleSmall
                            )
                            val smsCount = contacts.count { it.enableSMS && it.phoneNumber.isNotBlank() }
                            val emailCount = contacts.count { it.enableEmail && it.email.isNotBlank() }
                            Text(
                                "$smsCount SMS • $emailCount Email enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (contacts.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "No contacts added! SOS alerts won't be sent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Show first 3 contacts
                        contacts.take(3).forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    contact.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (contact.enableSMS && contact.phoneNumber.isNotBlank()) {
                                    Icon(
                                        Icons.Default.Sms,
                                        contentDescription = "SMS",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                if (contact.enableEmail && contact.email.isNotBlank()) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = "Email",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (contacts.size > 3) {
                            Text(
                                "+${contacts.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Manage Contacts Button
                    if (onNavigateToContacts != null) {
                        Button(
                            onClick = onNavigateToContacts,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Emergency Contacts")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alert Settings
            Text(
                "Alert Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siren Alert")
                            Text(
                                "Play loud alarm sound",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enableSirenOnSOS,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enableSirenOnSOS = it))
                            }
                        )
                    }

                    if (settings.enableSirenOnSOS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Duration: ${settings.sirenDurationSeconds} seconds")
                        Slider(
                            value = settings.sirenDurationSeconds.toFloat(),
                            onValueChange = {
                                viewModel.updateUserSettings(settings.copy(sirenDurationSeconds = it.toInt()))
                            },
                            valueRange = 10f..120f,
                            steps = 10
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Flashlight")
                            Text(
                                "Flash SOS pattern with camera light",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enableFlashlightOnSOS,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enableFlashlightOnSOS = it))
                            }
                        )
                    }

                    if (settings.enableFlashlightOnSOS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SOS", "CONTINUOUS", "STROBE").forEach { pattern ->
                                FilterChip(
                                    selected = settings.flashlightPattern == pattern,
                                    onClick = {
                                        viewModel.updateUserSettings(settings.copy(flashlightPattern = pattern))
                                    },
                                    label = { Text(pattern) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto Call Settings
            Text(
                "Auto Call",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-dial Emergency")
                            Text(
                                "Automatically call after SOS triggers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enableAutoCall,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enableAutoCall = it))
                            }
                        )
                    }

                    if (settings.enableAutoCall) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Delay before calling: ${settings.autoCallDelay} seconds")
                        Slider(
                            value = settings.autoCallDelay.toFloat(),
                            onValueChange = {
                                viewModel.updateUserSettings(settings.copy(autoCallDelay = it.toInt()))
                            },
                            valueRange = 5f..60f,
                            steps = 10
                        )

                        OutlinedTextField(
                            value = settings.primaryEmergencyNumber,
                            onValueChange = {
                                viewModel.updateUserSettings(settings.copy(primaryEmergencyNumber = it))
                            },
                            label = { Text("Primary Number to Call") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            supportingText = { 
                                Text("Leave empty to call regional emergency (${regionalSettings.emergencyNumber})") 
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location Updates
            Text(
                "Live Location Updates",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Location SMS")
                            Text(
                                "Send live location updates to contacts during SOS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enablePeriodicLocationUpdates,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enablePeriodicLocationUpdates = it))
                            }
                        )
                    }

                    if (settings.enablePeriodicLocationUpdates) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Update Frequency",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Interval selection chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 30, 60).forEach { seconds ->
                                FilterChip(
                                    selected = settings.locationUpdateIntervalSeconds == seconds,
                                    onClick = {
                                        viewModel.updateUserSettings(
                                            settings.copy(locationUpdateIntervalSeconds = seconds)
                                        )
                                    },
                                    label = { 
                                        Text(
                                            if (seconds < 60) "${seconds}s" else "1m"
                                        ) 
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Current: Every ${settings.locationUpdateIntervalSeconds} seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Maximum Updates: ${settings.maxLocationUpdates}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "SMS will stop after ${settings.maxLocationUpdates} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.maxLocationUpdates.toFloat(),
                            onValueChange = {
                                viewModel.updateUserSettings(settings.copy(maxLocationUpdates = it.toInt()))
                            },
                            valueRange = 10f..200f,
                            steps = 18
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Info card
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Contacts with SMS enabled will receive live location updates with Google Maps links",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SMS Template
            Text(
                "SMS Template",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = settings.smsTemplate,
                        onValueChange = {
                            viewModel.updateUserSettings(settings.copy(smsTemplate = it))
                        },
                        label = { Text("Emergency Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Available placeholders: {LOCATION}, {MAPS_LINK}, {TIMESTAMP}, {BATTERY}, {PERSONAL_INFO}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Google Maps link")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.includeMapLink,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(includeMapLink = it))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include timestamp")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.includeTimestamp,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(includeTimestamp = it))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include battery level")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.includeBatteryLevel,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(includeBatteryLevel = it))
                            }
                        )
                    }
                }
            }
        }
    }
}
