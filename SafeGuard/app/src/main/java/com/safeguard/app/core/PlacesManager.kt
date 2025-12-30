package com.safeguard.app.core

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Manages nearby places search for safety-related locations using Google Places API
 */
class PlacesManager(private val context: Context) {

    private var placesClient: PlacesClient? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "PlacesManager"
        
        // Place types relevant for safety
        const val TYPE_POLICE = "police"
        const val TYPE_HOSPITAL = "hospital"
        const val TYPE_FIRE_STATION = "fire_station"
        const val TYPE_PHARMACY = "pharmacy"
        
        // Default search radius in meters
        const val DEFAULT_RADIUS = 5000
        const val MAX_RADIUS = 50000
    }

    /**
     * Initialize Places API client
     */
    fun initialize(apiKey: String) {
        if (!isInitialized && apiKey.isNotBlank() && apiKey != "YOUR_GOOGLE_MAPS_API_KEY") {
            try {
                if (!Places.isInitialized()) {
                    Places.initialize(context, apiKey)
                }
                placesClient = Places.createClient(context)
                isInitialized = true
                Log.d(TAG, "Places API initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Places API", e)
            }
        }
    }

    /**
     * Data class representing a nearby place
     */
    data class NearbyPlace(
        val id: String,
        val name: String,
        val address: String,
        val location: LatLng,
        val type: SafePlaceType,
        val rating: Float? = null,
        val userRatingsTotal: Int? = null,
        val isOpen: Boolean? = null,
        val phoneNumber: String? = null,
        val distance: Float? = null,
        val photoReference: String? = null,
        val websiteUri: String? = null
    )

    enum class SafePlaceType {
        POLICE_STATION,
        HOSPITAL,
        FIRE_STATION,
        PHARMACY,
        OTHER;

        fun toGooglePlaceType(): String = when (this) {
            POLICE_STATION -> PlaceTypes.POLICE
            HOSPITAL -> PlaceTypes.HOSPITAL
            FIRE_STATION -> PlaceTypes.FIRE_STATION
            PHARMACY -> PlaceTypes.PHARMACY
            OTHER -> ""
        }

        fun toDisplayName(): String = when (this) {
            POLICE_STATION -> "Police Station"
            HOSPITAL -> "Hospital"
            FIRE_STATION -> "Fire Station"
            PHARMACY -> "Pharmacy"
            OTHER -> "Place"
        }

        companion object {
            fun fromGoogleType(types: List<String>): SafePlaceType {
                return when {
                    types.contains(PlaceTypes.POLICE) -> POLICE_STATION
                    types.contains(PlaceTypes.HOSPITAL) -> HOSPITAL
                    types.contains(PlaceTypes.FIRE_STATION) -> FIRE_STATION
                    types.contains(PlaceTypes.PHARMACY) -> PHARMACY
                    else -> OTHER
                }
            }
        }
    }

    /**
     * Search result wrapper
     */
    sealed class PlacesResult {
        data class Success(val places: List<NearbyPlace>) : PlacesResult()
        data class Error(val message: String) : PlacesResult()
        object Loading : PlacesResult()
    }

    /**
     * Get nearby places of a specific type using Google Places API
     * Note: Using mock data as SearchNearbyRequest requires Places API (New) setup
     */
    suspend fun getNearbyPlaces(
        location: Location,
        type: SafePlaceType,
        radiusMeters: Int = DEFAULT_RADIUS
    ): PlacesResult = withContext(Dispatchers.IO) {
        // Use mock data - SearchNearbyRequest requires Places API (New) which needs additional setup
        Log.d(TAG, "Using mock data for nearby places")
        return@withContext PlacesResult.Success(generateMockPlaces(location, type))
    }

    /**
     * Get all nearby safety places (police, hospital, fire station, pharmacy)
     */
    suspend fun getAllNearbySafetyPlaces(
        location: Location,
        radiusMeters: Int = DEFAULT_RADIUS
    ): PlacesResult = withContext(Dispatchers.IO) {
        try {
            val allPlaces = mutableListOf<NearbyPlace>()
            
            SafePlaceType.values().filter { it != SafePlaceType.OTHER }.forEach { type ->
                val result = getNearbyPlaces(location, type, radiusMeters)
                if (result is PlacesResult.Success) {
                    allPlaces.addAll(result.places)
                }
            }
            
            // Sort by distance and remove duplicates
            val sortedPlaces = allPlaces
                .distinctBy { it.id }
                .sortedBy { it.distance }
            
            PlacesResult.Success(sortedPlaces)
        } catch (e: Exception) {
            PlacesResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Find the nearest place of a specific type
     */
    suspend fun findNearestPlace(
        location: Location,
        type: SafePlaceType
    ): NearbyPlace? {
        val result = getNearbyPlaces(location, type, MAX_RADIUS)
        return if (result is PlacesResult.Success) {
            result.places.minByOrNull { it.distance ?: Float.MAX_VALUE }
        } else null
    }

    /**
     * Get place details by ID
     */
    suspend fun getPlaceDetails(placeId: String): NearbyPlace? = withContext(Dispatchers.IO) {
        if (!isInitialized || placesClient == null) return@withContext null

        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI
            )

            val request = FetchPlaceRequest.newInstance(placeId, placeFields)

            val response = suspendCancellableCoroutine { continuation ->
                placesClient!!.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        continuation.resume(response)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Fetch place failed", exception)
                        continuation.resume(null)
                    }
            }

            response?.place?.let { place ->
                place.latLng?.let { latLng ->
                    NearbyPlace(
                        id = place.id ?: "",
                        name = place.name ?: "Unknown",
                        address = place.address ?: "",
                        location = latLng,
                        type = SafePlaceType.fromGoogleType(place.placeTypes ?: emptyList()),
                        rating = place.rating?.toFloat(),
                        userRatingsTotal = place.userRatingsTotal,
                        isOpen = place.currentOpeningHours?.let { true }, // Simplified - just check if hours exist
                        phoneNumber = place.phoneNumber,
                        websiteUri = place.websiteUri?.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching place details", e)
            null
        }
    }

    /**
     * Calculate distance between two points
     */
    fun calculateDistance(from: Location, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }

    /**
     * Format distance for display
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 1000 -> "${meters.toInt()} m"
            else -> String.format("%.1f km", meters / 1000)
        }
    }

    /**
     * Generate mock places for demonstration (fallback when API not available)
     */
    private fun generateMockPlaces(
        location: Location,
        type: SafePlaceType
    ): List<NearbyPlace> {
        val baseLat = location.latitude
        val baseLng = location.longitude
        
        val placeNames = when (type) {
            SafePlaceType.POLICE_STATION -> listOf(
                "City Police Station", "Central Police HQ", "Traffic Police Station",
                "Women's Police Station", "Cyber Crime Cell"
            )
            SafePlaceType.HOSPITAL -> listOf(
                "City General Hospital", "Emergency Care Center", "Apollo Hospital",
                "Max Healthcare", "Fortis Hospital"
            )
            SafePlaceType.FIRE_STATION -> listOf(
                "Central Fire Station", "City Fire Brigade", "Emergency Fire Services",
                "Fire & Rescue Station", "Municipal Fire Dept"
            )
            SafePlaceType.PHARMACY -> listOf(
                "24/7 Medical Store", "Apollo Pharmacy", "MedPlus",
                "Wellness Forever", "Guardian Pharmacy"
            )
            SafePlaceType.OTHER -> listOf("Safe Place")
        }

        return placeNames.mapIndexed { index, name ->
            val offsetLat = (Math.random() - 0.5) * 0.02
            val offsetLng = (Math.random() - 0.5) * 0.02
            val placeLat = baseLat + offsetLat
            val placeLng = baseLng + offsetLng
            val placeLocation = LatLng(placeLat, placeLng)
            
            val distance = calculateDistance(location, placeLocation)
            
            NearbyPlace(
                id = "${type.name}_$index",
                name = name,
                address = "Street ${(Math.random() * 100).toInt()}, Sector ${index + 1}",
                location = placeLocation,
                type = type,
                rating = (3.5f + Math.random().toFloat() * 1.5f).let { 
                    (it * 10).toInt() / 10f
                },
                userRatingsTotal = (100..2000).random(),
                isOpen = Math.random() > 0.2,
                phoneNumber = "+91 ${(1000000000..9999999999).random()}",
                distance = distance
            )
        }.sortedBy { it.distance }
    }
}
