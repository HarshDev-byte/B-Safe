package com.safeguard.app.data.local

import androidx.room.*
import com.safeguard.app.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, isPrimary DESC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, isPrimary DESC")
    suspend fun getAllContactsList(): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryContact(): EmergencyContact?

    @Query("SELECT * FROM emergency_contacts WHERE enableSMS = 1")
    suspend fun getSMSEnabledContacts(): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE enableCall = 1 ORDER BY priority ASC")
    suspend fun getCallEnabledContacts(): List<EmergencyContact>

    @Query("SELECT * FROM emergency_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): EmergencyContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getContactCount(): Int
}

@Dao
interface SOSEventDao {
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SOSEvent>>

    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<SOSEvent>

    @Query("SELECT * FROM sos_events WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getEventsByStatus(status: SOSStatus): List<SOSEvent>

    @Query("SELECT * FROM sos_events WHERE status = 'ACTIVE' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActiveEvent(): SOSEvent?

    @Query("SELECT * FROM sos_events WHERE id = :id")
    suspend fun getEventById(id: Long): SOSEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SOSEvent): Long

    @Update
    suspend fun updateEvent(event: SOSEvent)

    @Query("UPDATE sos_events SET status = :status, endTimestamp = :endTime WHERE id = :id")
    suspend fun updateEventStatus(id: Long, status: SOSStatus, endTime: Long)

    @Query("UPDATE sos_events SET smsSentCount = smsSentCount + 1 WHERE id = :id")
    suspend fun incrementSMSCount(id: Long)

    @Query("UPDATE sos_events SET callsMadeCount = callsMadeCount + 1 WHERE id = :id")
    suspend fun incrementCallCount(id: Long)

    @Delete
    suspend fun deleteEvent(event: SOSEvent)

    @Query("DELETE FROM sos_events WHERE timestamp < :timestamp")
    suspend fun deleteOldEvents(timestamp: Long)
}

@Dao
interface LocationUpdateDao {
    @Query("SELECT * FROM location_updates WHERE sosEventId = :eventId ORDER BY timestamp DESC")
    fun getUpdatesForEvent(eventId: Long): Flow<List<LocationUpdate>>

    @Query("SELECT * FROM location_updates WHERE sosEventId = :eventId ORDER BY timestamp DESC")
    suspend fun getUpdatesForEventList(eventId: Long): List<LocationUpdate>

    @Query("SELECT * FROM location_updates WHERE sosEventId = :eventId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestUpdate(eventId: Long): LocationUpdate?

    @Query("SELECT * FROM location_updates WHERE isSynced = 0")
    suspend fun getUnsyncedUpdates(): List<LocationUpdate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: LocationUpdate): Long

    @Query("UPDATE location_updates SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM location_updates WHERE sosEventId = :eventId")
    suspend fun deleteUpdatesForEvent(eventId: Long)
}

@Dao
interface DangerZoneDao {
    @Query("SELECT * FROM danger_zones WHERE isEnabled = 1")
    fun getEnabledZones(): Flow<List<DangerZone>>

    @Query("SELECT * FROM danger_zones")
    fun getAllZones(): Flow<List<DangerZone>>

    @Query("SELECT * FROM danger_zones WHERE isEnabled = 1")
    suspend fun getEnabledZonesList(): List<DangerZone>

    @Query("SELECT * FROM danger_zones WHERE id = :id")
    suspend fun getZoneById(id: Long): DangerZone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: DangerZone): Long

    @Update
    suspend fun updateZone(zone: DangerZone)

    @Delete
    suspend fun deleteZone(zone: DangerZone)
}

@Dao
interface ScheduledCheckInDao {
    @Query("SELECT * FROM scheduled_checkins WHERE isEnabled = 1 ORDER BY scheduledTime ASC")
    fun getEnabledCheckIns(): Flow<List<ScheduledCheckIn>>

    @Query("SELECT * FROM scheduled_checkins ORDER BY scheduledTime ASC")
    fun getAllCheckIns(): Flow<List<ScheduledCheckIn>>

    @Query("SELECT * FROM scheduled_checkins WHERE status = :status")
    suspend fun getCheckInsByStatus(status: CheckInStatus): List<ScheduledCheckIn>

    @Query("SELECT * FROM scheduled_checkins WHERE id = :id")
    suspend fun getCheckInById(id: Long): ScheduledCheckIn?

    @Query("SELECT * FROM scheduled_checkins WHERE isEnabled = 1 AND scheduledTime <= :time AND status = 'PENDING'")
    suspend fun getPendingCheckInsBeforeTime(time: Long): List<ScheduledCheckIn>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: ScheduledCheckIn): Long

    @Update
    suspend fun updateCheckIn(checkIn: ScheduledCheckIn)

    @Query("UPDATE scheduled_checkins SET status = :status WHERE id = :id")
    suspend fun updateCheckInStatus(id: Long, status: CheckInStatus)

    @Delete
    suspend fun deleteCheckIn(checkIn: ScheduledCheckIn)
}
