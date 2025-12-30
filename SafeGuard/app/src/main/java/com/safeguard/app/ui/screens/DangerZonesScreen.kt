package com.safeguard.app.ui.screens

import android.location.Location
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safeguard.app.data.models.DangerZone
import com.safeguard.app.ui.components.*
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZonesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val zones by viewModel.dangerZones.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showMap by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedZone by remember { mutableStateOf<DangerZone?>(null) }
    var isSelectingLocation by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val sheetState = rememberModalBottomSheetState()
    
    // Camera position state
    val defaultLocation = currentLocation ?: LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Update camera when location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            if (zones.isEmpty()) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(it, 14f)
                )
            }
        }
    }

    // Fit all zones in view
    LaunchedEffect(zones) {
        if (zones.isNotEmpty()) {
            val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
            zones.forEach { zone ->
                bounds.include(LatLng(zone.latitude, zone.longitude))
            }
            currentLocation?.let { bounds.include(it) }
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                )
            } catch (e: Exception) {
                // Fallback if bounds are too small
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danger Zones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            imageVector = if (showMap) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMap) "Show list" else "Show map"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showMap) {
                // Map View
                MapContent(
                    zones = zones,
                    currentLocation = currentLocation,
                    cameraPositionState = cameraPositionState,
                    isSelectingLocation = isSelectingLocation,
                    selectedLocation = selectedLocation,
                    onMapClick = { latLng ->
                        if (isSelectingLocation) {
                            selectedLocation = latLng
                        }
                    },
                    onMapLongClick = { latLng ->
                        if (!isSelectingLocation) {
                            selectedLocation = latLng
                            showAddSheet = true
                        }
                    },
                    onZoneClick = { zone ->
                        selectedZone = zone
                    }
                )

                // Top controls overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    // Enable/Disable card
                    DangerZoneToggleCard(
                        enabled = settings.enableDangerZoneAlerts,
                        onToggle = {
                            viewModel.updateUserSettings(
                                settings.copy(enableDangerZoneAlerts = !settings.enableDangerZoneAlerts)
                            )
                        }
                    )
                }

                // Location selection mode indicator
                AnimatedVisibility(
                    visible = isSelectingLocation,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 80.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tap on map to select location",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // My location button
                FloatingActionButton(
                    onClick = {
                        currentLocation?.let {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(it, 15f)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "My location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Add zone FAB
                if (settings.enableDangerZoneAlerts) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (isSelectingLocation) {
                                selectedLocation?.let {
                                    showAddSheet = true
                                }
                                isSelectingLocation = false
                            } else {
                                isSelectingLocation = true
                                selectedLocation = cameraPositionState.position.target
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        containerColor = if (isSelectingLocation && selectedLocation != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isSelectingLocation && selectedLocation != null)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = if (isSelectingLocation) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isSelectingLocation) "Confirm Location" else "Add Danger Zone"
                        )
                    }
                }

                // Cancel selection button
                if (isSelectingLocation) {
                    FloatingActionButton(
                        onClick = {
                            isSelectingLocation = false
                            selectedLocation = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // List View
                ListContent(
                    zones = zones,
                    enabled = settings.enableDangerZoneAlerts,
                    onToggleEnabled = {
                        viewModel.updateUserSettings(
                            settings.copy(enableDangerZoneAlerts = !settings.enableDangerZoneAlerts)
                        )
                    },
                    onZoneClick = { zone ->
                        selectedZone = zone
                        showMap = true
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(zone.latitude, zone.longitude),
                                    16f
                                )
                            )
                        }
                    },
                    onToggleZone = { zone ->
                        viewModel.updateDangerZone(zone.copy(isEnabled = !zone.isEnabled))
                    },
                    onDeleteZone = { zone ->
                        viewModel.deleteDangerZone(zone)
                    },
                    onAddZone = {
                        showMap = true
                        isSelectingLocation = true
                    }
                )
            }
        }
    }

    // Zone detail bottom sheet
    if (selectedZone != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedZone = null },
            sheetState = sheetState
        ) {
            ZoneDetailSheet(
                zone = selectedZone!!,
                onToggle = {
                    viewModel.updateDangerZone(selectedZone!!.copy(isEnabled = !selectedZone!!.isEnabled))
                    selectedZone = selectedZone!!.copy(isEnabled = !selectedZone!!.isEnabled)
                },
                onDelete = {
                    viewModel.deleteDangerZone(selectedZone!!)
                    selectedZone = null
                },
                onViewOnMap = {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(selectedZone!!.latitude, selectedZone!!.longitude),
                                16f
                            )
                        )
                    }
                    selectedZone = null
                }
            )
        }
    }

    // Add zone bottom sheet
    if (showAddSheet && selectedLocation != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showAddSheet = false
                selectedLocation = null
                isSelectingLocation = false
            },
            sheetState = sheetState
        ) {
            AddZoneSheet(
                location = selectedLocation!!,
                onAdd = { name, radius ->
                    viewModel.addDangerZone(
                        DangerZone(
                            name = name,
                            latitude = selectedLocation!!.latitude,
                            longitude = selectedLocation!!.longitude,
                            radiusMeters = radius
                        )
                    )
                    showAddSheet = false
                    selectedLocation = null
                    isSelectingLocation = false
                },
                onCancel = {
                    showAddSheet = false
                    selectedLocation = null
                    isSelectingLocation = false
                }
            )
        }
    }
}

