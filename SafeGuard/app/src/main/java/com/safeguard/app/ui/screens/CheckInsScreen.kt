package com.safeguard.app.ui.screens

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
import com.safeguard.app.data.models.CheckInStatus
import com.safeguard.app.data.models.ScheduledCheckIn
import com.safeguard.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val checkIns by viewModel.scheduledCheckIns.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Check-ins") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (settings.enableScheduledCheckIns) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Check-in")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Enable/Disable toggle
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Scheduled Check-ins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Alert contacts if you miss a check-in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enableScheduledCheckIns,
                            onCheckedChange = {
                                viewModel.updateUserSettings(settings.copy(enableScheduledCheckIns = it))
                            }
                        )
                    }

                    if (settings.enableScheduledCheckIns) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-trigger SOS on missed check-in")
                                Text(
                                    "Grace period: ${settings.missedCheckInGraceMinutes} minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoSOSOnMissedCheckIn,
                                onCheckedChange = {
                                    viewModel.updateUserSettings(settings.copy(autoSOSOnMissedCheckIn = it))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!settings.enableScheduledCheckIns) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enable check-ins to schedule safety reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (checkIns.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No check-ins scheduled",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Tap + to schedule a safety check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(checkIns, key = { it.id }) { checkIn ->
                        CheckInCard(
                            checkIn = checkIn,
                            onConfirm = { viewModel.confirmCheckIn(checkIn.id) },
                            onToggle = {
                                viewModel.updateCheckIn(checkIn.copy(isEnabled = !checkIn.isEnabled))
                            },
                            onDelete = { viewModel.deleteCheckIn(checkIn) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCheckInDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { checkIn ->
                viewModel.addCheckIn(checkIn)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CheckInCard(
    checkIn: ScheduledCheckIn,
    onConfirm: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (checkIn.status) {
                        CheckInStatus.PENDING -> Icons.Default.Schedule
                        CheckInStatus.CHECKED_IN -> Icons.Default.CheckCircle
                        CheckInStatus.MISSED -> Icons.Default.Warning
                        CheckInStatus.SOS_TRIGGERED -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (checkIn.status) {
                        CheckInStatus.PENDING -> MaterialTheme.colorScheme.primary
                        CheckInStatus.CHECKED_IN -> MaterialTheme.colorScheme.tertiary
                        CheckInStatus.MISSED -> MaterialTheme.colorScheme.error
                        CheckInStatus.SOS_TRIGGERED -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        checkIn.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dateFormat.format(Date(checkIn.scheduledTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checkIn.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            if (checkIn.status == CheckInStatus.PENDING && checkIn.isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I'm Safe - Check In")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(checkIn.status.name.replace("_", " ")) }
                )
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Check-in") },
            text = { Text("Remove this scheduled check-in?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AddCheckInDialog(
    onDismiss: () -> Unit,
    onAdd: (ScheduledCheckIn) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var graceMinutes by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Check-in") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Home from work") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = graceMinutes,
                    onValueChange = { graceMinutes = it },
                    label = { Text("Grace Period (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Check-in will be scheduled for 1 hour from now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            ScheduledCheckIn(
                                title = title,
                                scheduledTime = System.currentTimeMillis() + 3600000, // 1 hour
                                graceMinutes = graceMinutes.toIntOrNull() ?: 15
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
