package com.safeguard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTriggerSettings: () -> Unit,
    onNavigateToSOSSettings: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToDangerZones: () -> Unit,
    onNavigateToCheckIns: () -> Unit,
    onNavigateToStealthMode: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        ) {
            SettingsSection(title = "SOS Configuration") {
                SettingsItem(
                    icon = Icons.Default.TouchApp,
                    title = "Trigger Methods",
                    subtitle = "Configure how to activate SOS",
                    onClick = onNavigateToTriggerSettings
                )
                SettingsItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "SOS Behavior",
                    subtitle = "Siren, flashlight, auto-call settings",
                    onClick = onNavigateToSOSSettings
                )
            }

            SettingsSection(title = "Safety Features") {
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Danger Zones",
                    subtitle = "Set up geofence alerts",
                    onClick = onNavigateToDangerZones
                )
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = "Scheduled Check-ins",
                    subtitle = "Auto-alert if you miss a check-in",
                    onClick = onNavigateToCheckIns
                )
                SettingsItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "Stealth Mode",
                    subtitle = "Disguise app as calculator",
                    onClick = onNavigateToStealthMode
                )
            }

            SettingsSection(title = "Privacy & Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Settings",
                    subtitle = "Data encryption, auto-delete logs",
                    onClick = onNavigateToPrivacySettings
                )
            }

            SettingsSection(title = "Quick Toggles") {
                SettingsToggle(
                    icon = Icons.Default.VolumeUp,
                    title = "Silent Mode",
                    subtitle = "Disable siren and flashlight",
                    checked = settings.enableSilentMode,
                    onCheckedChange = {
                        viewModel.updateUserSettings(settings.copy(enableSilentMode = it))
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Phone,
                    title = "Fake Call",
                    subtitle = "Enable fake incoming call feature",
                    checked = settings.enableFakeCall,
                    onCheckedChange = {
                        viewModel.updateUserSettings(settings.copy(enableFakeCall = it))
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "B-Safe v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Your safety is our priority",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
