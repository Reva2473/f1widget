package com.reva2473.f1widget.data.repository

import android.content.Context
import com.reva2473.f1widget.data.local.CachedRaceResultEntity
import com.reva2473.f1widget.data.local.CachedScheduleEntity
import com.reva2473.f1widget.data.local.CachedStandingsEntity
import com.reva2473.f1widget.data.local.F1Database
import com.reva2473.f1widget.data.local.UserPreferencesStore
import com.reva2473.f1widget.data.local.isExpired
import com.reva2473.f1widget.data.remote.ConstructorStandingDto
import com.reva2473.f1widget.data.remote.DriverStandingDto
import com.reva2473.f1widget.data.remote.JolpicaApiService
import com.reva2473.f1widget.data.remote.JolpicaResponse
import com.reva2473.f1widget.data.remote.OpenF1ApiService
import com.reva2473.f1widget.data.remote.OpenF1DriverDto
import com.reva2473.f1widget.data.remote.OpenF1IntervalDto
import com.reva2473.f1widget.data.remote.OpenF1LapDto
import com.reva2473.f1widget.data.remote.OpenF1OvertakeDto
import com.reva2473.f1widget.data.remote.OpenF1PositionDto
import com.reva2473.f1widget.data.remote.OpenF1RaceControlDto
import com.reva2473.f1widget.data.remote.OpenF1SessionDto
import com.reva2473.f1widget.data.remote.OpenF1StintDto
import com.reva2473.f1widget.data.remote.OpenF1TeamRadioDto
import com.reva2473.f1widget.data.remote.OpenF1WeatherDto
import com.reva2473.f1widget.data.remote.PitStopDto
import com.reva2473.f1widget.data.remote.QualifyingResultDto
import com.reva2473.f1widget.data.remote.RaceDto
import com.reva2473.f1widget.data.remote.SessionDto
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.OvertakeEvent
import com.reva2473.f1widget.domain.model.RaceControlMessage
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.TeamRadioClip
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.domain.model.TyreStint
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.domain.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

val sharedOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(15))
        .build()
}

fun createDefaultJolpicaApi(): JolpicaApiService? = F1Repository.sharedJolpicaApi

fun createDefaultOpenF1Api(): OpenF1ApiService? = F1Repository.sharedOpenF1Api

/**
 * Main repository coordinating Jolpica Ergast API, OpenF1 API, offline cache,
 * and high-fidelity pre-seeded data for instant offline & demo access.
 */
