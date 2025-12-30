package com.safeguard.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.safeguard.app.data.models.*

@Database(
    entities = [
        EmergencyContact::class,
        SOSEvent::class,
        LocationUpdate::class,
        DangerZone::class,
        ScheduledCheckIn::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SafeGuardDatabase : RoomDatabase() {

    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosEventDao(): SOSEventDao
    abstract fun locationUpdateDao(): LocationUpdateDao
    abstract fun dangerZoneDao(): DangerZoneDao
    abstract fun scheduledCheckInDao(): ScheduledCheckInDao

    companion object {
        @Volatile
        private var INSTANCE: SafeGuardDatabase? = null

        // Migration from version 1 to 2: Add email fields to emergency_contacts
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns with default values - use try/catch in case columns already exist
                try {
                    database.execSQL("ALTER TABLE emergency_contacts ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { /* Column may already exist */ }
                
                try {
                    database.execSQL("ALTER TABLE emergency_contacts ADD COLUMN enableEmail INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) { /* Column may already exist */ }
                
                try {
                    database.execSQL("ALTER TABLE emergency_contacts ADD COLUMN enablePushNotification INTEGER NOT NULL DEFAULT 1")
                } catch (e: Exception) { /* Column may already exist */ }
                
                try {
                    database.execSQL("ALTER TABLE emergency_contacts ADD COLUMN telegramChatId TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { /* Column may already exist */ }
            }
        }

        fun getDatabase(context: Context): SafeGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeGuardDatabase::class.java,
                    "safeguard_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
