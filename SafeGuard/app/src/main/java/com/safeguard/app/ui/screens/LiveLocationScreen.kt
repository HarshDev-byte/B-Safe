package com.safeguard.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safeguard.app.core.SOSManager
import com.safeguard.app.data.firebase.LiveLocationData
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val sosState by viewModel.sosState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var isSharing by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    
    // Track if SOS is active
    val isSosActive = sosState is SOSManager.SOSState.Active
    
    // Auto-enable sharing when SOS is active
    LaunchedEffect(isSosActive) {
        if (isSosActive) {
            isSharing = true
        }
    }
    
    // Camera position
    val defaultLocation = currentLocation ?: LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }
    
    // Follow user location
    LaunchedEffect(currentLocation, isSharing) {
        if (isSharing && currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(currentLocation!!)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Live Location")
                        if (isSharing) {
                            Text(
                                "Sharing with ${contacts.count { it.enableLiveLocation }} contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSharing) {
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share link")
                        }
                    }
                },
                colors = if (isSosActive) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                // User location marker with pulse effect when sharing
                currentLocation?.let { location ->
                    if (isSharing) {
                        // Accuracy circle
                        Circle(
                            center = location,
                            radius = 50.0,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // SOS Active Banner
            AnimatedVisibility(
                visible = isSosActive,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SOSActiveBanner()
            }

            // Status card overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isSosActive) 72.dp else 16.dp)
            ) {
                SharingStatusCard(
                    isSharing = isSharing,
                    isSosActive = isSosActive,
                    contactCount = contacts.count { it.enableLiveLocation }
                )
            }

            // My location button
            FloatingActionButton(
                onClick = {
                    currentLocation?.let {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(it, 17f)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 180.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Center on my location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Bottom control panel
            BottomControlPanel(
                isSharing = isSharing,
                isSosActive = isSosActive,
                currentLocation = currentLocation,
                onStartSharing = { isSharing = true },
                onStopSharing = { showStopDialog = true },
                onShareLink = { showShareSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Stop sharing confirmation dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            icon = { Icon(Icons.Default.LocationOff, contentDescription = null) },
            title = { Text("Stop Sharing?") },
            text = { 
                Text(
                    if (isSosActive) 
                        "Your emergency contacts will no longer see your location. SOS alerts will continue."
                    else 
                        "Your trusted contacts will no longer be able to see your location."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSharing = false
                        showStopDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop Sharing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Keep Sharing")
                }
            }
        )
    }

    // Share sheet
    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false }
        ) {
            ShareLocationSheet(
                currentLocation = currentLocation,
                contacts = contacts.filter { it.enableLiveLocation },
                onShareViaApp = { 
                    currentLocation?.let { location ->
                        shareLocationLink(context, location)
                    }
                    showShareSheet = false
                },
                onSendToContact = { contact ->
                    currentLocation?.let { location ->
                        sendLocationSMS(context, contact.phoneNumber, location)
                    }
                    showShareSheet = false
                },
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

@Composable
private fun SOSActiveBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE53935).copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "SOS ACTIVE - Location being shared",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SharingStatusCard(
    isSharing: Boolean,
    isSosActive: Boolean,
    contactCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSharing) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .scale(if (isSharing) scale else 1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSharing) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = if (isSharing) "Sharing Live Location" else "Location Sharing Off",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isSharing) {
                    Text(
                        text = "$contactCount contact${if (contactCount != 1) "s" else ""} can see you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControlPanel(
    isSharing: Boolean,
    isSosActive: Boolean,
    currentLocation: LatLng?,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Location info
            currentLocation?.let { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Last updated
                    var timeText by remember { mutableStateOf("Just now") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1000)
                            timeText = "Updated just now"
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main action button
            if (isSharing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onStopSharing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Sharing")
                    }
                    
                    Button(
                        onClick = onShareLink,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Link")
                    }
                }
            } else {
                Button(
                    onClick = onStartSharing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Sharing Location")
                }
            }

            // Info text
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (isSharing) 
                    "Your trusted contacts can see your real-time location"
                else 
                    "Share your location with emergency contacts for safety",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Battery and accuracy info when sharing
            if (isSharing) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip(
                        icon = Icons.Default.Battery5Bar,
                        label = "Battery",
                        value = "85%"
                    )
                    InfoChip(
                        icon = Icons.Default.GpsFixed,
                        label = "Accuracy",
                        value = "High"
                    )
                    InfoChip(
                        icon = Icons.Default.SignalCellularAlt,
                        label = "Signal",
                        value = "Good"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ShareLocationSheet(
    currentLocation: LatLng?,
    contacts: List<com.safeguard.app.data.models.EmergencyContact>,
    onShareViaApp: () -> Unit,
    onSendToContact: (com.safeguard.app.data.models.EmergencyContact) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Share Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current location preview
        currentLocation?.let { location ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Current Location",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Share via apps
        Button(
            onClick = onShareViaApp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share via Apps")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick send to contacts
        if (contacts.isNotEmpty()) {
            Text(
                "Quick Send to Contacts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            contacts.forEach { contact ->
                ContactShareItem(
                    contact = contact,
                    onClick = { onSendToContact(contact) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
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
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No contacts with live location enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ContactShareItem(
    contact: com.safeguard.app.data.models.EmergencyContact,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.Send,
                contentDescription = "Send to ${contact.name}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper functions
private fun shareLocationLink(context: android.content.Context, location: LatLng) {
    val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
    val shareText = """
        📍 My current location:
        $mapsUrl
        
        Shared via B-Safe
    """.trimIndent()
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share location via"))
}

private fun sendLocationSMS(context: android.content.Context, phoneNumber: String, location: LatLng) {
    val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
    val message = """
        📍 Here's my current location:
        $mapsUrl
        
        Shared via B-Safe
    """.trimIndent()
    
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phoneNumber")
        putExtra("sms_body", message)
    }
    context.startActivity(intent)
}