class F1Repository(
    private val context: Context? = null,
    private val jolpicaApi: JolpicaApiService? = createDefaultJolpicaApi(),
    private val openF1Api: OpenF1ApiService? = createDefaultOpenF1Api(),
    private val database: F1Database = context?.let { F1Database.getInstance(it) }
        ?: F1Database.getInstance(File(System.getProperty("java.io.tmpdir") ?: ".", "f1_cache")),
    val preferencesStore: UserPreferencesStore = context?.let { UserPreferencesStore(it.applicationContext) }
        ?: UserPreferencesStore.forFallbackFile(
            File(System.getProperty("java.io.tmpdir") ?: ".", "user_preferences.preferences_pb")
        )
) {
    constructor(context: Context) : this(
        context = context.applicationContext,
        jolpicaApi = createDefaultJolpicaApi(),
        openF1Api = createDefaultOpenF1Api(),
        database = F1Database.getInstance(context),
        preferencesStore = UserPreferencesStore(context.applicationContext)
    )

    private val json: Json get() = sharedJson

    val userPreferencesFlow: Flow<UserPreferencesState> = preferencesStore.preferencesFlow
    val reminderSessionKeysFlow: Flow<Set<String>> = preferencesStore.reminderSessionKeysFlow

    suspend fun getReminderSessionKeys(): Flow<Set<String>> = preferencesStore.reminderSessionKeysFlow

    suspend fun setSessionReminder(sessionKey: String, enabled: Boolean) {
        preferencesStore.setSessionReminder(sessionKey, enabled)
    }

    private suspend fun isOfflineCacheEnabled(): Boolean {
        return try {
            preferencesStore.preferencesFlow.first().offlineCacheEnabled
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            true
        }
    }

    private suspend inline fun <T> safeSuspendCall(block: () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get the next upcoming race weekend from the current season schedule.
     */
    suspend fun getNextRaceWeekend(): RaceWeekend = withContext(Dispatchers.IO) {
        val currentSeason = LocalDate.now().year
        val schedule = getSeasonSchedule(currentSeason)
        val now = System.currentTimeMillis()
        val next = schedule.firstOrNull { weekend ->
            val raceSession = weekend.sessions.firstOrNull { it.isRace } ?: weekend.sessions.lastOrNull()
            val utcMillis = raceSession?.utcEpochMillis
            if (utcMillis != null) {
                utcMillis > now
            } else {
                !isRaceCompleted(weekend)
            }
        } ?: schedule.firstOrNull { !isRaceCompleted(it) }
            ?: schedule.lastOrNull()
            ?: SeedDataProvider.getNextRaceWeekend()
        updateWeekendCountdowns(next)
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
     * Enriches completed rounds with race results, qualifying results, and pit stops.
     */
    suspend fun getRaceWeekend(season: Int, round: Int): RaceWeekend = withContext(Dispatchers.IO) {
        val schedule = getSeasonSchedule(season)
        val weekend = schedule.firstOrNull { it.round == round }
            ?: schedule.firstOrNull()
            ?: SeedDataProvider.getNextRaceWeekend().copy(season = season, round = round, results = emptyList())
        val updated = updateWeekendCountdowns(weekend)
        if (updated.results.isEmpty() && isRaceCompleted(updated)) {
            enrichWeekendWithResults(updated, forceRefresh = false)
        } else {
            updated
        }
    }

    /**
     * Get the latest completed race weekend with podium results from the season schedule
     * (enriched from Jolpica results, qualifying & pit-stop APIs when available), walking
     * backwards through completed rounds if the latest round has no published results yet.
     */
    suspend fun getLatestCompletedRace(
        season: Int = 2026,
        forceRefresh: Boolean = false
    ): RaceWeekend = withContext(Dispatchers.IO) {
        val schedule = getSeasonSchedule(season)
        val completedCandidates = schedule.filter { isRaceCompleted(it) }.asReversed()

        for (candidate in completedCandidates) {
            if (!forceRefresh && candidate.results.isNotEmpty()) {
                return@withContext candidate
            }
            val enriched = enrichWeekendWithResults(candidate, forceRefresh = forceRefresh)
            if (enriched.results.isNotEmpty()) {
                return@withContext enriched
            }
        }

        if (!forceRefresh) {
            schedule.lastOrNull { it.results.isNotEmpty() }?.let { return@withContext it }
        }

        when (season) {
            2024 -> SeedDataProvider.getSaoPaulo2024Results()
            2023 -> SeedDataProvider.getAbuDhabi2023Results()
            else -> completedCandidates.firstOrNull()
                ?: schedule.firstOrNull()?.copy(results = emptyList())
                ?: SeedDataProvider.getNextRaceWeekend().copy(season = season, results = emptyList())
        }
    }

    private suspend fun enrichWeekendWithResults(
        weekend: RaceWeekend,
        forceRefresh: Boolean
    ): RaceWeekend {
        val cacheEnabled = isOfflineCacheEnabled()
        val resultCacheKey = "${weekend.season}_${weekend.round}_results"
        var cachedResults: List<RaceDriverResult>? = null

        if (cacheEnabled) {
            try {
                val cached = database.f1Dao().getRaceResult(resultCacheKey)
                if (cached != null) {
                    val parsed = json.decodeFromString<List<RaceDriverResult>>(cached.payloadJson)
                    if (parsed.isNotEmpty()) {
                        if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                            return weekend.copy(results = parsed)
                        }
                        cachedResults = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        if (jolpicaApi != null) {
            val (raceDto, qualifyingDtos, pitStopDtos) = coroutineScope {
                val raceDeferred = async {
                    safeSuspendCall {
                        jolpicaApi.getRaceResults(
                            weekend.season.toString(),
                            weekend.round.toString(),
                            limit = 100,
                            offset = 0
                        ).mrData.raceTable?.races?.firstOrNull()
                    }
                }
                val qualifyingDeferred = async {
                    safeSuspendCall {
                        jolpicaApi.getQualifyingResults(
                            weekend.season.toString(),
                            weekend.round.toString(),
                            limit = 100,
                            offset = 0
                        ).mrData.raceTable?.races?.firstOrNull()?.qualifyingResults.orEmpty()
                    }.orEmpty()
                }
                val pitStopsDeferred = async {
                    safeSuspendCall {
                        fetchAllPitStops(weekend.season, weekend.round)
                    }.orEmpty()
                }
                Triple(raceDeferred.await(), qualifyingDeferred.await(), pitStopsDeferred.await())
            }

            val qualiByDriverId = mutableMapOf<String, QualifyingResultDto>()
            val qualiByCode = mutableMapOf<String, QualifyingResultDto>()
            for (qDto in qualifyingDtos) {
                val id = qDto.driver.driverId.trim().lowercase(Locale.ROOT)
                if (id.isNotEmpty()) qualiByDriverId[id] = qDto
                val code = qDto.driver.code?.trim()?.uppercase(Locale.ROOT).orEmpty()
                if (code.isNotEmpty()) qualiByCode[code] = qDto
            }

            val pitStopsByDriverId = pitStopDtos
                .filter { it.driverId.isNotBlank() }
                .groupBy { it.driverId.trim().lowercase(Locale.ROOT) }

            val mappedResults = raceDto?.results?.mapIndexed { idx, dto ->
                val driverIdKey = dto.driver.driverId.trim().lowercase(Locale.ROOT)
                val driverCode = dto.driver.code?.ifBlank { null }
                    ?: dto.driver.familyName.take(3).uppercase(Locale.ROOT)
                val qualiDto = qualiByDriverId[driverIdKey] ?: qualiByCode[driverCode.uppercase(Locale.ROOT)]
                val qualiPos = qualiDto?.position?.trim()?.takeIf { it.isNotEmpty() }?.let { "P$it" } ?: ""
                val qualiTime = qualiDto?.let {
                    it.q3?.ifBlank { null } ?: it.q2?.ifBlank { null } ?: it.q1?.ifBlank { null }
                }.orEmpty()
                val driverTotalLaps = dto.laps.toIntOrNull() ?: weekend.totalLaps
                val stints = buildStintsFromPitStops(
                    pitStops = pitStopsByDriverId[driverIdKey].orEmpty(),
                    totalLaps = driverTotalLaps
                )

                RaceDriverResult(
                    position = dto.position.toIntOrNull() ?: (idx + 1),
                    driverCode = driverCode,
                    driverFullName = "${dto.driver.givenName} ${dto.driver.familyName}".trim(),
                    team = F1TeamId.fromName(dto.constructor.name),
                    finishTimeOrGap = dto.time?.time?.ifBlank { null } ?: dto.status.ifBlank { "Finished" },
                    points = dto.points.toDoubleOrNull() ?: 0.0,
                    qualifyingPosition = qualiPos,
                    qualifyingTime = qualiTime,
                    fastestLapPosition = dto.fastestLap?.rank?.trim()?.takeIf { it.isNotEmpty() }?.let { "P$it" } ?: "",
                    fastestLapTime = dto.fastestLap?.time?.time ?: "",
                    totalLaps = driverTotalLaps,
                    stints = stints
                )
            }.orEmpty()

            if (mappedResults.isNotEmpty()) {
                if (cacheEnabled) {
                    try {
                        database.f1Dao().insertRaceResult(
                            CachedRaceResultEntity(
                                cacheKey = resultCacheKey,
                                season = weekend.season,
                                round = weekend.round,
                                payloadJson = json.encodeToString(mappedResults)
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                }
                return weekend.copy(results = mappedResults)
            }
        }

        return if (cacheEnabled && !cachedResults.isNullOrEmpty()) {
            weekend.copy(results = cachedResults)
        } else {
            weekend
        }
    }

    private suspend fun fetchAllPitStops(season: Int, round: Int): List<PitStopDto> {
        val api = jolpicaApi ?: return emptyList()
        return fetchAllPaginated(
            fetchPage = { limit, offset ->
                api.getPitStops(season.toString(), round.toString(), limit = limit, offset = offset)
            },
            extractItems = { it.mrData.raceTable?.races?.firstOrNull()?.pitStops.orEmpty() }
        )
    }

    private fun buildStintsFromPitStops(
        pitStops: List<PitStopDto>,
        totalLaps: Int
    ): List<TyreStint> {
        if (pitStops.isEmpty()) return emptyList()
        val sortedStops = pitStops.sortedWith(
            compareBy<PitStopDto>(
                { it.stop.toIntOrNull() ?: Int.MAX_VALUE },
                { it.lap.toIntOrNull() ?: Int.MAX_VALUE }
            )
        )
        val stints = mutableListOf<TyreStint>()
        var prevLap = 0
        sortedStops.forEachIndexed { index, stop ->
            val stopLap = stop.lap.toIntOrNull()
            val lapsInStint = if (stopLap != null) (stopLap - prevLap).coerceAtLeast(1) else 0
            val rawDuration = stop.duration.trim()
            val formattedDuration = when {
                rawDuration.isEmpty() -> null
                rawDuration.endsWith("s", ignoreCase = true) -> rawDuration
                else -> "${rawDuration}s"
            }
            stints.add(
                TyreStint(
                    stintNumber = index + 1,
                    compound = if (index == 0) TyreCompound.MEDIUM else TyreCompound.HARD,
                    lapsCount = lapsInStint,
                    pitStopDuration = formattedDuration,
                    pitStopLap = stopLap
                )
            )
            if (stopLap != null && stopLap > prevLap) {
                prevLap = stopLap
            }
        }
        val finalLaps = (totalLaps - prevLap).coerceAtLeast(1)
        stints.add(
            TyreStint(
                stintNumber = sortedStops.size + 1,
                compound = TyreCompound.HARD,
                lapsCount = finalLaps,
                pitStopDuration = null,
                pitStopLap = null
            )
        )
        return stints
    }

    private fun isRaceCompleted(weekend: RaceWeekend): Boolean {
        if (weekend.results.isNotEmpty()) return true
        val currentYear = LocalDate.now().year
        if (weekend.season < currentYear && weekend.sessions.isEmpty()) return true
        val raceSession = weekend.sessions.firstOrNull { it.isRace } ?: weekend.sessions.lastOrNull()
            ?: return (weekend.season < currentYear)
        val completionBufferMillis = 3L * 60L * 60L * 1000L
        val utcMillis = raceSession.utcEpochMillis
        if (utcMillis != null) {
            return System.currentTimeMillis() >= utcMillis + completionBufferMillis
        }

        val month = monthNameToNumber(raceSession.dateMonth) ?: return (weekend.season < currentYear)
        val day = raceSession.dateDay.toIntOrNull() ?: return (weekend.season < currentYear)
        val raceDate = runCatching { LocalDate.of(weekend.season, month, day) }.getOrNull()
            ?: return (weekend.season < currentYear)
        val today = LocalDate.now()
        if (raceDate.isBefore(today)) return true
        if (raceDate.isAfter(today)) return false

        val localTime = runCatching { LocalTime.parse(raceSession.localTime.trim()) }.getOrNull() ?: return false
        val raceEndMillis = ZonedDateTime.of(raceDate, localTime, ZoneId.systemDefault())
            .plusHours(3)
            .toInstant()
            .toEpochMilli()
        return System.currentTimeMillis() >= raceEndMillis
    }

    private suspend fun <T, R> fetchAllPaginated(
        pageSize: Int = 100,
        fetchPage: suspend (limit: Int, offset: Int) -> JolpicaResponse<T>,
        extractItems: (JolpicaResponse<T>) -> List<R>,
        onFirstResponse: ((JolpicaResponse<T>) -> Unit)? = null
    ): List<R> {
        val allItems = mutableListOf<R>()
        var offset = 0
        while (true) {
            val response = fetchPage(pageSize, offset)
            if (offset == 0) {
                onFirstResponse?.invoke(response)
            }
            val pageItems = extractItems(response)
            if (pageItems.isEmpty()) break
            allItems.addAll(pageItems)

            val total = response.mrData.total?.toIntOrNull() ?: break
            offset += pageItems.size
            if (offset >= total) break
        }
        return allItems
    }

    private suspend fun fetchAllSeasonRaces(season: Int): List<RaceDto> {
        val api = jolpicaApi ?: return emptyList()
        return fetchAllPaginated(
            fetchPage = { limit, offset ->
                api.getSeasonSchedule(season.toString(), limit = limit, offset = offset)
            },
            extractItems = { it.mrData.raceTable?.races.orEmpty() }
        )
    }

    private suspend fun fetchAllDriverStandings(season: Int): Pair<Int?, List<DriverStandingDto>> {
        val api = jolpicaApi ?: return null to emptyList()
        var seasonRound: Int? = null
        val items = fetchAllPaginated(
            fetchPage = { limit, offset ->
                api.getDriverStandings(season.toString(), limit = limit, offset = offset)
            },
            extractItems = {
                it.mrData.standingsTable?.standingsLists?.firstOrNull()?.driverStandings.orEmpty()
            },
            onFirstResponse = { response ->
                val table = response.mrData.standingsTable
                seasonRound = table?.standingsLists?.firstOrNull()?.round?.toIntOrNull()
                    ?: table?.round?.toIntOrNull()
            }
        )
        return seasonRound to items
    }

    private suspend fun fetchAllConstructorStandings(season: Int): List<ConstructorStandingDto> {
        val api = jolpicaApi ?: return emptyList()
        return fetchAllPaginated(
            fetchPage = { limit, offset ->
                api.getConstructorStandings(season.toString(), limit = limit, offset = offset)
            },
            extractItems = {
                it.mrData.standingsTable?.standingsLists?.firstOrNull()?.constructorStandings.orEmpty()
            }
        )
    }

    /**
     * Get the full season schedule with cache fallback.
     */
    suspend fun getSeasonSchedule(season: Int, forceRefresh: Boolean = false): List<RaceWeekend> = withContext(Dispatchers.IO) {
        val cacheEnabled = isOfflineCacheEnabled()
        var cachedSchedule: List<RaceWeekend>? = null
        if (cacheEnabled) {
            try {
                val cached = database.f1Dao().getSchedule(season)
                if (cached != null) {
                    val parsed = json.decodeFromString<List<RaceWeekend>>(cached.payloadJson)
                        .map { updateWeekendCountdowns(it) }
                    if (parsed.isNotEmpty()) {
                        if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                            return@withContext parsed
                        }
                        cachedSchedule = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        // Try remote Jolpica API if available
        if (jolpicaApi != null) {
            try {
                val races = fetchAllSeasonRaces(season)
                if (races.isNotEmpty()) {
                    val converted = races.mapIndexed { idx, dto ->
                        mapRaceDtoToWeekend(dto, season, idx)
                    }
                    if (cacheEnabled) {
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
                    }
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        // Return expired cache if available (recomputed dynamically)
        if (cacheEnabled && !cachedSchedule.isNullOrEmpty()) {
            return@withContext cachedSchedule
        }

        // Use SeedDataProvider fallback
        SeedDataProvider.getSeasonSchedule(season).map { updateWeekendCountdowns(it) }
    }

    /**
     * Get driver standings with cache fallback.
     */
    suspend fun getDriverStandings(season: Int, forceRefresh: Boolean = false): List<DriverStanding> = withContext(Dispatchers.IO) {
        val cacheEnabled = isOfflineCacheEnabled()
        val cacheKey = "${season}_drivers"
        var cachedStandings: List<DriverStanding>? = null
        if (cacheEnabled) {
            try {
                val cached = database.f1Dao().getStandings(cacheKey)
                if (cached != null) {
                    val parsed = json.decodeFromString<List<DriverStanding>>(cached.payloadJson)
                    if (parsed.isNotEmpty()) {
                        if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                            return@withContext parsed
                        }
                        cachedStandings = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        // Remote Jolpica lookup
        if (jolpicaApi != null) {
            try {
                val (seasonRound, driverDtos) = fetchAllDriverStandings(season)
                val converted = mapDriverStandings(driverDtos, seasonRound)
                if (converted.isNotEmpty()) {
                    if (cacheEnabled) {
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
                    }
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        if (cacheEnabled && !cachedStandings.isNullOrEmpty()) {
            return@withContext cachedStandings
        }

        if (season == 2026) {
            return@withContext SeedDataProvider.getDriverStandings2026()
        }

        // For historical seasons when network/cache unavailable, return historical seed or empty list
        SeedDataProvider.getHistoricalDriverStandings(season)
    }

    private fun mapDriverStandings(
        list: List<DriverStandingDto>,
        seasonRound: Int?
    ): List<DriverStanding> {
        if (list.isEmpty()) return emptyList()

        val sorted = list.sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }
        val validRound = seasonRound?.takeIf { it > 0 }
        val racesCount = validRound ?: 0
        val leaderPts = sorted.firstOrNull()?.points?.toDoubleOrNull() ?: 0.0

        return sorted.mapIndexed { index, dto ->
            val constructor = dto.constructors.lastOrNull()
            val constructorName = constructor?.name ?: "Unknown"
            val team = F1TeamId.fromName(constructorName)
            val pts = dto.points.toDoubleOrNull() ?: 0.0
            val pos = dto.position.toIntOrNull() ?: (index + 1)
            val prevPos = if (index > 0) (sorted[index - 1].position.toIntOrNull() ?: index) else pos
            val prevPts = if (index > 0) sorted[index - 1].points.toDoubleOrNull() ?: pts else pts
            val gapLeaderStr = if (index > 0) formatGap(leaderPts - pts, prefix = "-", suffix = "to leader") else null
            val gapPrevStr = if (index > 0) formatGap(prevPts - pts, prefix = "-", suffix = "to P$prevPos") else null
            val winsCount = dto.wins.toIntOrNull() ?: 0
            val ptsStr = formatPoints(pts)

            DriverStanding(
                position = pos,
                driverId = dto.driver.driverId,
                code = dto.driver.code?.ifBlank { null } ?: dto.driver.familyName.take(3).uppercase(Locale.ROOT),
                firstName = dto.driver.givenName,
                lastName = dto.driver.familyName,
                fullName = "${dto.driver.givenName} ${dto.driver.familyName}".trim(),
                nationality = dto.driver.nationality,
                team = team,
                points = pts,
                wins = winsCount,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = if (validRound != null) (pts / validRound.coerceAtLeast(1)) else 0.0,
                racesCount = racesCount,
                gapToLeader = gapLeaderStr,
                gapToPrev = gapPrevStr,
                last3Summary = if (winsCount > 0) "$winsCount wins ($ptsStr pts)" else "$ptsStr pts",
                recentRaces = emptyList()
            )
        }
    }

    /**
     * Get constructor standings with cache fallback.
     */
    suspend fun getConstructorStandings(season: Int, forceRefresh: Boolean = false): List<ConstructorStanding> = withContext(Dispatchers.IO) {
        if (season < 1958) {
            return@withContext emptyList()
        }

        val cacheEnabled = isOfflineCacheEnabled()
        val cacheKey = "${season}_constructors"
        var cachedStandings: List<ConstructorStanding>? = null
        if (cacheEnabled) {
            try {
                val cached = database.f1Dao().getStandings(cacheKey)
                if (cached != null) {
                    val parsed = json.decodeFromString<List<ConstructorStanding>>(cached.payloadJson)
                    if (parsed.isNotEmpty()) {
                        if (!forceRefresh && !isExpired(cached.lastUpdatedMillis)) {
                            return@withContext parsed
                        }
                        cachedStandings = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        // Reuse valid non-expired cached driver standings before hitting network
        val validCachedDrivers: List<DriverStanding>? = if (!forceRefresh && cacheEnabled) {
            try {
                val cachedDrivers = database.f1Dao().getStandings("${season}_drivers")
                if (cachedDrivers != null && !isExpired(cachedDrivers.lastUpdatedMillis)) {
                    json.decodeFromString<List<DriverStanding>>(cachedDrivers.payloadJson)
                        .takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        // Remote Jolpica lookup
        if (jolpicaApi != null) {
            try {
                val (constructorDtos, driverDtos) = coroutineScope {
                    val constructorsDeferred = async {
                        fetchAllConstructorStandings(season)
                    }
                    val driversDeferred = if (validCachedDrivers == null) {
                        async {
                            safeSuspendCall { fetchAllDriverStandings(season).second }.orEmpty()
                        }
                    } else {
                        null
                    }
                    constructorsDeferred.await() to (driversDeferred?.await().orEmpty())
                }
                if (constructorDtos.isNotEmpty()) {
                    val driversByConstructorId = mutableMapOf<String, MutableList<ConstructorDriverPoints>>()
                    val driversByConstructorName = mutableMapOf<String, MutableList<ConstructorDriverPoints>>()
                    for (driverDto in driverDtos) {
                        val constructor = driverDto.constructors.lastOrNull() ?: continue
                        val driverPoints = ConstructorDriverPoints(
                            fullName = "${driverDto.driver.givenName} ${driverDto.driver.familyName}".trim(),
                            points = driverDto.points.toDoubleOrNull() ?: 0.0
                        )
                        val normId = constructor.constructorId.trim().lowercase(Locale.ROOT)
                        if (normId.isNotEmpty()) {
                            driversByConstructorId.getOrPut(normId) { mutableListOf() }.add(driverPoints)
                        }
                        val normName = constructor.name.trim().lowercase(Locale.ROOT)
                        if (normName.isNotEmpty()) {
                            driversByConstructorName.getOrPut(normName) { mutableListOf() }.add(driverPoints)
                        }
                    }

                    val cachedDriversByTeam: Map<F1TeamId, List<ConstructorDriverPoints>> =
                        if (driverDtos.isEmpty() && !validCachedDrivers.isNullOrEmpty()) {
                            validCachedDrivers
                                .groupBy { it.team }
                                .mapValues { (_, list) ->
                                    list.map {
                                        ConstructorDriverPoints(fullName = it.fullName, points = it.points)
                                    }
                                }
                        } else {
                            emptyMap()
                        }

                    val sortedConstructors = constructorDtos.sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }
                    val leaderPts = sortedConstructors.firstOrNull()?.points?.toDoubleOrNull() ?: 0.0
                    val converted = sortedConstructors.mapIndexed { index, dto ->
                        val team = F1TeamId.fromName(dto.constructor.name)
                        val pts = dto.points.toDoubleOrNull() ?: 0.0
                        val pos = dto.position.toIntOrNull() ?: (index + 1)
                        val prevPos = if (index > 0) (sortedConstructors[index - 1].position.toIntOrNull() ?: index) else pos
                        val prevPts = if (index > 0) sortedConstructors[index - 1].points.toDoubleOrNull() ?: pts else pts
                        val gapToP1 = if (index > 0) formatGap(leaderPts - pts, suffix = "to P1") else null
                        val gapToPrev = if (index > 0) formatGap(prevPts - pts, suffix = "to P$prevPos") else null
                        val winsCount = dto.wins.toIntOrNull() ?: 0

                        val teamDrivers = if (driverDtos.isNotEmpty()) {
                            val normId = dto.constructor.constructorId.trim().lowercase(Locale.ROOT)
                            val normName = dto.constructor.name.trim().lowercase(Locale.ROOT)
                            when {
                                normId.isNotEmpty() && driversByConstructorId.containsKey(normId) ->
                                    driversByConstructorId[normId].orEmpty()
                                normName.isNotEmpty() && driversByConstructorName.containsKey(normName) ->
                                    driversByConstructorName[normName].orEmpty()
                                else -> emptyList()
                            }
                        } else {
                            cachedDriversByTeam[team].orEmpty()
                        }

                        ConstructorStanding(
                            position = pos,
                            team = team,
                            points = pts,
                            wins = winsCount,
                            gapToP1 = gapToP1,
                            gapToPrev = gapToPrev,
                            drivers = teamDrivers,
                            seasonWins = emptyList(),
                            recentTeamForm = emptyList(),
                            constructorId = dto.constructor.constructorId,
                            constructorName = dto.constructor.name
                        )
                    }
                    if (cacheEnabled) {
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
                    }
                    return@withContext converted
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // fall through to expired cache or seed
            }
        }

        if (cacheEnabled && !cachedStandings.isNullOrEmpty()) {
            return@withContext cachedStandings
        }

        if (season == 2026) {
            return@withContext SeedDataProvider.getConstructorStandings2026()
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
     * Get live session state. Queries OpenF1 API concurrently when sessionKey is "latest" or numeric,
     * using a 20s in-memory cache and returning named/offline fixtures with `isLive = false` when offline.
     */
    suspend fun getLiveSessionState(sessionKey: String = "latest"): LiveSessionState = withContext(Dispatchers.IO) {
        val fallbackState = SeedDataProvider.getLiveSessionState(sessionKey).copy(isLive = false)
        val trimmedKey = sessionKey.trim()
        val shouldQueryOpenF1 = trimmedKey.equals("latest", ignoreCase = true) ||
            (trimmedKey.isNotBlank() && trimmedKey.all { it.isDigit() })
        if (!shouldQueryOpenF1 || openF1Api == null) {
            return@withContext fallbackState
        }

        val openF1SessionKey = if (trimmedKey.equals("latest", ignoreCase = true)) "latest" else trimmedKey
        val cacheKey = "${System.identityHashCode(openF1Api)}_${openF1SessionKey.lowercase(Locale.ROOT)}"
        val nowMillis = System.currentTimeMillis()
        liveSessionCache[cacheKey]?.let { cached ->
            if ((nowMillis - cached.fetchedAtMillis) < LIVE_SESSION_CACHE_TTL_MS) {
                return@withContext cached.state
            }
        }

        try {
            val fetched = coroutineScope {
                val sessionsDeferred = async { safeSuspendCall { openF1Api.getSessions(sessionKey = openF1SessionKey) } }
                val weatherDeferred = async { safeSuspendCall { openF1Api.getWeather(openF1SessionKey) } }
                val raceControlDeferred = async { safeSuspendCall { openF1Api.getRaceControl(openF1SessionKey) } }
                val driversDeferred = async { safeSuspendCall { openF1Api.getDrivers(openF1SessionKey) } }
                val positionsDeferred = async { safeSuspendCall { openF1Api.getPositions(openF1SessionKey) } }
                val intervalsDeferred = async { safeSuspendCall { openF1Api.getIntervals(openF1SessionKey) } }
                val lapsDeferred = async { safeSuspendCall { openF1Api.getLaps(openF1SessionKey) } }
                val stintsDeferred = async { safeSuspendCall { openF1Api.getStints(openF1SessionKey) } }
                val teamRadioDeferred = async { safeSuspendCall { openF1Api.getTeamRadio(openF1SessionKey) } }
                val overtakesDeferred = async { safeSuspendCall { openF1Api.getOvertakes(openF1SessionKey) } }

                OpenF1LiveFetchBundle(
                    sessions = sessionsDeferred.await().orEmpty(),
                    weather = weatherDeferred.await().orEmpty(),
                    raceControl = raceControlDeferred.await().orEmpty(),
                    drivers = driversDeferred.await().orEmpty(),
                    positions = positionsDeferred.await().orEmpty(),
                    intervals = intervalsDeferred.await().orEmpty(),
                    laps = lapsDeferred.await().orEmpty(),
                    stints = stintsDeferred.await().orEmpty(),
                    teamRadio = teamRadioDeferred.await().orEmpty(),
                    overtakes = overtakesDeferred.await().orEmpty()
                )
            }

            if (fetched.isEmpty()) {
                return@withContext fallbackState
            }

            val mappedState = mapOpenF1BundleToLiveState(fetched, fallbackState)
            liveSessionCache[cacheKey] = CachedLiveSessionEntry(
                state = mappedState,
                fetchedAtMillis = System.currentTimeMillis()
            )
            return@withContext mappedState
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // fall through to fallbackState
        }
        fallbackState
    }

    private data class OpenF1LiveFetchBundle(
        val sessions: List<OpenF1SessionDto>,
        val weather: List<OpenF1WeatherDto>,
        val raceControl: List<OpenF1RaceControlDto>,
        val drivers: List<OpenF1DriverDto>,
        val positions: List<OpenF1PositionDto>,
        val intervals: List<OpenF1IntervalDto>,
        val laps: List<OpenF1LapDto>,
        val stints: List<OpenF1StintDto>,
        val teamRadio: List<OpenF1TeamRadioDto>,
        val overtakes: List<OpenF1OvertakeDto>
    ) {
        fun isEmpty(): Boolean =
            sessions.isEmpty() &&
                weather.isEmpty() &&
                raceControl.isEmpty() &&
                drivers.isEmpty() &&
                positions.isEmpty() &&
                intervals.isEmpty() &&
                laps.isEmpty() &&
                stints.isEmpty() &&
                teamRadio.isEmpty() &&
                overtakes.isEmpty()
    }

    private fun mapOpenF1BundleToLiveState(
        bundle: OpenF1LiveFetchBundle,
        fallbackState: LiveSessionState
    ): LiveSessionState {
        val sessionDto = bundle.sessions.lastOrNull()
        val nowInstant = Instant.now()
        val startInstant = sessionDto?.dateStart?.let { parseIsoInstant(it) }
        val endInstant = sessionDto?.dateEnd?.let { parseIsoInstant(it) }
        val isLive = startInstant != null && endInstant != null &&
            !nowInstant.isBefore(startInstant) && !nowInstant.isAfter(endInstant)

        val timeRemaining = when {
            isLive -> {
                val secs = Duration.between(nowInstant, endInstant!!).seconds.coerceAtLeast(0)
                val hours = secs / 3600
                val mins = (secs % 3600) / 60
                val remSecs = secs % 60
                if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, remSecs)
                else String.format(Locale.ROOT, "%d:%02d", mins, remSecs)
            }
            startInstant != null && nowInstant.isBefore(startInstant) -> "Starts soon"
            endInstant != null && nowInstant.isAfter(endInstant) -> "Final"
            else -> fallbackState.timeRemaining
        }

        val latestWeatherDto = bundle.weather.lastOrNull()
        val enrichedWeather = if (latestWeatherDto != null) {
            WeatherSnapshot(
                trackTempC = latestWeatherDto.trackTemperature,
                airTempC = latestWeatherDto.airTemperature,
                humidityPercent = latestWeatherDto.humidity.roundToInt(),
                windSpeedKmh = ((latestWeatherDto.windSpeed * 3.6) * 10.0).roundToInt() / 10.0,
                windDirection = degreesToCompass(latestWeatherDto.windDirection),
                rainfallPercent = if (latestWeatherDto.rainfall > 0.0) 100 else 0,
                pressureMbar = latestWeatherDto.pressure
            )
        } else {
            fallbackState.weather
        }

        val enrichedMessages = if (bundle.raceControl.isNotEmpty()) {
            bundle.raceControl.takeLast(10).reversed().mapIndexed { index, dto ->
                RaceControlMessage(
                    id = "rc_${dto.sessionKey}_${dto.date}_${index}_${dto.message.hashCode()}",
                    title = dto.message.ifBlank { dto.flag ?: "FLAG" },
                    timestamp = formatRaceControlTimestamp(dto.date),
                    category = dto.category ?: "INFO"
                )
            }
        } else {
            fallbackState.raceControlMessages
        }

        val trackStatusText = if (bundle.raceControl.isNotEmpty()) {
            val latestRc = bundle.raceControl.last()
            val combined = "${latestRc.flag.orEmpty()} ${latestRc.message}".uppercase(Locale.ROOT)
            when {
                combined.contains("RED") -> "Red Flag"
                combined.contains("SAFETY CAR") || combined.contains("SC DEPLOYED") -> "Safety Car"
                combined.contains("VIRTUAL SAFETY CAR") || combined.contains("VSC") -> "Virtual Safety Car"
                combined.contains("DOUBLE YELLOW") -> "Double Yellow"
                combined.contains("YELLOW") -> "Yellow Flag"
                else -> "Track Clear"
            }
        } else {
            fallbackState.trackStatusText
        }

        val driverByNumber = bundle.drivers.associateBy { it.driverNumber }
        val mappedDrivers = if (bundle.drivers.isNotEmpty() || bundle.positions.isNotEmpty()) {
            buildLiveDrivers(
                driverByNumber = driverByNumber,
                positions = bundle.positions,
                intervals = bundle.intervals,
                laps = bundle.laps,
                stints = bundle.stints
            )
        } else {
            fallbackState.drivers
        }

        val bestDriver = mappedDrivers.firstOrNull { it.isOverallBest } ?: mappedDrivers.firstOrNull()
        val bestLapDriverCode = if (mappedDrivers.isNotEmpty()) {
            bestDriver?.code ?: fallbackState.bestLapDriverCode
        } else {
            fallbackState.bestLapDriverCode
        }
        val bestLapTime = if (mappedDrivers.isNotEmpty()) {
            bestDriver?.bestLapTime?.takeIf { it != "—" } ?: fallbackState.bestLapTime
        } else {
            fallbackState.bestLapTime
        }

        val mappedRadioClips = if (bundle.teamRadio.isNotEmpty()) {
            bundle.teamRadio.takeLast(10).reversed().mapIndexed { idx, dto ->
                val driver = driverByNumber[dto.driverNumber]
                val code = driver?.nameAcronym?.ifBlank { null } ?: "#${dto.driverNumber}"
                val name = driver?.fullName?.ifBlank { null } ?: "Driver #${dto.driverNumber}"
                val team = driver?.teamName?.takeIf { it.isNotBlank() }?.let { F1TeamId.fromName(it) } ?: F1TeamId.OTHER
                TeamRadioClip(
                    id = "rad_${dto.sessionKey}_${dto.driverNumber}_${dto.date}_$idx",
                    driverName = name,
                    driverCode = code,
                    team = team,
                    timestamp = formatRaceControlTimestamp(dto.date),
                    transcript = dto.transcript?.takeIf { it.isNotBlank() }
                        ?: if (!dto.recordingUrl.isNullOrBlank()) "Team radio audio available" else "Team radio transmission",
                    durationSeconds = 6,
                    recordingUrl = dto.recordingUrl?.takeIf { it.isNotBlank() }
                )
            }
        } else {
            fallbackState.radioClips
        }

        val latestRadio = mappedRadioClips.firstOrNull() ?: fallbackState.latestRadio

        val mappedOvertakes = if (bundle.overtakes.isNotEmpty()) {
            val maxLap = bundle.laps.maxOfOrNull { it.lapNumber } ?: 0
            bundle.overtakes.takeLast(20).reversed().map { dto ->
                val overtakingDriver = driverByNumber[dto.overtakingDriverNumber]
                val overtakenDriver = driverByNumber[dto.overtakenDriverNumber]
                OvertakeEvent(
                    lap = maxLap,
                    overtakingDriver = overtakingDriver?.nameAcronym?.ifBlank { null } ?: "#${dto.overtakingDriverNumber}",
                    overtakingTeam = overtakingDriver?.teamName?.takeIf { it.isNotBlank() }?.let { F1TeamId.fromName(it) } ?: F1TeamId.OTHER,
                    overtakenDriver = overtakenDriver?.nameAcronym?.ifBlank { null } ?: "#${dto.overtakenDriverNumber}",
                    overtakenTeam = overtakenDriver?.teamName?.takeIf { it.isNotBlank() }?.let { F1TeamId.fromName(it) } ?: F1TeamId.OTHER,
                    positionGained = 1,
                    cornerText = dto.position?.let { "P$it" } ?: "Track"
                )
            }
        } else {
            fallbackState.overtakes
        }

        val grandPrixName = sessionDto?.let { s ->
            val base = s.countryName?.takeIf { it.isNotBlank() } ?: s.location?.takeIf { it.isNotBlank() }
            if (base != null) {
                if (base.contains("Grand Prix", ignoreCase = true)) base else "$base Grand Prix"
            } else {
                fallbackState.grandPrixName
            }
        } ?: fallbackState.grandPrixName

        val sessionName = sessionDto?.sessionName?.takeIf { it.isNotBlank() } ?: fallbackState.sessionName
        val shortSessionCode = sessionDto?.sessionName?.takeIf { it.isNotBlank() }?.let { deriveShortSessionCode(it) }
            ?: fallbackState.shortSessionCode
        val circuitId = sessionDto?.let { s ->
            val raw = s.circuitShortName?.takeIf { it.isNotBlank() } ?: s.location.orEmpty()
            if (raw.isNotBlank()) lookupCircuitSpecs(raw, raw).circuitId else fallbackState.circuitId
        } ?: fallbackState.circuitId

        return LiveSessionState(
            grandPrixName = grandPrixName,
            sessionName = sessionName,
            shortSessionCode = shortSessionCode,
            timeRemaining = timeRemaining,
            bestLapDriverCode = bestLapDriverCode,
            bestLapTime = bestLapTime,
            carsOnTrack = if (mappedDrivers.isNotEmpty()) mappedDrivers.count { !it.isInPit } else fallbackState.carsOnTrack,
            trackStatusText = trackStatusText,
            sector1LengthKm = fallbackState.sector1LengthKm,
            sector2LengthKm = fallbackState.sector2LengthKm,
            sector3LengthKm = fallbackState.sector3LengthKm,
            circuitId = circuitId,
            drivers = mappedDrivers,
            latestRadio = latestRadio,
            radioClips = mappedRadioClips,
            raceControlMessages = enrichedMessages,
            overtakes = mappedOvertakes,
            weather = enrichedWeather,
            isLive = isLive
        )
    }

    private fun buildLiveDrivers(
        driverByNumber: Map<Int, OpenF1DriverDto>,
        positions: List<OpenF1PositionDto>,
        intervals: List<OpenF1IntervalDto>,
        laps: List<OpenF1LapDto>,
        stints: List<OpenF1StintDto>
    ): List<LiveDriverTiming> {
        val latestPosByDriver = mutableMapOf<Int, OpenF1PositionDto>()
        for (pos in positions) {
            if (pos.driverNumber > 0) latestPosByDriver[pos.driverNumber] = pos
        }

        val latestIntervalByDriver = mutableMapOf<Int, OpenF1IntervalDto>()
        for (iv in intervals) {
            if (iv.driverNumber > 0) latestIntervalByDriver[iv.driverNumber] = iv
        }

        val lapsByDriver = laps.filter { it.driverNumber > 0 }.groupBy { it.driverNumber }
        val latestStintByDriver = stints
            .filter { it.driverNumber > 0 }
            .groupBy { it.driverNumber }
            .mapValues { (_, list) -> list.maxByOrNull { it.stintNumber } }

        val overallBestLapSeconds = laps.mapNotNull { it.lapDuration?.takeIf { d -> d > 0.0 } }.minOrNull()
        val overallBestS1 = laps.mapNotNull { it.durationSector1?.takeIf { d -> d > 0.0 } }.minOrNull()
        val overallBestS2 = laps.mapNotNull { it.durationSector2?.takeIf { d -> d > 0.0 } }.minOrNull()
        val overallBestS3 = laps.mapNotNull { it.durationSector3?.takeIf { d -> d > 0.0 } }.minOrNull()

        val allDriverNumbers = (driverByNumber.keys + latestPosByDriver.keys + lapsByDriver.keys).distinct()
        val sortedNumbers = allDriverNumbers.sortedWith(
            compareBy<Int>(
                { latestPosByDriver[it]?.position?.takeIf { p -> p > 0 } ?: Int.MAX_VALUE },
                {
                    lapsByDriver[it]?.mapNotNull { l -> l.lapDuration?.takeIf { d -> d > 0.0 } }?.minOrNull()
                        ?: Double.MAX_VALUE
                },
                { it }
            )
        )

        return sortedNumbers.mapIndexed { idx, driverNumber ->
            val driverDto = driverByNumber[driverNumber]
            val pos = latestPosByDriver[driverNumber]?.position?.takeIf { it > 0 } ?: (idx + 1)
            val driverLaps = lapsByDriver[driverNumber].orEmpty()
            val lastLap = driverLaps.maxByOrNull { it.lapNumber }
            val bestLapDuration = driverLaps.mapNotNull { it.lapDuration?.takeIf { d -> d > 0.0 } }.minOrNull()
            val bestS1 = driverLaps.mapNotNull { it.durationSector1?.takeIf { d -> d > 0.0 } }.minOrNull()
            val bestS2 = driverLaps.mapNotNull { it.durationSector2?.takeIf { d -> d > 0.0 } }.minOrNull()
            val bestS3 = driverLaps.mapNotNull { it.durationSector3?.takeIf { d -> d > 0.0 } }.minOrNull()

            val iv = latestIntervalByDriver[driverNumber]
            val gapLeaderStr = if (pos == 1) {
                "Leader"
            } else {
                iv?.gapToLeaderDouble?.let { String.format(Locale.ROOT, "+%.3f", it) }
                    ?: iv?.gapToLeader?.toString()?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
                    ?: "—"
            }
            val intervalAheadStr = if (pos == 1) {
                "Leader"
            } else {
                iv?.intervalDouble?.let { String.format(Locale.ROOT, "+%.3f", it) }
                    ?: iv?.interval?.toString()?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
                    ?: "—"
            }

            val stint = latestStintByDriver[driverNumber]
            val compound = mapOpenF1Compound(stint?.compound)
            val tyreAge = if (stint != null) {
                val startAge = stint.tyreAgeAtStart ?: 0
                val currentLap = lastLap?.lapNumber ?: stint.lapStart
                (startAge + (currentLap - stint.lapStart).coerceAtLeast(0)).coerceAtLeast(0)
            } else {
                0
            }

            val isOverallBest = bestLapDuration != null && overallBestLapSeconds != null &&
                kotlin.math.abs(bestLapDuration - overallBestLapSeconds) < 0.0005

            val s1 = lastLap?.durationSector1
            val s2 = lastLap?.durationSector2
            val s3 = lastLap?.durationSector3

            LiveDriverTiming(
                position = pos,
                code = driverDto?.nameAcronym?.ifBlank { null } ?: "#$driverNumber",
                fullName = driverDto?.fullName?.ifBlank { null } ?: "Driver #$driverNumber",
                team = driverDto?.teamName?.takeIf { it.isNotBlank() }?.let { F1TeamId.fromName(it) } ?: F1TeamId.OTHER,
                bestLapTime = formatLapDuration(bestLapDuration),
                lastLapTime = formatLapDuration(lastLap?.lapDuration),
                gapToLeader = gapLeaderStr,
                intervalToAhead = intervalAheadStr,
                isOverallBest = isOverallBest,
                isInPit = false,
                isOutLap = lastLap?.isPitOutLap == true,
                tyreCompound = compound,
                tyreAgeLaps = tyreAge,
                sector1Status = resolveSectorStatus(s1, bestS1, overallBestS1),
                sector2Status = resolveSectorStatus(s2, bestS2, overallBestS2),
                sector3Status = resolveSectorStatus(s3, bestS3, overallBestS3),
                sector1Time = s1?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "—",
                sector2Time = s2?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "—",
                sector3Time = s3?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "—",
                trackProgress = ((idx + 1).toFloat() / (sortedNumbers.size + 1).coerceAtLeast(2).toFloat())
            )
        }
    }

    private fun resolveSectorStatus(
        sectorTime: Double?,
        driverBest: Double?,
        overallBest: Double?
    ): SectorStatus {
        if (sectorTime == null || sectorTime <= 0.0) return SectorStatus.INACTIVE
        if (overallBest != null && kotlin.math.abs(sectorTime - overallBest) < 0.0005) return SectorStatus.OVERALL_BEST
        if (driverBest != null && kotlin.math.abs(sectorTime - driverBest) < 0.0005) return SectorStatus.PERSONAL_BEST
        return SectorStatus.NORMAL
    }

    private fun formatLapDuration(seconds: Double?): String {
        if (seconds == null || seconds <= 0.0) return "—"
        val totalMillis = (seconds * 1000.0).roundToInt()
        val mins = totalMillis / 60000
        val remSecs = (totalMillis % 60000) / 1000.0
        return if (mins > 0) {
            String.format(Locale.ROOT, "%d:%06.3f", mins, remSecs)
        } else {
            String.format(Locale.ROOT, "%.3f", remSecs)
        }
    }

    private fun mapOpenF1Compound(raw: String?): TyreCompound {
        return when (raw?.trim()?.uppercase(Locale.ROOT)) {
            "SOFT" -> TyreCompound.SOFT
            "MEDIUM" -> TyreCompound.MEDIUM
            "HARD" -> TyreCompound.HARD
            "INTERMEDIATE", "INTER" -> TyreCompound.INTERMEDIATE
            "WET" -> TyreCompound.WET
            else -> TyreCompound.MEDIUM
        }
    }

    private fun deriveShortSessionCode(sessionName: String): String {
        val lower = sessionName.lowercase(Locale.ROOT)
        return when {
            lower.contains("practice 1") || lower == "fp1" -> "FP1"
            lower.contains("practice 2") || lower == "fp2" -> "FP2"
            lower.contains("practice 3") || lower == "fp3" -> "FP3"
            lower.contains("sprint qualifying") || lower.contains("shootout") -> "SQ"
            lower.contains("sprint") -> "SPR"
            lower.contains("qualifying") -> "Q"
            lower.contains("race") -> "RACE"
            else -> sessionName.take(4).uppercase(Locale.ROOT)
        }
    }

    private fun parseIsoInstant(raw: String): Instant? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")
        return runCatching { OffsetDateTime.parse(normalized).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(normalized) }.getOrNull()
    }

    private fun degreesToCompass(degrees: Double): String {
        val directions = listOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        val index = (normalized / 22.5).roundToInt() % 16
        return directions[index]
    }

    private fun formatRaceControlTimestamp(rawDate: String): String {
        if (rawDate.isBlank()) return "LIVE"
        val instant = try {
            OffsetDateTime.parse(rawDate).toInstant()
        } catch (_: Exception) {
            try {
                Instant.parse(rawDate)
            } catch (_: Exception) {
                null
            }
        }
        return if (instant != null) {
            instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        } else {
            rawDate.takeLast(8).ifBlank { "LIVE" }
        }
    }

    private data class ParsedSessionHolder(
        val item: SessionScheduleItem,
        val localDateTime: ZonedDateTime
    )

    private fun mapRaceDtoToWeekend(dto: RaceDto, season: Int, idx: Int): RaceWeekend {
        val parsedList = mutableListOf<ParsedSessionHolder>()

        dto.firstPractice?.let {
            parseSessionWithTime(it, "FP1", "Free Practice 1", false)?.let { p -> parsedList.add(p) }
        }
        dto.secondPractice?.let {
            parseSessionWithTime(it, "FP2", "Free Practice 2", false)?.let { p -> parsedList.add(p) }
        }
        dto.thirdPractice?.let {
            parseSessionWithTime(it, "FP3", "Free Practice 3", false)?.let { p -> parsedList.add(p) }
        }
        (dto.sprintQualifying ?: dto.sprintShootout)?.let {
            parseSessionWithTime(it, "SQ", "Sprint Qualifying", false)?.let { p -> parsedList.add(p) }
        }
        dto.sprint?.let {
            parseSessionWithTime(it, "Sprint", "Sprint Race", false)?.let { p -> parsedList.add(p) }
        }
        dto.qualifying?.let {
            parseSessionWithTime(it, "Quali", "Qualifying", false)?.let { p -> parsedList.add(p) }
        }
        parseSessionWithTime(SessionDto(dto.date, dto.time), "Race", "Race", true)?.let { p ->
            parsedList.add(p)
        }

        // Sort chronologically by local date-time
        parsedList.sortBy { it.localDateTime }
        val sortedSessions = parsedList.map { it.item }

        val firstSession = sortedSessions.firstOrNull()
        val raceSession = sortedSessions.firstOrNull { it.isRace } ?: sortedSessions.lastOrNull()

        val dateRangeText = if (firstSession != null && raceSession != null && firstSession != raceSession) {
            "${firstSession.dayOfWeek.uppercase(Locale.ROOT)} ${firstSession.dateDay} ${firstSession.dateMonth.uppercase(Locale.ROOT)} - ${raceSession.dayOfWeek.uppercase(Locale.ROOT)} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase(Locale.ROOT)}"
        } else if (raceSession != null) {
            "${raceSession.dayOfWeek.uppercase(Locale.ROOT)} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase(Locale.ROOT)}"
        } else {
            dto.date
        }

        val raceLocalDate = raceSession?.let { r ->
            if (r.utcEpochMillis != null) {
                Instant.ofEpochMilli(r.utcEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                val month = monthNameToNumber(r.dateMonth)
                val day = r.dateDay.toIntOrNull()
                if (month != null && day != null) {
                    runCatching { LocalDate.of(dto.season.toIntOrNull() ?: season, month, day) }.getOrNull()
                } else null
            }
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
            isSprintWeekend = dto.sprint != null || dto.sprintQualifying != null || dto.sprintShootout != null
        )
    }

    private fun parseSessionWithTime(
        sessionDto: SessionDto,
        shortName: String,
        fullName: String,
        isRace: Boolean
    ): ParsedSessionHolder? {
        if (sessionDto.date.isBlank()) return null
        val parsedDate = runCatching { LocalDate.parse(sessionDto.date.trim()) }.getOrNull() ?: return null
        val rawTime = sessionDto.time?.trim()?.takeIf { it.isNotBlank() }

        val offsetTime = if (rawTime != null) {
            val normalizedTime = rawTime.replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")
            runCatching { OffsetTime.parse(normalizedTime) }.getOrNull()
                ?: runCatching { LocalTime.parse(normalizedTime).atOffset(ZoneOffset.UTC) }.getOrNull()
                ?: runCatching {
                    val parts = normalizedTime.split(":")
                    val local = when (parts.size) {
                        1 -> LocalTime.of(parts[0].toInt(), 0)
                        2 -> LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        else -> LocalTime.of(parts[0].toInt(), parts[1].toInt(), parts[2].substringBefore('.').toInt())
                    }
                    local.atOffset(ZoneOffset.UTC)
                }.getOrNull()
                ?: OffsetTime.of(LocalTime.of(14, 0), ZoneOffset.UTC)
        } else {
            OffsetTime.of(LocalTime.of(14, 0), ZoneOffset.UTC)
        }

        val instant = OffsetDateTime.of(parsedDate, offsetTime.toLocalTime(), offsetTime.offset).toInstant()
        val localZonedDateTime = instant.atZone(ZoneId.systemDefault())

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
            isRace = isRace,
            utcEpochMillis = instant.toEpochMilli()
        )
        return ParsedSessionHolder(item, localZonedDateTime)
    }

    companion object {
        val sharedHttpClient: OkHttpClient get() = sharedOkHttpClient

        val sharedJson: Json by lazy {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
        }

        val sharedJolpicaApi: JolpicaApiService? by lazy {
            try {
                JolpicaApiService.create(sharedOkHttpClient)
            } catch (_: Throwable) {
                null
            }
        }

        val sharedOpenF1Api: OpenF1ApiService? by lazy {
            try {
                OpenF1ApiService.create(sharedOkHttpClient)
            } catch (_: Throwable) {
                null
            }
        }

        private const val LIVE_SESSION_CACHE_TTL_MS = 20_000L

        private data class CachedLiveSessionEntry(
            val state: LiveSessionState,
            val fetchedAtMillis: Long
        )

        private val liveSessionCache = ConcurrentHashMap<String, CachedLiveSessionEntry>()

        internal fun clearLiveSessionCacheForTesting() {
            liveSessionCache.clear()
        }

        private fun formatPoints(value: Double): String {
            val rounded = (value * 10.0).roundToInt() / 10.0
            return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
        }

        private fun formatGap(diff: Double, prefix: String = "", suffix: String): String? {
            val rounded = (diff * 10.0).roundToInt() / 10.0
            if (rounded <= 0.0) return null
            val formatted = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
            return "$prefix$formatted $suffix"
        }

        fun formatCountdown(daysToGo: Long, isRace: Boolean): String {
            return when {
                daysToGo < 0 -> if (isRace) "Final" else "Completed"
                daysToGo == 0L -> if (isRace) "Race Day" else "Today"
                daysToGo == 1L -> "In 1 day"
                else -> "In $daysToGo days"
            }
        }

        private fun monthNameToNumber(monthName: String): Int? {
            return when (monthName.lowercase(Locale.ROOT).trim()) {
                "jan", "january" -> 1
                "feb", "february" -> 2
                "mar", "march" -> 3
                "apr", "april" -> 4
                "may" -> 5
                "jun", "june" -> 6
                "jul", "july" -> 7
                "aug", "august" -> 8
                "sep", "sept", "september" -> 9
                "oct", "october" -> 10
                "nov", "november" -> 11
                "dec", "december" -> 12
                else -> null
            }
        }

        fun updateWeekendCountdowns(weekend: RaceWeekend): RaceWeekend {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val updatedSessions = weekend.sessions.map { session ->
                val utcMillis = session.utcEpochMillis
                if (utcMillis != null) {
                    val localZdt = Instant.ofEpochMilli(utcMillis).atZone(zone)
                    val daysDiff = ChronoUnit.DAYS.between(today, localZdt.toLocalDate())
                    session.copy(
                        dayOfWeek = localZdt.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                        fullDayName = localZdt.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)),
                        dateDay = localZdt.format(DateTimeFormatter.ofPattern("dd")),
                        dateMonth = localZdt.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)),
                        localTime = localZdt.format(DateTimeFormatter.ofPattern("HH:mm")),
                        countdownBadge = formatCountdown(daysDiff, session.isRace)
                    )
                } else {
                    val month = monthNameToNumber(session.dateMonth)
                    val day = session.dateDay.toIntOrNull()
                    val sessionDate = if (month != null && day != null) {
                        runCatching { LocalDate.of(weekend.season, month, day) }.getOrNull()
                    } else null

                    if (sessionDate != null) {
                        val daysDiff = ChronoUnit.DAYS.between(today, sessionDate)
                        session.copy(countdownBadge = formatCountdown(daysDiff, session.isRace))
                    } else {
                        session
                    }
                }
            }

            val raceSession = updatedSessions.firstOrNull { it.isRace } ?: updatedSessions.lastOrNull()
            val raceDate = raceSession?.let { r ->
                if (r.utcEpochMillis != null) {
                    Instant.ofEpochMilli(r.utcEpochMillis).atZone(zone).toLocalDate()
                } else {
                    val month = monthNameToNumber(r.dateMonth)
                    val day = r.dateDay.toIntOrNull()
                    if (month != null && day != null) {
                        runCatching { LocalDate.of(weekend.season, month, day) }.getOrNull()
                    } else null
                }
            }

            val daysToGo = if (raceDate != null) {
                ChronoUnit.DAYS.between(today, raceDate).coerceAtLeast(0).toInt()
            } else {
                weekend.daysToGo
            }

            val firstSession = updatedSessions.firstOrNull()
            val updatedDateRangeText = if (firstSession != null && raceSession != null && firstSession != raceSession) {
                "${firstSession.dayOfWeek.uppercase(Locale.ROOT)} ${firstSession.dateDay} ${firstSession.dateMonth.uppercase(Locale.ROOT)} - ${raceSession.dayOfWeek.uppercase(Locale.ROOT)} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase(Locale.ROOT)}"
            } else if (weekend.dateRangeText.isBlank() && raceSession != null) {
                "${raceSession.dayOfWeek.uppercase(Locale.ROOT)} ${raceSession.dateDay} ${raceSession.dateMonth.uppercase(Locale.ROOT)}"
            } else {
                weekend.dateRangeText
            }

            return weekend.copy(
                dateRangeText = updatedDateRangeText,
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

        private val PACE_REGEX = Regex("\\bpace\\b")
        private val SPA_REGEX = Regex("\\bspa\\b")
        private val COTA_REGEX = Regex("\\bcota\\b")
        private val AUSTIN_REGEX = Regex("\\baustin\\b")
        private val BAKU_REGEX = Regex("\\bbaku\\b")
        private val IMOLA_REGEX = Regex("\\bimola\\b")
        private val ENZO_REGEX = Regex("\\benzo\\b")

        fun lookupCircuitSpecs(circuitIdRaw: String, circuitNameRaw: String): CircuitSpecs {
            val key = "${circuitIdRaw.lowercase(Locale.ROOT)} ${circuitNameRaw.lowercase(Locale.ROOT)}"
                .replace('_', ' ')
                .replace('-', ' ')
            return when {
                key.contains("interlagos") || PACE_REGEX.containsMatchIn(key) || key.contains("brazil") ->
                    CircuitSpecs("INTERLAGOS", "4.309", 71, 15)
                key.contains("sepang") || key.contains("malaysia") ->
                    CircuitSpecs("SEPANG", "5.543", 56, 15)
                key.contains("bahrain") || key.contains("sakhir") ->
                    CircuitSpecs("BAHRAIN", "5.412", 57, 15)
                key.contains("yas marina") || key.contains("abu dhabi") ->
                    CircuitSpecs("YAS_MARINA", "5.281", 58, 16)
                key.contains("silverstone") ->
                    CircuitSpecs("SILVERSTONE", "5.891", 52, 18)
                key.contains("monza") ->
                    CircuitSpecs("MONZA", "5.793", 53, 11)
                key.contains("monaco") || key.contains("monte carlo") ->
                    CircuitSpecs("MONACO", "3.337", 78, 19)
                key.contains("catalunya") || key.contains("barcelona") ->
                    CircuitSpecs("CATALUNYA", "4.657", 66, 14)
                SPA_REGEX.containsMatchIn(key) || key.contains("francorchamps") ->
                    CircuitSpecs("SPA", "7.004", 44, 19)
                key.contains("suzuka") ->
                    CircuitSpecs("SUZUKA", "5.807", 53, 18)
                key.contains("albert park") || key.contains("melbourne") ->
                    CircuitSpecs("MELBOURNE", "5.278", 58, 14)
                key.contains("shanghai") ->
                    CircuitSpecs("SHANGHAI", "5.451", 56, 16)
                key.contains("miami") ->
                    CircuitSpecs("MIAMI", "5.412", 57, 19)
                key.contains("villeneuve") || key.contains("montreal") ->
                    CircuitSpecs("MONTREAL", "4.361", 70, 14)
                key.contains("red bull ring") || key.contains("spielberg") || key.contains("austria") ->
                    CircuitSpecs("SPIELBERG", "4.318", 71, 10)
                key.contains("hungaroring") || key.contains("budapest") ->
                    CircuitSpecs("HUNGARORING", "4.381", 70, 14)
                key.contains("zandvoort") ->
                    CircuitSpecs("ZANDVOORT", "4.259", 72, 14)
                key.contains("marina bay") || key.contains("singapore") ->
                    CircuitSpecs("SINGAPORE", "4.940", 62, 19)
                key.contains("americas") || COTA_REGEX.containsMatchIn(key) || AUSTIN_REGEX.containsMatchIn(key) ->
                    CircuitSpecs("COTA", "5.513", 56, 20)
                key.contains("rodriguez") || key.contains("mexico") ->
                    CircuitSpecs("MEXICO", "4.304", 71, 17)
                key.contains("vegas") ->
                    CircuitSpecs("LAS_VEGAS", "6.201", 50, 17)
                key.contains("losail") || key.contains("lusail") || key.contains("qatar") ->
                    CircuitSpecs("QATAR", "5.419", 57, 16)
                key.contains("jeddah") || key.contains("corniche") ->
                    CircuitSpecs("JEDDAH", "6.174", 50, 27)
                BAKU_REGEX.containsMatchIn(key) || key.contains("azerbaijan") ->
                    CircuitSpecs("BAKU", "6.003", 51, 20)
                IMOLA_REGEX.containsMatchIn(key) || ENZO_REGEX.containsMatchIn(key) ->
                    CircuitSpecs("IMOLA", "4.909", 63, 19)
                else ->
                    CircuitSpecs(circuitIdRaw.uppercase(Locale.ROOT).ifBlank { "SEPANG" }, "5.000", 50, 15)
            }
        }
    }

    fun getCountryFlag(country: String): String {
        return when (country.lowercase(Locale.ROOT).trim()) {
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
