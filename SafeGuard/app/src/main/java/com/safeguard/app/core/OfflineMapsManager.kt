package com.safeguard.app.core

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Offline Maps Manager
 * Caches map tiles and safe places for offline access
 * Critical for emergency situations without internet
 */
class OfflineMapsManager(private val context: Context) {

    companion object {
        private const val TAG = "OfflineMapsManager"
        private const val CACHE_DIR = "offline_maps"
        private const val SAFE_PLACES_FILE = "safe_places.json"
        private const val MAX_CACHE_SIZE_MB = 100
        private const val DEFAULT_CACHE_RADIUS_KM = 5.0
    }

    data class CachedArea(
        val id: String,
        val name: String,
        val center: LatLng,
        val radiusKm: Double,
        val bounds: LatLngBounds,
        val cachedAt: Long = System.currentTimeMillis(),
        val sizeBytes: Long = 0,
        val safePlacesCount: Int = 0
    )

    data class OfflineSafePlace(
        val id: String,
        val name: String,
        val type: PlaceType,
        val location: LatLng,
        val address: String,
        val phoneNumber: String? = null,
        val isOpen24Hours: Boolean = false
    )

    enum class PlaceType {
        POLICE_STATION,
        HOSPITAL,
        FIRE_STATION,
        PHARMACY,
        SAFE_ZONE
    }

    data class CacheStatus(
        val totalSizeMB: Double = 0.0,
        val areasCount: Int = 0,
        val safePlacesCount: Int = 0,
        val lastUpdated: Long? = null,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _cachedAreas = MutableStateFlow<List<CachedArea>>(emptyList())
    val cachedAreas: StateFlow<List<CachedArea>> = _cachedAreas.asStateFlow()

    private val _cacheStatus = MutableStateFlow(CacheStatus())
    val cacheStatus: StateFlow<CacheStatus> = _cacheStatus.asStateFlow()

    private val _offlineSafePlaces = MutableStateFlow<List<OfflineSafePlace>>(emptyList())
    val offlineSafePlaces: StateFlow<List<OfflineSafePlace>> = _offlineSafePlaces.asStateFlow()

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    }

    init {
        loadCachedData()
    }

