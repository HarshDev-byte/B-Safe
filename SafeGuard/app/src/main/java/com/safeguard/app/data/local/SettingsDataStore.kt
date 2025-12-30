package com.safeguard.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.safeguard.app.data.models.RegionalSettings
import com.safeguard.app.data.models.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safeguard_settings")

class SettingsDataStore(private val context: Context) {

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    companion object {
        private val USER_SETTINGS_KEY = stringPreferencesKey("user_settings")
        private val REGIONAL_SETTINGS_KEY = stringPreferencesKey("regional_settings")
        private val SOS_ACTIVE_KEY = booleanPreferencesKey("sos_active")
        private val ACTIVE_SOS_EVENT_ID_KEY = longPreferencesKey("active_sos_event_id")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val settingsJson = preferences[USER_SETTINGS_KEY]
            if (settingsJson != null) {
                try {
                    json.decodeFromString<UserSettings>(settingsJson)
                } catch (e: Exception) {
                    UserSettings()
                }
            } else {
                UserSettings()
            }
        }

    val regionalSettings: Flow<RegionalSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val settingsJson = preferences[REGIONAL_SETTINGS_KEY]
            if (settingsJson != null) {
                try {
                    json.decodeFromString<RegionalSettings>(settingsJson)
                } catch (e: Exception) {
                    RegionalSettings()
                }
            } else {
                RegionalSettings()
            }
        }

    val isSOSActive: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SOS_ACTIVE_KEY] ?: false
        }

    val activeSOSEventId: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[ACTIVE_SOS_EVENT_ID_KEY] ?: 0L
        }

    suspend fun updateUserSettings(settings: UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[USER_SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    suspend fun updateUserSettings(update: (UserSettings) -> UserSettings) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[USER_SETTINGS_KEY]
            val current = if (currentJson != null) {
                try {
                    json.decodeFromString<UserSettings>(currentJson)
                } catch (e: Exception) {
                    UserSettings()
                }
            } else {
                UserSettings()
            }
            val updated = update(current)
            preferences[USER_SETTINGS_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun updateRegionalSettings(settings: RegionalSettings) {
        context.dataStore.edit { preferences ->
            preferences[REGIONAL_SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    suspend fun setSOSActive(active: Boolean, eventId: Long = 0L) {
        context.dataStore.edit { preferences ->
            preferences[SOS_ACTIVE_KEY] = active
            preferences[ACTIVE_SOS_EVENT_ID_KEY] = eventId
        }
    }

    suspend fun getUserSettingsOnce(): UserSettings {
        val preferences = context.dataStore.data.catch { emit(emptyPreferences()) }
        var settings = UserSettings()
        preferences.collect { prefs ->
            val settingsJson = prefs[USER_SETTINGS_KEY]
            if (settingsJson != null) {
                try {
                    settings = json.decodeFromString(settingsJson)
                } catch (e: Exception) {
                    settings = UserSettings()
                }
            }
        }
        return settings
    }
}
