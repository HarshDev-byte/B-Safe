package com.safeguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.safeguard.app.core.JourneyMonitor
import com.safeguard.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeJourney by viewModel.activeJourney.collectAsState()
    var showStartDialog by remember { mutableStateOf(false) }
    var showExtendDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journey Monitor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeJourney == null) {
                ExtendedFloatingActionButton(
                    onClick = { showStartDialog = true },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    text = { Text("Start Journey") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            item {
                InfoCard()
            }
            
            // Active journey card
            if (activeJourney != null) {
                item {
                    ActiveJourneyCard(
                        journey = activeJourney!!,
                        onConfirmArrival = { viewModel.confirmJourneyArrival() },
                        onExtendTime = { showExtendDialog = true },
                        onCancel = { viewModel.cancelJourney() }
                    )
                }
            } else {
                item {
                    NoActiveJourneyCard(onStart = { showStartDialog = true })
                }
            }
            
            // How it works
            item {
                HowItWorksCard()
            }
        }
    }
    
    // Start Journey Dialog
    if (showStartDialog) {
        StartJourneyDialog(
            onDismiss = { showStartDialog = false },
            onStart = { destination, name, minutes, graceMinutes, autoSOS ->
                viewModel.startJourney(
                    destination = destination,
                    destinationName = name,
                    expectedArrivalMinutes = minutes,
                    graceMinutes = graceMinutes,
                    autoSOS = autoSOS
                )
                showStartDialog = false
            }
        )
    }
    
    // Extend Time Dialog
    if (showExtendDialog) {
        ExtendTimeDialog(
            onDismiss = { showExtendDialog = false },
            onExtend = { minutes ->
                viewModel.extendJourneyTime(minutes)
                showExtendDialog = false
            }
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Travel Safety Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Set your destination and expected arrival time. If you don't arrive, your contacts will be alerted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ActiveJourneyCard(
    journey: JourneyMonitor.Journey,
    onConfirmArrival: () -> Unit,
    onExtendTime: () -> Unit,
    onCancel: () -> Unit
) {
    val isOverdue = journey.status == JourneyMonitor.JourneyStatus.OVERDUE
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOverdue) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = alpha)
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverdue) MaterialTheme.colorScheme.error
                                else Color(0xFF4CAF50)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOverdue) "OVERDUE" else "IN PROGRESS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    )
                }
                
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = journey.destinationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ETA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Expected Arrival",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(Date(journey.expectedArrivalTime)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (isOverdue) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You're past your expected arrival time. Tap 'I'm Safe' if you've arrived.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExtendTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MoreTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Extend")
                }
                
                Button(
                    onClick = onConfirmArrival,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I'm Safe")
                }
            }
        }
    }
}

@Composable
private fun NoActiveJourneyCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start monitoring your trip for added safety",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStart) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Journey")
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            HowItWorksStep(
                number = "1",
                title = "Set Destination",
                description = "Enter where you're going and expected arrival time"
            )
            HowItWorksStep(
                number = "2",
                title = "Travel Safely",
                description = "We'll monitor your journey in the background"
            )
            HowItWorksStep(
                number = "3",
                title = "Confirm Arrival",
                description = "Tap 'I'm Safe' when you arrive, or we'll alert your contacts"
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartJourneyDialog(
    onDismiss: () -> Unit,
    onStart: (LatLng, String, Int, Int, Boolean) -> Unit
) {
    var destinationName by remember { mutableStateOf("") }
    var arrivalMinutes by remember { mutableStateOf("30") }
    var graceMinutes by remember { mutableStateOf("15") }
    var autoSOS by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Journey") },
        text = {
            Column {
                OutlinedTextField(
                    value = destinationName,
                    onValueChange = { destinationName = it },
                    label = { Text("Destination Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Home, Office, Friend's place") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = arrivalMinutes,
                    onValueChange = { arrivalMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected arrival (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = graceMinutes,
                    onValueChange = { graceMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Grace period (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Extra time before alerting contacts") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-trigger SOS", fontWeight = FontWeight.Medium)
                        Text(
                            "Automatically send SOS if overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSOS,
                        onCheckedChange = { autoSOS = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = arrivalMinutes.toIntOrNull() ?: 30
                    val grace = graceMinutes.toIntOrNull() ?: 15
                    // Using a placeholder location - in real app, use location picker
                    onStart(LatLng(0.0, 0.0), destinationName, minutes, grace, autoSOS)
                },
                enabled = destinationName.isNotBlank() && arrivalMinutes.isNotBlank()
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExtendTimeDialog(
    onDismiss: () -> Unit,
    onExtend: (Int) -> Unit
) {
    var minutes by remember { mutableStateOf("15") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extend Time") },
        text = {
            Column {
                Text("How many more minutes do you need?")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = minutes == mins.toString(),
                            onClick = { minutes = mins.toString() },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExtend(minutes.toIntOrNull() ?: 15) }) {
                Text("Extend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
