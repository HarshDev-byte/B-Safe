package com.safeguard.app.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.safeguard.app.data.models.EmergencyContact
import com.safeguard.app.data.models.SOSEvent
import com.safeguard.app.data.models.UserSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore repository for cloud data sync
 */
class FirebaseRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    
    companion object {
        private const val TAG = "FirebaseRepository"
        private const val USERS_COLLECTION = "users"
        private const val CONTACTS_COLLECTION = "emergencyContacts"
        private const val EVENTS_COLLECTION = "sosEvents"
        private const val SETTINGS_COLLECTION = "settings"
        private const val LIVE_LOCATIONS_COLLECTION = "liveLocations"
    }
    
    // ==================== Emergency Contacts ====================
    
    /**
     * Sync emergency contacts to Firebase
     */
    suspend fun syncContacts(contacts: List<EmergencyContact>): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val batch = firestore.batch()
            val contactsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CONTACTS_COLLECTION)
            
            // Delete existing contacts
            val existingContacts = contactsRef.get().await()
            existingContacts.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Add new contacts
            contacts.forEach { contact ->
                val docRef = contactsRef.document(contact.id.toString())
                batch.set(docRef, contact.toFirebaseMap())
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync contacts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get contacts from Firebase
     */
    suspend fun getContacts(): Result<List<EmergencyContact>> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CONTACTS_COLLECTION)
                .get()
                .await()
            
            val contacts = snapshot.documents.mapNotNull { doc ->
                doc.toEmergencyContact()
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contacts", e)
            Result.failure(e)
        }
    }
    
    // ==================== SOS Events ====================
    
    /**
     * Save SOS event to Firebase
     */
    suspend fun saveSosEvent(event: SOSEvent): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(event.id.toString())
                .set(event.toFirebaseMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save SOS event", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get SOS events from Firebase
     */
    suspend fun getSosEvents(): Result<List<SOSEvent>> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toSOSEvent()
            }
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SOS events", e)
            Result.failure(e)
        }
    }
    
    // ==================== Live Location Sharing ====================
    
    /**
     * Update live location in Firebase (for trusted contacts to view)
     */
    suspend fun updateLiveLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        batteryLevel: Int,
        isSOSActive: Boolean
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val locationData = hashMapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy" to accuracy,
                "batteryLevel" to batteryLevel,
                "isSOSActive" to isSOSActive,
                "timestamp" to System.currentTimeMillis(),
                "userId" to userId
            )
            
            firestore.collection(LIVE_LOCATIONS_COLLECTION)
                .document(userId)
                .set(locationData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update live location", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop sharing live location
     */
    suspend fun stopLiveLocationSharing(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            firestore.collection(LIVE_LOCATIONS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop live location sharing", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen to a user's live location (for trusted contacts)
     */
    fun observeLiveLocation(userId: String): Flow<LiveLocationData?> = callbackFlow {
        val listener = firestore.collection(LIVE_LOCATIONS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Live location listener error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = snapshot?.data?.let { map ->
                    LiveLocationData(
                        latitude = map["latitude"] as? Double ?: 0.0,
                        longitude = map["longitude"] as? Double ?: 0.0,
                        accuracy = (map["accuracy"] as? Number)?.toFloat() ?: 0f,
                        batteryLevel = (map["batteryLevel"] as? Number)?.toInt() ?: 0,
                        isSOSActive = map["isSOSActive"] as? Boolean ?: false,
                        timestamp = map["timestamp"] as? Long ?: 0
                    )
                }
                trySend(data)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ==================== User Settings ====================
    
    /**
     * Sync user settings to Firebase
     */
    suspend fun syncSettings(settings: UserSettings): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document("userSettings")
                .set(settings.toFirebaseMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync settings", e)
            Result.failure(e)
        }
    }
}

/**
 * Live location data class
 */
data class LiveLocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val batteryLevel: Int,
    val isSOSActive: Boolean,
    val timestamp: Long
)

// Extension functions for Firebase conversion
private fun EmergencyContact.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "phoneNumber" to phoneNumber,
    "relationship" to relationship,
    "isPrimary" to isPrimary,
    "enableSMS" to enableSMS,
    "enableCall" to enableCall,
    "enableLiveLocation" to enableLiveLocation
)

private fun com.google.firebase.firestore.DocumentSnapshot.toEmergencyContact(): EmergencyContact? {
    return try {
        EmergencyContact(
            id = getLong("id") ?: 0,
            name = getString("name") ?: "",
            phoneNumber = getString("phoneNumber") ?: "",
            relationship = getString("relationship") ?: "",
            isPrimary = getBoolean("isPrimary") ?: false,
            enableSMS = getBoolean("enableSMS") ?: true,
            enableCall = getBoolean("enableCall") ?: false,
            enableLiveLocation = getBoolean("enableLiveLocation") ?: false
        )
    } catch (e: Exception) {
        null
    }
}

private fun SOSEvent.toFirebaseMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "timestamp" to timestamp,
    "latitude" to latitude,
    "longitude" to longitude,
    "accuracy" to accuracy,
    "address" to address,
    "batteryLevel" to batteryLevel,
    "isCharging" to isCharging,
    "networkType" to networkType,
    "triggerType" to triggerType.name,
    "status" to status.name,
    "smsSentCount" to smsSentCount,
    "callsMadeCount" to callsMadeCount,
    "endTimestamp" to endTimestamp,
    "notes" to notes
)

private fun com.google.firebase.firestore.DocumentSnapshot.toSOSEvent(): SOSEvent? {
    return try {
        SOSEvent(
            id = getLong("id") ?: 0,
            triggerType = com.safeguard.app.data.models.TriggerType.valueOf(
                getString("triggerType") ?: "MANUAL_BUTTON"
            ),
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
            accuracy = (get("accuracy") as? Number)?.toFloat(),
            address = getString("address"),
            batteryLevel = getLong("batteryLevel")?.toInt() ?: 0,
            isCharging = getBoolean("isCharging") ?: false,
            networkType = getString("networkType"),
            timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
            endTimestamp = getLong("endTimestamp"),
            status = com.safeguard.app.data.models.SOSStatus.valueOf(
                getString("status") ?: "ACTIVE"
            ),
            smsSentCount = getLong("smsSentCount")?.toInt() ?: 0,
            callsMadeCount = getLong("callsMadeCount")?.toInt() ?: 0,
            notes = getString("notes")
        )
    } catch (e: Exception) {
        null
    }
}

private fun UserSettings.toFirebaseMap(): Map<String, Any?> = mapOf(
    "userName" to userName,
    "bloodGroup" to bloodGroup,
    "medicalNotes" to medicalNotes,
    "enableVolumeButtonTrigger" to enableVolumeButtonTrigger,
    "enableShakeTrigger" to enableShakeTrigger,
    "enablePowerButtonTrigger" to enablePowerButtonTrigger,
    "enableWidgetTrigger" to enableWidgetTrigger,
    "enableSirenOnSOS" to enableSirenOnSOS,
    "enableFlashlightOnSOS" to enableFlashlightOnSOS,
    "enableAutoCall" to enableAutoCall,
    "sosCountdownSeconds" to sosCountdownSeconds
)
