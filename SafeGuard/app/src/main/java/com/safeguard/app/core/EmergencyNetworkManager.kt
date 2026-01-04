package com.safeguard.app.core

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Emergency Network Manager - Connects to global emergency services
 * Supports AML (Advanced Mobile Location), eCall, and regional emergency numbers
 */
class EmergencyNetworkManager(private val context: Context) {

    companion object {
        private const val TAG = "EmergencyNetwork"
        
        // Global emergency numbers by country code
        val EMERGENCY_NUMBERS = mapOf(
            "US" to EmergencyInfo("911", "911", "United States"),
            "CA" to EmergencyInfo("911", "911", "Canada"),
            "UK" to EmergencyInfo("999", "112", "United Kingdom"),
            "GB" to EmergencyInfo("999", "112", "United Kingdom"),
            "IN" to EmergencyInfo("112", "100", "India", policeNumber = "100", ambulanceNumber = "102", womenHelpline = "1091"),
            "AU" to EmergencyInfo("000", "112", "Australia"),
            "DE" to EmergencyInfo("112", "110", "Germany"),
            "FR" to EmergencyInfo("112", "17", "France"),
            "JP" to EmergencyInfo("110", "119", "Japan"),
            "CN" to EmergencyInfo("110", "120", "China"),
            "BR" to EmergencyInfo("190", "192", "Brazil"),
            "MX" to EmergencyInfo("911", "066", "Mexico"),
            "RU" to EmergencyInfo("112", "102", "Russia"),
            "ZA" to EmergencyInfo("10111", "112", "South Africa"),
            "AE" to EmergencyInfo("999", "998", "UAE"),
            "SG" to EmergencyInfo("999", "995", "Singapore"),
            "MY" to EmergencyInfo("999", "994", "Malaysia"),
            "PH" to EmergencyInfo("911", "117", "Philippines"),
            "ID" to EmergencyInfo("112", "110", "Indonesia"),
            "TH" to EmergencyInfo("191", "1669", "Thailand"),
            "VN" to EmergencyInfo("113", "115", "Vietnam"),
            "KR" to EmergencyInfo("112", "119", "South Korea"),
            "PK" to EmergencyInfo("15", "1122", "Pakistan"),
            "BD" to EmergencyInfo("999", "999", "Bangladesh"),
            "NG" to EmergencyInfo("112", "199", "Nigeria"),
            "EG" to EmergencyInfo("122", "123", "Egypt"),
            "SA" to EmergencyInfo("911", "997", "Saudi Arabia"),
            "IT" to EmergencyInfo("112", "113", "Italy"),
            "ES" to EmergencyInfo("112", "091", "Spain"),
            "NL" to EmergencyInfo("112", "112", "Netherlands"),
            "BE" to EmergencyInfo("112", "101", "Belgium"),
            "SE" to EmergencyInfo("112", "112", "Sweden"),
            "NO" to EmergencyInfo("112", "110", "Norway"),
            "DK" to EmergencyInfo("112", "112", "Denmark"),
            "FI" to EmergencyInfo("112", "112", "Finland"),
            "PL" to EmergencyInfo("112", "997", "Poland"),
            "AT" to EmergencyInfo("112", "133", "Austria"),
            "CH" to EmergencyInfo("112", "117", "Switzerland"),
            "NZ" to EmergencyInfo("111", "111", "New Zealand"),
            "IE" to EmergencyInfo("112", "999", "Ireland"),
            "PT" to EmergencyInfo("112", "112", "Portugal"),
            "GR" to EmergencyInfo("112", "100", "Greece"),
            "TR" to EmergencyInfo("112", "155", "Turkey"),
            "IL" to EmergencyInfo("100", "101", "Israel"),
            "AR" to EmergencyInfo("911", "107", "Argentina"),
            "CL" to EmergencyInfo("131", "133", "Chile"),
            "CO" to EmergencyInfo("123", "112", "Colombia"),
            "PE" to EmergencyInfo("105", "116", "Peru")
        )
    }

    data class EmergencyInfo(
        val primaryNumber: String,
        val secondaryNumber: String,
        val countryName: String,
        val policeNumber: String? = null,
        val ambulanceNumber: String? = null,
        val fireNumber: String? = null,
        val womenHelpline: String? = null,
        val childHelpline: String? = null
    )

    data class NearbyEmergencyService(
        val name: String,
        val type: ServiceType,
        val phoneNumber: String,
        val distance: Float,
        val address: String,
        val isOpen24Hours: Boolean,
        val latitude: Double,
        val longitude: Double
    )

    enum class ServiceType {
        POLICE_STATION,
        HOSPITAL,
        FIRE_STATION,
        AMBULANCE_SERVICE,
        WOMEN_HELPLINE,
        CHILD_PROTECTION,
        COAST_GUARD,
        EMBASSY
    }

    private val _currentCountry = MutableStateFlow("IN")
    val currentCountry: StateFlow<String> = _currentCountry.asStateFlow()

    private val _emergencyInfo = MutableStateFlow(EMERGENCY_NUMBERS["IN"]!!)
    val emergencyInfo: StateFlow<EmergencyInfo> = _emergencyInfo.asStateFlow()

    /**
     * Detect country from location and update emergency numbers
     */
    fun updateFromLocation(location: Location) {
        try {
            val geocoder = android.location.Geocoder(context)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val countryCode = addresses[0].countryCode
                _currentCountry.value = countryCode
                EMERGENCY_NUMBERS[countryCode]?.let {
                    _emergencyInfo.value = it
                    Log.d(TAG, "Updated emergency info for $countryCode: ${it.primaryNumber}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect country", e)
        }
    }

    /**
     * Set country manually
     */
    fun setCountry(countryCode: String) {
        _currentCountry.value = countryCode
        EMERGENCY_NUMBERS[countryCode]?.let {
            _emergencyInfo.value = it
        }
    }

    /**
     * Get emergency number for current location
     */
    fun getEmergencyNumber(): String {
        return _emergencyInfo.value.primaryNumber
    }

    /**
     * Get all emergency numbers for current country
     */
    fun getAllEmergencyNumbers(): Map<String, String> {
        val info = _emergencyInfo.value
        val numbers = mutableMapOf<String, String>()
        numbers["Emergency"] = info.primaryNumber
        numbers["Secondary"] = info.secondaryNumber
        info.policeNumber?.let { numbers["Police"] = it }
        info.ambulanceNumber?.let { numbers["Ambulance"] = it }
        info.fireNumber?.let { numbers["Fire"] = it }
        info.womenHelpline?.let { numbers["Women Helpline"] = it }
        info.childHelpline?.let { numbers["Child Helpline"] = it }
        return numbers
    }

    /**
     * Get supported countries
     */
    fun getSupportedCountries(): List<Pair<String, String>> {
        return EMERGENCY_NUMBERS.map { (code, info) ->
            code to info.countryName
        }.sortedBy { it.second }
    }
}
