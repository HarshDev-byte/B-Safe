package com.safeguard.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.safeguard.app.core.PlacesManager
import com.safeguard.app.core.PlacesManager.SafePlaceType
import com.safeguard.app.data.models.EmergencyContact
import com.safeguard.app.ui.components.LoadingIndicator
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

/**
 * Destination types for safe route navigation
 */
sealed class SafeDestination {
    abstract val displayName: String
    abstract val location: LatLng?
    
    data class NearbyPlace(val place: PlacesManager.NearbyPlace) : SafeDestination() {
        override val displayName: String get() = place.name
        override val location: LatLng get() = place.location
    }
    
    data class Contact(val contact: EmergencyContact, val contactLocation: LatLng?) : SafeDestination() {
        override val displayName: String get() = contact.name
        override val location: LatLng? get() = contactLocation
    }
    
    data class Custom(val name: String, val customLocation: LatLng) : SafeDestination() {
        override val displayName: String get() = name
        override val location: LatLng get() = customLocation
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeRouteScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get PlacesManager from application
    val app = context.applicationContext as com.safeguard.app.SafeGuardApplication
    val placesManager = remember { app.placesManager }
    
    val currentLocation by viewModel.currentLocation.collectAsState()
    val contacts by viewModel.emergencyContacts.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<SafePlaceType?>(SafePlaceType.POLICE_STATION) }
    var nearbyPlaces by remember { mutableStateOf<List<PlacesManager.NearbyPlace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDestination by remember { mutableStateOf<SafeDestination?>(null) }
    var showDestinationSheet by remember { mutableStateOf(false) }
    
    // Camera position
    val defaultLocation = currentLocation ?: LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }
    
    // Load nearby places
    LaunchedEffect(selectedCategory, currentLocation) {
        currentLocation?.let { location ->
            isLoading = true
            val androidLocation = android.location.Location("").apply {
                latitude = location.latitude
                longitude = location.longitude
            }
            
            val result = if (selectedCategory != null) {
                placesManager.getNearbyPlaces(androidLocation, selectedCategory!!)
            } else {
                placesManager.getAllNearbySafetyPlaces(androidLocation)
            }
            
            when (result) {
                is PlacesManager.PlacesResult.Success -> {
                    nearbyPlaces = result.places.take(5) // Show top 5 nearest
                }
                else -> {}
            }
            isLoading = false
        }
    }
    