@Composable
private fun MapContent(
    zones: List<DangerZone>,
    currentLocation: LatLng?,
    cameraPositionState: CameraPositionState,
    isSelectingLocation: Boolean,
    selectedLocation: LatLng?,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onZoneClick: (DangerZone) -> Unit
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = currentLocation != null,
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true
        ),
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick
    ) {
        // Draw danger zones
        zones.forEach { zone ->
            val zoneColor = if (zone.isEnabled) Color(0xFFE53935) else Color(0xFF9E9E9E)
            
            Circle(
                center = LatLng(zone.latitude, zone.longitude),
                radius = zone.radiusMeters.toDouble(),
                fillColor = zoneColor.copy(alpha = 0.2f),
                strokeColor = zoneColor,
                strokeWidth = 3f
            )
            
            Marker(
                state = MarkerState(position = LatLng(zone.latitude, zone.longitude)),
                title = zone.name,
                snippet = "Radius: ${zone.radiusMeters.toInt()}m",
                onClick = {
                    onZoneClick(zone)
                    true
                }
            )
        }

        // Selection marker
        if (isSelectingLocation && selectedLocation != null) {
            Marker(
                state = MarkerState(position = selectedLocation),
                title = "New Danger Zone",
                snippet = "Tap confirm to add"
            )
            Circle(
                center = selectedLocation,
                radius = 500.0, // Default preview radius
                fillColor = Color(0xFFFF9800).copy(alpha = 0.3f),
                strokeColor = Color(0xFFFF9800),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun DangerZoneToggleCard(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (enabled) "Alerts enabled" else "Alerts disabled",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    zones: List<DangerZone>,
    enabled: Boolean,
    onToggleEnabled: () -> Unit,
    onZoneClick: (DangerZone) -> Unit,
    onToggleZone: (DangerZone) -> Unit,
    onDeleteZone: (DangerZone) -> Unit,
    onAddZone: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle card
        item {
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
                                if (enabled) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Danger Zone Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Get notified when entering marked areas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggleEnabled() }
                    )
                }
            }
        }

        if (!enabled) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enable alerts to manage danger zones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (zones.isEmpty()) {
            item {
                Card(
                    onClick = onAddZone,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No danger zones yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap to add your first danger zone on the map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "${zones.size} danger zone${if (zones.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            items(zones, key = { it.id }) { zone ->
                ZoneListItem(
                    zone = zone,
                    onClick = { onZoneClick(zone) },
                    onToggle = { onToggleZone(zone) },
                    onDelete = { onDeleteZone(zone) }
                )
            }
            
            item {
                OutlinedButton(
                    onClick = onAddZone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Danger Zone")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneListItem(
    zone: DangerZone,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.semantics { 
            contentDescription = "Danger zone ${zone.name}, radius ${zone.radiusMeters.toInt()} meters"
        }
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
                    .background(
                        if (zone.isEnabled) Color(0xFFE53935).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (zone.isEnabled) Color(0xFFE53935)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Radius: ${zone.radiusMeters.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${String.format("%.4f", zone.latitude)}, ${String.format("%.4f", zone.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete Zone") },
            text = { Text("Remove \"${zone.name}\" from danger zones?") },
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
private fun ZoneDetailSheet(
    zone: DangerZone,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onViewOnMap: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (zone.isEnabled) Color(0xFFE53935).copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (zone.isEnabled) Color(0xFFE53935)
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    zone.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (zone.isEnabled) Color(0xFFE53935).copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (zone.isEnabled) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (zone.isEnabled) Color(0xFFE53935)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info rows
        InfoRow(
            icon = Icons.Default.RadioButtonChecked,
            label = "Radius",
            value = "${zone.radiusMeters.toInt()} meters"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        InfoRow(
            icon = Icons.Default.LocationOn,
            label = "Location",
            value = "${String.format("%.6f", zone.latitude)}, ${String.format("%.6f", zone.longitude)}"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle
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
                Icon(
                    imageVector = if (zone.isEnabled) Icons.Default.NotificationsActive 
                                  else Icons.Default.NotificationsOff,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Alert when entering zone",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewOnMap,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on Map")
            }
            
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Zone") },
            text = { Text("Remove \"${zone.name}\" from danger zones?") },
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
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddZoneSheet(
    location: LatLng,
    onAdd: (name: String, radius: Float) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(500f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddLocationAlt,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Add Danger Zone",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Mark an area to receive alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location info
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
                Text(
                    "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name input
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Zone Name") },
            placeholder = { Text("e.g., Dark alley, Unsafe area") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Label, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Radius slider
        Text(
            "Alert Radius: ${radius.toInt()} meters",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = radius,
            onValueChange = { radius = it },
            valueRange = 100f..2000f,
            steps = 18,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "100m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "2km",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = { onAdd(name, radius) },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Zone")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
