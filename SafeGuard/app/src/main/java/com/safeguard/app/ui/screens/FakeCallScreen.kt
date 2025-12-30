package com.safeguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.ui.components.LoadingIndicator
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsState()
    var isCallActive by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }

    // Call duration timer
    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            while (isCallActive) {
                delay(1000)
                callDuration++
            }
        }
    }

    if (isCallActive) {
        // Active call UI
        FakeCallActiveScreen(
            callerName = settings.fakeCallerName,
            callerNumber = settings.fakeCallerNumber,
            duration = callDuration,
            onEndCall = {
                isCallActive = false
                callDuration = 0
                onNavigateBack()
            }
        )
    } else {
        // Setup/trigger screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Fake Call") },
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Fake Incoming Call",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Simulate an incoming call to help you exit uncomfortable situations discreetly.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Caller settings
                OutlinedTextField(
                    value = settings.fakeCallerName,
                    onValueChange = {
                        viewModel.updateUserSettings(settings.copy(fakeCallerName = it))
                    },
                    label = { Text("Caller Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = settings.fakeCallerNumber,
                    onValueChange = {
                        viewModel.updateUserSettings(settings.copy(fakeCallerNumber = it))
                    },
                    label = { Text("Caller Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delay: ${settings.fakeCallDelaySeconds} seconds")
                    Spacer(modifier = Modifier.weight(1f))
                }
                Slider(
                    value = settings.fakeCallDelaySeconds.toFloat(),
                    onValueChange = {
                        viewModel.updateUserSettings(settings.copy(fakeCallDelaySeconds = it.toInt()))
                    },
                    valueRange = 0f..30f,
                    steps = 29
                )

                Spacer(modifier = Modifier.weight(1f))

                // Trigger button
                var isStarting by remember { mutableStateOf(false) }
                var countdown by remember { mutableStateOf(0) }
                
                LaunchedEffect(isStarting) {
                    if (isStarting && settings.fakeCallDelaySeconds > 0) {
                        countdown = settings.fakeCallDelaySeconds
                        while (countdown > 0) {
                            delay(1000)
                            countdown--
                        }
                        isCallActive = true
                        isStarting = false
                    } else if (isStarting) {
                        isCallActive = true
                        isStarting = false
                    }
                }
                
                Button(
                    onClick = {
                        if (settings.fakeCallDelaySeconds > 0) {
                            isStarting = true
                        } else {
                            isCallActive = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isStarting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isStarting) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Starting in $countdown...", fontSize = 18.sp)
                    } else {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Fake Call", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "The call will start in ${settings.fakeCallDelaySeconds} seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FakeCallActiveScreen(
    callerName: String,
    callerNumber: String,
    duration: Int,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Caller avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = callerNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Call duration
            val minutes = duration / 60
            val seconds = duration % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            // Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallControlButton(
                    icon = Icons.Default.VolumeUp,
                    label = "Speaker",
                    onClick = {}
                )
                CallControlButton(
                    icon = Icons.Default.MicOff,
                    label = "Mute",
                    onClick = {}
                )
                CallControlButton(
                    icon = Icons.Default.Dialpad,
                    label = "Keypad",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // End call button
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
