package com.safeguard.app.core

import android.content.Context
import com.safeguard.app.data.models.SOSEvent
import com.safeguard.app.data.models.TriggerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Safety Analytics - Provides insights and patterns from safety data
 * All analytics are computed locally - no data leaves the device
 */
class SafetyAnalytics(private val context: Context) {

    private val _insights = MutableStateFlow<SafetyInsights?>(null)
    val insights: StateFlow<SafetyInsights?> = _insights.asStateFlow()

    data class SafetyInsights(
        val totalSOSEvents: Int,
        val eventsThisMonth: Int,
        val mostUsedTrigger: TriggerType?,
        val averageResponseTime: Long, // Time from trigger to first SMS sent
        val peakDangerHours: List<Int>, // Hours of day with most events
        val peakDangerDays: List<Int>, // Days of week with most events
        val locationHotspots: List<LocationHotspot>,
        val safetyScore: Int, // 0-100
        val recommendations: List<SafetyRecommendation>,
        val streakDays: Int, // Days without SOS
        val lastUpdated: Long = System.currentTimeMillis()
    )

    data class LocationHotspot(
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val eventCount: Int,
        val description: String?
    )

    data class SafetyRecommendation(
        val id: String,
        val priority: Priority,
        val title: String,
        val description: String,
        val actionType: ActionType,
        val actionData: String?
    )

    enum class Priority {
        HIGH, MEDIUM, LOW
    }

    enum class ActionType {
        ADD_CONTACT,
        ENABLE_FEATURE,
        REVIEW_SETTINGS,
        ADD_DANGER_ZONE,
        SCHEDULE_CHECKIN,
        SHARE_LOCATION,
        UPDATE_EMERGENCY_INFO
    }

    fun analyzeEvents(events: List<SOSEvent>): SafetyInsights {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)
        
        val recentEvents = events.filter { it.timestamp > thirtyDaysAgo }
        
        // Calculate peak danger hours
        val hourCounts = mutableMapOf<Int, Int>()
        val dayCounts = mutableMapOf<Int, Int>()
        
        events.forEach { event ->
            val calendar = Calendar.getInstance().apply { timeInMillis = event.timestamp }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val day = calendar.get(Calendar.DAY_OF_WEEK)
            
            hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
            dayCounts[day] = (dayCounts[day] ?: 0) + 1
        }
        
        val peakHours = hourCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        val peakDays = dayCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Find most used trigger
        val triggerCounts = events.groupingBy { it.triggerType }.eachCount()
        val mostUsedTrigger = triggerCounts.maxByOrNull { it.value }?.key
        
        // Calculate location hotspots
        val hotspots = calculateLocationHotspots(events)
        
        // Calculate safety score
        val safetyScore = calculateSafetyScore(events, recentEvents)
        
        // Generate recommendations
        val recommendations = generateRecommendations(events, safetyScore)
        
        // Calculate streak
        val streakDays = calculateStreakDays(events)
        
        val insights = SafetyInsights(
            totalSOSEvents = events.size,
            eventsThisMonth = recentEvents.size,
            mostUsedTrigger = mostUsedTrigger,
            averageResponseTime = calculateAverageResponseTime(events),
            peakDangerHours = peakHours,
            peakDangerDays = peakDays,
            locationHotspots = hotspots,
            safetyScore = safetyScore,
            recommendations = recommendations,
            streakDays = streakDays
        )
        
