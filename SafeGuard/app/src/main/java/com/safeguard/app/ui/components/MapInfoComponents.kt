package com.safeguard.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Data class for place information
 */
data class PlaceInfo(
    val id: String,
    val name: String,
    val address: String,
    val type: PlaceType,
    val distance: String? = null,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val isOpen: Boolean? = null,
    val openingHours: String? = null,
    val phoneNumber: String? = null
)

enum class PlaceType {
    POLICE_STATION,
    HOSPITAL,
    FIRE_STATION,
    PHARMACY,
    SAFE_PLACE,
    EMERGENCY_CONTACT,
    OTHER
}

/**
 * Place details card shown at bottom of map (like Google Maps)
 */
@Composable
fun PlaceDetailsCard(
    place: PlaceInfo,
    modifier: Modifier = Modifier,
    onDirectionsClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics { contentDescription = "Place details for ${place.name}" },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with name and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Rating and reviews
                    if (place.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = place.rating.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            RatingStars(rating = place.rating)
                            place.reviewCount?.let {
                                Text(
                                    text = " ($it)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Type and distance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = place.type.toIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = place.type.toColor()
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = place.type.toDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                place.distance?.let {
                    Text(
                        text = " • $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Opening status
            place.isOpen?.let { isOpen ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isOpen) "Open" else "Closed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                    place.openingHours?.let {
                        Text(
                            text = " • $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Address
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDirectionsClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions")
                }

                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f),
                    enabled = place.phoneNumber != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call")
                }

                OutlinedButton(
                    onClick = onShareClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share location"
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingStars(rating: Float) {
    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
        repeat(5) { index ->
            val starRating = index + 1
            Icon(
                imageVector = when {
                    rating >= starRating -> Icons.Default.Star
                    rating >= starRating - 0.5f -> Icons.Default.StarHalf
                    else -> Icons.Outlined.StarOutline
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFFFFC107)
            )
        }
    }
}

/**
 * Navigation info panel shown during active navigation
 */
@Composable
fun NavigationInfoPanel(
    instruction: String,
    distance: String,
    duration: String,
    nextTurn: String? = null,
    modifier: Modifier = Modifier,
    onEndNavigation: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Top instruction bar (green like Google Maps)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1B5E20),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TurnRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    nextTurn?.let {
                        Text(
                            text = "Then $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Voice button
                IconButton(onClick = { /* Toggle voice */ }) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Voice guidance",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom info bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route icon
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Duration and distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // End navigation button
                IconButton(
                    onClick = onEndNavigation,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End navigation",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Quick action chips for map (like Google Maps categories)
 */
@Composable
fun MapQuickActions(
    modifier: Modifier = Modifier,
    onPoliceClick: () -> Unit = {},
    onHospitalClick: () -> Unit = {},
    onPharmacyClick: () -> Unit = {},
    onSafePlaceClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionChip(
            icon = Icons.Default.LocalPolice,
            label = "Police",
            onClick = onPoliceClick
        )
        QuickActionChip(
            icon = Icons.Default.LocalHospital,
            label = "Hospital",
            onClick = onHospitalClick
        )
        QuickActionChip(
            icon = Icons.Default.LocalPharmacy,
            label = "Pharmacy",
            onClick = onPharmacyClick
        )
        QuickActionChip(
            icon = Icons.Default.Shield,
            label = "Safe",
            onClick = onSafePlaceClick
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = AssistChipDefaults.assistChipElevation(elevation = 2.dp)
    )
}

// Extension functions
private fun PlaceType.toIcon() = when (this) {
    PlaceType.POLICE_STATION -> Icons.Default.LocalPolice
    PlaceType.HOSPITAL -> Icons.Default.LocalHospital
    PlaceType.FIRE_STATION -> Icons.Default.LocalFireDepartment
    PlaceType.PHARMACY -> Icons.Default.LocalPharmacy
    PlaceType.SAFE_PLACE -> Icons.Default.Shield
    PlaceType.EMERGENCY_CONTACT -> Icons.Default.Person
    PlaceType.OTHER -> Icons.Default.Place
}

private fun PlaceType.toColor() = when (this) {
    PlaceType.POLICE_STATION -> Color(0xFF1565C0)
    PlaceType.HOSPITAL -> Color(0xFFE53935)
    PlaceType.FIRE_STATION -> Color(0xFFFF5722)
    PlaceType.PHARMACY -> Color(0xFF2E7D32)
    PlaceType.SAFE_PLACE -> Color(0xFF2E7D32)
    PlaceType.EMERGENCY_CONTACT -> Color(0xFF7B1FA2)
    PlaceType.OTHER -> Color(0xFF757575)
}

private fun PlaceType.toDisplayName() = when (this) {
    PlaceType.POLICE_STATION -> "Police Station"
    PlaceType.HOSPITAL -> "Hospital"
    PlaceType.FIRE_STATION -> "Fire Station"
    PlaceType.PHARMACY -> "Pharmacy"
    PlaceType.SAFE_PLACE -> "Safe Place"
    PlaceType.EMERGENCY_CONTACT -> "Emergency Contact"
    PlaceType.OTHER -> "Place"
}
