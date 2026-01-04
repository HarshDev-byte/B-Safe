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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.safeguard.app.core.SafeWalkManager
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeWalkScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val activeSession by viewModel.activeSafeWalk.collectAsState()
    val pendingCheckIn by viewModel.pendingSafeWalkCheckIn.collectAsState()
    
    SafeWalkScreenContent(
        activeSession = activeSession,
        pendingCheckIn = pendingCheckIn,
        onStartWalk = { name, phone, dest, mins ->
            // For now, use a placeholder location - in production, use geocoding
            viewModel.startSafeWalk(name, phone, LatLng(0.0, 0.0), dest, mins)
        },
        onRespondCheckIn = { isOk -> viewModel.respondToSafeWalkCheckIn(isOk) },
        onConfirmArrival = { viewModel.confirmSafeWalkArrival() },
        onExtendTime = { mins -> viewModel.extendSafeWalkTime(mins) },
        onCancelSession = { viewModel.cancelSafeWalk() },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeWalkScreenContent(
    activeSession: SafeWalkManager.SafeWalkSession?,
    pendingCheckIn: SafeWalkManager.CheckIn?,
    onStartWalk: (companionName: String, companionPhone: String, destination: String, minutes: Int) -> Unit,
    onRespondCheckIn: (Boolean) -> Unit,
    onConfirmArrival: () -> Unit,
    onExtendTime: (Int) -> Unit,
    onCancelSession: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showStartDialog by remember { mutableStateOf(false) }
    var showExtendDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Safe Walk")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚶", fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeSession == null) {
                ExtendedFloatingActionButton(
                    onClick = { showStartDialog = true },
                    icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) },
                    text = { Text("Start Safe Walk") },
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
            // Check-in Alert
            if (pendingCheckIn != null && pendingCheckIn.status == SafeWalkManager.CheckInStatus.PENDING) {
                item {
                    CheckInAlertCard(
                        onRespondOk = { onRespondCheckIn(true) },
                        onRespondHelp = { onRespondCheckIn(false) }
                    )
                }
            }

            // Active Session Card
            if (activeSession != null) {
                item {
                    ActiveWalkCard(
                        session = activeSession,
                        onConfirmArrival = onConfirmArrival,
                        onExtendTime = { showExtendDialog = true },
                        onCancel = onCancelSession
                    )
                }
            } else {
                item {
                    NoActiveWalkCard(onStart = { showStartDialog = true })
                }
            }

            // Info Card
            item {
                SafeWalkInfoCard()
            }

            // How it works
            item {
                HowSafeWalkWorksCard()
            }
        }
    }

    // Start Walk Dialog
    if (showStartDialog) {
        StartSafeWalkDialog(
            onDismiss = { showStartDialog = false },
            onStart = { name, phone, dest, mins ->
                onStartWalk(name, phone, dest, mins)
                showStartDialog = false
            }
        )
    }

    // Extend Time Dialog
    if (showExtendDialog) {
        ExtendTimeDialog(
            onDismiss = { showExtendDialog = false },
            onExtend = { mins ->
                onExtendTime(mins)
                showExtendDialog = false
            }
        )
    }
}

@Composable
private fun CheckInAlertCard(
    onRespondOk: () -> Unit,
    onRespondHelp: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏰", fontSize = 48.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Check-In Time!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Are you doing okay?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRespondOk,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I'm OK")
                }
                
                Button(
                    onClick = onRespondHelp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Need Help")
                }
            }
        }
    }
}

@Composable
private fun ActiveWalkCard(
    session: SafeWalkManager.SafeWalkSession,
    onConfirmArrival: () -> Unit,
    onExtendTime: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (session.status) {
        SafeWalkManager.SessionStatus.ACTIVE -> Color(0xFF4CAF50)
        SafeWalkManager.SessionStatus.PAUSED -> Color(0xFFFFC107)
        SafeWalkManager.SessionStatus.COMPANION_ALERTED -> Color(0xFFFF9800)
        SafeWalkManager.SessionStatus.EMERGENCY_TRIGGERED -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Status Header
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
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Companion Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Walking with",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = session.companionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = session.destinationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Distance & ETA
            session.distanceRemaining?.let { distance ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (distance > 1000) "${(distance/1000).toInt()} km" else "${distance.toInt()} m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    session.etaMinutes?.let { eta ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$eta min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ETA",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
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
                    Text("Arrived!")
                }
            }
        }
    }
}

@Composable
private fun NoActiveWalkCard(onStart: () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🚶", fontSize = 40.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Virtual Safety Companion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Never walk alone. A trusted contact virtually accompanies you and gets alerted if something goes wrong.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onStart) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Safe Walk")
            }
        }
    }
}

@Composable
private fun SafeWalkInfoCard() {
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
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Your Safety Net",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your companion receives real-time updates and gets alerted if you miss check-ins or don't arrive on time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun HowSafeWalkWorksCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val steps = listOf(
                "👤" to "Choose a trusted companion who will virtually walk with you",
                "📍" to "Set your destination and expected arrival time",
                "🚶" to "Walk safely - we'll check in with you periodically",
                "✅" to "Confirm arrival or your companion gets alerted automatically"
            )
            
            steps.forEachIndexed { index, (emoji, text) ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Step ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSafeWalkDialog(
    onDismiss: () -> Unit,
    onStart: (String, String, String, Int) -> Unit
) {
    var companionName by remember { mutableStateOf("") }
    var companionPhone by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Safe Walk") },
        text = {
            Column {
                OutlinedTextField(
                    value = companionName,
                    onValueChange = { companionName = it },
                    label = { Text("Companion Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = companionPhone,
                    onValueChange = { companionPhone = it },
                    label = { Text("Companion Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected time (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Schedule, null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStart(companionName, companionPhone, destination, minutes.toIntOrNull() ?: 30)
                },
                enabled = companionName.isNotBlank() && companionPhone.isNotBlank() && destination.isNotBlank()
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
    var selectedMinutes by remember { mutableStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extend Time") },
        text = {
            Column {
                Text("How much more time do you need?")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExtend(selectedMinutes) }) {
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
