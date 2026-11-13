package com.reva2473.f1widget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.UserPreferencesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// ============================================================================
// Cache TTL Constants & Helpers
// ============================================================================

const val DEFAULT_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

fun isExpired(lastUpdatedMillis: Long, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
    return (System.currentTimeMillis() - lastUpdatedMillis) > ttlMs
}

// ============================================================================
// Offline Cache Entities
// ============================================================================

@Serializable
data class CachedStandingsEntity(
    val seasonKey: String, // e.g. "2026_drivers", "2026_constructors"
    val season: Int,
    val type: String, // "DRIVERS" or "CONSTRUCTORS"
    val payloadJson: String,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Serializable
data class CachedScheduleEntity(
    val season: Int,
    val payloadJson: String,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

// ============================================================================
// Offline Cache DAO
// ============================================================================

interface F1Dao {
    suspend fun getStandings(key: String): CachedStandingsEntity?
    suspend fun insertStandings(entity: CachedStandingsEntity)
    suspend fun getSchedule(season: Int): CachedScheduleEntity?
    suspend fun insertSchedule(entity: CachedScheduleEntity)
    suspend fun clearStandings()
    suspend fun clearSchedule()

    companion object {
        const val DEFAULT_TTL_MS = com.reva2473.f1widget.data.local.DEFAULT_TTL_MS
        fun isExpired(lastUpdatedMillis: Long, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
            return com.reva2473.f1widget.data.local.isExpired(lastUpdatedMillis, ttlMs)
        }
    }
}

// ============================================================================
// Disk-Backed Local Cache Database
// ============================================================================

class F1Database internal constructor(internal val cacheDir: File) {

    constructor(context: Context) : this(File(context.applicationContext.filesDir, "f1_offline_cache"))

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun ensureCacheDir() {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private fun atomicWrite(targetFile: File, content: String) {
        ensureCacheDir()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.${System.currentTimeMillis()}.tmp")
        try {
            tempFile.writeText(content)
            try {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }

    private val daoImpl = object : F1Dao {
        override suspend fun getStandings(key: String): CachedStandingsEntity? = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "standings_${key.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.json")
                if (!file.exists()) return@withLock null
                runCatching { json.decodeFromString<CachedStandingsEntity>(file.readText()) }.getOrNull()
            }
        }

        override suspend fun insertStandings(entity: CachedStandingsEntity) = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "standings_${entity.seasonKey.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.json")
                runCatching { atomicWrite(file, json.encodeToString(entity)) }
                Unit
            }
        }

        override suspend fun getSchedule(season: Int): CachedScheduleEntity? = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "schedule_${season}.json")
                if (!file.exists()) return@withLock null
                runCatching { json.decodeFromString<CachedScheduleEntity>(file.readText()) }.getOrNull()
            }
        }

        override suspend fun insertSchedule(entity: CachedScheduleEntity) = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "schedule_${entity.season}.json")
                runCatching { atomicWrite(file, json.encodeToString(entity)) }
                Unit
            }
        }

        override suspend fun clearStandings() = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.filter { it.name.startsWith("standings_") }?.forEach { it.delete() }
                }
                Unit
            }
        }

        override suspend fun clearSchedule() = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.filter { it.name.startsWith("schedule_") }?.forEach { it.delete() }
                }
                Unit
            }
        }
    }

    fun f1Dao(): F1Dao = daoImpl

    companion object {
        @Volatile
        private var INSTANCE: F1Database? = null

        fun getInstance(context: Context): F1Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: F1Database(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(cacheDir: File): F1Database {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: F1Database(cacheDir).also { INSTANCE = it }
            }
        }

        internal fun resetInstanceForTesting() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}

// ============================================================================
// DataStore Preferences for UserPreferencesState
// ============================================================================

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val FAVORITE_TEAM = stringPreferencesKey("favorite_team")
        val FAVORITE_DRIVER = stringPreferencesKey("favorite_driver")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_TEAM_COLOR = booleanPreferencesKey("use_team_color")
        val PIT_RADIO_ALERTS = booleanPreferencesKey("pit_radio_alerts")
        val SESSION_REMINDERS = booleanPreferencesKey("session_reminders")
        val LAP_RECORD_ALERTS = booleanPreferencesKey("lap_record_alerts")
        val OFFLINE_CACHE = booleanPreferencesKey("offline_cache")
    }

    val preferencesFlow: Flow<UserPreferencesState> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs ->
            val teamStr = prefs[Keys.FAVORITE_TEAM] ?: F1TeamId.FERRARI.name
            val favoriteTeam = try {
                F1TeamId.valueOf(teamStr)
            } catch (_: Exception) {
                F1TeamId.FERRARI
            }

            UserPreferencesState(
                favoriteTeam = favoriteTeam,
                favoriteDriverCode = prefs[Keys.FAVORITE_DRIVER] ?: "LEC",
                themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
                useTeamColorAccent = prefs[Keys.USE_TEAM_COLOR] ?: true,
                pitRadioAlertsEnabled = prefs[Keys.PIT_RADIO_ALERTS] ?: true,
                sessionRemindersEnabled = prefs[Keys.SESSION_REMINDERS] ?: true,
                lapRecordAlertsEnabled = prefs[Keys.LAP_RECORD_ALERTS] ?: true,
                offlineCacheEnabled = prefs[Keys.OFFLINE_CACHE] ?: true
            )
        }

    suspend fun updateAllPreferences(prefs: UserPreferencesState) {
        dataStore.edit { preferences ->
            preferences[Keys.FAVORITE_TEAM] = prefs.favoriteTeam.name
            preferences[Keys.FAVORITE_DRIVER] = prefs.favoriteDriverCode
            preferences[Keys.THEME_MODE] = prefs.themeMode
            preferences[Keys.USE_TEAM_COLOR] = prefs.useTeamColorAccent
            preferences[Keys.PIT_RADIO_ALERTS] = prefs.pitRadioAlertsEnabled
            preferences[Keys.SESSION_REMINDERS] = prefs.sessionRemindersEnabled
            preferences[Keys.LAP_RECORD_ALERTS] = prefs.lapRecordAlertsEnabled
            preferences[Keys.OFFLINE_CACHE] = prefs.offlineCacheEnabled
        }
    }

    suspend fun updateFavoriteTeam(team: F1TeamId) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_TEAM] = team.name
        }
    }

    suspend fun updateFavoriteDriver(driverCode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_DRIVER] = driverCode
        }
    }

    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun updateUseTeamColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.USE_TEAM_COLOR] = enabled
        }
    }

    suspend fun updatePitRadioAlerts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.PIT_RADIO_ALERTS] = enabled
        }
    }

    suspend fun updateSessionReminders(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SESSION_REMINDERS] = enabled
        }
    }

    suspend fun updateLapRecordAlerts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.LAP_RECORD_ALERTS] = enabled
        }
    }

    suspend fun updateOfflineCache(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_CACHE] = enabled
        }
    }
}