    // Fit bounds when places load
    LaunchedEffect(nearbyPlaces, currentLocation) {
        if (nearbyPlaces.isNotEmpty() && currentLocation != null) {
            try {
                val bounds = LatLngBounds.builder()
                bounds.include(currentLocation!!)
                nearbyPlaces.forEach { bounds.include(it.location) }
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                )
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Safe Route") },
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
        ) {
            // Category filter
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // Map section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
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
                        myLocationButtonEnabled = false
                    )
                ) {
                    // Nearby place markers
                    nearbyPlaces.forEach { place ->
                        Marker(
                            state = MarkerState(position = place.location),
                            title = place.name,
                            snippet = place.distance?.let { placesManager.formatDistance(it) },
                            onClick = {
                                selectedDestination = SafeDestination.NearbyPlace(place)
                                showDestinationSheet = true
                                true
                            }
                        )
                    }
                    
                    // Draw route line to selected destination
                    selectedDestination?.location?.let { destLocation ->
                        currentLocation?.let { origin ->
                            Polyline(
                                points = listOf(origin, destLocation),
                                color = MaterialTheme.colorScheme.primary,
                                width = 8f
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
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "My location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Loading indicator
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            }
            
            // Destinations list
            DestinationsList(
                nearbyPlaces = nearbyPlaces,
                contacts = contacts,
                placesManager = placesManager,
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    destination.location?.let { loc ->
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(loc, 16f)
                            )
                        }
                    }
                },
                onNavigateClick = { destination ->
                    destination.location?.let { loc ->
                        openNavigation(context, loc, destination.displayName)
                    }
                },
                modifier = Modifier.weight(0.5f)
            )
        }
    }
    
    // Destination detail sheet
    if (showDestinationSheet && selectedDestination != null) {
        ModalBottomSheet(
            onDismissRequest = { showDestinationSheet = false }
        ) {
            DestinationDetailSheet(
                destination = selectedDestination!!,
                currentLocation = currentLocation,
                placesManager = placesManager,
                onNavigate = {
                    selectedDestination?.location?.let { loc ->
                        openNavigation(context, loc, selectedDestination!!.displayName)
                    }
                    showDestinationSheet = false
                },
                onCall = {
                    when (val dest = selectedDestination) {
                        is SafeDestination.NearbyPlace -> {
                            dest.place.phoneNumber?.let { openDialer(context, it) }
                        }
                        is SafeDestination.Contact -> {
                            openDialer(context, dest.contact.phoneNumber)
                        }
                        else -> {}
                    }
                },
                onDismiss = { showDestinationSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: SafePlaceType?,
    onCategorySelected: (SafePlaceType?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == SafePlaceType.POLICE_STATION,
                onClick = { onCategorySelected(SafePlaceType.POLICE_STATION) },
                label = { Text("Police") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalPolice,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == SafePlaceType.HOSPITAL,
                onClick = { onCategorySelected(SafePlaceType.HOSPITAL) },
                label = { Text("Hospital") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == SafePlaceType.FIRE_STATION,
                onClick = { onCategorySelected(SafePlaceType.FIRE_STATION) },
                label = { Text("Fire Station") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == SafePlaceType.PHARMACY,
                onClick = { onCategorySelected(SafePlaceType.PHARMACY) },
                label = { Text("Pharmacy") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DestinationsList(
    nearbyPlaces: List<PlacesManager.NearbyPlace>,
    contacts: List<EmergencyContact>,
    placesManager: PlacesManager,
    selectedDestination: SafeDestination?,
    onDestinationSelected: (SafeDestination) -> Unit,
    onNavigateClick: (SafeDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nearest safe places section
        if (nearbyPlaces.isNotEmpty()) {
            item {
                Text(
                    "Nearest Safe Places",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(nearbyPlaces) { place ->
                val destination = SafeDestination.NearbyPlace(place)
                DestinationCard(
                    icon = place.type.toIcon(),
                    iconColor = place.type.toColor(),
                    title = place.name,
                    subtitle = place.type.toDisplayName(),
                    distance = place.distance?.let { placesManager.formatDistance(it) },
                    isOpen = place.isOpen,
                    isSelected = selectedDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    onNavigateClick = { onNavigateClick(destination) }
                )
            }
        }
        
        // Emergency contacts section
        val contactsWithLocation = contacts.filter { it.enableLiveLocation }
        if (contactsWithLocation.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Trusted Contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(contactsWithLocation) { contact ->
                // Note: In real app, you'd fetch contact's shared location
                val destination = SafeDestination.Contact(contact, null)
                ContactDestinationCard(
                    contact = contact,
                    isSelected = selectedDestination is SafeDestination.Contact && 
                                (selectedDestination as SafeDestination.Contact).contact.id == contact.id,
                    onClick = { onDestinationSelected(destination) },
                    onCallClick = { openDialer(context, contact.phoneNumber) }
                )
            }
        }
        
        // Quick actions
        item {
            Spacer(modifier = Modifier.height(8.dp))
            QuickNavigationActions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    distance: String?,
    isOpen: Boolean?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, $subtitle, ${distance ?: "unknown distance"}" },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder() 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    isOpen?.let { open ->
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (open) "Open" else "Closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (open) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                }
            }
            
            // Distance and navigate button
            Column(horizontalAlignment = Alignment.End) {
                distance?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Directions,
                        contentDescription = "Navigate to $title",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDestinationCard(
    contact: EmergencyContact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contact.relationship.ifEmpty { "Emergency Contact" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Call button
            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickNavigationActions() {
    val context = LocalContext.current
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.LocalPolice,
                    label = "Nearest\nPolice",
                    color = Color(0xFF1565C0),
                    onClick = {
                        searchInMaps(context, "police station near me")
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.LocalHospital,
                    label = "Nearest\nHospital",
                    color = Color(0xFFE53935),
                    onClick = {
                        searchInMaps(context, "hospital near me")
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.Home,
                    label = "Navigate\nHome",
                    color = Color(0xFF2E7D32),
                    onClick = {
                        searchInMaps(context, "home")
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DestinationDetailSheet(
    destination: SafeDestination,
    currentLocation: LatLng?,
    placesManager: PlacesManager,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, color) = when (destination) {
                is SafeDestination.NearbyPlace -> destination.place.type.toIcon() to destination.place.type.toColor()
                is SafeDestination.Contact -> Icons.Default.Person to MaterialTheme.colorScheme.tertiary
                is SafeDestination.Custom -> Icons.Default.Place to MaterialTheme.colorScheme.primary
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                when (destination) {
                    is SafeDestination.NearbyPlace -> {
                        Text(
                            text = destination.place.type.toDisplayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is SafeDestination.Contact -> {
                        Text(
                            text = destination.contact.relationship.ifEmpty { "Emergency Contact" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Distance and ETA
        currentLocation?.let { origin ->
            destination.location?.let { dest ->
                val distance = placesManager.calculateDistance(
                    android.location.Location("").apply {
                        latitude = origin.latitude
                        longitude = origin.longitude
                    },
                    dest
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoColumn(
                        icon = Icons.Default.NearMe,
                        label = "Distance",
                        value = placesManager.formatDistance(distance)
                    )
                    InfoColumn(
                        icon = Icons.Default.Schedule,
                        label = "Est. Time",
                        value = estimateTime(distance)
                    )
                    InfoColumn(
                        icon = Icons.Default.DirectionsCar,
                        label = "By Car",
                        value = estimateCarTime(distance)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Place details
        when (destination) {
            is SafeDestination.NearbyPlace -> {
                val place = destination.place
                
                // Rating
                place.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        place.userRatingsTotal?.let {
                            Text(
                                text = " ($it reviews)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Open status
                place.isOpen?.let { isOpen ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOpen) Color(0xFF4CAF50) else Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOpen) "Open now" else "Closed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOpen) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Address
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
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
            }
            is SafeDestination.Contact -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destination.contact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigate,
                modifier = Modifier.weight(1f),
                enabled = destination.location != null
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navigate")
            }
            
            OutlinedButton(
                onClick = onCall,
                modifier = Modifier.weight(1f),
                enabled = when (destination) {
                    is SafeDestination.NearbyPlace -> destination.place.phoneNumber != null
                    is SafeDestination.Contact -> true
                    else -> false
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoColumn(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions
private fun openNavigation(context: android.content.Context, destination: LatLng, name: String) {
    val uri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}&mode=d")
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

private fun searchInMaps(context: android.content.Context, query: String) {
    val uri = Uri.parse("geo:0,0?q=$query")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

private fun estimateTime(distanceMeters: Float): String {
    // Assuming walking speed of 5 km/h
    val hours = distanceMeters / 5000f
    val minutes = (hours * 60).toInt()
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun estimateCarTime(distanceMeters: Float): String {
    // Assuming average city driving speed of 30 km/h
    val hours = distanceMeters / 30000f
    val minutes = (hours * 60).toInt()
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
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
