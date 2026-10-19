package com.zensa.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UserPreferences — typed access to the Zensa DataStore.
 *
 * DataStore is Jetpack's modern replacement for SharedPreferences.
 * It is asynchronous, transactional, and safe from the apply()
 * race conditions that SharedPreferences suffers from.
 *
 * This class stores NON-sensitive user settings. The JWT token is
 * NOT stored here — it lives in EncryptedSharedPreferences
 * (TokenStorage) because it is sensitive.
 */
class UserPreferences(private val context: Context) {

    // ---------- Keys ----------

    private object Keys {
        val THEME = stringPreferencesKey("theme")            // LIGHT / DARK / SYSTEM
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_STUDY_DURATION = intPreferencesKey("default_study_duration_min")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val LAST_SYNC_AT = stringPreferencesKey("last_sync_at")
        val LANGUAGE = stringPreferencesKey("language")       // reserved for POE
    }

    // ---------- Read (as flows) ----------

    val theme: Flow<String> = context.dataStore.data
        .map { it[Keys.THEME] ?: THEME_SYSTEM }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val defaultStudyDurationMinutes: Flow<Int> = context.dataStore.data
        .map { it[Keys.DEFAULT_STUDY_DURATION] ?: 25 }

    val defaultPriority: Flow<String> = context.dataStore.data
        .map { it[Keys.DEFAULT_PRIORITY] ?: PRIORITY_MEDIUM }

    val lastSyncAt: Flow<String?> = context.dataStore.data
        .map { it[Keys.LAST_SYNC_AT] }

    // ---------- Write ----------

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setDefaultStudyDuration(minutes: Int) {
        val safe = minutes.coerceIn(MIN_STUDY_MINUTES, MAX_STUDY_MINUTES)
        context.dataStore.edit { it[Keys.DEFAULT_STUDY_DURATION] = safe }
    }

    suspend fun setDefaultPriority(value: String) {
        val normalised = value.uppercase().takeIf {
            it in VALID_PRIORITIES
        } ?: PRIORITY_MEDIUM
        context.dataStore.edit { it[Keys.DEFAULT_PRIORITY] = normalised }
    }

    suspend fun setLastSyncAt(isoDateTime: String) {
        context.dataStore.edit { it[Keys.LAST_SYNC_AT] = isoDateTime }
    }

    // ---------- Constants ----------

    companion object {
        // Theme values
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"

        // Priority values (must match the backend's accepted set)
        const val PRIORITY_LOW = "LOW"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_URGENT = "URGENT"

        val VALID_PRIORITIES = setOf(
            PRIORITY_LOW, PRIORITY_MEDIUM, PRIORITY_HIGH, PRIORITY_URGENT
        )

        // Study duration bounds
        const val MIN_STUDY_MINUTES = 5
        const val MAX_STUDY_MINUTES = 180
    }
}

/**
 * One DataStore instance per process, keyed by the property
 * delegate below. This is the official Android pattern.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zensa_preferences"
)
