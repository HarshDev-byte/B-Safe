package com.safeguard.app.core

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Crowd-Sourced Safety Manager
 * Community-driven safety reports and real-time incident alerts
 */
class CrowdSourcedSafetyManager(private val context: Context) {

    companion object {
        private const val TAG = "CrowdSourcedSafety"
        private const val REPORT_RADIUS_METERS = 2000.0 // 2km radius
        private const val REPORT_EXPIRY_HOURS = 24
    }

    data class SafetyReport(
        val id: String = UUID.randomUUID().toString(),
        val type: ReportType,
        val severity: Severity,
        val location: LatLng,
        val description: String,
        val timestamp: Long = System.currentTimeMillis(),
        val reportedBy: String = "anonymous",
        val upvotes: Int = 0,
        val downvotes: Int = 0,
        val isVerified: Boolean = false,
        val mediaUrls: List<String> = emptyList()
    )

    enum class ReportType(val emoji: String, val label: String) {
        THEFT("🔓", "Theft/Robbery"),
        HARASSMENT("⚠️", "Harassment"),
        ASSAULT("🚨", "Assault"),
        SUSPICIOUS_ACTIVITY("👁️", "Suspicious Activity"),
        POOR_LIGHTING("💡", "Poor Lighting"),
        UNSAFE_AREA("🚫", "Unsafe Area"),
        ACCIDENT("🚗", "Accident"),
        POLICE_PRESENCE("👮", "Police Presence"),
        SAFE_SPOT("✅", "Safe Spot"),
        CROWD_GATHERING("👥", "Crowd Gathering"),
        ROAD_CLOSURE("🚧", "Road Closure"),
        NATURAL_HAZARD("🌊", "Natural Hazard"),
        STRAY_ANIMALS("🐕", "Stray Animals"),
        CONSTRUCTION("🏗️", "Construction Zone"),
        PROTEST("📢", "Protest/Rally")
    }

    enum class Severity(val color: Long, val weight: Int) {
        LOW(0xFF4CAF50, 1),      // Green
        MEDIUM(0xFFFFC107, 2),   // Yellow
        HIGH(0xFFFF9800, 3),     // Orange
        CRITICAL(0xFFF44336, 4)  // Red
    }

    data class AreaSafetyScore(
        val score: Int, // 0-100
        val level: SafetyLevel,
        val recentIncidents: Int,
        val lastIncidentTime: Long?,
        val topConcerns: List<ReportType>,
        val recommendation: String
    )

    enum class SafetyLevel(val label: String, val color: Long) {
        VERY_SAFE("Very Safe", 0xFF4CAF50),
        SAFE("Safe", 0xFF8BC34A),
        MODERATE("Moderate", 0xFFFFC107),
        CAUTION("Use Caution", 0xFFFF9800),
        DANGEROUS("Dangerous", 0xFFF44336)
    }

    private val _nearbyReports = MutableStateFlow<List<SafetyReport>>(emptyList())
    val nearbyReports: StateFlow<List<SafetyReport>> = _nearbyReports.asStateFlow()

    private val _areaSafetyScore = MutableStateFlow<AreaSafetyScore?>(null)
    val areaSafetyScore: StateFlow<AreaSafetyScore?> = _areaSafetyScore.asStateFlow()

    // Local cache of reports (in production, this would be Firebase/backend)
    private val allReports = mutableListOf<SafetyReport>()

    init {
        // Add some sample data for demo
        addSampleReports()
    }

    /**
     * Submit a new safety report
     */
    fun submitReport(
        type: ReportType,
        severity: Severity,
        location: LatLng,
        description: String,
        mediaUrls: List<String> = emptyList()
    ): SafetyReport {
        val report = SafetyReport(
            type = type,
            severity = severity,
            location = location,
            description = description,
            mediaUrls = mediaUrls
        )
        allReports.add(report)
        return report
    }

    /**
     * Get reports near a location
     */
    fun getReportsNearLocation(location: Location, radiusMeters: Double = REPORT_RADIUS_METERS): List<SafetyReport> {
        val now = System.currentTimeMillis()
        val expiryTime = REPORT_EXPIRY_HOURS * 60 * 60 * 1000L

        val nearbyReports = allReports.filter { report ->
            // Check if not expired
            val isRecent = (now - report.timestamp) < expiryTime
            
            // Check distance
            val distance = calculateDistance(
                location.latitude, location.longitude,
                report.location.latitude, report.location.longitude
            )
            val isNearby = distance <= radiusMeters

            isRecent && isNearby
        }.sortedByDescending { it.timestamp }

        _nearbyReports.value = nearbyReports
        calculateAreaSafetyScore(nearbyReports)
        
        return nearbyReports
    }

