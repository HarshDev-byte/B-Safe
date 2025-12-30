package com.safeguard.app.core

import android.content.Context
import com.safeguard.app.data.models.EmergencyContact
import com.safeguard.app.data.models.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Safety Score Manager - Gamification to encourage complete safety setup
 * Shows users how protected they are and what they can do to improve
 */
class SafetyScoreManager(private val context: Context) {
    
    data class SafetyScore(
        val totalScore: Int, // 0-100
        val grade: SafetyGrade,
        val breakdown: ScoreBreakdown,
        val recommendations: List<Recommendation>
    )
    
    data class ScoreBreakdown(
        val contactsScore: Int, // Max 30
        val triggersScore: Int, // Max 25
        val profileScore: Int, // Max 15
        val settingsScore: Int, // Max 20
        val activityScore: Int // Max 10
    )
    
    data class Recommendation(
        val title: String,
        val description: String,
        val pointsToGain: Int,
        val action: RecommendationAction,
        val priority: Priority
    )
    
    enum class SafetyGrade(val label: String, val emoji: String, val color: Long) {
        EXCELLENT("Excellent", "🛡️", 0xFF4CAF50),
        GOOD("Good", "✅", 0xFF8BC34A),
        FAIR("Fair", "⚠️", 0xFFFFC107),
        NEEDS_WORK("Needs Work", "🔶", 0xFFFF9800),
        CRITICAL("Critical", "🚨", 0xFFF44336)
    }
    
    enum class RecommendationAction {
        ADD_CONTACT,
        ADD_EMAIL_TO_CONTACT,
        ENABLE_TRIGGER,
        COMPLETE_PROFILE,
        ENABLE_LOCATION_UPDATES,
        ADD_DANGER_ZONE,
        ENABLE_AUDIO_RECORDING,
        VERIFY_PERMISSIONS,
        TEST_SOS
    }
    
    enum class Priority { HIGH, MEDIUM, LOW }
    
    private val _safetyScore = MutableStateFlow(SafetyScore(
        totalScore = 0,
        grade = SafetyGrade.CRITICAL,
        breakdown = ScoreBreakdown(0, 0, 0, 0, 0),
        recommendations = emptyList()
    ))
    val safetyScore: StateFlow<SafetyScore> = _safetyScore.asStateFlow()
    
