package com.safeguard.app.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeguard.app.core.SafetyAnalytics
import com.safeguard.app.ui.components.SafetyScoreIndicator
import com.safeguard.app.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Safety Dashboard - Award-winning analytics and insights screen
 * Provides actionable safety intelligence based on user's history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyDashboardScreen(
    viewModel: MainViewModel,
    insights: SafetyAnalytics.SafetyInsights?,
    onNavigateBack: () -> Unit
) {
    val events by viewModel.sosEvents.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Safety Dashboard")
                        Text(
                            "Your personal safety insights",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Safety Score Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SafetyScoreIndicator(
                        score = insights?.safetyScore ?: 100,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Streak Card
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${insights?.streakDays ?: 0}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Day Streak",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Without incidents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Stats
            item {
                QuickStatsRow(insights)
            }

            // Recommendations
            if (insights?.recommendations?.isNotEmpty() == true) {
                item {
                    Text(
                        text = "Recommendations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(insights.recommendations) { recommendation ->
                    RecommendationCard(recommendation)
                }
            }

            // Peak Times Analysis
            if (insights != null) {
                item {
                    PeakTimesCard(insights)
                }
            }

            // Location Hotspots
            if (insights?.locationHotspots?.isNotEmpty() == true) {
                item {
                    LocationHotspotsCard(insights.locationHotspots)
                }
            }

            // Trigger Analysis
            if (insights?.mostUsedTrigger != null) {
                item {
                    TriggerAnalysisCard(insights)
                }
            }

            // Privacy Notice
            item {
                PrivacyNoticeCard()
            }
        }
    }
}

@Composable
private fun QuickStatsRow(insights: SafetyAnalytics.SafetyInsights?) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickStatChip(
                icon = Icons.Default.Warning,
                value = "${insights?.totalSOSEvents ?: 0}",
                label = "Total Events",
                color = MaterialTheme.colorScheme.error
            )
        }
        item {
            QuickStatChip(
                icon = Icons.Default.CalendarMonth,
                value = "${insights?.eventsThisMonth ?: 0}",
                label = "This Month",
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            QuickStatChip(
                icon = Icons.Default.Speed,
                value = "${(insights?.averageResponseTime ?: 0) / 1000}s",
                label = "Avg Response",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun QuickStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: SafetyAnalytics.SafetyRecommendation) {
    val priorityColor = when (recommendation.priority) {
        SafetyAnalytics.Priority.HIGH -> Color(0xFFD32F2F)
        SafetyAnalytics.Priority.MEDIUM -> Color(0xFFFF6D00)
        SafetyAnalytics.Priority.LOW -> Color(0xFF1976D2)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(priorityColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (recommendation.actionType) {
                        SafetyAnalytics.ActionType.ADD_CONTACT -> Icons.Default.PersonAdd
                        SafetyAnalytics.ActionType.ENABLE_FEATURE -> Icons.Default.ToggleOn
                        SafetyAnalytics.ActionType.REVIEW_SETTINGS -> Icons.Default.Settings
                        SafetyAnalytics.ActionType.ADD_DANGER_ZONE -> Icons.Default.LocationOn
                        SafetyAnalytics.ActionType.SCHEDULE_CHECKIN -> Icons.Default.Schedule
                        SafetyAnalytics.ActionType.SHARE_LOCATION -> Icons.Default.Share
                        SafetyAnalytics.ActionType.UPDATE_EMERGENCY_INFO -> Icons.Default.Edit
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor
                    ) {
                        Text(
                            text = recommendation.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* Navigate to action */ }) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Take action"
                )
            }
        }
    }
}

@Composable
private fun PeakTimesCard(insights: SafetyAnalytics.SafetyInsights) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Peak Risk Times",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hour visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..23).forEach { hour ->
                    val isPeak = hour in insights.peakDangerHours
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(if (isPeak) 40.dp else 20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPeak) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12am", style = MaterialTheme.typography.labelSmall)
                Text("6am", style = MaterialTheme.typography.labelSmall)
                Text("12pm", style = MaterialTheme.typography.labelSmall)
                Text("6pm", style = MaterialTheme.typography.labelSmall)
                Text("12am", style = MaterialTheme.typography.labelSmall)
            }
            
            if (insights.peakDangerHours.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Most incidents occur around ${insights.peakDangerHours.first()}:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocationHotspotsCard(hotspots: List<SafetyAnalytics.LocationHotspot>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Location Hotspots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Areas with multiple incidents",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            hotspots.forEachIndexed { index, hotspot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hotspot.description ?: "Unknown Location",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${hotspot.eventCount} incidents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { /* Add as danger zone */ }) {
                        Text("Add Zone")
                    }
                }
                if (index < hotspots.lastIndex) {
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun TriggerAnalysisCard(insights: SafetyAnalytics.SafetyInsights) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trigger Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.mostUsedTrigger?.let { trigger ->
                Text(
                    text = "Most used: ${trigger.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PrivacyNoticeCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "All analytics are computed locally on your device. No data is shared externally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