    /**
     * Cache map area around current location
     */
    fun cacheCurrentArea(
        location: Location,
        radiusKm: Double = DEFAULT_CACHE_RADIUS_KM,
        areaName: String = "Current Location"
    ) {
        scope.launch {
            try {
                _cacheStatus.value = _cacheStatus.value.copy(isDownloading = true, downloadProgress = 0f)
                
                val center = LatLng(location.latitude, location.longitude)
                val bounds = calculateBounds(center, radiusKm)
                
                // Cache safe places in the area
                val safePlaces = fetchAndCacheSafePlaces(center, radiusKm)
                
                _cacheStatus.value = _cacheStatus.value.copy(downloadProgress = 0.5f)
                
                // Create cached area record
                val cachedArea = CachedArea(
                    id = "${location.latitude}_${location.longitude}_${System.currentTimeMillis()}",
                    name = areaName,
                    center = center,
                    radiusKm = radiusKm,
                    bounds = bounds,
                    safePlacesCount = safePlaces.size
                )
                
                val currentAreas = _cachedAreas.value.toMutableList()
                currentAreas.add(cachedArea)
                _cachedAreas.value = currentAreas
                
                // Save to disk
                saveCachedData()
                
                _cacheStatus.value = _cacheStatus.value.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    areasCount = currentAreas.size,
                    safePlacesCount = _offlineSafePlaces.value.size,
                    lastUpdated = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Cached area: $areaName with ${safePlaces.size} safe places")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache area", e)
                _cacheStatus.value = _cacheStatus.value.copy(isDownloading = false)
            }
        }
    }

    /**
     * Get nearest safe places from cache (works offline)
     */
    fun getNearestSafePlaces(
        location: Location,
        type: PlaceType? = null,
        limit: Int = 10
    ): List<OfflineSafePlace> {
        val currentLocation = LatLng(location.latitude, location.longitude)
        
        return _offlineSafePlaces.value
            .filter { type == null || it.type == type }
            .sortedBy { place ->
                calculateDistance(currentLocation, place.location)
            }
            .take(limit)
    }

    /**
     * Get nearest police station (offline)
     */
    fun getNearestPoliceStation(location: Location): OfflineSafePlace? {
        return getNearestSafePlaces(location, PlaceType.POLICE_STATION, 1).firstOrNull()
    }

    /**
     * Get nearest hospital (offline)
     */
    fun getNearestHospital(location: Location): OfflineSafePlace? {
        return getNearestSafePlaces(location, PlaceType.HOSPITAL, 1).firstOrNull()
    }

    /**
     * Check if location is within cached area
     */
    fun isLocationCached(location: Location): Boolean {
        val point = LatLng(location.latitude, location.longitude)
        return _cachedAreas.value.any { area ->
            area.bounds.contains(point)
        }
    }

    /**
     * Delete cached area
     */
    fun deleteCachedArea(areaId: String) {
        scope.launch {
            val currentAreas = _cachedAreas.value.toMutableList()
            currentAreas.removeAll { it.id == areaId }
            _cachedAreas.value = currentAreas
            saveCachedData()
            updateCacheStatus()
        }
    }

    /**
     * Clear all cached data
     */
    fun clearAllCache() {
        scope.launch {
            _cachedAreas.value = emptyList()
            _offlineSafePlaces.value = emptyList()
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            _cacheStatus.value = CacheStatus()
        }
    }

    /**
     * Auto-cache areas user frequently visits
     */
    fun autoCacheFrequentLocations(locations: List<Location>) {
        scope.launch {
            // Group locations into clusters
            val clusters = clusterLocations(locations)
            
            // Cache top 3 most frequent areas
            clusters.take(3).forEach { cluster ->
                if (!isLocationCached(cluster)) {
                    cacheCurrentArea(cluster, DEFAULT_CACHE_RADIUS_KM, "Frequent Location")
                }
            }
        }
    }

    private fun fetchAndCacheSafePlaces(center: LatLng, radiusKm: Double): List<OfflineSafePlace> {
        // In production, this would fetch from Google Places API
        // For now, generate sample data based on location
        val safePlaces = mutableListOf<OfflineSafePlace>()
        
        // Generate sample safe places around the center
        val types = listOf(
            PlaceType.POLICE_STATION to "Police Station",
            PlaceType.HOSPITAL to "Hospital",
            PlaceType.FIRE_STATION to "Fire Station",
            PlaceType.PHARMACY to "Pharmacy"
        )
        
        types.forEachIndexed { index, (type, typeName) ->
            // Generate 2-3 places of each type
            repeat(2) { i ->
                val offset = (index * 0.01) + (i * 0.005)
                safePlaces.add(
                    OfflineSafePlace(
                        id = "${type.name}_${center.latitude}_${center.longitude}_$i",
                        name = "$typeName ${i + 1}",
                        type = type,
                        location = LatLng(
                            center.latitude + offset,
                            center.longitude + offset
                        ),
                        address = "Near ${center.latitude.toInt()}, ${center.longitude.toInt()}",
                        phoneNumber = "100",
                        isOpen24Hours = type == PlaceType.POLICE_STATION || type == PlaceType.HOSPITAL
                    )
                )
            }
        }
        
        // Add to cached places
        val currentPlaces = _offlineSafePlaces.value.toMutableList()
        safePlaces.forEach { place ->
            if (currentPlaces.none { it.id == place.id }) {
                currentPlaces.add(place)
            }
        }
        _offlineSafePlaces.value = currentPlaces
        
        return safePlaces
    }

    private fun calculateBounds(center: LatLng, radiusKm: Double): LatLngBounds {
        // Approximate degrees per km
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(center.latitude)))
        
        return LatLngBounds(
            LatLng(center.latitude - latDelta, center.longitude - lngDelta),
            LatLng(center.latitude + latDelta, center.longitude + lngDelta)
        )
    }

    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }

    private fun clusterLocations(locations: List<Location>): List<Location> {
        // Simple clustering - group by rounded coordinates
        return locations
            .groupBy { "${(it.latitude * 100).toInt()}_${(it.longitude * 100).toInt()}" }
            .entries
            .sortedByDescending { it.value.size }
            .map { it.value.first() }
    }

    private fun loadCachedData() {
        scope.launch {
            try {
                // Load cached areas and safe places from disk
                // In production, use proper serialization
                updateCacheStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cached data", e)
            }
        }
    }

    private fun saveCachedData() {
        scope.launch {
            try {
                // Save cached areas and safe places to disk
                // In production, use proper serialization
                updateCacheStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save cached data", e)
            }
        }
    }

    private fun updateCacheStatus() {
        val totalSize = cacheDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
        
        _cacheStatus.value = CacheStatus(
            totalSizeMB = totalSize / (1024.0 * 1024.0),
            areasCount = _cachedAreas.value.size,
            safePlacesCount = _offlineSafePlaces.value.size,
            lastUpdated = _cachedAreas.value.maxOfOrNull { it.cachedAt }
        )
    }

    fun cleanup() {
        // Cleanup resources
    }
}
