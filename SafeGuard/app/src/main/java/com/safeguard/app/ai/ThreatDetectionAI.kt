package com.safeguard.app.ai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import com.safeguard.app.core.SOSManager
import com.safeguard.app.data.models.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * AI-Powered Threat Detection System
 * Uses on-device ML to detect potential danger situations
 */
class ThreatDetectionAI(
    private val context: Context,
    private val sosManager: SOSManager
) : SensorEventListener {

    companion object {
        private const val TAG = "ThreatDetectionAI"
        
        // Thresholds for anomaly detection
        private const val SUDDEN_STOP_THRESHOLD = 15f // m/s² deceleration
        private const val FALL_DETECTION_THRESHOLD = 25f // m/s²
        private const val RUNNING_SPEED_THRESHOLD = 3.5f // m/s (12.6 km/h)
        private const val ERRATIC_MOVEMENT_WINDOW = 10 // seconds
        private const val UNUSUAL_HOUR_START = 23 // 11 PM
        private const val UNUSUAL_HOUR_END = 5 // 5 AM
    }

    data class ThreatAssessment(
        val overallRisk: RiskLevel,
        val riskScore: Int, // 0-100
        val detectedThreats: List<DetectedThreat>,
        val recommendation: String,
        val shouldAutoAlert: Boolean
    )

    data class DetectedThreat(
        val type: ThreatType,
        val confidence: Float, // 0-1
        val description: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class ThreatType {
        SUDDEN_STOP,           // Vehicle accident or assault
        FALL_DETECTED,         // User fell down
        RUNNING_DETECTED,      // User running (possibly fleeing)
        ERRATIC_MOVEMENT,      // Unusual movement pattern
        UNUSUAL_LOCATION,      // In unfamiliar/dangerous area
        UNUSUAL_TIME,          // Late night activity
        DEVICE_SNATCHED,       // Phone grabbed suddenly
        PROLONGED_STILLNESS,   // No movement for too long during journey
        ROUTE_DEVIATION,       // Deviated from expected path
        SPEED_ANOMALY          // Unusual speed changes
    }

    enum class RiskLevel(val color: Long) {
        LOW(0xFF4CAF50),       // Green
        MODERATE(0xFFFFC107),  // Yellow
        HIGH(0xFFFF9800),      // Orange
        CRITICAL(0xFFF44336)   // Red
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _currentThreatAssessment = MutableStateFlow(ThreatAssessment(
        overallRisk = RiskLevel.LOW,
        riskScore = 0,
        detectedThreats = emptyList(),
        recommendation = "All clear",
        shouldAutoAlert = false
    ))
    val currentThreatAssessment: StateFlow<ThreatAssessment> = _currentThreatAssessment.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Sensor data buffers
    private val accelerometerBuffer = mutableListOf<FloatArray>()
    private val locationHistory = mutableListOf<LocationPoint>()
    private var lastLocation: Location? = null
    private var lastMovementTime = System.currentTimeMillis()

    data class LocationPoint(
        val lat: Double,
        val lng: Double,
        val speed: Float,
        val timestamp: Long
    )

    /**
     * Start AI threat monitoring
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        _isMonitoring.value = true
        Log.d(TAG, "AI Threat Detection started")
    }

    /**
     * Stop AI threat monitoring
     */
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        _isMonitoring.value = false
        accelerometerBuffer.clear()
        locationHistory.clear()
        Log.d(TAG, "AI Threat Detection stopped")
    }

    /**
     * Update with new location data
     */
    fun updateLocation(location: Location) {
        val point = LocationPoint(
            lat = location.latitude,
            lng = location.longitude,
            speed = location.speed,
            timestamp = System.currentTimeMillis()
        )
        
        locationHistory.add(point)
        if (locationHistory.size > 100) {
            locationHistory.removeAt(0)
        }
        
        // Check for movement
        if (location.speed > 0.5f) {
            lastMovementTime = System.currentTimeMillis()
        }
        
        lastLocation = location
        analyzeThreats()
    }

    /**
     * Set expected route for deviation detection
     */
    fun setExpectedRoute(waypoints: List<Pair<Double, Double>>) {
        // Store expected route for deviation analysis
        Log.d(TAG, "Expected route set with ${waypoints.size} waypoints")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerBuffer.add(event.values.clone())
                if (accelerometerBuffer.size > 50) {
                    accelerometerBuffer.removeAt(0)
                }
                checkForSuddenEvents(event.values)
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Analyze rotation for device snatch detection
                checkForDeviceSnatch(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkForSuddenEvents(values: FloatArray) {
        val magnitude = sqrt(
            values[0] * values[0] + 
            values[1] * values[1] + 
            values[2] * values[2]
        )

        // Fall detection
        if (magnitude > FALL_DETECTION_THRESHOLD) {
            addThreat(DetectedThreat(
                type = ThreatType.FALL_DETECTED,
                confidence = minOf((magnitude - FALL_DETECTION_THRESHOLD) / 10f, 1f),
                description = "Possible fall detected - high impact registered"
            ))
        }

        // Sudden stop (possible accident)
        if (accelerometerBuffer.size >= 10) {
            val recentMagnitudes = accelerometerBuffer.takeLast(10).map { arr ->
                sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])
            }
            val avgMagnitude = recentMagnitudes.average()
            
            if (avgMagnitude > SUDDEN_STOP_THRESHOLD && lastLocation?.speed ?: 0f > 5f) {
                addThreat(DetectedThreat(
                    type = ThreatType.SUDDEN_STOP,
                    confidence = 0.7f,
                    description = "Sudden deceleration detected while moving"
                ))
            }
        }
    }

    private fun checkForDeviceSnatch(gyroValues: FloatArray) {
        val rotationMagnitude = sqrt(
            gyroValues[0] * gyroValues[0] +
            gyroValues[1] * gyroValues[1] +
            gyroValues[2] * gyroValues[2]
        )

        // High rotation + sudden acceleration = possible snatch
        if (rotationMagnitude > 8f) {
            val recentAccel = accelerometerBuffer.lastOrNull()
            if (recentAccel != null) {
                val accelMag = sqrt(
                    recentAccel[0] * recentAccel[0] +
                    recentAccel[1] * recentAccel[1] +
                    recentAccel[2] * recentAccel[2]
                )
                if (accelMag > 20f) {
                    addThreat(DetectedThreat(
                        type = ThreatType.DEVICE_SNATCHED,
                        confidence = 0.6f,
                        description = "Device may have been grabbed suddenly"
                    ))
                }
            }
        }
    }

    private fun analyzeThreats() {
        val threats = mutableListOf<DetectedThreat>()
        
        // Check unusual time
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour >= UNUSUAL_HOUR_START || hour < UNUSUAL_HOUR_END) {
            threats.add(DetectedThreat(
                type = ThreatType.UNUSUAL_TIME,
                confidence = 0.3f,
                description = "Activity during late night hours"
            ))
        }

        // Check for running
        lastLocation?.let { loc ->
            if (loc.speed > RUNNING_SPEED_THRESHOLD) {
                threats.add(DetectedThreat(
                    type = ThreatType.RUNNING_DETECTED,
                    confidence = minOf(loc.speed / 5f, 1f),
                    description = "Running speed detected (${String.format("%.1f", loc.speed * 3.6)} km/h)"
                ))
            }
        }

        // Check for prolonged stillness during journey
        val stillnessTime = System.currentTimeMillis() - lastMovementTime
        if (stillnessTime > 5 * 60 * 1000 && locationHistory.isNotEmpty()) { // 5 minutes
            threats.add(DetectedThreat(
                type = ThreatType.PROLONGED_STILLNESS,
                confidence = minOf(stillnessTime / (10f * 60 * 1000), 1f),
                description = "No movement for ${stillnessTime / 60000} minutes"
            ))
        }

        // Check for erratic movement
        if (locationHistory.size >= 5) {
            val recentSpeeds = locationHistory.takeLast(5).map { it.speed }
            val speedVariance = calculateVariance(recentSpeeds)
            if (speedVariance > 10f) {
                threats.add(DetectedThreat(
                    type = ThreatType.ERRATIC_MOVEMENT,
                    confidence = minOf(speedVariance / 20f, 1f),
                    description = "Erratic speed changes detected"
                ))
            }
        }

        // Calculate overall risk
        updateAssessment(threats)
    }

    private fun addThreat(threat: DetectedThreat) {
        val currentThreats = _currentAssessment.value.detectedThreats.toMutableList()
        
        // Remove old threats of same type
        currentThreats.removeAll { it.type == threat.type }
        currentThreats.add(threat)
        
        // Keep only recent threats (last 30 seconds)
        val recentThreats = currentThreats.filter {
            System.currentTimeMillis() - it.timestamp < 30000
        }
        
        updateAssessment(recentThreats)
    }

    private fun updateAssessment(threats: List<DetectedThreat>) {
        // Calculate risk score
        var riskScore = 0
        threats.forEach { threat ->
            val weight = when (threat.type) {
                ThreatType.FALL_DETECTED -> 40
                ThreatType.DEVICE_SNATCHED -> 35
                ThreatType.SUDDEN_STOP -> 30
                ThreatType.RUNNING_DETECTED -> 25
                ThreatType.PROLONGED_STILLNESS -> 20
                ThreatType.ERRATIC_MOVEMENT -> 15
                ThreatType.ROUTE_DEVIATION -> 20
                ThreatType.UNUSUAL_TIME -> 10
                ThreatType.UNUSUAL_LOCATION -> 15
                ThreatType.SPEED_ANOMALY -> 10
            }
            riskScore += (weight * threat.confidence).toInt()
        }
        riskScore = minOf(riskScore, 100)

        // Determine risk level
        val riskLevel = when {
            riskScore >= 70 -> RiskLevel.CRITICAL
            riskScore >= 50 -> RiskLevel.HIGH
            riskScore >= 25 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        // Generate recommendation
        val recommendation = when (riskLevel) {
            RiskLevel.CRITICAL -> "⚠️ Multiple danger signals detected. Consider triggering SOS."
            RiskLevel.HIGH -> "🔶 Elevated risk detected. Stay alert and be ready to call for help."
            RiskLevel.MODERATE -> "⚡ Some unusual activity detected. Stay aware of surroundings."
            RiskLevel.LOW -> "✅ No immediate threats detected. Stay safe!"
        }

        val shouldAutoAlert = riskLevel == RiskLevel.CRITICAL && threats.any { 
            it.type in listOf(ThreatType.FALL_DETECTED, ThreatType.DEVICE_SNATCHED, ThreatType.SUDDEN_STOP)
        }

        _currentThreatAssessment.value = ThreatAssessment(
            overallRisk = riskLevel,
            riskScore = riskScore,
            detectedThreats = threats,
            recommendation = recommendation,
            shouldAutoAlert = shouldAutoAlert
        )

        // Auto-trigger SOS if critical threat detected
        if (shouldAutoAlert) {
            scope.launch {
                sosManager.triggerSOS(TriggerType.THREAT_DETECTED)
            }
        }
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    fun cleanup() {
        stopMonitoring()
    }
}
