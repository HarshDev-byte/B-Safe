package com.safeguard.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000L // 10 seconds
    ).apply {
        setMinUpdateIntervalMillis(5000L)
        setWaitForAccurateLocation(true)
    }.build()

    private var locationCallback: LocationCallback? = null

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            android.util.Log.w("LocationManager", "No location permission")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                // Request fresh high-accuracy location
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("LocationManager", "Got fresh location: ${location.latitude}, ${location.longitude}")
                        continuation.resume(location)
                    } else {
                        // Fallback to last known location
                        android.util.Log.d("LocationManager", "Fresh location null, trying last known")
                        getLastKnownLocation { lastLocation ->
                            continuation.resume(lastLocation)
                        }
                    }
                }.addOnFailureListener { exception ->
                    android.util.Log.e("LocationManager", "Failed to get location: ${exception.message}")
                    // Try to get last known location as fallback
                    getLastKnownLocation { lastLocation ->
                        continuation.resume(lastLocation)
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("LocationManager", "Security exception: ${e.message}")
                continuation.resume(null)
            }
        }
    }

    /**
     * Force request a fresh location update - useful for SOS periodic updates
     */
    suspend fun getFreshLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val freshLocationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L // 1 second interval for fresh request
            ).apply {
                setMaxUpdates(1) // Only need one update
                setWaitForAccurateLocation(false) // Don't wait, get whatever is available
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    android.util.Log.d("LocationManager", "Fresh location received: ${location?.latitude}, ${location?.longitude}")
                    continuation.resume(location)
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    freshLocationRequest,
                    callback,
                    Looper.getMainLooper()
                )
                
                // Timeout after 10 seconds
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    if (continuation.isActive) {
                        fusedLocationClient.removeLocationUpdates(callback)
                        android.util.Log.w("LocationManager", "Fresh location timeout, using last known")
                        getLastKnownLocation { lastLocation ->
                            if (continuation.isActive) {
                                continuation.resume(lastLocation)
                            }
                        }
                    }
                }, 10000)
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }

    private fun getLastKnownLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    callback(location)
                }
                .addOnFailureListener {
                    callback(null)
                }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            stopLocationUpdates()
        }
    }

    fun startContinuousTracking(callback: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    fun getAddressFromLocation(location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var address: String? = null
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    address = formatAddress(addresses.firstOrNull())
                }
                address
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                formatAddress(addresses?.firstOrNull())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAddress(address: Address?): String? {
        return address?.let {
            buildString {
                if (!it.thoroughfare.isNullOrEmpty()) {
                    append(it.thoroughfare)
                }
                if (!it.subLocality.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(it.subLocality)
                }
                if (!it.locality.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(it.locality)
                }
                if (!it.adminArea.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(it.adminArea)
                }
                if (!it.countryName.isNullOrEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(it.countryName)
                }
            }.takeIf { it.isNotEmpty() }
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            hasLocationPermission()
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}
