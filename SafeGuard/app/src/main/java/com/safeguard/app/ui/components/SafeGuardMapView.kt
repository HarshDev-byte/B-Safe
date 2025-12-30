package com.safeguard.app.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

/**
 * Marker types for different safety-related locations
 */
enum class SafetyMarkerType {
    USER_LOCATION,
    EMERGENCY_CONTACT,
    DANGER_ZONE,
    SAFE_PLACE,
    POLICE_STATION,
    HOSPITAL,
    FIRE_STATION,
    CUSTOM
}

/**
 * Data class representing a marker on the map
 */
data class SafetyMarker(
    val id: String,
    val position: LatLng,
    val title: String,
    val snippet: String? = null,
    val type: SafetyMarkerType = SafetyMarkerType.CUSTOM,
    val isSelected: Boolean = false
)

/**
 * Data class representing a danger zone circle
 */
data class DangerZoneCircle(
    val id: String,
    val center: LatLng,
    val radiusMeters: Double,
    val name: String,
    val severity: DangerSeverity = DangerSeverity.MEDIUM
)

enum class DangerSeverity {
    LOW, MEDIUM, HIGH
}

/**
 * Reusable SafeGuard Map View component
 */
@Composable
fun SafeGuardMapView(
    modifier: Modifier = Modifier,
    userLocation: Location? = null,
    markers: List<SafetyMarker> = emptyList(),
    dangerZones: List<DangerZoneCircle> = emptyList(),
    showUserLocation: Boolean = true,
    showZoomControls: Boolean = true,
    showMyLocationButton: Boolean = true,
    showCompass: Boolean = true,
    initialZoom: Float = 15f,
    onMarkerClick: (SafetyMarker) -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onCameraMove: (CameraPosition) -> Unit = {},
    bottomContent: @Composable () -> Unit = {}
) {
    val defaultLocation = LatLng(28.6139, 77.2090) // Default to Delhi
    
    val initialPosition = userLocation?.let {
        LatLng(it.latitude, it.longitude)
    } ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom)
    }

    // Update camera when user location changes
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude),
                    initialZoom
                )
            )
        }
    }

    // Track camera movement
    LaunchedEffect(cameraPositionState.position) {
        onCameraMove(cameraPositionState.position)
    }

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = showUserLocation && userLocation != null,
                mapType = MapType.NORMAL,
                isTrafficEnabled = false
            )
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false, // We'll use custom controls
                myLocationButtonEnabled = false, // We'll use custom button
                compassEnabled = showCompass,
                mapToolbarEnabled = false
            )
        )
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = onMapClick,
            onMapLongClick = onMapLongClick
        ) {
            // Draw danger zones as circles
            dangerZones.forEach { zone ->
                Circle(
                    center = zone.center,
                    radius = zone.radiusMeters,
                    fillColor = zone.severity.toFillColor(),
                    strokeColor = zone.severity.toStrokeColor(),
                    strokeWidth = 3f
                )
                
                // Danger zone marker
                Marker(
                    state = MarkerState(position = zone.center),
                    title = zone.name,
                    snippet = "Danger Zone - ${zone.severity.name}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

            // Draw markers
            markers.forEach { marker ->
                Marker(
                    state = MarkerState(position = marker.position),
                    title = marker.title,
                    snippet = marker.snippet,
                    icon = marker.type.toMarkerIcon(),
                    onClick = {
                        onMarkerClick(marker)
                        true
                    }
                )
            }

            // User location marker (custom blue dot if not using default)
            if (showUserLocation && userLocation != null && !mapProperties.isMyLocationEnabled) {
                Marker(
                    state = MarkerState(
                        position = LatLng(userLocation.latitude, userLocation.longitude)
                    ),
                    title = "You are here",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        // Map controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showZoomControls) {
                MapControlButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Zoom in",
                    onClick = {
                        cameraPositionState.move(CameraUpdateFactory.zoomIn())
                    }
                )
                
                MapControlButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    onClick = {
                        cameraPositionState.move(CameraUpdateFactory.zoomOut())
                    }
                )
            }
        }

        // My location button
        if (showMyLocationButton && userLocation != null) {
            FloatingActionButton(
                onClick = {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(userLocation.latitude, userLocation.longitude),
                            initialZoom
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My location"
                )
            }
        }

        // Bottom content slot (for place details, navigation info, etc.)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            bottomContent()
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Extension functions for marker styling
private fun SafetyMarkerType.toMarkerIcon() = when (this) {
    SafetyMarkerType.USER_LOCATION -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    SafetyMarkerType.EMERGENCY_CONTACT -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    SafetyMarkerType.DANGER_ZONE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    SafetyMarkerType.SAFE_PLACE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    SafetyMarkerType.POLICE_STATION -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
    SafetyMarkerType.HOSPITAL -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)
    SafetyMarkerType.FIRE_STATION -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    SafetyMarkerType.CUSTOM -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
}

private fun DangerSeverity.toFillColor() = when (this) {
    DangerSeverity.LOW -> Color(0x33FFC107) // Yellow with alpha
    DangerSeverity.MEDIUM -> Color(0x33FF9800) // Orange with alpha
    DangerSeverity.HIGH -> Color(0x33F44336) // Red with alpha
}

private fun DangerSeverity.toStrokeColor() = when (this) {
    DangerSeverity.LOW -> Color(0xFFFFC107)
    DangerSeverity.MEDIUM -> Color(0xFFFF9800)
    DangerSeverity.HIGH -> Color(0xFFF44336)
}
