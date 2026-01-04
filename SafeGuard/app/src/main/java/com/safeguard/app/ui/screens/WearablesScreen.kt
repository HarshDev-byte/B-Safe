package com.safeguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.core.PanicButtonManager
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearablesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.panicButtonConnectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredPanicButtons.collectAsState()
    val pairedDevices by viewModel.pairedPanicButtons.collectAsState()
    
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Wearables & Panic Buttons")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⌚", fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(connectionState)
            }

            // Paired Devices Section
            item {
                Text(
                    text = "Paired Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pairedDevices.isEmpty()) {
                item {
                    EmptyPairedDevicesCard()
                }
            } else {
                items(pairedDevices) { device ->
                    PairedDeviceCard(
                        device = device,
                        isConnected = connectionState is PanicButtonManager.ConnectionState.Connected &&
                                (connectionState as PanicButtonManager.ConnectionState.Connected).device.address == device.address,
                        onConnect = { viewModel.connectToPanicButton(device) },
                        onDisconnect = { viewModel.disconnectPanicButton() },
                        onRemove = { viewModel.unpairPanicButton(device) }
                    )
                }
            }

            // Scan Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            if (isScanning) {
                                viewModel.stopPanicButtonScan()
                                isScanning = false
                            } else {
                                viewModel.startPanicButtonScan()
                                isScanning = true
                            }
                        }
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Scan")
                        } else {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan")
                        }
                    }
                }
            }

            // Update scanning state based on connection state
            LaunchedEffect(connectionState) {
                isScanning = connectionState is PanicButtonManager.ConnectionState.Scanning
            }

            if (discoveredDevices.isEmpty() && !isScanning) {
                item {
                    NoDevicesFoundCard()
                }
            } else {
                items(discoveredDevices.filter { discovered ->
                    pairedDevices.none { it.address == discovered.address }
                }) { device ->
                    DiscoveredDeviceCard(
                        device = device,
                        onPair = { 
                            viewModel.pairPanicButton(device)
                            viewModel.connectToPanicButton(device)
                        }
                    )
                }
            }

            // Info Cards
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SupportedDevicesCard()
            }

            item {
                HowItWorksCard()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: PanicButtonManager.ConnectionState) {
    val (statusColor, statusText, statusIcon) = when (state) {
        is PanicButtonManager.ConnectionState.Connected -> Triple(
            Color(0xFF4CAF50),
            "Connected to ${state.device.name}",
            Icons.Default.BluetoothConnected
        )
        is PanicButtonManager.ConnectionState.Connecting -> Triple(
            Color(0xFFFFC107),
            "Connecting...",
            Icons.Default.Bluetooth
        )
        is PanicButtonManager.ConnectionState.Scanning -> Triple(
            Color(0xFF2196F3),
            "Scanning for devices...",
            Icons.Default.BluetoothSearching
        )
        is PanicButtonManager.ConnectionState.Error -> Triple(
            Color(0xFFF44336),
            state.message,
            Icons.Default.BluetoothDisabled
        )
        else -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            "No device connected",
            Icons.Default.BluetoothDisabled
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun PairedDeviceCard(
    device: PanicButtonManager.PanicButton,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🔘", fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isConnected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                device.batteryLevel?.let { battery ->
                    Text(
                        text = "Battery: $battery%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                if (isConnected) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                } else {
                    Button(onClick = onConnect) {
                        Text("Connect")
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: PanicButtonManager.PanicButton,
    onPair: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Signal: ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(onClick = onPair) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pair")
            }
        }
    }
}

@Composable
private fun EmptyPairedDevicesCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⌚", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Paired Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan for nearby panic buttons or wearables to pair",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoDevicesFoundCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No devices found",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Make sure your panic button is in pairing mode and nearby",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SupportedDevicesCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Supported Devices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val devices = listOf(
                "🔘" to "Bluetooth Panic Buttons (iTags, etc.)",
                "⌚" to "Wear OS Smartwatches",
                "🔑" to "BLE Key Fobs",
                "📿" to "Safety Pendants"
            )
            
            devices.forEach { (emoji, name) ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val steps = listOf(
                "1️⃣" to "Pair your panic button or wearable device",
                "2️⃣" to "Keep Bluetooth enabled on your phone",
                "3️⃣" to "Press the panic button to instantly trigger SOS",
                "4️⃣" to "Emergency contacts are notified immediately"
            )
            
            steps.forEach { (emoji, text) ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
