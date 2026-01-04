package com.safeguard.app

import android.app.Application
import android.content.pm.PackageManager
import com.google.firebase.FirebaseApp
import com.safeguard.app.auth.AuthManager
import com.safeguard.app.core.*
import com.safeguard.app.ai.*
import com.safeguard.app.data.local.SafeGuardDatabase
import com.safeguard.app.data.local.SettingsDataStore
import com.safeguard.app.data.repository.SafeGuardRepository

class SafeGuardApplication : Application() {

    lateinit var database: SafeGuardDatabase
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var repository: SafeGuardRepository
        private set

    lateinit var locationManager: LocationManager
        private set

    lateinit var alertManager: AlertManager
        private set

    lateinit var triggerDetector: TriggerDetector
        private set

    lateinit var sosManager: SOSManager
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var placesManager: PlacesManager
        private set

    // Premium Managers
    lateinit var safetyScoreManager: SafetyScoreManager
        private set

    lateinit var journeyMonitor: JourneyMonitor
        private set

    lateinit var audioEvidenceManager: AudioEvidenceManager
        private set

    lateinit var quickEscapeManager: QuickEscapeManager
        private set

    // AI Managers
    lateinit var threatDetectionAI: ThreatDetectionAI
        private set

    lateinit var smartSafetyAssistant: SmartSafetyAssistant
        private set

    lateinit var voiceCommandAI: VoiceCommandAI
        private set

    // Advanced Safety Managers
    lateinit var safeWalkManager: SafeWalkManager
        private set

    lateinit var crowdSourcedSafetyManager: CrowdSourcedSafetyManager
        private set

    lateinit var wearableManager: WearableManager
        private set

    lateinit var guardianCircleManager: GuardianCircleManager
        private set

    lateinit var emergencyNetworkManager: EmergencyNetworkManager
        private set

    // Offline & Hardware Managers
    lateinit var offlineMapsManager: OfflineMapsManager
        private set

    lateinit var panicButtonManager: PanicButtonManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize database
        database = SafeGuardDatabase.getDatabase(this)

        // Initialize DataStore
        settingsDataStore = SettingsDataStore(this)

        // Initialize repository
        repository = SafeGuardRepository(database, settingsDataStore)

        // Initialize managers
        locationManager = LocationManager(this)
        alertManager = AlertManager(this)
        triggerDetector = TriggerDetector(this)
        authManager = AuthManager(this)

        // Initialize Places Manager
        placesManager = PlacesManager(this)
        initializePlacesApi()

        // Initialize SOS Manager (depends on other managers)
        sosManager = SOSManager(
            context = this,
            repository = repository,
            locationManager = locationManager,
            alertManager = alertManager
        )

        // Initialize Premium Managers
        safetyScoreManager = SafetyScoreManager(this)
        journeyMonitor = JourneyMonitor(this, locationManager, sosManager)
        audioEvidenceManager = AudioEvidenceManager(this)
        quickEscapeManager = QuickEscapeManager(this)

        // Initialize AI Managers
        threatDetectionAI = ThreatDetectionAI(this, sosManager)
        smartSafetyAssistant = SmartSafetyAssistant(this)
        voiceCommandAI = VoiceCommandAI(this, sosManager)

        // Initialize Advanced Safety Managers
        safeWalkManager = SafeWalkManager(this, locationManager, sosManager)
        crowdSourcedSafetyManager = CrowdSourcedSafetyManager(this)
        wearableManager = WearableManager(this, sosManager)
        guardianCircleManager = GuardianCircleManager(this, locationManager, sosManager)
        emergencyNetworkManager = EmergencyNetworkManager(this)

        // Initialize Offline & Hardware Managers
        offlineMapsManager = OfflineMapsManager(this)
        panicButtonManager = PanicButtonManager(this, sosManager)
    }

    /**
     * Initialize Google Places API with the API key from manifest
     */
    private fun initializePlacesApi() {
        try {
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
            if (!apiKey.isNullOrBlank()) {
                placesManager.initialize(apiKey)
            }
        } catch (e: Exception) {
            // Places API will use mock data if initialization fails
        }
    }

    override fun onTerminate() {
        sosManager.cleanup()
        alertManager.cleanup()
        triggerDetector.cleanup()
        journeyMonitor.cleanup()
        threatDetectionAI.cleanup()
        voiceCommandAI.cleanup()
        safeWalkManager.cleanup()
        crowdSourcedSafetyManager.cleanup()
        wearableManager.cleanup()
        guardianCircleManager.cleanup()
        panicButtonManager.cleanup()
        offlineMapsManager.cleanup()
        super.onTerminate()
    }
}
