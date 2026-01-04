package com.safeguard.app.ai

import android.content.Context
import android.location.Location
import com.safeguard.app.data.models.DangerZone
import com.safeguard.app.data.models.SOSEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.*

/**
 * AI-Powered Smart Safety Assistant
 * Provides personalized safety recommendations and insights
 */
class SmartSafetyAssistant(private val context: Context) {

    companion object {
        private const val TAG = "SmartSafetyAssistant"
    }

    data class SafetyInsight(
        val id: String,
        val title: String,
        val description: String,
        val type: InsightType,
        val priority: Priority,
        val actionLabel: String? = null,
        val actionRoute: String? = null,
        val icon: String = "💡"
    )

    enum class InsightType {
        TIP,
        WARNING,
        ACHIEVEMENT,
        REMINDER,
        PREDICTION
    }

    enum class Priority { LOW, MEDIUM, HIGH, URGENT }

    private val _insights = MutableStateFlow<List<SafetyInsight>>(emptyList())
    val insights: StateFlow<List<SafetyInsight>> = _insights.asStateFlow()

    private val _dailyPrediction = MutableStateFlow<DailyPrediction?>(null)
    val dailyPrediction: StateFlow<DailyPrediction?> = _dailyPrediction.asStateFlow()

    // Learning data
    private val locationPatterns = mutableMapOf<String, Int>() // location hash -> visit count
    private val timePatterns = mutableMapOf<Int, Int>() // hour -> activity count
    private val sosHistory = mutableListOf<SOSEvent>()

    /**
     * Generate personalized safety insights based on user behavior
     */
    fun generateInsights(
        currentLocation: Location?,
        dangerZones: List<DangerZone>,
        sosEvents: List<SOSEvent>,
        hasContacts: Boolean,
        triggersEnabled: Int
    ): List<SafetyInsight> {
        val insights = mutableListOf<SafetyInsight>()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Time-based insights
        if (hour >= 22 || hour < 5) {
            insights.add(SafetyInsight(
                id = "night_safety",
                title = "Late Night Safety",
                description = "It's late. Make sure someone knows your location. Consider sharing live location with a trusted contact.",
                type = InsightType.TIP,
                priority = Priority.MEDIUM,
                actionLabel = "Share Location",
                actionRoute = "live_location",
                icon = "🌙"
            ))
        }

        // Weekend night warning
        if ((dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) && hour >= 20) {
            insights.add(SafetyInsight(
                id = "weekend_alert",
                title = "Weekend Night Alert",
                description = "Weekend nights see higher incident rates. Stay with friends and keep your phone charged.",
                type = InsightType.WARNING,
                priority = Priority.MEDIUM,
                icon = "🎉"
            ))
        }

        // Location-based insights
        currentLocation?.let { loc ->
            // Check proximity to danger zones
            dangerZones.forEach { zone ->
                val distance = calculateDistance(
                    loc.latitude, loc.longitude,
                    zone.latitude, zone.longitude
                )
                if (distance < zone.radiusMeters * 2) {
                    insights.add(SafetyInsight(
                        id = "near_danger_${zone.id}",
                        title = "Approaching Danger Zone",
                        description = "You're ${distance.toInt()}m from '${zone.name}'. Stay alert.",
                        type = InsightType.WARNING,
                        priority = Priority.HIGH,
                        icon = "⚠️"
                    ))
                }
            }
        }

        // Setup completion insights
        if (!hasContacts) {
            insights.add(SafetyInsight(
                id = "no_contacts",
                title = "Add Emergency Contacts",
                description = "You haven't added any emergency contacts yet. Add at least one trusted person.",
                type = InsightType.REMINDER,
                priority = Priority.URGENT,
                actionLabel = "Add Contact",
                actionRoute = "contacts",
                icon = "👥"
            ))
        }

        if (triggersEnabled == 0) {
            insights.add(SafetyInsight(
                id = "no_triggers",
                title = "Enable SOS Triggers",
                description = "Enable at least one SOS trigger method for quick emergency activation.",
                type = InsightType.REMINDER,
                priority = Priority.HIGH,
                actionLabel = "Setup Triggers",
                actionRoute = "trigger_settings",
                icon = "⚡"
            ))
        }

        // Achievement insights
        if (sosEvents.isEmpty() && hasContacts && triggersEnabled > 0) {
            insights.add(SafetyInsight(
                id = "fully_protected",
                title = "You're Protected! 🛡️",
                description = "Great job! Your safety setup is complete. Stay safe out there!",
                type = InsightType.ACHIEVEMENT,
                priority = Priority.LOW,
                icon = "🏆"
            ))
        }

        // SOS history insights
        if (sosEvents.size >= 3) {
            val recentEvents = sosEvents.takeLast(5)
            val avgHour = recentEvents.map { 
                Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    .get(Calendar.HOUR_OF_DAY)
            }.average().toInt()
            
            insights.add(SafetyInsight(
                id = "sos_pattern",
                title = "SOS Pattern Detected",
                description = "Most of your SOS events occur around ${formatHour(avgHour)}. Be extra cautious during this time.",
                type = InsightType.PREDICTION,
                priority = Priority.MEDIUM,
                icon = "📊"
            ))
        }

        // Seasonal/weather insights (simplified)
        val month = calendar.get(Calendar.MONTH)
        if (month in listOf(Calendar.NOVEMBER, Calendar.DECEMBER, Calendar.JANUARY)) {
            insights.add(SafetyInsight(
                id = "winter_safety",
                title = "Winter Safety Tip",
                description = "Days are shorter. Avoid poorly lit areas and let someone know your route.",
                type = InsightType.TIP,
                priority = Priority.LOW,
                icon = "❄️"
            ))
        }

        _insights.value = insights.sortedByDescending { it.priority.ordinal }
        return insights
    }