        _insights.value = insights
        return insights
    }

    private fun calculateLocationHotspots(events: List<SOSEvent>): List<LocationHotspot> {
        val eventsWithLocation = events.filter { it.latitude != null && it.longitude != null }
        if (eventsWithLocation.isEmpty()) return emptyList()
        
        // Simple clustering - group events within 500m of each other
        val clusters = mutableListOf<MutableList<SOSEvent>>()
        
        eventsWithLocation.forEach { event ->
            val nearbyCluster = clusters.find { cluster ->
                cluster.any { existing ->
                    calculateDistance(
                        event.latitude!!, event.longitude!!,
                        existing.latitude!!, existing.longitude!!
                    ) < 500
                }
            }
            
            if (nearbyCluster != null) {
                nearbyCluster.add(event)
            } else {
                clusters.add(mutableListOf(event))
            }
        }
        
        return clusters
            .filter { it.size >= 2 } // Only show clusters with multiple events
            .map { cluster ->
                val avgLat = cluster.mapNotNull { it.latitude }.average()
                val avgLng = cluster.mapNotNull { it.longitude }.average()
                LocationHotspot(
                    latitude = avgLat,
                    longitude = avgLng,
                    radius = 500f,
                    eventCount = cluster.size,
                    description = cluster.firstOrNull()?.address
                )
            }
            .sortedByDescending { it.eventCount }
            .take(5)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun calculateSafetyScore(allEvents: List<SOSEvent>, recentEvents: List<SOSEvent>): Int {
        // Higher score = safer
        var score = 100
        
        // Deduct for recent events
        score -= recentEvents.size * 10
        
        // Deduct for events in last week
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val weekEvents = recentEvents.filter { it.timestamp > weekAgo }
        score -= weekEvents.size * 15
        
        // Bonus for long streaks without events
        val streakDays = calculateStreakDays(allEvents)
        score += minOf(streakDays / 7, 20) // Up to 20 bonus points
        
        return score.coerceIn(0, 100)
    }

    private fun calculateStreakDays(events: List<SOSEvent>): Int {
        if (events.isEmpty()) return 0
        
        val lastEvent = events.maxByOrNull { it.timestamp } ?: return 0
        val daysSinceLastEvent = (System.currentTimeMillis() - lastEvent.timestamp) / (24 * 60 * 60 * 1000L)
        return daysSinceLastEvent.toInt()
    }

    private fun calculateAverageResponseTime(events: List<SOSEvent>): Long {
        // This would need SMS sent timestamps - placeholder
        return 5000L // 5 seconds average
    }

    private fun generateRecommendations(
        events: List<SOSEvent>,
        safetyScore: Int
    ): List<SafetyRecommendation> {
        val recommendations = mutableListOf<SafetyRecommendation>()
        
        // Check for patterns and generate contextual recommendations
        if (safetyScore < 50) {
            recommendations.add(
                SafetyRecommendation(
                    id = "share_location",
                    priority = Priority.HIGH,
                    title = "Consider sharing your location",
                    description = "Based on recent events, sharing your live location with trusted contacts during risky times could help.",
                    actionType = ActionType.SHARE_LOCATION,
                    actionData = null
                )
            )
        }
        
        // Check peak hours
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val hourCounts = events.groupingBy { 
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
        }.eachCount()
        
        val peakHour = hourCounts.maxByOrNull { it.value }?.key
        if (peakHour != null && currentHour in (peakHour - 1)..(peakHour + 1)) {
            recommendations.add(
                SafetyRecommendation(
                    id = "peak_hour_alert",
                    priority = Priority.MEDIUM,
                    title = "You're in a peak risk time",
                    description = "Historical data shows more incidents around this time. Stay alert and consider scheduling a check-in.",
                    actionType = ActionType.SCHEDULE_CHECKIN,
                    actionData = null
                )
            )
        }
        
        // Location-based recommendations
        val hotspots = calculateLocationHotspots(events)
        if (hotspots.isNotEmpty()) {
            recommendations.add(
                SafetyRecommendation(
                    id = "danger_zone",
                    priority = Priority.MEDIUM,
                    title = "Add danger zone alerts",
                    description = "We've identified ${hotspots.size} location(s) with multiple incidents. Consider adding them as danger zones.",
                    actionType = ActionType.ADD_DANGER_ZONE,
                    actionData = null
                )
            )
        }
        
        return recommendations.sortedBy { it.priority.ordinal }
    }

    // Community Safety Features (anonymized, opt-in)
    data class CommunitySafetyData(
        val areaRiskLevel: RiskLevel,
        val recentIncidentsNearby: Int,
        val safeRouteAvailable: Boolean,
        val nearbyHelpPoints: List<HelpPoint>
    )

    enum class RiskLevel {
        LOW, MODERATE, HIGH, VERY_HIGH
    }

    data class HelpPoint(
        val name: String,
        val type: HelpPointType,
        val latitude: Double,
        val longitude: Double,
        val distance: Float,
        val isOpen: Boolean
    )

    enum class HelpPointType {
        POLICE_STATION,
        HOSPITAL,
        FIRE_STATION,
        SAFE_HAVEN, // Participating businesses
        PUBLIC_TRANSPORT,
        WELL_LIT_AREA
    }
}
