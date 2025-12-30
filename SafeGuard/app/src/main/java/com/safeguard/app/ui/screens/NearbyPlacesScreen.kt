package com.safeguard.app.ui.screens

import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.safeguard.app.core.PlacesManager
import com.safeguard.app.core.PlacesManager.SafePlaceType
import com.safeguard.app.ui.components.*
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPlacesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get PlacesManager from application
    val app = context.applicationContext as com.safeguard.app.SafeGuardApplication
    val placesManager = remember { app.placesManager }
    
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var selectedType by remember { mutableStateOf<SafePlaceType?>(null) }
    var places by remember { mutableStateOf<List<PlacesManager.NearbyPlace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlace by remember { mutableStateOf<PlacesManager.NearbyPlace?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Get user location on launch
    LaunchedEffect(Unit) {
        viewModel.getCurrentLocation()?.let { location ->
            userLocation = location
            loadPlaces(placesManager, location, selectedType) { result ->
                when (result) {
                    is PlacesManager.PlacesResult.Success -> {
                        places = result.places
                        isLoading = false
                    }
                    is PlacesManager.PlacesResult.Error -> {
                        errorMessage = result.message
                        isLoading = false
                    }
                    PlacesManager.PlacesResult.Loading -> isLoading = true
                }
            }
        } ?: run {
            errorMessage = "Unable to get your location"
            isLoading = false
        }
    }

    // Reload when filter changes
    LaunchedEffect(selectedType) {
        userLocation?.let { location ->
            isLoading = true
            loadPlaces(placesManager, location, selectedType) { result ->
                when (result) {
                    is PlacesManager.PlacesResult.Success -> {
                        places = result.places
                        isLoading = false
                    }
                    is PlacesManager.PlacesResult.Error -> {
                        errorMessage = result.message
                        isLoading = false
                    }
                    PlacesManager.PlacesResult.Loading -> isLoading = true
                }
            }
        }
    }

    // Filter places by search query
    val filteredPlaces = remember(places, searchQuery) {
        if (searchQuery.isBlank()) places
        else places.filter { 
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Safe Places") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            MapSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* Search handled by filter */ },
                placeholder = "Search nearby places",
                onVoiceSearchClick = { /* Voice search */ },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Category filter chips
            PlaceTypeFilterChips(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorState(
                        message = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                            isLoading = true
                            userLocation?.let { location ->
                                scope.launch {
                                    loadPlaces(placesManager, location, selectedType) { result ->
                                        when (result) {
                                            is PlacesManager.PlacesResult.Success -> {
                                                places = result.places
                                                isLoading = false
                                            }
                                            is PlacesManager.PlacesResult.Error -> {
                                                errorMessage = result.message
                                                isLoading = false
                                            }
                                            PlacesManager.PlacesResult.Loading -> {}
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                showMap -> {
                    // Map view
                    Box(modifier = Modifier.fillMaxSize()) {
                        SafeGuardMapView(
                            userLocation = userLocation,
                            markers = filteredPlaces.map { place ->
                                SafetyMarker(
                                    id = place.id,
                                    position = place.location,
                                    title = place.name,
                                    snippet = place.address,
                                    type = place.type.toMarkerType()
                                )
                            },
                            onMarkerClick = { marker ->
                                selectedPlace = filteredPlaces.find { it.id == marker.id }
                            },
                            bottomContent = {
                                selectedPlace?.let { place ->
                                    PlaceDetailsCard(
                                        place = place.toPlaceInfo(placesManager),
                                        onDirectionsClick = {
                                            openDirections(context, place.location)
                                        },
                                        onCallClick = {
                                            place.phoneNumber?.let { phone ->
                                                openDialer(context, phone)
                                            }
                                        },
                                        onShareClick = {
                                            shareLocation(context, place)
                                        },
                                        onDismiss = { selectedPlace = null }
                                    )
                                }
                            }
                        )
                    }
                }
                filteredPlaces.isEmpty() -> {
                    EmptyState(
                        message = if (searchQuery.isNotBlank()) 
                            "No places found for \"$searchQuery\"" 
                        else 
                            "No nearby places found"
                    )
                }
                else -> {
                    // List view
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPlaces, key = { it.id }) { place ->
                            PlaceListItem(
                                place = place,
                                placesManager = placesManager,
                                onClick = { selectedPlace = place },
                                onDirectionsClick = { openDirections(context, place.location) },
                                onCallClick = { 
                                    place.phoneNumber?.let { openDialer(context, it) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom sheet for place details (list view)
        if (selectedPlace != null && !showMap) {
            ModalBottomSheet(
                onDismissRequest = { selectedPlace = null },
                sheetState = rememberModalBottomSheetState()
            ) {
                selectedPlace?.let { place ->
                    PlaceDetailBottomSheet(
                        place = place,
                        placesManager = placesManager,
                        onDirectionsClick = {
                            openDirections(context, place.location)
                        },
                        onCallClick = {
                            place.phoneNumber?.let { openDialer(context, it) }
                        },
                        onShareClick = {
                            shareLocation(context, place)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceTypeFilterChips(
    selectedType: SafePlaceType?,
    onTypeSelected: (SafePlaceType?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("All") },
                leadingIcon = if (selectedType == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
        
        item {
            FilterChip(
                selected = selectedType == SafePlaceType.POLICE_STATION,
                onClick = { onTypeSelected(SafePlaceType.POLICE_STATION) },
                label = { Text("Police") },
                leadingIcon = {
                    Icon(Icons.Default.LocalPolice, contentDescription = null, Modifier.size(18.dp))
                }
            )
        }
        
        item {
            FilterChip(
                selected = selectedType == SafePlaceType.HOSPITAL,
                onClick = { onTypeSelected(SafePlaceType.HOSPITAL) },
                label = { Text("Hospital") },
                leadingIcon = {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, Modifier.size(18.dp))
                }
            )
        }
        
        item {
            FilterChip(
                selected = selectedType == SafePlaceType.FIRE_STATION,
                onClick = { onTypeSelected(SafePlaceType.FIRE_STATION) },
                label = { Text("Fire Station") },
                leadingIcon = {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, Modifier.size(18.dp))
                }
            )
        }
        
        item {
            FilterChip(
                selected = selectedType == SafePlaceType.PHARMACY,
                onClick = { onTypeSelected(SafePlaceType.PHARMACY) },
                label = { Text("Pharmacy") },
                leadingIcon = {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null, Modifier.size(18.dp))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceListItem(
    place: PlacesManager.NearbyPlace,
    placesManager: PlacesManager,
    onClick: () -> Unit,
    onDirectionsClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${place.name}, ${place.type.toDisplayName()}" },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(place.type.toColor().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = place.type.toIcon(),
                    contentDescription = null,
                    tint = place.type.toColor(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Rating
                place.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = rating.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFFC107)
                        )
                        place.userRatingsTotal?.let {
                            Text(
                                text = " ($it)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Open status and distance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    place.isOpen?.let { isOpen ->
                        Text(
                            text = if (isOpen) "Open" else "Closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFE53935)
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    place.distance?.let { distance ->
                        Text(
                            text = placesManager.formatDistance(distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDirectionsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Get directions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (place.phoneNumber != null) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceDetailBottomSheet(
    place: PlacesManager.NearbyPlace,
    placesManager: PlacesManager,
    onDirectionsClick: () -> Unit,
    onCallClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(place.type.toColor().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = place.type.toIcon(),
                    contentDescription = null,
                    tint = place.type.toColor(),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = place.type.toDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rating
        place.rating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < rating.toInt()) Icons.Default.Star 
                            else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFFC107)
                    )
                }
                place.userRatingsTotal?.let {
                    Text(
                        text = " ($it reviews)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status and distance
        Row(verticalAlignment = Alignment.CenterVertically) {
            place.isOpen?.let { isOpen ->
                Surface(
                    color = if (isOpen) Color(0xFF2E7D32).copy(alpha = 0.1f) 
                        else Color(0xFFE53935).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isOpen) "Open now" else "Closed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFE53935),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            place.distance?.let { distance ->
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = placesManager.formatDistance(distance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Address
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Phone
        place.phoneNumber?.let { phone ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDirectionsClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Directions")
            }
            
            OutlinedButton(
                onClick = onCallClick,
                modifier = Modifier.weight(1f),
                enabled = place.phoneNumber != null
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call")
            }
            
            OutlinedButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = null)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions
private suspend fun loadPlaces(
    placesManager: PlacesManager,
    location: Location,
    type: SafePlaceType?,
    onResult: (PlacesManager.PlacesResult) -> Unit
) {
    val result = if (type != null) {
        placesManager.getNearbyPlaces(location, type)
    } else {
        placesManager.getAllNearbySafetyPlaces(location)
    }
    onResult(result)
}

private fun openDirections(context: android.content.Context, destination: LatLng) {
    val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

private fun openDialer(context: android.content.Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    context.startActivity(intent)
}

private fun shareLocation(context: android.content.Context, place: PlacesManager.NearbyPlace) {
    val shareText = """
        ${place.name}
        ${place.address}
        
        https://www.google.com/maps/search/?api=1&query=${place.location.latitude},${place.location.longitude}
    """.trimIndent()
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share location"))
}

// Extension functions
private fun SafePlaceType.toIcon(): ImageVector = when (this) {
    SafePlaceType.POLICE_STATION -> Icons.Default.LocalPolice
    SafePlaceType.HOSPITAL -> Icons.Default.LocalHospital
    SafePlaceType.FIRE_STATION -> Icons.Default.LocalFireDepartment
    SafePlaceType.PHARMACY -> Icons.Default.LocalPharmacy
    SafePlaceType.OTHER -> Icons.Default.Place
}

private fun SafePlaceType.toColor(): Color = when (this) {
    SafePlaceType.POLICE_STATION -> Color(0xFF1565C0)
    SafePlaceType.HOSPITAL -> Color(0xFFE53935)
    SafePlaceType.FIRE_STATION -> Color(0xFFFF5722)
    SafePlaceType.PHARMACY -> Color(0xFF2E7D32)
    SafePlaceType.OTHER -> Color(0xFF757575)
}

private fun SafePlaceType.toDisplayName(): String = when (this) {
    SafePlaceType.POLICE_STATION -> "Police Station"
    SafePlaceType.HOSPITAL -> "Hospital"
    SafePlaceType.FIRE_STATION -> "Fire Station"
    SafePlaceType.PHARMACY -> "Pharmacy"
    SafePlaceType.OTHER -> "Place"
}

private fun SafePlaceType.toMarkerType(): SafetyMarkerType = when (this) {
    SafePlaceType.POLICE_STATION -> SafetyMarkerType.POLICE_STATION
    SafePlaceType.HOSPITAL -> SafetyMarkerType.HOSPITAL
    SafePlaceType.FIRE_STATION -> SafetyMarkerType.FIRE_STATION
    SafePlaceType.PHARMACY -> SafetyMarkerType.SAFE_PLACE
    SafePlaceType.OTHER -> SafetyMarkerType.CUSTOM
}

private fun PlacesManager.NearbyPlace.toPlaceInfo(placesManager: PlacesManager): PlaceInfo {
    return PlaceInfo(
        id = id,
        name = name,
        address = address,
        type = when (type) {
            SafePlaceType.POLICE_STATION -> PlaceType.POLICE_STATION
            SafePlaceType.HOSPITAL -> PlaceType.HOSPITAL
            SafePlaceType.FIRE_STATION -> PlaceType.FIRE_STATION
            SafePlaceType.PHARMACY -> PlaceType.PHARMACY
            SafePlaceType.OTHER -> PlaceType.OTHER
        },
        distance = distance?.let { placesManager.formatDistance(it) },
        rating = rating,
        reviewCount = userRatingsTotal,
        isOpen = isOpen,
        phoneNumber = phoneNumber
    )
}
