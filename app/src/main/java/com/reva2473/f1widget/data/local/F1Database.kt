package com.reva2473.f1widget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.UserPreferencesState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.util.concurrent.ConcurrentHashMap

// ============================================================================
// Cache TTL Constants & Helpers
// ============================================================================

const val DEFAULT_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

fun isExpired(lastUpdatedMillis: Long, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
    return (System.currentTimeMillis() - lastUpdatedMillis) > ttlMs
}

private fun File.safeCanonicalPath(): String {
    return try {
        canonicalPath
    } catch (_: IOException) {
        absolutePath
    }
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

@Serializable
data class CachedRaceResultEntity(
    val cacheKey: String,
    val season: Int,
    val round: Int,
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
    suspend fun getRaceResult(cacheKey: String): CachedRaceResultEntity?
    suspend fun insertRaceResult(entity: CachedRaceResultEntity)
    suspend fun clearStandings()
    suspend fun clearSchedule()
    suspend fun clearRaceResults()

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

    private fun sanitizeCacheKey(key: String): String {
        return key.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    private inline fun <T> safeCacheRead(block: () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private inline fun safeCacheWrite(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Safely ignore disk/serialization failures while propagating cancellation
        }
    }

    private fun atomicWrite(targetFile: File, content: String) {
        ensureCacheDir()
        val parentDir = targetFile.parentFile ?: cacheDir
        val tempFile = File.createTempFile("${targetFile.name}.", ".tmp", parentDir)
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
                val file = File(cacheDir, "standings_${sanitizeCacheKey(key)}.json")
                if (!file.exists()) return@withLock null
                safeCacheRead { json.decodeFromString<CachedStandingsEntity>(file.readText()) }
            }
        }

        override suspend fun insertStandings(entity: CachedStandingsEntity) = withContext(Dispatchers.IO) {
            mutex.withLock {
                safeCacheWrite {
                    ensureCacheDir()
                    val file = File(cacheDir, "standings_${sanitizeCacheKey(entity.seasonKey)}.json")
                    atomicWrite(file, json.encodeToString(entity))
                }
            }
        }

        override suspend fun getSchedule(season: Int): CachedScheduleEntity? = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "schedule_${season}.json")
                if (!file.exists()) return@withLock null
                safeCacheRead { json.decodeFromString<CachedScheduleEntity>(file.readText()) }
            }
        }

        override suspend fun insertSchedule(entity: CachedScheduleEntity) = withContext(Dispatchers.IO) {
            mutex.withLock {
                safeCacheWrite {
                    ensureCacheDir()
                    val file = File(cacheDir, "schedule_${entity.season}.json")
                    atomicWrite(file, json.encodeToString(entity))
                }
            }
        }

        override suspend fun getRaceResult(cacheKey: String): CachedRaceResultEntity? = withContext(Dispatchers.IO) {
            mutex.withLock {
                ensureCacheDir()
                val file = File(cacheDir, "race_result_${sanitizeCacheKey(cacheKey)}.json")
                if (!file.exists()) return@withLock null
                safeCacheRead { json.decodeFromString<CachedRaceResultEntity>(file.readText()) }
            }
        }

        override suspend fun insertRaceResult(entity: CachedRaceResultEntity) = withContext(Dispatchers.IO) {
            mutex.withLock {
                safeCacheWrite {
                    ensureCacheDir()
                    val file = File(cacheDir, "race_result_${sanitizeCacheKey(entity.cacheKey)}.json")
                    atomicWrite(file, json.encodeToString(entity))
                }
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

        override suspend fun clearRaceResults() = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.filter { it.name.startsWith("race_result_") }?.forEach { it.delete() }
                }
                Unit
            }
        }
    }

    fun f1Dao(): F1Dao = daoImpl

    suspend fun clearCacheForTesting() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            Unit
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: F1Database? = null
        private val instancesByPath = ConcurrentHashMap<String, F1Database>()

        fun getInstance(context: Context): F1Database {
            val cacheDir = File(context.applicationContext.filesDir, "f1_offline_cache")
            return getInstance(cacheDir)
        }

        fun getInstance(cacheDir: File): F1Database {
            val targetPath = cacheDir.safeCanonicalPath()
            val current = INSTANCE
            if (current != null && current.cacheDir.safeCanonicalPath() == targetPath) {
                return current
            }
            instancesByPath[targetPath]?.let { return it }
            return synchronized(this) {
                val doubleChecked = INSTANCE
                if (doubleChecked != null && doubleChecked.cacheDir.safeCanonicalPath() == targetPath) {
                    doubleChecked
                } else {
                    instancesByPath.getOrPut(targetPath) {
                        F1Database(cacheDir)
                    }.also { db ->
                        if (INSTANCE == null) {
                            INSTANCE = db
                        }
                    }
                }
            }
        }

        internal fun resetInstanceForTesting() {
            synchronized(this) {
                INSTANCE = null
                instancesByPath.clear()
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
        val REMINDER_SESSION_KEYS = stringSetPreferencesKey("reminder_session_keys")
    }

    private fun Flow<Preferences>.catchIOAsEmpty(): Flow<Preferences> = catch { e ->
        if (e is IOException) {
            emit(emptyPreferences())
        } else {
            throw e
        }
    }

    val preferencesFlow: Flow<UserPreferencesState> = dataStore.data
        .catchIOAsEmpty()
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

    val reminderSessionKeysFlow: Flow<Set<String>> = dataStore.data
        .catchIOAsEmpty()
        .map { prefs ->
            prefs[Keys.REMINDER_SESSION_KEYS] ?: emptySet()
        }

    suspend fun setSessionReminder(sessionKey: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.REMINDER_SESSION_KEYS] ?: emptySet()
            prefs[Keys.REMINDER_SESSION_KEYS] = if (enabled) {
                current + sessionKey
            } else {
                current - sessionKey
            }
        }
    }

    suspend fun clearSessionReminders() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.REMINDER_SESSION_KEYS)
        }
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

    companion object {
        private val fallbackDataStores = ConcurrentHashMap<String, DataStore<Preferences>>()

        /**
         * Returns a process-safe singleton [DataStore] instance for the given fallback [file],
         * preventing DataStore's multiple-instance [IllegalStateException] in non-Context tests
         * or fallback repository instantiations.
         */
        fun getOrCreateFallbackDataStore(file: File): DataStore<Preferences> {
            val canonicalPath = file.safeCanonicalPath()
            return fallbackDataStores[canonicalPath] ?: synchronized(fallbackDataStores) {
                fallbackDataStores.getOrPut(canonicalPath) {
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                        produceFile = { file }
                    )
                }
            }
        }

        /**
         * Creates or reuses a [UserPreferencesStore] backed by a process-safe fallback [DataStore]
         * keyed by [file]'s canonical path.
         */
        fun forFallbackFile(file: File): UserPreferencesStore {
            return UserPreferencesStore(getOrCreateFallbackDataStore(file))
        }
    }
}
