package com.safeguard.app.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.safeguard.app.SafeGuardApplication
import com.safeguard.app.auth.AuthManager
import com.safeguard.app.core.JourneyMonitor
import com.safeguard.app.core.SafetyScoreManager
import com.safeguard.app.core.SOSManager
import com.safeguard.app.data.firebase.FirebaseRepository
import com.safeguard.app.data.models.*
import com.safeguard.app.data.repository.SafeGuardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SafeGuardApplication
    private val repository: SafeGuardRepository = app.repository
    private val sosManager: SOSManager = app.sosManager
    private val authManager: AuthManager = app.authManager
    private val firebaseRepository: FirebaseRepository = FirebaseRepository()
    private val locationManager = app.locationManager
    
    // New premium managers
    private val safetyScoreManager = SafetyScoreManager(application)
    private val journeyMonitor = JourneyMonitor(application, locationManager, sosManager)

    companion object {
        private const val TAG = "MainViewModel"
    }

    val sosState: StateFlow<SOSManager.SOSState> = sosManager.sosState
    val currentSOSEvent: StateFlow<SOSEvent?> = sosManager.currentEvent

    val userSettings: StateFlow<UserSettings> = repository.getUserSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val regionalSettings: StateFlow<RegionalSettings> = repository.getRegionalSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, RegionalSettings())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sosEvents: StateFlow<List<SOSEvent>> = repository.getAllSOSEvents()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dangerZones: StateFlow<List<DangerZone>> = repository.getAllDangerZones()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val scheduledCheckIns: StateFlow<List<ScheduledCheckIn>> = repository.getAllCheckIns()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Auth state
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Current location for map
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Safety Score
    val safetyScore: StateFlow<SafetyScoreManager.SafetyScore> = safetyScoreManager.safetyScore
    
    // Journey Monitor
    val activeJourney: StateFlow<JourneyMonitor.Journey?> = journeyMonitor.activeJourney
    val journeyHistory: StateFlow<List<JourneyMonitor.Journey>> = journeyMonitor.journeyHistory

    init {
        // Observe auth state
        viewModelScope.launch {
            authManager.authStateFlow.collect { user ->
                _currentUser.value = user
                if (user != null) {
                    // Sync data from cloud when user signs in
                    syncFromCloud()
                }
            }
        }

        // Observe user settings
        viewModelScope.launch {
            userSettings.collect { settings ->
                _uiState.update { it.copy(isOnboardingComplete = settings.isOnboardingComplete) }
            }
        }
        
        // Calculate safety score when data changes
        viewModelScope.launch {
            combine(
                emergencyContacts,
                userSettings,
                dangerZones,
                sosEvents
            ) { contacts, settings, zones, events ->
                calculateSafetyScore(contacts, settings, zones.size, events.size)
            }.collect()
        }

        // Start location updates
        startLocationUpdates()
    }
    
    // ==================== Safety Score ====================
    
    private fun calculateSafetyScore(
        contacts: List<EmergencyContact>,
        settings: UserSettings,
        dangerZonesCount: Int,
        sosEventsCount: Int
    ) {
        // Check permissions (simplified - in real app check actual permissions)
        val hasLocationPermission = true
        val hasSmsPermission = true
        val hasCallPermission = true
        
        safetyScoreManager.calculateScore(
            contacts = contacts,
            settings = settings,
            hasLocationPermission = hasLocationPermission,
            hasSmsPermission = hasSmsPermission,
            hasCallPermission = hasCallPermission,
            dangerZonesCount = dangerZonesCount,
            sosEventsCount = sosEventsCount
        )
    }
    
    // ==================== Journey Monitor ====================
    
    fun startJourney(
        destination: LatLng,
        destinationName: String,
        expectedArrivalMinutes: Int,
        graceMinutes: Int = 15,
        autoSOS: Boolean = false
    ) {
        journeyMonitor.startJourney(
            destination = destination,
            destinationName = destinationName,
            expectedArrivalMinutes = expectedArrivalMinutes,
            graceMinutes = graceMinutes,
            notifyContacts = true,
            autoSOS = autoSOS
        )
    }
    
    fun confirmJourneyArrival() {
        journeyMonitor.confirmArrival()
    }
    
    fun cancelJourney() {
        journeyMonitor.cancelJourney()
    }
    
    fun extendJourneyTime(additionalMinutes: Int) {
        journeyMonitor.extendTime(additionalMinutes)
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                // Get initial location
                val initialLocation = locationManager.getCurrentLocation()
                initialLocation?.let {
                    _currentLocation.value = LatLng(it.latitude, it.longitude)
                }
                
                // Start continuous updates
                locationManager.getLocationUpdates().collect { location ->
                    _currentLocation.value = LatLng(location.latitude, location.longitude)
                    
                    // Update live location in Firebase if SOS is active
                    if (sosState.value is SOSManager.SOSState.Active && _currentUser.value != null) {
                        firebaseRepository.updateLiveLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            batteryLevel = getBatteryLevel(),
                            isSOSActive = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Location updates failed", e)
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation()
                location?.let {
                    _currentLocation.value = LatLng(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh location", e)
            }
        }
    }

    /**
     * Get current location as Android Location object
     */
    suspend fun getCurrentLocation(): android.location.Location? {
        return try {
            locationManager.getCurrentLocation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location", e)
            null
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = app.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    // ==================== Auth Functions ====================

    fun signInWithGoogleToken(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = authManager.firebaseAuthWithGoogle(idToken)
                _uiState.update { it.copy(isLoading = false) }
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        syncFromCloud()
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Sign in failed", error)
                        onError(error.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sign in crashed", e)
                _uiState.update { it.copy(isLoading = false) }
                onError("Sign in failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    // Keep old method for compatibility but redirect to skip
    fun signInWithGoogle(activityContext: android.content.Context? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onError("Please use the Google Sign-In button")
    }

    fun signOut() {
        authManager.signOut()
        _currentUser.value = null
    }

    fun deleteAccount() {
        viewModelScope.launch {
            authManager.deleteAccount()
            _currentUser.value = null
        }
    }

    // ==================== Cloud Sync ====================

    fun syncToCloud() {
        if (_currentUser.value == null) return
        
        viewModelScope.launch {
            try {
                // Sync contacts
                firebaseRepository.syncContacts(emergencyContacts.value)
                
                // Sync settings
                firebaseRepository.syncSettings(userSettings.value)
                
                // Sync SOS events
                sosEvents.value.forEach { event ->
                    firebaseRepository.saveSosEvent(event)
                }
                
                Log.d(TAG, "Cloud sync completed")
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync failed", e)
            }
        }
    }

    private fun syncFromCloud() {
        if (_currentUser.value == null) return
        
        viewModelScope.launch {
            try {
                // Get contacts from cloud
                val cloudContacts = firebaseRepository.getContacts().getOrNull()
                cloudContacts?.forEach { contact ->
                    repository.insertContact(contact)
                }
                
                Log.d(TAG, "Cloud sync from server completed")
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync from server failed", e)
            }
        }
    }

    // ==================== SOS Functions ====================

    fun triggerSOS(triggerType: TriggerType = TriggerType.MANUAL_BUTTON) {
        viewModelScope.launch {
            sosManager.triggerSOS(triggerType)
            
            // Start sharing live location if signed in
            if (_currentUser.value != null) {
                _currentLocation.value?.let { location ->
                    firebaseRepository.updateLiveLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = 0f,
                        batteryLevel = getBatteryLevel(),
                        isSOSActive = true
                    )
                }
            }
        }
    }

    fun cancelSOS() {
        viewModelScope.launch {
            sosManager.cancelSOS()
            
            // Stop sharing live location
            if (_currentUser.value != null) {
                firebaseRepository.stopLiveLocationSharing()
            }
        }
    }

    fun cancelCountdown() {
        sosManager.cancelCountdown()
    }

    // ==================== Contact Management ====================

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.insertContact(contact)
            syncToCloud()
        }
    }

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(contact)
            syncToCloud()
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            syncToCloud()
        }
    }

    // ==================== Settings Management ====================

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.updateUserSettings(settings)
            syncToCloud()
        }
    }

    fun updateUserSettings(update: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            repository.updateUserSettings(update)
        }
    }

    fun updateRegionalSettings(settings: RegionalSettings) {
        viewModelScope.launch {
            repository.updateRegionalSettings(settings)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.updateUserSettings { it.copy(isOnboardingComplete = true) }
        }
    }

    // ==================== Danger Zone Management ====================

    fun addDangerZone(zone: DangerZone) {
        viewModelScope.launch {
            repository.insertDangerZone(zone)
        }
    }

    fun updateDangerZone(zone: DangerZone) {
        viewModelScope.launch {
            repository.updateDangerZone(zone)
        }
    }

    fun deleteDangerZone(zone: DangerZone) {
        viewModelScope.launch {
            repository.deleteDangerZone(zone)
        }
    }

    // ==================== Check-in Management ====================

    fun addCheckIn(checkIn: ScheduledCheckIn) {
        viewModelScope.launch {
            repository.insertCheckIn(checkIn)
        }
    }

    fun updateCheckIn(checkIn: ScheduledCheckIn) {
        viewModelScope.launch {
            repository.updateCheckIn(checkIn)
        }
    }

    fun deleteCheckIn(checkIn: ScheduledCheckIn) {
        viewModelScope.launch {
            repository.deleteCheckIn(checkIn)
        }
    }

    fun confirmCheckIn(checkInId: Long) {
        viewModelScope.launch {
            repository.updateCheckInStatus(checkInId, CheckInStatus.CHECKED_IN)
        }
    }

    // ==================== Cleanup ====================

    fun cleanupOldData() {
        viewModelScope.launch {
            val settings = userSettings.value
            repository.deleteOldSOSEvents(settings.autoDeleteLogsAfterDays)
        }
    }
}

data class MainUiState(
    val isOnboardingComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
