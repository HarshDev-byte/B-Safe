package com.safeguard.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Emergency Network Manager - Handles all network-based emergency communications
 * Supports multiple fallback methods and integrates with emergency services APIs
 */
class EmergencyNetworkManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _pendingAlerts = MutableStateFlow<List<PendingAlert>>(emptyList())
    val pendingAlerts: StateFlow<List<PendingAlert>> = _pendingAlerts.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    sealed class NetworkState {
        object Unknown : NetworkState()
        object Offline : NetworkState()
        data class Online(val type: ConnectionType) : NetworkState()
        object EmergencyOnly : NetworkState() // Can only reach emergency services
    }

    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        SATELLITE // For future satellite SOS support
    }

    @Serializable
    data class PendingAlert(
        val id: String,
        val type: AlertType,
        val payload: String,
        val createdAt: Long,
        val retryCount: Int = 0,
        val maxRetries: Int = 5
    )

    enum class AlertType {
        LIVE_LOCATION,
        SOS_NOTIFICATION,
        CHECK_IN_MISSED,
        HEALTH_EMERGENCY,
        DANGER_ZONE_ALERT
    }

    // Emergency Services Integration
    @Serializable
    data class EmergencyServiceEndpoint(
        val countryCode: String,
        val serviceName: String,
        val apiEndpoint: String?,
        val supportsAML: Boolean, // Advanced Mobile Location
        val supportsNG911: Boolean, // Next Generation 911
        val supportsECall: Boolean, // European eCall
        val phoneNumber: String,
        val smsNumber: String?,
        val dataFormat: DataFormat
    )

    enum class DataFormat {
        CAP, // Common Alerting Protocol
        EDXL, // Emergency Data Exchange Language
        NIEM, // National Information Exchange Model
        CUSTOM
    }

    // Regional emergency service configurations
    private val emergencyServices = mapOf(
        "US" to EmergencyServiceEndpoint(
            countryCode = "US",
            serviceName = "911",
            apiEndpoint = null, // NG911 APIs vary by region
            supportsAML = true,
            supportsNG911 = true,
            supportsECall = false,
            phoneNumber = "911",
            smsNumber = "911", // Text-to-911 where available
            dataFormat = DataFormat.NIEM
        ),
        "EU" to EmergencyServiceEndpoint(
            countryCode = "EU",
            serviceName = "112",
            apiEndpoint = null,
            supportsAML = true,
            supportsNG911 = false,
            supportsECall = true,
            phoneNumber = "112",
            smsNumber = null,
            dataFormat = DataFormat.CAP
        ),
        "UK" to EmergencyServiceEndpoint(
            countryCode = "UK",
            serviceName = "999",
            apiEndpoint = null,
            supportsAML = true,
            supportsNG911 = false,
            supportsECall = false,
            phoneNumber = "999",
            smsNumber = null,
            dataFormat = DataFormat.CAP
        ),
        "IN" to EmergencyServiceEndpoint(
            countryCode = "IN",
            serviceName = "112",
            apiEndpoint = null,
            supportsAML = false,
            supportsNG911 = false,
            supportsECall = false,
            phoneNumber = "112",
            smsNumber = null,
            dataFormat = DataFormat.CUSTOM
        ),
        "AU" to EmergencyServiceEndpoint(
            countryCode = "AU",
            serviceName = "000",
            apiEndpoint = null,
            supportsAML = true,
            supportsNG911 = false,
            supportsECall = false,
            phoneNumber = "000",
            smsNumber = "106", // For deaf/hearing impaired
            dataFormat = DataFormat.CAP
        )
    )

    fun initialize() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
        checkCurrentNetwork()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkState()
                processPendingAlerts()
            }

            override fun onLost(network: Network) {
                updateNetworkState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                updateNetworkState()
            }
        }

        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun checkCurrentNetwork() {
        updateNetworkState()
    }

    private fun updateNetworkState() {
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)

        _networkState.value = when {
            capabilities == null -> NetworkState.Offline
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 
                NetworkState.Online(ConnectionType.WIFI)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 
                NetworkState.Online(ConnectionType.CELLULAR)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                NetworkState.Online(ConnectionType.ETHERNET)
            else -> NetworkState.Offline
        }
    }

    fun isOnline(): Boolean = networkState.value is NetworkState.Online

    // Live Location Sharing
    @Serializable
    data class LiveLocationSession(
        val sessionId: String,
        val startTime: Long,
        val expiresAt: Long,
        val shareUrl: String,
        val accessCode: String,
        val authorizedContacts: List<String>
    )

    suspend fun startLiveLocationSharing(
        durationMinutes: Int,
        contactIds: List<Long>
    ): Result<LiveLocationSession> {
        // In production, this would:
        // 1. Create a secure session on backend
        // 2. Generate shareable URL with access code
        // 3. Notify contacts via SMS/push
        // 4. Start location streaming
        
        val sessionId = java.util.UUID.randomUUID().toString()
        val accessCode = generateAccessCode()
        
        return Result.success(
            LiveLocationSession(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L),
                shareUrl = "https://safeguard.app/live/$sessionId",
                accessCode = accessCode,
                authorizedContacts = contactIds.map { it.toString() }
            )
        )
    }

    private fun generateAccessCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluding confusing chars
        return (1..6).map { chars.random() }.joinToString("")
    }

    // Queue alerts for when network becomes available
    fun queueAlert(type: AlertType, payload: String) {
        val alert = PendingAlert(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            payload = payload,
            createdAt = System.currentTimeMillis()
        )
        _pendingAlerts.value = _pendingAlerts.value + alert
        
        if (isOnline()) {
            processPendingAlerts()
        }
    }

    private fun processPendingAlerts() {
        scope.launch {
            val pending = _pendingAlerts.value.toMutableList()
            val processed = mutableListOf<String>()
            
            pending.forEach { alert ->
                if (sendAlert(alert)) {
                    processed.add(alert.id)
                } else if (alert.retryCount < alert.maxRetries) {
                    // Will retry later
                } else {
                    processed.add(alert.id) // Give up after max retries
                }
            }
            
            _pendingAlerts.value = pending.filter { it.id !in processed }
        }
    }

    private suspend fun sendAlert(alert: PendingAlert): Boolean {
        // Send alert to backend/emergency services
        return try {
            // HTTP request to backend
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEmergencyServiceForCountry(countryCode: String): EmergencyServiceEndpoint? {
        return emergencyServices[countryCode.uppercase()]
    }

    // Advanced Mobile Location (AML) support
    @Serializable
    data class AMLData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val altitude: Double?,
        val floor: Int?, // Indoor positioning
        val confidence: Int, // 0-100
        val positioningMethod: String,
        val timestamp: Long,
        val imei: String?,
        val imsi: String?
    )

    fun generateAMLPayload(
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ): String {
        val amlData = AMLData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = null,
            floor = null,
            confidence = calculateConfidence(accuracy),
            positioningMethod = "GPS",
            timestamp = System.currentTimeMillis(),
            imei = null, // Would need READ_PHONE_STATE permission
            imsi = null
        )
        return json.encodeToString(amlData)
    }

    private fun calculateConfidence(accuracy: Float): Int {
        return when {
            accuracy < 10 -> 95
            accuracy < 50 -> 80
            accuracy < 100 -> 60
            accuracy < 500 -> 40
            else -> 20
        }
    }

    fun cleanup() {
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
    }
}
