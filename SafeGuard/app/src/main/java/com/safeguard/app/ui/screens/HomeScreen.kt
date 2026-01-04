package com.safeguard.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safeguard.app.core.SOSManager
import com.safeguard.app.data.models.UserSettings
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNearbyPlaces: () -> Unit = {},
    onNavigateToLiveLocation: () -> Unit = {},
    onNavigateToSafeRoute: () -> Unit = {},
    onNavigateToSafetyScore: () -> Unit = {},
    onNavigateToJourney: () -> Unit = {},
    onNavigateToAIInsights: () -> Unit = {},
    onNavigateToSafeWalk: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToWearables: () -> Unit = {},
    onSignIn: () -> Unit = {}
) {
    val sosState by viewModel.sosState.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val dangerZones by viewModel.dangerZones.collectAsState()
    val safetyScore by viewModel.safetyScore.collectAsState()
    val activeJourney by viewModel.activeJourney.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Bottom sheet state
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                               permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Map camera position
    val defaultLocation = LatLng(28.6139, 77.2090) // Default to Delhi
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: defaultLocation, 15f)
    }

    // Update camera when location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 15f),
                durationMs = 1000
            )
        }
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 180.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        },
        sheetContent = {
            BottomSheetContent(
                sosState = sosState,
                contacts = contacts,
                settings = settings,
                currentUser = currentUser,
                safetyScore = safetyScore,
                activeJourney = activeJourney,
                onTriggerSOS = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerSOS()
                },
                onCancelSOS = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.cancelSOS()
                },
                onCancelCountdown = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.cancelCountdown()
                },
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToFakeCall = onNavigateToFakeCall,
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToNearbyPlaces = onNavigateToNearbyPlaces,
                onNavigateToLiveLocation = onNavigateToLiveLocation,
                onNavigateToSafeRoute = onNavigateToSafeRoute,
                onNavigateToSafetyScore = onNavigateToSafetyScore,
                onNavigateToJourney = onNavigateToJourney,
                onNavigateToAIInsights = onNavigateToAIInsights,
                onNavigateToSafeWalk = onNavigateToSafeWalk,
                onNavigateToCommunity = onNavigateToCommunity
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Full screen Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Show danger zones on map
                dangerZones.filter { it.isEnabled }.forEach { zone ->
                    Circle(
                        center = LatLng(zone.latitude, zone.longitude),
                        radius = zone.radiusMeters.toDouble(),
                        fillColor = Color.Red.copy(alpha = 0.2f),
                        strokeColor = Color.Red,
                        strokeWidth = 2f
                    )
                    Marker(
                        state = MarkerState(position = LatLng(zone.latitude, zone.longitude)),
                        title = zone.name,
                        snippet = "Danger Zone"
                    )
                }
            }

            // Top bar overlay
            TopBarOverlay(
                currentUser = currentUser,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )

            // My Location FAB
            FloatingActionButton(
                onClick = {
                    viewModel.refreshLocation()
                    currentLocation?.let {
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(it, 17f),
                                durationMs = 500
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 200.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }

            // SOS Active Overlay
            if (sosState is SOSManager.SOSState.Active) {
                SOSActiveOverlay(
                    onCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.cancelSOS()
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBarOverlay(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onProfileClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser.photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Search bar style title
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "B-Safe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Settings button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onSettingsClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomSheetContent(
    sosState: SOSManager.SOSState,
    contacts: List<com.safeguard.app.data.models.EmergencyContact>,
    settings: UserSettings,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    safetyScore: com.safeguard.app.core.SafetyScoreManager.SafetyScore,
    activeJourney: com.safeguard.app.core.JourneyMonitor.Journey?,
    onTriggerSOS: () -> Unit,
    onCancelSOS: () -> Unit,
    onCancelCountdown: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNearbyPlaces: () -> Unit,
    onNavigateToLiveLocation: () -> Unit,
    onNavigateToSafeRoute: () -> Unit,
    onNavigateToSafetyScore: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToAIInsights: () -> Unit,
    onNavigateToSafeWalk: () -> Unit,
    onNavigateToCommunity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // SOS Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (currentUser != null) "Hi, ${currentUser.displayName?.split(" ")?.firstOrNull() ?: "User"}" else "Welcome",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (contacts.isNotEmpty()) 
                        "${contacts.size} emergency contact${if (contacts.size > 1) "s" else ""} • Protected"
                    else 
                        "Add contacts to get protected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (contacts.isNotEmpty()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }

            // SOS Button
            SOSButtonCompact(
                sosState = sosState,
                onTriggerSOS = onTriggerSOS,
                onCancelSOS = onCancelSOS,
                onCancelCountdown = onCancelCountdown
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety Score Card (Premium Feature)
        SafetyScoreCard(
            score = safetyScore.totalScore,
            grade = safetyScore.grade,
            onClick = onNavigateToSafetyScore
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Active Journey Card (if journey is active)
        activeJourney?.let { journey ->
            ActiveJourneyCard(
                journey = journey,
                onClick = onNavigateToJourney
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Actions Row
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickActionChip(
                    icon = Icons.Default.Contacts,
                    label = "Contacts",
                    badge = if (contacts.isNotEmpty()) "${contacts.size}" else null,
                    onClick = onNavigateToContacts
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.NearMe,
                    label = "Nearby",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onNavigateToNearbyPlaces
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.ShareLocation,
                    label = "Live",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onNavigateToLiveLocation
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Route,
                    label = "Safe Route",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = onNavigateToSafeRoute
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Journey",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onNavigateToJourney
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Security,
                    label = "Score",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onNavigateToSafetyScore
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Psychology,
                    label = "AI",
                    containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                    onClick = onNavigateToAIInsights
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Accessibility,
                    label = "Safe Walk",
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.2f),
                    onClick = onNavigateToSafeWalk
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Groups,
                    label = "Community",
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    onClick = onNavigateToCommunity
                )
            }
            if (settings.enableFakeCall) {
                item {
                    QuickActionChip(
                        icon = Icons.Default.Phone,
                        label = "Fake Call",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateToFakeCall
                    )
                }
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = onNavigateToHistory
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Dashboard,
                    label = "Dashboard",
                    onClick = onNavigateToDashboard
                )
            }
            item {
                QuickActionChip(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = onNavigateToSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Triggers
        if (settings.enableVolumeButtonTrigger || settings.enableShakeTrigger || 
            settings.enablePowerButtonTrigger || settings.enableWidgetTrigger) {
            ActiveTriggersSection(settings)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Setup prompt if no contacts
        if (contacts.isEmpty()) {
            SetupPromptCard(onAddContact = onNavigateToContacts)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Safety Tips
        SafetyTipsCard()
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SOSButtonCompact(
    sosState: SOSManager.SOSState,
    onTriggerSOS: () -> Unit,
    onCancelSOS: () -> Unit,
    onCancelCountdown: () -> Unit
) {
    val isActive = sosState is SOSManager.SOSState.Active
    val isCountdown = sosState is SOSManager.SOSState.Countdown

    val infiniteTransition = rememberInfiniteTransition(label = "sos")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .size(80.dp)
            .scale(if (isActive) scale else 1f)
            .shadow(8.dp, CircleShape)
            .clickable {
                when (sosState) {
                    is SOSManager.SOSState.Idle -> onTriggerSOS()
                    is SOSManager.SOSState.Countdown -> onCancelCountdown()
                    is SOSManager.SOSState.Active -> onCancelSOS()
                    else -> {}
                }
            },
        shape = CircleShape,
        color = when {
            isActive -> Color(0xFFD32F2F)
            isCountdown -> Color(0xFFFF6D00)
            else -> Color(0xFFE53935)
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (sosState) {
                is SOSManager.SOSState.Countdown -> {
                    Text(
                        text = "${sosState.secondsRemaining}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                is SOSManager.SOSState.Active -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                else -> {
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
                if (badge != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(14.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActiveTriggersSection(settings: UserSettings) {
    val activeTriggers = mutableListOf<Pair<ImageVector, String>>()
    if (settings.enableVolumeButtonTrigger) activeTriggers.add(Icons.Default.VolumeUp to "Volume")
    if (settings.enableShakeTrigger) activeTriggers.add(Icons.Default.Vibration to "Shake")
    if (settings.enablePowerButtonTrigger) activeTriggers.add(Icons.Default.Power to "Power")
    if (settings.enableWidgetTrigger) activeTriggers.add(Icons.Default.Widgets to "Widget")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active SOS Triggers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeTriggers.forEach { (icon, label) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPromptCard(onAddContact: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddContact)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Complete Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Add emergency contacts to activate protection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SafetyTipsCard() {
    val tips = listOf(
        "Share your live location with trusted contacts during travel",
        "Set up danger zones for areas you want to avoid",
        "Enable shake detection for hands-free SOS activation",
        "Schedule check-ins when going to unfamiliar places"
    )
    var currentTip by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            currentTip = (currentTip + 1) % tips.size
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Safety Tip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tips[currentTip],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SOSActiveOverlay(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = alpha))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOS ACTIVE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Emergency contacts are being notified",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                )
            ) {
                Text("TAP TO CANCEL", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SafetyScoreCard(
    score: Int,
    grade: com.safeguard.app.core.SafetyScoreManager.SafetyGrade,
    onClick: () -> Unit
) {
    val scoreColor = Color(grade.color)
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safety Score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${grade.emoji} ${grade.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scoreColor
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveJourneyCard(
    journey: com.safeguard.app.core.JourneyMonitor.Journey,
    onClick: () -> Unit
) {
    val remainingMinutes = ((journey.expectedArrivalTime - System.currentTimeMillis()) / 60000).toInt()
    val isOverdue = remainingMinutes < 0
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOverdue) "Journey Overdue!" else "Journey Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "To: ${journey.destinationName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isOverdue) "Overdue by ${-remainingMinutes} min" else "$remainingMinutes min remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