    /**
     * Calculate safety score based on current setup
     */
    fun calculateScore(
        contacts: List<EmergencyContact>,
        settings: UserSettings,
        hasLocationPermission: Boolean,
        hasSmsPermission: Boolean,
        hasCallPermission: Boolean,
        dangerZonesCount: Int,
        sosEventsCount: Int
    ): SafetyScore {
        val recommendations = mutableListOf<Recommendation>()
        
        // === CONTACTS SCORE (Max 30) ===
        var contactsScore = 0
        
        // At least 1 contact = 10 points
        if (contacts.isNotEmpty()) {
            contactsScore += 10
        } else {
            recommendations.add(Recommendation(
                title = "Add Emergency Contact",
                description = "Add at least one trusted person who will be notified during emergencies",
                pointsToGain = 10,
                action = RecommendationAction.ADD_CONTACT,
                priority = Priority.HIGH
            ))
        }
        
        // 2+ contacts = +5 points
        if (contacts.size >= 2) {
            contactsScore += 5
        } else if (contacts.size == 1) {
            recommendations.add(Recommendation(
                title = "Add More Contacts",
                description = "Having multiple contacts increases your safety net",
                pointsToGain = 5,
                action = RecommendationAction.ADD_CONTACT,
                priority = Priority.MEDIUM
            ))
        }
        
        // 3+ contacts = +5 points
        if (contacts.size >= 3) {
            contactsScore += 5
        }
        
        // At least one contact with email = +5 points
        val hasEmailContact = contacts.any { it.email.isNotBlank() && it.enableEmail }
        if (hasEmailContact) {
            contactsScore += 5
        } else if (contacts.isNotEmpty()) {
            recommendations.add(Recommendation(
                title = "Add Email to Contact",
                description = "Email alerts work without SIM card - great backup option",
                pointsToGain = 5,
                action = RecommendationAction.ADD_EMAIL_TO_CONTACT,
                priority = Priority.MEDIUM
            ))
        }
        
        // Primary contact set = +5 points
        if (contacts.any { it.isPrimary }) {
            contactsScore += 5
        }
        
        // === TRIGGERS SCORE (Max 25) ===
        var triggersScore = 0
        val enabledTriggers = listOf(
            settings.enableVolumeButtonTrigger,
            settings.enableShakeTrigger,
            settings.enablePowerButtonTrigger,
            settings.enableWidgetTrigger
        ).count { it }
        
        // Each trigger = 5 points (max 20)
        triggersScore += minOf(enabledTriggers * 5, 20)
        
        if (enabledTriggers == 0) {
            recommendations.add(Recommendation(
                title = "Enable SOS Trigger",
                description = "Set up at least one way to trigger SOS (shake, volume buttons, etc.)",
                pointsToGain = 5,
                action = RecommendationAction.ENABLE_TRIGGER,
                priority = Priority.HIGH
            ))
        } else if (enabledTriggers < 2) {
            recommendations.add(Recommendation(
                title = "Add Backup Trigger",
                description = "Enable multiple triggers for different situations",
                pointsToGain = 5,
                action = RecommendationAction.ENABLE_TRIGGER,
                priority = Priority.LOW
            ))
        }
        
        // Location updates enabled = +5 points
        if (settings.enablePeriodicLocationUpdates) {
            triggersScore += 5
        } else {
            recommendations.add(Recommendation(
                title = "Enable Live Location",
                description = "Send continuous location updates during SOS",
                pointsToGain = 5,
                action = RecommendationAction.ENABLE_LOCATION_UPDATES,
                priority = Priority.MEDIUM
            ))
        }
        
        // === PROFILE SCORE (Max 15) ===
        var profileScore = 0
        
        // Name set = +5 points
        if (settings.userName.isNotBlank()) {
            profileScore += 5
        } else {
            recommendations.add(Recommendation(
                title = "Add Your Name",
                description = "Your name will be included in emergency messages",
                pointsToGain = 5,
                action = RecommendationAction.COMPLETE_PROFILE,
                priority = Priority.LOW
            ))
        }
        
        // Blood group = +5 points
        if (settings.bloodGroup.isNotBlank()) {
            profileScore += 5
        }
        
        // Medical info = +5 points
        if (settings.medicalNotes.isNotBlank() || settings.allergies.isNotBlank()) {
            profileScore += 5
        }
        
        // === SETTINGS SCORE (Max 20) ===
        var settingsScore = 0
        
        // Permissions granted
        if (hasLocationPermission) settingsScore += 5
        if (hasSmsPermission) settingsScore += 5
        if (hasCallPermission) settingsScore += 3
        
        // Danger zones set up
        if (dangerZonesCount > 0) {
            settingsScore += 5
        } else {
            recommendations.add(Recommendation(
                title = "Set Up Danger Zones",
                description = "Get alerts when entering unsafe areas",
                pointsToGain = 5,
                action = RecommendationAction.ADD_DANGER_ZONE,
                priority = Priority.LOW
            ))
        }
        
        // Custom SMS template
        if (settings.smsTemplate != settings.smsTemplate) { // Check if customized
            settingsScore += 2
        }
        
        // === ACTIVITY SCORE (Max 10) ===
        var activityScore = 0
        
        // Has tested SOS (has events)
        if (sosEventsCount > 0) {
            activityScore += 10
        } else {
            recommendations.add(Recommendation(
                title = "Test Your Setup",
                description = "Do a test SOS to make sure everything works",
                pointsToGain = 10,
                action = RecommendationAction.TEST_SOS,
                priority = Priority.MEDIUM
            ))
        }
        
        // Calculate total
        val totalScore = contactsScore + triggersScore + profileScore + settingsScore + activityScore
        
        // Determine grade
        val grade = when {
            totalScore >= 85 -> SafetyGrade.EXCELLENT
            totalScore >= 70 -> SafetyGrade.GOOD
            totalScore >= 50 -> SafetyGrade.FAIR
            totalScore >= 30 -> SafetyGrade.NEEDS_WORK
            else -> SafetyGrade.CRITICAL
        }
        
        // Sort recommendations by priority
        val sortedRecommendations = recommendations.sortedBy { 
            when (it.priority) {
                Priority.HIGH -> 0
                Priority.MEDIUM -> 1
                Priority.LOW -> 2
            }
        }
        
        val score = SafetyScore(
            totalScore = totalScore,
            grade = grade,
            breakdown = ScoreBreakdown(
                contactsScore = contactsScore,
                triggersScore = triggersScore,
                profileScore = profileScore,
                settingsScore = settingsScore,
                activityScore = activityScore
            ),
            recommendations = sortedRecommendations
        )
        
        _safetyScore.value = score
        return score
    }
    
    fun getScoreColor(score: Int): Long {
        return when {
            score >= 85 -> 0xFF4CAF50 // Green
            score >= 70 -> 0xFF8BC34A // Light Green
            score >= 50 -> 0xFFFFC107 // Yellow
            score >= 30 -> 0xFFFF9800 // Orange
            else -> 0xFFF44336 // Red
        }
    }
}
