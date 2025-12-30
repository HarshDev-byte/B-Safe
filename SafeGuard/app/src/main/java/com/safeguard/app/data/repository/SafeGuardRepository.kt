package com.safeguard.app.data.repository

import com.safeguard.app.data.local.*
import com.safeguard.app.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SafeGuardRepository(
    private val database: SafeGuardDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    // Emergency Contacts
    fun getAllContacts(): Flow<List<EmergencyContact>> = 
        database.emergencyContactDao().getAllContacts()

    suspend fun getAllContactsList(): List<EmergencyContact> = 
        database.emergencyContactDao().getAllContactsList()

    suspend fun getPrimaryContact(): EmergencyContact? = 
        database.emergencyContactDao().getPrimaryContact()

    suspend fun getSMSEnabledContacts(): List<EmergencyContact> = 
        database.emergencyContactDao().getSMSEnabledContacts()

    suspend fun getCallEnabledContacts(): List<EmergencyContact> = 
        database.emergencyContactDao().getCallEnabledContacts()

    suspend fun getContactById(id: Long): EmergencyContact? = 
        database.emergencyContactDao().getContactById(id)

    suspend fun insertContact(contact: EmergencyContact): Long = 
        database.emergencyContactDao().insertContact(contact)

    suspend fun updateContact(contact: EmergencyContact) = 
        database.emergencyContactDao().updateContact(contact)

    suspend fun deleteContact(contact: EmergencyContact) = 
        database.emergencyContactDao().deleteContact(contact)

    suspend fun deleteContactById(id: Long) = 
        database.emergencyContactDao().deleteContactById(id)

    suspend fun getContactCount(): Int = 
        database.emergencyContactDao().getContactCount()

    // SOS Events
    fun getAllSOSEvents(): Flow<List<SOSEvent>> = 
        database.sosEventDao().getAllEvents()

    suspend fun getRecentSOSEvents(limit: Int): List<SOSEvent> = 
        database.sosEventDao().getRecentEvents(limit)

    suspend fun getActiveSOSEvent(): SOSEvent? = 
        database.sosEventDao().getActiveEvent()

    suspend fun getSOSEventById(id: Long): SOSEvent? = 
        database.sosEventDao().getEventById(id)

    suspend fun insertSOSEvent(event: SOSEvent): Long = 
        database.sosEventDao().insertEvent(event)

    suspend fun updateSOSEvent(event: SOSEvent) = 
        database.sosEventDao().updateEvent(event)

    suspend fun updateSOSEventStatus(id: Long, status: SOSStatus) = 
        database.sosEventDao().updateEventStatus(id, status, System.currentTimeMillis())

    suspend fun incrementSMSCount(eventId: Long) = 
        database.sosEventDao().incrementSMSCount(eventId)

    suspend fun incrementCallCount(eventId: Long) = 
        database.sosEventDao().incrementCallCount(eventId)

    suspend fun deleteOldSOSEvents(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        database.sosEventDao().deleteOldEvents(cutoffTime)
    }

    // Location Updates
    fun getLocationUpdatesForEvent(eventId: Long): Flow<List<LocationUpdate>> = 
        database.locationUpdateDao().getUpdatesForEvent(eventId)

    suspend fun getLocationUpdatesForEventList(eventId: Long): List<LocationUpdate> = 
        database.locationUpdateDao().getUpdatesForEventList(eventId)

    suspend fun getLatestLocationUpdate(eventId: Long): LocationUpdate? = 
        database.locationUpdateDao().getLatestUpdate(eventId)

    suspend fun insertLocationUpdate(update: LocationUpdate): Long = 
        database.locationUpdateDao().insertUpdate(update)

    suspend fun getUnsyncedLocationUpdates(): List<LocationUpdate> = 
        database.locationUpdateDao().getUnsyncedUpdates()

    suspend fun markLocationUpdateSynced(id: Long) = 
        database.locationUpdateDao().markAsSynced(id)

    // Danger Zones
    fun getAllDangerZones(): Flow<List<DangerZone>> = 
        database.dangerZoneDao().getAllZones()

    fun getEnabledDangerZones(): Flow<List<DangerZone>> = 
        database.dangerZoneDao().getEnabledZones()

    suspend fun getEnabledDangerZonesList(): List<DangerZone> = 
        database.dangerZoneDao().getEnabledZonesList()

    suspend fun getDangerZoneById(id: Long): DangerZone? = 
        database.dangerZoneDao().getZoneById(id)

    suspend fun insertDangerZone(zone: DangerZone): Long = 
        database.dangerZoneDao().insertZone(zone)

    suspend fun updateDangerZone(zone: DangerZone) = 
        database.dangerZoneDao().updateZone(zone)

    suspend fun deleteDangerZone(zone: DangerZone) = 
        database.dangerZoneDao().deleteZone(zone)

    // Scheduled Check-ins
    fun getAllCheckIns(): Flow<List<ScheduledCheckIn>> = 
        database.scheduledCheckInDao().getAllCheckIns()

    fun getEnabledCheckIns(): Flow<List<ScheduledCheckIn>> = 
        database.scheduledCheckInDao().getEnabledCheckIns()

    suspend fun getCheckInById(id: Long): ScheduledCheckIn? = 
        database.scheduledCheckInDao().getCheckInById(id)

    suspend fun getPendingCheckInsBeforeTime(time: Long): List<ScheduledCheckIn> = 
        database.scheduledCheckInDao().getPendingCheckInsBeforeTime(time)

    suspend fun insertCheckIn(checkIn: ScheduledCheckIn): Long = 
        database.scheduledCheckInDao().insertCheckIn(checkIn)

    suspend fun updateCheckIn(checkIn: ScheduledCheckIn) = 
        database.scheduledCheckInDao().updateCheckIn(checkIn)

    suspend fun updateCheckInStatus(id: Long, status: CheckInStatus) = 
        database.scheduledCheckInDao().updateCheckInStatus(id, status)

    suspend fun deleteCheckIn(checkIn: ScheduledCheckIn) = 
        database.scheduledCheckInDao().deleteCheckIn(checkIn)

    // User Settings
    fun getUserSettings(): Flow<UserSettings> = settingsDataStore.userSettings

    fun getRegionalSettings(): Flow<RegionalSettings> = settingsDataStore.regionalSettings

    fun isSOSActive(): Flow<Boolean> = settingsDataStore.isSOSActive

    fun getActiveSOSEventId(): Flow<Long> = settingsDataStore.activeSOSEventId

    suspend fun updateUserSettings(settings: UserSettings) = 
        settingsDataStore.updateUserSettings(settings)

    suspend fun updateUserSettings(update: (UserSettings) -> UserSettings) = 
        settingsDataStore.updateUserSettings(update)

    suspend fun updateRegionalSettings(settings: RegionalSettings) = 
        settingsDataStore.updateRegionalSettings(settings)

    suspend fun setSOSActive(active: Boolean, eventId: Long = 0L) = 
        settingsDataStore.setSOSActive(active, eventId)

    suspend fun getUserSettingsOnce(): UserSettings = 
        settingsDataStore.userSettings.first()

    suspend fun getRegionalSettingsOnce(): RegionalSettings = 
        settingsDataStore.regionalSettings.first()
}