    /**
     * Calculate safety score for an area based on reports
     */
    private fun calculateAreaSafetyScore(reports: List<SafetyReport>) {
        if (reports.isEmpty()) {
            _areaSafetyScore.value = AreaSafetyScore(
                score = 95,
                level = SafetyLevel.VERY_SAFE,
                recentIncidents = 0,
                lastIncidentTime = null,
                topConcerns = emptyList(),
                recommendation = "This area appears safe. No recent incidents reported."
            )
            return
        }

        // Calculate weighted score
        var totalWeight = 0
        val concernCounts = mutableMapOf<ReportType, Int>()
        
        reports.forEach { report ->
            totalWeight += report.severity.weight
            concernCounts[report.type] = (concernCounts[report.type] ?: 0) + 1
        }

        // Score decreases with more/severe incidents
        val baseScore = 100
        val deduction = minOf(totalWeight * 5, 80) // Max 80 point deduction
        val score = baseScore - deduction

        val level = when {
            score >= 85 -> SafetyLevel.VERY_SAFE
            score >= 70 -> SafetyLevel.SAFE
            score >= 50 -> SafetyLevel.MODERATE
            score >= 30 -> SafetyLevel.CAUTION
            else -> SafetyLevel.DANGEROUS
        }

        val topConcerns = concernCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val recommendation = when (level) {
            SafetyLevel.VERY_SAFE -> "Area is generally safe. Stay aware of your surroundings."
            SafetyLevel.SAFE -> "Minor incidents reported. Normal precautions advised."
            SafetyLevel.MODERATE -> "Some safety concerns. Stay alert and avoid isolated areas."
            SafetyLevel.CAUTION -> "Multiple incidents reported. Consider alternative routes."
            SafetyLevel.DANGEROUS -> "High risk area. Avoid if possible or travel with others."
        }

        _areaSafetyScore.value = AreaSafetyScore(
            score = score,
            level = level,
            recentIncidents = reports.size,
            lastIncidentTime = reports.maxOfOrNull { it.timestamp },
            topConcerns = topConcerns,
            recommendation = recommendation
        )
    }

    /**
     * Upvote a report (confirms accuracy)
     */
    fun upvoteReport(reportId: String) {
        allReports.find { it.id == reportId }?.let { report ->
            val index = allReports.indexOf(report)
            allReports[index] = report.copy(upvotes = report.upvotes + 1)
        }
    }

    /**
     * Downvote a report (disputes accuracy)
     */
    fun downvoteReport(reportId: String) {
        allReports.find { it.id == reportId }?.let { report ->
            val index = allReports.indexOf(report)
            allReports[index] = report.copy(downvotes = report.downvotes + 1)
        }
    }

    /**
     * Get safety heatmap data for map visualization
     */
    fun getHeatmapData(): List<Pair<LatLng, Float>> {
        return allReports.map { report ->
            report.location to (report.severity.weight / 4f)
        }
    }

    /**
     * Get trending safety concerns in an area
     */
    fun getTrendingConcerns(location: Location): List<Pair<ReportType, Int>> {
        val nearbyReports = getReportsNearLocation(location, 5000.0) // 5km radius
        val counts = mutableMapOf<ReportType, Int>()
        
        nearbyReports.forEach { report ->
            counts[report.type] = (counts[report.type] ?: 0) + 1
        }
        
        return counts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun addSampleReports() {
        // Sample reports for demo (Delhi area)
        val sampleReports = listOf(
            SafetyReport(
                type = ReportType.POOR_LIGHTING,
                severity = Severity.MEDIUM,
                location = LatLng(28.6139, 77.2090),
                description = "Street lights not working near metro station"
            ),
            SafetyReport(
                type = ReportType.POLICE_PRESENCE,
                severity = Severity.LOW,
                location = LatLng(28.6145, 77.2095),
                description = "Police patrol active in this area"
            ),
            SafetyReport(
                type = ReportType.SAFE_SPOT,
                severity = Severity.LOW,
                location = LatLng(28.6150, 77.2100),
                description = "Well-lit area with 24/7 security"
            )
        )
        allReports.addAll(sampleReports)
    }

    /**
     * Fetch nearby reports for a given location
     */
    fun fetchNearbyReports(latitude: Double, longitude: Double, radiusMeters: Double = REPORT_RADIUS_METERS) {
        val location = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        getReportsNearLocation(location, radiusMeters)
    }

    /**
     * Submit a report with latitude/longitude
     */
    fun submitReport(
        type: ReportType,
        severity: Severity,
        description: String,
        latitude: Double,
        longitude: Double,
        mediaUrls: List<String> = emptyList()
    ): SafetyReport {
        return submitReport(
            type = type,
            severity = severity,
            location = LatLng(latitude, longitude),
            description = description,
            mediaUrls = mediaUrls
        )
    }

    fun cleanup() {
        // Cleanup resources if needed
        allReports.clear()
    }
}
