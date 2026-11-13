package com.reva2473.f1widget.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.reva2473.f1widget.data.local.CachedScheduleEntity
import com.reva2473.f1widget.data.local.CachedStandingsEntity
import com.reva2473.f1widget.data.local.F1Database
import com.reva2473.f1widget.data.local.UserPreferencesStore
import com.reva2473.f1widget.data.local.isExpired
import com.reva2473.f1widget.data.remote.JolpicaApiService
import com.reva2473.f1widget.data.remote.OpenF1ApiService
import com.reva2473.f1widget.data.remote.RaceDto
import com.reva2473.f1widget.data.remote.SessionDto
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceControlMessage
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.UserPreferencesState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val sharedOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(15))
        .build()
}

fun createDefaultJolpicaApi(): JolpicaApiService? = try {
    JolpicaApiService.create(sharedOkHttpClient)
} catch (_: Throwable) {
    null
}

fun createDefaultOpenF1Api(): OpenF1ApiService? = try {
    OpenF1ApiService.create(sharedOkHttpClient)
} catch (_: Throwable) {
    null
}

/**
 * Main repository coordinating Jolpica Ergast API, OpenF1 API, offline cache,
 * and high-fidelity pre-seeded data for instant offline & demo access.
 */
class F1Repository(
    private val context: Context? = null,
    private val jolpicaApi: JolpicaApiService? = createDefaultJolpicaApi(),
    private val openF1Api: OpenF1ApiService? = createDefaultOpenF1Api(),
    private val database: F1Database = context?.let { F1Database.getInstance(it) } ?: F1Database(File(System.getProperty("java.io.tmpdir") ?: ".", "f1_cache")),
    val preferencesStore: UserPreferencesStore = context?.let { UserPreferencesStore(it.applicationContext) }
        ?: UserPreferencesStore(PreferenceDataStoreFactory.create { File(System.getProperty("java.io.tmpdir") ?: ".", "user_preferences.preferences_pb") })
) {
    constructor(context: Context) : this(
        context = context.applicationContext,
        jolpicaApi = createDefaultJolpicaApi(),
        openF1Api = createDefaultOpenF1Api(),
        database = F1Database.getInstance(context),
        preferencesStore = UserPreferencesStore(context.applicationContext)
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val userPreferencesFlow: Flow<UserPreferencesState> = preferencesStore.preferencesFlow

    /**
     * Get the next upcoming race weekend (Bahrain Sepang 2026 default).
     */
    suspend fun getNextRaceWeekend(): RaceWeekend = withContext(Dispatchers.IO) {
        updateWeekendCountdowns(SeedDataProvider.getNextRaceWeekend())
    }

    /**
     * Get the Brazilian GP 2026 weekend schedule.
     */
    suspend fun getBrazilianGpWeekend2026(): RaceWeekend = withContext(Dispatchers.IO) {
        updateWeekendCountdowns(SeedDataProvider.getBrazilianGpWeekend2026())
    }

    /**
     * Get São Paulo 2024 results.
     */
    suspend fun getSaoPaulo2024Results(): RaceWeekend = withContext(Dispatchers.IO) {
        SeedDataProvider.getSaoPaulo2024Results()
    }

    /**
     * Get Abu Dhabi 2023 results with stints.
     */
    suspend fun getAbuDhabi2023Results(): RaceWeekend = withContext(Dispatchers.IO) {
        SeedDataProvider.getAbuDhabi2023Results()
    }

    /**
     * Get race weekend for a specific season and round.
     */
    suspend fun getRaceWeekend(season: Int, round: Int): RaceWeekend = withContext(Dispatchers.IO) {
        val schedule = getSeasonSchedule(season)
        schedule.firstOrNull { it.round == round } ?: SeedDataProvider.getNextRaceWeekend()
    }

    /**
     * Get the full season schedule with cache fallback.
     */
    suspend fun getSeasonSchedule(season: Int): List<RaceWeekend> = withContext(Dispatchers.IO) {
        var cachedSchedule: List<RaceWeekend>? = null
        try {
            val cached = database.f1Dao().getSchedule(season)
            if (cached != null) {
                val parsed = json.decodeFromString<List<RaceWeekend>>(cached.payloadJson).map { updateWeekendCountdowns(it) }
                if (!isExpired(cached.lastUpdatedMillis)) {
                    return@withContext parsed
                }
                cachedSchedule = parsed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // Try remote Jolpica API if available
        if (jolpicaApi != null) {
            try {
                val response = jolpicaApi.getSeasonSchedule(season.toString())
                val races = response.mrData.raceTable?.races
                if (!races.isNullOrEmpty()) {
                    val converted = races.mapIndexed { idx, dto ->
                        mapRaceDtoToWeekend(dto, season, idx)
                    }
                    try {
                        database.f1Dao().insertSchedule(
                            CachedScheduleEntity(
                                season = season,
                                payloadJson = json.encodeToString(converted)
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        // Return expired cache if available (recomputed dynamically)
        if (cachedSchedule != null) {
            return@withContext cachedSchedule
        }

        // Use SeedDataProvider fallback
        SeedDataProvider.getSeasonSchedule(season).map { updateWeekendCountdowns(it) }
    }

    /**
     * Get driver standings with cache fallback.
     */
    suspend fun getDriverStandings(season: Int, forceRefresh: Boolean = false): List<DriverStanding> = withContext(Dispatchers.IO) {
        val cacheKey = "${season}_drivers"
        var cachedStandings: List<DriverStanding>? = null
        try {
            val cached = database.f1Dao().getStandings(cacheKey)
            if (cached != null) {
                val parsed = json.decodeFromString<List<DriverStanding>>(cached.payloadJson)
                if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                    return@withContext parsed
                }
                cachedStandings = parsed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // Remote Jolpica lookup
        if (jolpicaApi != null) {
            try {
                val response = jolpicaApi.getDriverStandings(season.toString())
                val standingsTable = response.mrData.standingsTable
                val standingsListDto = standingsTable?.standingsLists?.firstOrNull()
                val list = standingsListDto?.driverStandings
                val seasonRound = standingsListDto?.round?.toIntOrNull()?.coerceAtLeast(1)
                    ?: standingsTable?.round?.toIntOrNull()?.coerceAtLeast(1)
                    ?: 24

                if (!list.isNullOrEmpty()) {
                    val leaderPts = list.firstOrNull()?.points?.toDoubleOrNull() ?: 0.0
                    val converted = list.mapIndexed { index, dto ->
                        val constructor = dto.constructors.lastOrNull()
                        val constructorName = constructor?.name ?: "Unknown"
                        val team = F1TeamId.fromName(constructorName)
                        val pts = dto.points.toDoubleOrNull() ?: 0.0
                        val prevPts = if (index > 0) list[index - 1].points.toDoubleOrNull() ?: pts else pts
                        val diffLeader = leaderPts - pts
                        val diffPrev = prevPts - pts
                        val gapLeaderStr = if (index > 0 && diffLeader > 0.0) {
                            "-${if (diffLeader % 1.0 == 0.0) diffLeader.toInt().toString() else diffLeader.toString()} to leader"
                        } else null
                        val gapPrevStr = if (index > 0 && diffPrev > 0.0) {
                            "-${if (diffPrev % 1.0 == 0.0) diffPrev.toInt().toString() else diffPrev.toString()} to P$index"
                        } else null
                        val winsCount = dto.wins.toIntOrNull() ?: 0
                        val ptsStr = if (pts % 1.0 == 0.0) pts.toInt().toString() else pts.toString()

                        DriverStanding(
                            position = dto.position.toIntOrNull() ?: (index + 1),
                            driverId = dto.driver.driverId,
                            code = dto.driver.code?.ifBlank { null } ?: dto.driver.familyName.take(3).uppercase(),
                            firstName = dto.driver.givenName,
                            lastName = dto.driver.familyName,
                            fullName = "${dto.driver.givenName} ${dto.driver.familyName}".trim(),
                            nationality = dto.driver.nationality,
                            team = team,
                            points = pts,
                            wins = winsCount,
                            podiums = 0,
                            poles = 0,
                            avgPtsPerRace = if (seasonRound > 0) (pts / seasonRound) else 0.0,
                            racesCount = seasonRound,
                            gapToLeader = gapLeaderStr,
                            gapToPrev = gapPrevStr,
                            last3Summary = if (winsCount > 0) "$winsCount wins ($ptsStr pts)" else "$ptsStr pts",
                            recentRaces = emptyList()
                        )
                    }
                    try {
                        database.f1Dao().insertStandings(
                            CachedStandingsEntity(
                                seasonKey = cacheKey,
                                season = season,
                                type = "DRIVERS",
                                payloadJson = json.encodeToString(converted)
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        if (!cachedStandings.isNullOrEmpty()) {
            return@withContext cachedStandings
        }

        if (season == 2026) {
            val standings = SeedDataProvider.getDriverStandings2026()
            try {
                database.f1Dao().insertStandings(
                    CachedStandingsEntity(
                        seasonKey = cacheKey,
                        season = season,
                        type = "DRIVERS",
                        payloadJson = json.encodeToString(standings)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
            return@withContext standings
        }

        // For historical seasons when network/cache unavailable, return historical seed or empty list
        SeedDataProvider.getHistoricalDriverStandings(season)
    }

    /**
     * Get constructor standings with cache fallback.
     */
    suspend fun getConstructorStandings(season: Int, forceRefresh: Boolean = false): List<ConstructorStanding> = withContext(Dispatchers.IO) {
        if (season < 1958) {
            return@withContext emptyList()
        }

        val cacheKey = "${season}_constructors"
        var cachedStandings: List<ConstructorStanding>? = null
        try {
            val cached = database.f1Dao().getStandings(cacheKey)
            if (cached != null) {
                val parsed = json.decodeFromString<List<ConstructorStanding>>(cached.payloadJson)
                if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                    return@withContext parsed
                }
                cachedStandings = parsed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // Remote Jolpica lookup
        if (jolpicaApi != null) {
            try {
                val response = jolpicaApi.getConstructorStandings(season.toString())
                val list = response.mrData.standingsTable?.standingsLists?.firstOrNull()?.constructorStandings
                if (!list.isNullOrEmpty()) {
                    val leaderPts = list.firstOrNull()?.points?.toDoubleOrNull() ?: 0.0
                    val converted = list.mapIndexed { index, dto ->
                        val team = F1TeamId.fromName(dto.constructor.name)
                        val pts = dto.points.toDoubleOrNull() ?: 0.0
                        val prevPts = if (index > 0) list[index - 1].points.toDoubleOrNull() ?: pts else pts
                        val diffLeader = leaderPts - pts
                        val diffPrev = prevPts - pts
                        val gapToP1 = if (index > 0 && diffLeader > 0.0) {
                            "${if (diffLeader % 1.0 == 0.0) diffLeader.toInt().toString() else diffLeader.toString()} to P1"
                        } else null
                        val gapToPrev = if (index > 0 && diffPrev > 0.0) {
                            "${if (diffPrev % 1.0 == 0.0) diffPrev.toInt().toString() else diffPrev.toString()} to P$index"
                        } else null
                        val winsCount = dto.wins.toIntOrNull() ?: 0

                        ConstructorStanding(
                            position = dto.position.toIntOrNull() ?: (index + 1),
                            team = team,
                            points = pts,
                            wins = winsCount,
                            gapToP1 = gapToP1,
                            gapToPrev = gapToPrev,
                            drivers = emptyList(),
                            seasonWins = emptyList(),
                            recentTeamForm = emptyList()
                        )
                    }
                    try {
                        database.f1Dao().insertStandings(
                            CachedStandingsEntity(
                                seasonKey = cacheKey,
                                season = season,
                                type = "CONSTRUCTORS",
                                payloadJson = json.encodeToString(converted)
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        if (!cachedStandings.isNullOrEmpty()) {
            return@withContext cachedStandings
        }

        if (season == 2026) {
            val standings = SeedDataProvider.getConstructorStandings2026()
            try {
                database.f1Dao().insertStandings(
                    CachedStandingsEntity(
                        seasonKey = cacheKey,
                        season = season,
                        type = "CONSTRUCTORS",
                        payloadJson = json.encodeToString(standings)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
            return@withContext standings
        }

        // For historical seasons when network/cache unavailable, return historical seed or empty list
        SeedDataProvider.getHistoricalConstructorStandings(season)
    }

    /**
     * Get driver career comparisons (VER vs NOR, HAM, LEC, etc.).
     */
    suspend fun getDriverComparisons(): List<DriverCareerComparison> = withContext(Dispatchers.IO) {
        SeedDataProvider.getDriverComparisons()
    }

    /**
     * Get live session state. Queries OpenF1 API when available to enrich weather & race control,
     * cleanly falling back to SeedDataProvider fixtures.
     */
    suspend fun getLiveSessionState(sessionKey: String = "SAO_PAULO"): LiveSessionState = withContext(Dispatchers.IO) {
        val fallbackState = SeedDataProvider.getLiveSessionState(sessionKey)
        if (openF1Api != null) {
            try {
                val weatherList = runCatching { openF1Api.getWeather(sessionKey) }.getOrNull()
                val latestWeatherDto = weatherList?.lastOrNull()
                val enrichedWeather = if (latestWeatherDto != null) {
                    fallbackState.weather.copy(
                        trackTempC = latestWeatherDto.trackTemperature,
                        airTempC = latestWeatherDto.airTemperature,
                        humidityPercent = latestWeatherDto.humidity.toInt(),
                        windSpeedKmh = latestWeatherDto.windSpeed,
                        pressureMbar = latestWeatherDto.pressure,
                        rainfallPercent = latestWeatherDto.rainfall.toInt()
                    )
                } else fallbackState.weather

                val raceControlDtos = runCatching { openF1Api.getRaceControl(sessionKey) }.getOrNull()
                val enrichedMessages = if (!raceControlDtos.isNullOrEmpty()) {
                    raceControlDtos.takeLast(10).reversed().mapIndexed { index, dto ->
                        RaceControlMessage(
                            id = "rc_${dto.sessionKey}_$index",
                            title = dto.message.ifBlank { dto.flag ?: "FLAG" },
                            timestamp = dto.date.takeLast(8).ifBlank { "LIVE" },
                            category = dto.category ?: "INFO"
                        )
                    }
                } else fallbackState.raceControlMessages

                return@withContext fallbackState.copy(
                    weather = enrichedWeather,
                    raceControlMessages = enrichedMessages
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to fallbackState
            }
        }
        fallbackState
    }

    private data class ParsedSessionHolder(
        val item: SessionScheduleItem,
        val localDateTime: ZonedDateTime
    )

    private fun mapRaceDtoToWeekend(dto: RaceDto, season: Int, idx: Int): RaceWeekend {
        val parsedList = mutableListOf<ParsedSessionHolder>()

        dto.firstPractice?.let {
            parseSessionWithTime(it, "FP1", "Free Practice 1", false, "Fri", "Friday")?.let { p -> parsedList.add(p) }
        }
        dto.secondPractice?.let {
            parseSessionWithTime(it, "FP2", "Free Practice 2", false, "Fri", "Friday")?.let { p -> parsedList.add(p) }
        }
        dto.thirdPractice?.let {
            parseSessionWithTime(it, "FP3", "Free Practice 3", false, "Sat", "Saturday")?.let { p -> parsedList.add(p) }
        }
        dto.sprint?.let {
            parseSessionWithTime(it, "Sprint", "Sprint Race", false, "Sat", "Saturday")?.let { p -> parsedList.add(p) }
        }
        dto.qualifying?.let {
            parseSessionWithTime(it, "Quali", "Qualifying", false, "Sat", "Saturday")?.let { p -> parsedList.add(p) }
        }
        parseSessionWithTime(SessionDto(dto.date, dto.time), "Race", "Race", true, "Sun", "Sunday")?.let { p ->
            parsedList.add(p)
        }

        // Sort chronologically by local date-time
        parsedList.sortBy { it.localDateTime }
        val sortedSessions = parsedList.map { it.item }

        val firstSession = sortedSessions.firstOrNull()
        val raceSession = sortedSessions.firstOrNull { it.isRace } ?: sortedSessions.lastOrNull()

        val dateRangeText = if (firstSession != null && raceSession != null && firstSession != raceSession) {
            "${firstSession.dayOfWeek.uppercase()} ${firstSession.dateDay} ${firstSession.dateMonth.uppercase()} - ${raceSession.dayOfWeek.uppercase()} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase()}"
        } else if (raceSession != null) {
            "${raceSession.dayOfWeek.uppercase()} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase()}"
        } else {
            dto.date
        }

        val raceLocalDate = raceSession?.let { r ->
            runCatching {
                val month = monthNameToNumber(r.dateMonth)
                LocalDate.of(dto.season.toIntOrNull() ?: season, month, r.dateDay.toInt())
            }.getOrNull()
        } ?: runCatching { LocalDate.parse(dto.date) }.getOrNull()

        val daysToGo = if (raceLocalDate != null) {
            ChronoUnit.DAYS.between(LocalDate.now(), raceLocalDate).coerceAtLeast(0).toInt()
        } else 0

        val specs = lookupCircuitSpecs(dto.circuit.circuitId, dto.circuit.circuitName)

        return RaceWeekend(
            season = dto.season.toIntOrNull() ?: season,
            round = dto.round.toIntOrNull() ?: (idx + 1),
            grandPrixName = dto.raceName,
            circuitName = dto.circuit.circuitName.ifBlank { specs.circuitId },
            location = "${dto.circuit.location.locality}, ${dto.circuit.location.country}".trim().removePrefix(", ").removeSuffix(","),
            countryFlagEmoji = getCountryFlag(dto.circuit.location.country),
            dateRangeText = dateRangeText,
            daysToGo = daysToGo,
            circuitLengthKm = specs.lengthKm,
            totalLaps = specs.laps,
            totalCorners = specs.corners,
            circuitType = specs.circuitId,
            circuitId = specs.circuitId,
            sessions = sortedSessions,
            isSprintWeekend = dto.sprint != null
        )
    }

    private fun parseSessionWithTime(
        sessionDto: SessionDto,
        shortName: String,
        fullName: String,
        isRace: Boolean,
        defaultDayOfWeek: String,
        defaultFullDayName: String
    ): ParsedSessionHolder? {
        if (sessionDto.date.isBlank()) return null
        val parsedUtcDate = runCatching { LocalDate.parse(sessionDto.date) }.getOrNull() ?: return null
        val rawTime = sessionDto.time?.removeSuffix("Z")?.trim()
        val parsedUtcTime = runCatching {
            if (!rawTime.isNullOrBlank()) {
                val parts = rawTime.split(":")
                when (parts.size) {
                    1 -> LocalTime.of(parts[0].toInt(), 0)
                    2 -> LocalTime.of(parts[0].toInt(), parts[1].toInt())
                    else -> LocalTime.of(parts[0].toInt(), parts[1].toInt(), parts[2].substringBefore('.').toInt())
                }
            } else LocalTime.of(14, 0)
        }.getOrNull() ?: LocalTime.of(14, 0)

        val utcZonedDateTime = ZonedDateTime.of(parsedUtcDate, parsedUtcTime, ZoneOffset.UTC)
        val localZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.systemDefault())

        val localDate = localZonedDateTime.toLocalDate()
        val localTimeStr = localZonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val dateDay = localZonedDateTime.format(DateTimeFormatter.ofPattern("dd"))
        val dateMonth = localZonedDateTime.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
        val dayOfWeek = localZonedDateTime.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
        val fullDayName = localZonedDateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))

        val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), localDate)
        val countdownBadge = formatCountdown(daysDiff, isRace)

        val item = SessionScheduleItem(
            shortName = shortName,
            fullName = fullName,
            dayOfWeek = dayOfWeek,
            fullDayName = fullDayName,
            dateDay = dateDay,
            dateMonth = dateMonth,
            localTime = localTimeStr,
            countdownBadge = countdownBadge,
            isRace = isRace
        )
        return ParsedSessionHolder(item, localZonedDateTime)
    }

    companion object {
        val sharedHttpClient: OkHttpClient get() = sharedOkHttpClient

        fun formatCountdown(daysToGo: Long, isRace: Boolean): String {
            return when {
                daysToGo < 0 -> if (isRace) "Final" else "Completed"
                daysToGo == 0L -> if (isRace) "Race Day" else "Today"
                daysToGo == 1L -> "In 1 day"
                else -> "In $daysToGo days"
            }
        }

        private fun monthNameToNumber(monthName: String): Int {
            return when (monthName.lowercase().trim()) {
                "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
                "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
                else -> 1
            }
        }

        fun updateWeekendCountdowns(weekend: RaceWeekend): RaceWeekend {
            val raceSession = weekend.sessions.firstOrNull { it.isRace } ?: weekend.sessions.lastOrNull()
            val updatedSessions = weekend.sessions.map { session ->
                val sessionDate = runCatching {
                    val month = monthNameToNumber(session.dateMonth)
                    LocalDate.of(weekend.season, month, session.dateDay.toInt())
                }.getOrNull()

                if (sessionDate != null) {
                    val daysDiff = ChronoUnit.DAYS.between(LocalDate.now(), sessionDate)
                    session.copy(countdownBadge = formatCountdown(daysDiff, session.isRace))
                } else {
                    session
                }
            }

            val raceDate = runCatching {
                if (raceSession != null) {
                    val month = monthNameToNumber(raceSession.dateMonth)
                    LocalDate.of(weekend.season, month, raceSession.dateDay.toInt())
                } else null
            }.getOrNull()

            val daysToGo = if (raceDate != null) {
                ChronoUnit.DAYS.between(LocalDate.now(), raceDate).coerceAtLeast(0).toInt()
            } else {
                weekend.daysToGo
            }

            return weekend.copy(
                daysToGo = daysToGo,
                sessions = updatedSessions
            )
        }

        data class CircuitSpecs(
            val circuitId: String,
            val lengthKm: String,
            val laps: Int,
            val corners: Int
        )

        fun lookupCircuitSpecs(circuitIdRaw: String, circuitNameRaw: String): CircuitSpecs {
            val key = "${circuitIdRaw.lowercase()} ${circuitNameRaw.lowercase()}"
            return when {
                key.contains("interlagos") || key.contains("pace") || key.contains("brazil") ->
                    CircuitSpecs("INTERLAGOS", "4.309", 71, 15)
                key.contains("sepang") || key.contains("malaysia") ->
                    CircuitSpecs("SEPANG", "5.543", 56, 15)
                key.contains("bahrain") || key.contains("sakhir") ->
                    CircuitSpecs("BAHRAIN", "5.412", 57, 15)
                key.contains("yas_marina") || key.contains("yas marina") || key.contains("abu dhabi") ->
                    CircuitSpecs("YAS_MARINA", "5.281", 58, 16)
                key.contains("silverstone") ->
                    CircuitSpecs("SILVERSTONE", "5.891", 52, 18)
                key.contains("monza") ->
                    CircuitSpecs("MONZA", "5.793", 53, 11)
                key.contains("monaco") || key.contains("monte carlo") ->
                    CircuitSpecs("MONACO", "3.337", 78, 19)
                key.contains("spa") || key.contains("francorchamps") ->
                    CircuitSpecs("SPA", "7.004", 44, 19)
                key.contains("suzuka") ->
                    CircuitSpecs("SUZUKA", "5.807", 53, 18)
                key.contains("albert_park") || key.contains("albert park") || key.contains("melbourne") ->
                    CircuitSpecs("MELBOURNE", "5.278", 58, 14)
                key.contains("shanghai") ->
                    CircuitSpecs("SHANGHAI", "5.451", 56, 16)
                key.contains("miami") ->
                    CircuitSpecs("MIAMI", "5.412", 57, 19)
                key.contains("villeneuve") || key.contains("montreal") ->
                    CircuitSpecs("MONTREAL", "4.361", 70, 14)
                key.contains("red_bull_ring") || key.contains("spielberg") || key.contains("austria") ->
                    CircuitSpecs("SPIELBERG", "4.318", 71, 10)
                key.contains("hungaroring") || key.contains("budapest") ->
                    CircuitSpecs("HUNGARORING", "4.381", 70, 14)
                key.contains("zandvoort") ->
                    CircuitSpecs("ZANDVOORT", "4.259", 72, 14)
                key.contains("marina_bay") || key.contains("marina bay") || key.contains("singapore") ->
                    CircuitSpecs("SINGAPORE", "4.940", 62, 19)
                key.contains("americas") || key.contains("cota") || key.contains("austin") ->
                    CircuitSpecs("COTA", "5.513", 56, 20)
                key.contains("rodriguez") || key.contains("mexico") ->
                    CircuitSpecs("MEXICO", "4.304", 71, 17)
                key.contains("vegas") ->
                    CircuitSpecs("LAS_VEGAS", "6.201", 50, 17)
                key.contains("losail") || key.contains("lusail") || key.contains("qatar") ->
                    CircuitSpecs("QATAR", "5.419", 57, 16)
                key.contains("jeddah") || key.contains("corniche") ->
                    CircuitSpecs("JEDDAH", "6.174", 50, 27)
                key.contains("baku") || key.contains("azerbaijan") ->
                    CircuitSpecs("BAKU", "6.003", 51, 20)
                key.contains("imola") || key.contains("enzo") ->
                    CircuitSpecs("IMOLA", "4.909", 63, 19)
                key.contains("catalunya") || key.contains("barcelona") ->
                    CircuitSpecs("CATALUNYA", "4.657", 66, 14)
                else ->
                    CircuitSpecs(circuitIdRaw.uppercase().ifBlank { "SEPANG" }, "5.000", 50, 15)
            }
        }
    }

    fun getCountryFlag(country: String): String {
        return when (country.lowercase().trim()) {
            "australia" -> "🇦🇺"
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "azerbaijan" -> "🇦🇿"
            "usa", "united states", "united states of america" -> "🇺🇸"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "canada" -> "🇨🇦"
            "austria" -> "🇦🇹"
            "uk", "great britain", "united kingdom" -> "🇬🇧"
            "hungary" -> "🇭🇺"
            "belgium" -> "🇧🇪"
            "netherlands" -> "🇳🇱"
            "italy" -> "🇮🇹"
            "singapore" -> "🇸🇬"
            "japan" -> "🇯🇵"
            "qatar" -> "🇶🇦"
            "mexico" -> "🇲🇽"
            "brazil" -> "🇧🇷"
            "uae", "united arab emirates", "abu dhabi" -> "🇦🇪"
            "malaysia" -> "🇲🇾"
            "china" -> "🇨🇳"
            "france" -> "🇫🇷"
            "germany" -> "🇩🇪"
            "portugal" -> "🇵🇹"
            "south africa" -> "🇿🇦"
            "argentina" -> "🇦🇷"
            "sweden" -> "🇸🇪"
            "switzerland" -> "🇨🇭"
            "turkey" -> "🇹🇷"
            "india" -> "🇮🇳"
            "south korea", "korea" -> "🇰🇷"
            "russia" -> "🇷🇺"
            "morocco" -> "🇲🇦"
            else -> "🏁"
        }
    }
}