    /**
     * Generate daily safety prediction based on patterns
     */
    fun generateDailyPrediction(
        sosEvents: List<com.safeguard.app.data.models.SOSEvent>,
        dangerZones: List<com.safeguard.app.data.models.DangerZone>
    ): DailyPrediction {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val factors = mutableListOf<String>()
        var riskScore = 20 // Base risk

        // Time factors
        val timeOfDay = when (hour) {
            in 6..11 -> "Morning"
            in 12..17 -> "Afternoon"
            in 18..21 -> "Evening"
            else -> "Night"
        }

        if (hour >= 22 || hour < 6) {
            riskScore += 20
            factors.add("Late night hours")
        }

        // Day factors
        val dayName = when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }

        if (dayOfWeek in listOf(Calendar.FRIDAY, Calendar.SATURDAY)) {
            riskScore += 15
            factors.add("Weekend (higher incident rate)")
        }

        // Historical factors
        val eventsThisDay = sosEvents.count { event ->
            Calendar.getInstance().apply { timeInMillis = event.timestamp }
                .get(Calendar.DAY_OF_WEEK) == dayOfWeek
        }
        if (eventsThisDay > 0) {
            riskScore += 10
            factors.add("Previous incidents on $dayName")
        }

        // Danger zone factors
        if (dangerZones.isNotEmpty()) {
            factors.add("${dangerZones.size} danger zone(s) configured")
        }

        riskScore = minOf(riskScore, 100)

        val riskLevel = when {
            riskScore >= 60 -> "High"
            riskScore >= 40 -> "Moderate"
            else -> "Low"
        }

        val recommendation = when (riskLevel) {
            "High" -> "Stay extra vigilant today. Share your location with trusted contacts."
            "Moderate" -> "Normal precautions advised. Keep your phone charged and accessible."
            else -> "Low risk day. Enjoy, but stay aware of your surroundings."
        }

        val prediction = DailyPrediction(
            riskLevel = riskLevel,
            riskScore = riskScore,
            factors = factors,
            timeOfDay = timeOfDay,
            dayOfWeek = dayName,
            recommendation = recommendation
        )

        _dailyPrediction.value = prediction
        return prediction
    }

    /**
     * Analyze location patterns for anomaly detection
     */
    fun analyzeLocationPattern(location: Location): Boolean {
        val hash = "${(location.latitude * 1000).toInt()}_${(location.longitude * 1000).toInt()}"
        val visitCount = locationPatterns.getOrDefault(hash, 0)
        locationPatterns[hash] = visitCount + 1
        
        // If this is a new location (visited less than 3 times), flag it
        return visitCount < 3
    }

    /**
     * Get smart route suggestions
     */
    fun getRouteSuggestions(
        from: Location,
        to: Location,
        dangerZones: List<DangerZone>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Check if route passes through danger zones
        dangerZones.forEach { zone ->
            val distanceToZone = calculateDistance(
                from.latitude, from.longitude,
                zone.latitude, zone.longitude
            )
            if (distanceToZone < 1000) { // Within 1km
                suggestions.add("⚠️ Route may pass near '${zone.name}'. Consider alternative path.")
            }
        }

        // Time-based suggestions
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 21 || hour < 6) {
            suggestions.add("🌙 It's dark outside. Stick to well-lit main roads.")
            suggestions.add("📍 Share your live location with a trusted contact.")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("✅ Route looks safe. Have a good trip!")
        }

        return suggestions
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }

    /**
     * Analyze user behavior and generate insights
     */
    fun analyzeUserBehavior(
        contacts: List<com.safeguard.app.data.models.EmergencyContact>,
        settings: com.safeguard.app.data.models.UserSettings,
        sosEvents: List<com.safeguard.app.data.models.SOSEvent>,
        dangerZones: List<com.safeguard.app.data.models.DangerZone>
    ) {
        // Count enabled triggers
        var triggersEnabled = 0
        if (settings.enableVolumeButtonTrigger) triggersEnabled++
        if (settings.enableShakeTrigger) triggersEnabled++
        if (settings.enablePowerButtonTrigger) triggersEnabled++
        if (settings.enableWidgetTrigger) triggersEnabled++

        // Generate insights
        generateInsights(
            currentLocation = null,
            dangerZones = dangerZones,
            sosEvents = sosEvents,
            hasContacts = contacts.isNotEmpty(),
            triggersEnabled = triggersEnabled
        )

        // Generate daily prediction
        generateDailyPrediction(sosEvents, dangerZones)
    }

    data class DailyPrediction(
        val riskLevel: String,
        val riskScore: Int,
        val factors: List<String>,
        val timeOfDay: String,
        val dayOfWeek: String,
        val recommendation: String
    )
}
