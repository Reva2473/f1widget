package com.reva2473.f1widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.reva2473.f1widget.data.local.CachedScheduleEntity
import com.reva2473.f1widget.data.local.CachedStandingsEntity
import com.reva2473.f1widget.data.local.DEFAULT_TTL_MS
import com.reva2473.f1widget.data.local.F1Dao
import com.reva2473.f1widget.data.local.F1Database
import com.reva2473.f1widget.data.local.UserPreferencesStore
import com.reva2473.f1widget.data.local.isExpired
import com.reva2473.f1widget.data.remote.AverageSpeedDto
import com.reva2473.f1widget.data.remote.CircuitDto
import com.reva2473.f1widget.data.remote.ConstructorDto
import com.reva2473.f1widget.data.remote.ConstructorStandingDto
import com.reva2473.f1widget.data.remote.DriverDto
import com.reva2473.f1widget.data.remote.DriverStandingDto
import com.reva2473.f1widget.data.remote.FastestLapDto
import com.reva2473.f1widget.data.remote.JolpicaApiService
import com.reva2473.f1widget.data.remote.JolpicaResponse
import com.reva2473.f1widget.data.remote.LapTimeDto
import com.reva2473.f1widget.data.remote.LocationDto
import com.reva2473.f1widget.data.remote.MRData
import com.reva2473.f1widget.data.remote.OpenF1ApiService
import com.reva2473.f1widget.data.remote.OpenF1IntervalDto
import com.reva2473.f1widget.data.remote.OpenF1LapDto
import com.reva2473.f1widget.data.remote.OpenF1OvertakeDto
import com.reva2473.f1widget.data.remote.OpenF1PositionDto
import com.reva2473.f1widget.data.remote.OpenF1RaceControlDto
import com.reva2473.f1widget.data.remote.OpenF1SessionDto
import com.reva2473.f1widget.data.remote.OpenF1StintDto
import com.reva2473.f1widget.data.remote.OpenF1TeamRadioDto
import com.reva2473.f1widget.data.remote.OpenF1WeatherDto
import com.reva2473.f1widget.data.remote.RaceDto
import com.reva2473.f1widget.data.remote.RaceTableWrapper
import com.reva2473.f1widget.data.remote.ResultDto
import com.reva2473.f1widget.data.remote.SessionDto
import com.reva2473.f1widget.data.remote.StandingsListDto
import com.reva2473.f1widget.data.remote.StandingsTableWrapper
import com.reva2473.f1widget.data.remote.TimeDto
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.domain.model.UserPreferencesState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Unit tests verifying SeedDataProvider integrity, standings calculations,
 * tyre stint structures, driver career comparisons, live timing state,
 * F1TeamId historical mapping without RB collision, F1Dao disk cache TTL & atomic I/O,
 * DTO serialization/deserialization resilience, fake network integration, and F1Repository operations.
 */
class F1RepositoryTest {

    @Test
    fun testNextRaceWeekendMatchesSepangHero() {
        val weekend = SeedDataProvider.getNextRaceWeekend()
        assertEquals(2026, weekend.season)
        assertEquals(16, weekend.round)
        assertEquals("Bahrain Grand Prix in Malaysia", weekend.grandPrixName)
        assertEquals("Sepang International Circuit", weekend.circuitName)
        assertEquals("Kuala Lumpur, Malaysia", weekend.location)
        assertEquals("5.543", weekend.circuitLengthKm)
        assertEquals(56, weekend.totalLaps)
        assertEquals(15, weekend.totalCorners)
        assertEquals(1, weekend.daysToGo)
        assertEquals(5, weekend.sessions.size)

        val fp1 = weekend.sessions.first { it.shortName == "FP1" }
        assertEquals("05:30", fp1.localTime)
        assertEquals("Fri", fp1.dayOfWeek)

        val race = weekend.sessions.first { it.shortName == "Race" }
        assertEquals("08:00", race.localTime)
        assertEquals("Sun", race.dayOfWeek)
        assertTrue(race.isRace)
    }

    @Test
    fun testBrazilianGrandPrix2026Schedule() {
        val weekend = SeedDataProvider.getBrazilianGpWeekend2026()
        assertEquals(2026, weekend.season)
        assertEquals(22, weekend.round)
        assertEquals("Brazilian Grand Prix", weekend.grandPrixName)
        assertEquals("Autódromo José Carlos Pace", weekend.circuitName)
        assertEquals("São Paulo, Brazil", weekend.location)
        assertEquals(71, weekend.daysToGo)
        assertEquals(5, weekend.sessions.size)

        // Chronologically ordered: FP1 -> FP2 -> FP3 -> Quali -> Race
        assertEquals("FP1", weekend.sessions[0].shortName)
        assertEquals("FP2", weekend.sessions[1].shortName)
        assertEquals("FP3", weekend.sessions[2].shortName)
        assertEquals("Quali", weekend.sessions[3].shortName)
        assertEquals("Race", weekend.sessions[4].shortName)

        val race = weekend.sessions.first { it.shortName == "Race" }
        assertEquals("17:00", race.localTime)
        assertEquals("08", race.dateDay)
        assertEquals("Nov", race.dateMonth)
        assertEquals("Race in 71 days", race.countdownBadge)

        // Verify chronological order across 2026 schedule: US=20, Mexico=21, Brazil=22
        val schedule2026 = SeedDataProvider.getSeasonSchedule(2026)
        assertEquals(24, schedule2026.size)
        assertEquals("United States Grand Prix", schedule2026.first { it.round == 20 }.grandPrixName)
        assertEquals("Mexico City Grand Prix", schedule2026.first { it.round == 21 }.grandPrixName)
        assertEquals("Brazilian Grand Prix", schedule2026.first { it.round == 22 }.grandPrixName)
    }

    @Test
    fun testSaoPaulo2024ResultsTopFive() {
        val weekend = SeedDataProvider.getSaoPaulo2024Results()
        assertEquals(2024, weekend.season)
        assertEquals(21, weekend.round)
        assertEquals(69, weekend.totalLaps)

        val results = weekend.results
        assertTrue(results.size >= 5)

        val p1 = results[0]
        assertEquals(1, p1.position)
        assertEquals("Max Verstappen", p1.driverFullName)
        assertEquals(F1TeamId.RED_BULL, p1.team)
        assertEquals("2:06:54.430", p1.finishTimeOrGap)
        assertEquals(26.0, p1.points, 0.001)
        assertEquals("26", p1.formattedPoints)

        val p2 = results[1]
        assertEquals(2, p2.position)
        assertEquals("Esteban Ocon", p2.driverFullName)
        assertEquals(F1TeamId.ALPINE, p2.team)
        assertEquals("+19.477", p2.finishTimeOrGap)

        val p3 = results[2]
        assertEquals(3, p3.position)
        assertEquals("Pierre Gasly", p3.driverFullName)
        assertEquals(F1TeamId.ALPINE, p3.team)
        assertEquals("+22.532", p3.finishTimeOrGap)

        val p4 = results[3]
        assertEquals(4, p4.position)
        assertEquals("George Russell", p4.driverFullName)
        assertEquals(F1TeamId.MERCEDES, p4.team)

        val p5 = results[4]
        assertEquals(5, p5.position)
        assertEquals("Charles Leclerc", p5.driverFullName)
        assertEquals(F1TeamId.FERRARI, p5.team)
    }

    @Test
    fun testAbuDhabi2023ResultsStintsSequence() {
        val weekend = SeedDataProvider.getAbuDhabi2023Results()
        assertEquals(2023, weekend.season)
        assertEquals(22, weekend.round)
        assertEquals(58, weekend.totalLaps)

        val results = weekend.results
        assertTrue(results.size >= 3)

        // Verstappen: Medium -> Hard -> Hard
        val ver = results[0]
        assertEquals("VER", ver.driverCode)
        assertEquals(3, ver.stints.size)
        assertEquals(TyreCompound.MEDIUM, ver.stints[0].compound)
        assertEquals(16, ver.stints[0].lapsCount)
        assertEquals("21.6s", ver.stints[0].pitStopDuration)
        assertEquals(16, ver.stints[0].pitStopLap)

        assertEquals(TyreCompound.HARD, ver.stints[1].compound)
        assertEquals(27, ver.stints[1].lapsCount)
        assertEquals("21.2s", ver.stints[1].pitStopDuration)
        assertEquals(43, ver.stints[1].pitStopLap)

        assertEquals(TyreCompound.HARD, ver.stints[2].compound)
        assertEquals(15, ver.stints[2].lapsCount)
        assertNull(ver.stints[2].pitStopDuration)

        // Leclerc: Medium -> Hard -> Hard
        val lec = results[1]
        assertEquals("LEC", lec.driverCode)
        assertEquals(3, lec.stints.size)
        assertEquals(TyreCompound.MEDIUM, lec.stints[0].compound)
        assertEquals(17, lec.stints[0].lapsCount)

        // Russell: Medium -> Hard -> Hard
        val rus = results[2]
        assertEquals("RUS", rus.driverCode)
        assertEquals(3, rus.stints.size)
        assertEquals(TyreCompound.MEDIUM, rus.stints[0].compound)
        assertEquals(15, rus.stints[0].lapsCount)
    }

    @Test
    fun testDriverStandings2026MatchesScreenshots() {
        val standings = SeedDataProvider.getDriverStandings2026()
        assertTrue(standings.size >= 8)

        // 1. Antonelli
        val p1 = standings[0]
        assertEquals(1, p1.position)
        assertEquals("ANT", p1.code)
        assertEquals("Kimi Antonelli", p1.fullName)
        assertEquals(F1TeamId.MERCEDES, p1.team)
        assertEquals(302.0, p1.points, 0.01)
        assertEquals(8, p1.wins)
        assertEquals(12, p1.podiums)
        assertEquals(6, p1.poles)
        assertEquals(20.1, p1.avgPtsPerRace, 0.01)
        assertEquals(15, p1.racesCount)
        assertEquals("P5, P1, P1 (60 pts)", p1.last3Summary)

        // 2. Russell
        val p2 = standings[1]
        assertEquals(2, p2.position)
        assertEquals("RUS", p2.code)
        assertEquals("George Russell", p2.fullName)
        assertEquals(236.0, p2.points, 0.01)
        assertEquals(3, p2.wins)
        assertEquals(8, p2.podiums)
        assertEquals("-66 to leader", p2.gapToLeader)

        // 3. Hamilton
        val p3 = standings[2]
        assertEquals(3, p3.position)
        assertEquals("HAM", p3.code)
        assertEquals("Lewis Hamilton", p3.fullName)
        assertEquals(F1TeamId.FERRARI, p3.team)
        assertEquals(199.0, p3.points, 0.01)
        assertEquals("-103 to leader", p3.gapToLeader)

        // 4. Norris
        val p4 = standings[3]
        assertEquals(4, p4.position)
        assertEquals(186.0, p4.points, 0.01)
        assertEquals(2, p4.wins)
        assertEquals("P4, P2, P4 (42 pts)", p4.last3Summary)

        // 5. Leclerc
        val p5 = standings[4]
        assertEquals(5, p5.position)
        assertEquals(179.0, p5.points, 0.01)

        // 6. Verstappen
        val p6 = standings[5]
        assertEquals(6, p6.position)
        assertEquals(163.0, p6.points, 0.01)

        // 7. Piastri
        val p7 = standings[6]
        assertEquals(7, p7.position)
        assertEquals(120.0, p7.points, 0.01)

        // 8. Hadjar
        val p8 = standings[7]
        assertEquals(8, p8.position)
        assertEquals(86.0, p8.points, 0.01)
    }

    @Test
    fun testConstructorStandings2026MatchesScreenshots() {
        val constructors = SeedDataProvider.getConstructorStandings2026()
        assertEquals(11, constructors.size)

        // Mercedes
        val merc = constructors[0]
        assertEquals(1, merc.position)
        assertEquals(F1TeamId.MERCEDES, merc.team)
        assertEquals(538.0, merc.points, 0.01)
        assertEquals(11, merc.wins)
        assertEquals(11, merc.seasonWins.size)
        assertEquals(2, merc.drivers.size)
        assertEquals("Kimi Antonelli", merc.drivers[0].fullName)
        assertEquals(302.0, merc.drivers[0].points, 0.01)
        assertEquals("George Russell", merc.drivers[1].fullName)
        assertEquals(236.0, merc.drivers[1].points, 0.01)
        assertEquals("Mercedes", merc.resolvedConstructorName)
        assertEquals("mercedes", merc.stableKey)

        // Ferrari
        val fer = constructors[1]
        assertEquals(2, fer.position)
        assertEquals(F1TeamId.FERRARI, fer.team)
        assertEquals(378.0, fer.points, 0.01)
        assertEquals("160 to P1", fer.gapToP1)

        // McLaren
        val mcl = constructors[2]
        assertEquals(3, mcl.position)
        assertEquals(F1TeamId.MCLAREN, mcl.team)
        assertEquals(306.0, mcl.points, 0.01)
        assertEquals("232 to P1", mcl.gapToP1)
        assertEquals("72 to P2", mcl.gapToPrev)

        // Red Bull
        val rbr = constructors[3]
        assertEquals(4, rbr.position)
        assertEquals(F1TeamId.RED_BULL, rbr.team)
        assertEquals(249.0, rbr.points, 0.01)
    }

    @Test
    fun testDriverCareerComparisonVerstappenVsNorris() {
        val comparisons = SeedDataProvider.getDriverComparisons()
        val ver = comparisons.first { it.code == "VER" }
        val nor = comparisons.first { it.code == "NOR" }

        // Verstappen stats
        assertEquals("Max", ver.firstName)
        assertEquals("Verstappen", ver.lastName)
        assertEquals(4, ver.championships)
        assertEquals(71, ver.raceWins)
        assertEquals(134, ver.podiums)
        assertEquals(48, ver.polePositions)
        assertEquals(37, ver.fastestLaps)
        assertEquals(3607.5, ver.careerPoints, 0.01)
        assertEquals(248, ver.raceStarts)
        assertEquals(37, ver.dnfs)
        assertEquals(28.6, ver.winRatePercent, 0.01)
        assertEquals(54.0, ver.podiumRatePercent, 0.01)
        assertEquals(19.4, ver.poleRatePercent, 0.01)

        // Norris stats
        assertEquals("Lando", nor.firstName)
        assertEquals("Norris", nor.lastName)
        assertEquals(1, nor.championships)
        assertEquals(13, nor.raceWins)
        assertEquals(49, nor.podiums)
        assertEquals(19, nor.polePositions)
        assertEquals(20, nor.fastestLaps)
        assertEquals(1616.0, nor.careerPoints, 0.01)
        assertEquals(167, nor.raceStarts)
        assertEquals(18, nor.dnfs)
        assertEquals(7.8, nor.winRatePercent, 0.01)
        assertEquals(29.3, nor.podiumRatePercent, 0.01)
        assertEquals(11.4, nor.poleRatePercent, 0.01)
    }

    @Test
    fun testLiveTimingStateStructure() {
        val session = SeedDataProvider.getLiveSessionState("BAHRAIN")
        assertEquals("Bahrain Grand Prix", session.grandPrixName)
        assertEquals("Qualifying · Q1", session.sessionName)
        assertEquals("8:21", session.timeRemaining)
        assertEquals("VER", session.bestLapDriverCode)
        assertEquals("1:36.477", session.bestLapTime)
        assertEquals(21, session.carsOnTrack)
        assertEquals("Track Clear", session.trackStatusText)

        // Drivers in timing tower
        assertTrue(session.drivers.size >= 20)
        val p1 = session.drivers[0]
        assertEquals("VER", p1.code)
        assertEquals("1:36.477", p1.bestLapTime)
        assertTrue(p1.isOverallBest)
        assertEquals(SectorStatus.OVERALL_BEST, p1.sector1Status)
        assertEquals(TyreCompound.SOFT, p1.tyreCompound)

        val p2 = session.drivers[1]
        assertEquals("HAM", p2.code)
        assertEquals("1:36.595", p2.bestLapTime)
        assertEquals("+0.118", p2.gapToLeader)
        assertEquals(TyreCompound.MEDIUM, p2.tyreCompound)

        // Team radio clip
        assertNotNull(session.latestRadio)
        assertEquals("Fernando Alonso", session.latestRadio.driverName)
        assertEquals("ALO", session.latestRadio.driverCode)
        assertEquals("08:55:53", session.latestRadio.timestamp)

        // Race control
        assertTrue(session.raceControlMessages.isNotEmpty())
        assertEquals("SESSION STARTED", session.raceControlMessages[0].title)
    }

    @Test
    fun testSaoPauloLiveSessionStateFp1Data() {
        val session = SeedDataProvider.getSaoPauloLiveSessionState()
        assertEquals("São Paulo Grand Prix", session.grandPrixName)
        assertEquals("FP1", session.sessionName)
        assertEquals("FP1", session.shortSessionCode)
        assertEquals("38:29", session.timeRemaining)
        assertEquals("VER", session.bestLapDriverCode)
        assertEquals("1:12.959", session.bestLapTime)
        assertEquals(session.drivers.count { !it.isInPit }, session.carsOnTrack)
        assertEquals(20, session.carsOnTrack)
        assertEquals("Track Clear", session.trackStatusText)
        assertEquals("INTERLAGOS", session.circuitId)
        assertEquals("T1-T3  1.2 km", session.sector1LengthKm)
        assertEquals("T4-T11  1.9 km", session.sector2LengthKm)
        assertEquals("T12-T15  1.1 km", session.sector3LengthKm)

        // Timing tower drivers
        assertEquals(22, session.drivers.size)

        // P1: Verstappen
        val p1 = session.drivers[0]
        assertEquals("VER", p1.code)
        assertEquals("1:12.959", p1.bestLapTime)
        assertTrue(p1.isOverallBest)
        assertTrue(p1.isInPit)
        assertEquals(SectorStatus.PERSONAL_BEST, p1.sector1Status)
        assertEquals(SectorStatus.OVERALL_BEST, p1.sector2Status)
        assertEquals(SectorStatus.OVERALL_BEST, p1.sector3Status)
    }

    @Test
    fun testF1TeamIdHistoricalMappingWithoutRbCollision() {
        assertEquals(F1TeamId.RED_BULL, F1TeamId.fromName("Red Bull Racing"))
        assertEquals(F1TeamId.RED_BULL, F1TeamId.fromName("Red Bull"))
        assertEquals(F1TeamId.RED_BULL, F1TeamId.fromName("RBR"))

        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("RB"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("RB F1"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("Racing Bulls"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("AlphaTauri"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("Toro Rosso"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("VCARB"))

        assertEquals(F1TeamId.MERCEDES, F1TeamId.fromName("Mercedes-AMG"))
        assertEquals(F1TeamId.MERCEDES, F1TeamId.fromName("Brawn GP"))
        assertEquals(F1TeamId.FERRARI, F1TeamId.fromName("Scuderia Ferrari"))
        assertEquals(F1TeamId.MCLAREN, F1TeamId.fromName("McLaren Formula 1"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("Aston Martin Aramco"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("Racing Point"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("Force India"))
        assertEquals(F1TeamId.ALPINE, F1TeamId.fromName("Alpine F1 Team"))
        assertEquals(F1TeamId.ALPINE, F1TeamId.fromName("Renault"))
        assertEquals(F1TeamId.WILLIAMS, F1TeamId.fromName("Williams Racing"))
        assertEquals(F1TeamId.HAAS, F1TeamId.fromName("MoneyGram Haas F1 Team"))
        assertEquals(F1TeamId.AUDI_SAUBER, F1TeamId.fromName("Kick Sauber"))
        assertEquals(F1TeamId.AUDI_SAUBER, F1TeamId.fromName("Alfa Romeo Racing"))
        assertEquals(F1TeamId.AUDI_SAUBER, F1TeamId.fromName("Audi"))
        assertEquals(F1TeamId.CADILLAC, F1TeamId.fromName("Cadillac Racing"))
        assertEquals(F1TeamId.CADILLAC, F1TeamId.fromName("Andretti Cadillac"))
    }

    @Test
    fun testSectorStatusAndTyreCompoundProperties() {
        assertEquals(0xFFB855FF, SectorStatus.OVERALL_BEST.colorHex)
        assertEquals(0xFF2CE580, SectorStatus.PERSONAL_BEST.colorHex)
        assertEquals(0xFFF5C518, SectorStatus.NORMAL.colorHex)
        assertEquals(0xFF3A3A44, SectorStatus.INACTIVE.colorHex)

        assertEquals("S", TyreCompound.SOFT.code)
        assertEquals("Soft", TyreCompound.SOFT.label)
        assertEquals(0xFFE1251B, TyreCompound.SOFT.colorHex)

        assertEquals("M", TyreCompound.MEDIUM.code)
        assertEquals("Medium", TyreCompound.MEDIUM.label)
        assertEquals(0xFFF5C518, TyreCompound.MEDIUM.colorHex)

        assertEquals("H", TyreCompound.HARD.code)
        assertEquals("Hard", TyreCompound.HARD.label)
        assertEquals(0xFFF0F0F5, TyreCompound.HARD.colorHex)

        assertEquals("I", TyreCompound.INTERMEDIATE.code)
        assertEquals("Inter", TyreCompound.INTERMEDIATE.label)
        assertEquals(0xFF2CE580, TyreCompound.INTERMEDIATE.colorHex)

        assertEquals("W", TyreCompound.WET.code)
        assertEquals("Wet", TyreCompound.WET.label)
        assertEquals(0xFF00B4D8, TyreCompound.WET.colorHex)
    }

    @Test
    fun testF1DaoCacheTtlLogic() {
        val now = System.currentTimeMillis()
        assertFalse(isExpired(now, DEFAULT_TTL_MS))
        assertFalse(isExpired(now - 1000L, DEFAULT_TTL_MS))
        assertFalse(isExpired(now - (DEFAULT_TTL_MS - 1000L), DEFAULT_TTL_MS))

        assertTrue(isExpired(now - (DEFAULT_TTL_MS + 1000L), DEFAULT_TTL_MS))
        assertTrue(isExpired(now - 2 * DEFAULT_TTL_MS, DEFAULT_TTL_MS))

        assertFalse(F1Dao.isExpired(now, DEFAULT_TTL_MS))
        assertTrue(F1Dao.isExpired(now - (DEFAULT_TTL_MS + 5000L), DEFAULT_TTL_MS))
    }

    @Test
    fun testF1DaoDiskCacheInsertGetClearAndAtomicWrites() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_cache_test").toFile()
        try {
            val cacheDir = File(tempDir, "f1_offline_cache")
            val db = F1Database(cacheDir)
            val dao = db.f1Dao()

            // Test 1: Insert & Get Standings
            val standingsPayload = """[{"position":1,"driverId":"antonelli","points":302.0}]"""
            val entity = CachedStandingsEntity(
                seasonKey = "2026_drivers",
                season = 2026,
                type = "DRIVERS",
                payloadJson = standingsPayload,
                lastUpdatedMillis = System.currentTimeMillis()
            )
            dao.insertStandings(entity)

            val standingsFile = File(cacheDir, "standings_2026_drivers.json")
            assertTrue(standingsFile.exists())
            assertTrue(standingsFile.readText().contains("antonelli"))

            val retrieved = dao.getStandings("2026_drivers")
            assertNotNull(retrieved)
            assertEquals("2026_drivers", retrieved!!.seasonKey)
            assertEquals(2026, retrieved.season)
            assertEquals("DRIVERS", retrieved.type)
            assertEquals(standingsPayload, retrieved.payloadJson)

            // Test 2: Sanitized key file naming
            val specialKeyEntity = CachedStandingsEntity(
                seasonKey = "season:2026/test_key#1",
                season = 2026,
                type = "DRIVERS",
                payloadJson = "{}"
            )
            dao.insertStandings(specialKeyEntity)
            val sanitizedFile = File(cacheDir, "standings_season_2026_test_key_1.json")
            assertTrue(sanitizedFile.exists())

            // Test 3: Insert & Get Schedule
            val schedulePayload = """[{"season":2026,"round":16,"grandPrixName":"Bahrain Sepang"}]"""
            val schedEntity = CachedScheduleEntity(
                season = 2026,
                payloadJson = schedulePayload,
                lastUpdatedMillis = System.currentTimeMillis()
            )
            dao.insertSchedule(schedEntity)

            val scheduleFile = File(cacheDir, "schedule_2026.json")
            assertTrue(scheduleFile.exists())

            val retrievedSched = dao.getSchedule(2026)
            assertNotNull(retrievedSched)
            assertEquals(2026, retrievedSched!!.season)
            assertEquals(schedulePayload, retrievedSched.payloadJson)

            // Test 4: Atomic overwrite
            val updatedSchedulePayload = """[{"season":2026,"round":16,"grandPrixName":"Updated Grand Prix"}]"""
            dao.insertSchedule(schedEntity.copy(payloadJson = updatedSchedulePayload))
            val updatedSched = dao.getSchedule(2026)
            assertEquals(updatedSchedulePayload, updatedSched?.payloadJson)
            val tempFiles = cacheDir.listFiles()?.filter { it.name.endsWith(".tmp") } ?: emptyList()
            assertTrue(tempFiles.isEmpty())

            // Test 5: Clear Standings
            dao.clearStandings()
            assertNull(dao.getStandings("2026_drivers"))
            assertFalse(standingsFile.exists())
            assertNotNull(dao.getSchedule(2026))

            // Test 6: Clear Schedule
            dao.clearSchedule()
            assertNull(dao.getSchedule(2026))
            assertFalse(scheduleFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testF1RepositoryInstantiationAndOperations() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_repo_test").toFile()
        try {
            val (repository, db) = createRepository(tempDir)

            // 1. Next Race Weekend
            val nextWeekend = repository.getNextRaceWeekend()
            assertEquals("Bahrain Grand Prix in Malaysia", nextWeekend.grandPrixName)
            assertEquals("Sepang International Circuit", nextWeekend.circuitName)
            assertEquals(56, nextWeekend.totalLaps)

            // 2. Brazilian GP 2026 Weekend
            val brazil2026 = repository.getBrazilianGpWeekend2026()
            assertEquals("Brazilian Grand Prix", brazil2026.grandPrixName)
            assertEquals("Autódromo José Carlos Pace", brazil2026.circuitName)
            assertEquals(22, brazil2026.round)

            // 3. São Paulo 2024 Results
            val sp2024 = repository.getSaoPaulo2024Results()
            assertEquals(2024, sp2024.season)
            assertEquals(21, sp2024.round)
            assertEquals("Max Verstappen", sp2024.results.first().driverFullName)

            // 4. Abu Dhabi 2023 Results
            val ad2023 = repository.getAbuDhabi2023Results()
            assertEquals(2023, ad2023.season)
            assertEquals(22, ad2023.round)
            assertTrue(ad2023.results.first().stints.isNotEmpty())

            // 5. Driver Standings 2026 seed fallback must NOT poison offline cache
            val driverStandings = repository.getDriverStandings(2026)
            assertTrue(driverStandings.size >= 8)
            assertEquals("Kimi Antonelli", driverStandings[0].fullName)
            assertEquals(F1TeamId.MERCEDES, driverStandings[0].team)
            assertNull(db.f1Dao().getStandings("2026_drivers"))

            // 6. Constructor Standings 2026 seed fallback must NOT poison offline cache
            val constructorStandings = repository.getConstructorStandings(2026)
            assertEquals(11, constructorStandings.size)
            assertEquals(F1TeamId.MERCEDES, constructorStandings[0].team)
            assertEquals(F1TeamId.FERRARI, constructorStandings[1].team)
            assertNull(db.f1Dao().getStandings("2026_constructors"))

            // 7. Driver Comparisons
            val comparisons = repository.getDriverComparisons()
            assertTrue(comparisons.any { it.code == "VER" })
            assertTrue(comparisons.any { it.code == "NOR" })

            // 8. Country Flag resolution
            assertEquals("🇦🇺", repository.getCountryFlag("Australia"))
            assertEquals("🇧🇷", repository.getCountryFlag("Brazil"))
            assertEquals("🇬🇧", repository.getCountryFlag("United Kingdom"))
            assertEquals("🇬🇧", repository.getCountryFlag("UK"))
            assertEquals("🇮🇹", repository.getCountryFlag("Italy"))
            assertEquals("🇯🇵", repository.getCountryFlag("Japan"))
            assertEquals("🇲🇾", repository.getCountryFlag("Malaysia"))
            assertEquals("🇺🇸", repository.getCountryFlag("USA"))
            assertEquals("🇲🇨", repository.getCountryFlag("Monaco"))
            assertEquals("🏁", repository.getCountryFlag("Atlantis"))

            // 9. Season Schedule & specific round lookup
            val round16 = repository.getRaceWeekend(2026, 16)
            assertEquals(16, round16.round)
            assertEquals("Sepang International Circuit", round16.circuitName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testStandingsHistoricalFallbackAndConstructorPre1958() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_standings_test").toFile()
        try {
            val (repository, _) = createRepository(tempDir)

            // 1. Constructor standings for seasons < 1958 must return emptyList
            val constructors1950 = repository.getConstructorStandings(1950)
            assertTrue("Constructors standings in 1950 should be empty", constructors1950.isEmpty())

            val constructors1957 = repository.getConstructorStandings(1957)
            assertTrue("Constructors standings in 1957 should be empty", constructors1957.isEmpty())

            // 2. Driver standings for historical unseeded season when offline must return emptyList
            val drivers1999 = repository.getDriverStandings(1999)
            assertTrue("Driver standings for unseeded year 1999 when offline should be empty", drivers1999.isEmpty())

            // 3. Constructor standings for historical unseeded season (>= 1958) when offline must return emptyList
            val constructors1999 = repository.getConstructorStandings(1999)
            assertTrue("Constructor standings for unseeded year 1999 when offline should be empty", constructors1999.isEmpty())

            // 4. 2026 Standings fall back to 2026 seed data when jolpicaApi is null
            val drivers2026 = repository.getDriverStandings(2026)
            assertFalse(drivers2026.isEmpty())
            assertEquals("Kimi Antonelli", drivers2026[0].fullName)

            val constructors2026 = repository.getConstructorStandings(2026)
            assertFalse(constructors2026.isEmpty())
            assertEquals(F1TeamId.MERCEDES, constructors2026[0].team)

            // 5. LiveSessionState through repository
            val bahrainLive = repository.getLiveSessionState("BAHRAIN")
            assertEquals("Bahrain Grand Prix", bahrainLive.grandPrixName)
            assertEquals("Qualifying · Q1", bahrainLive.sessionName)

            val saoPauloLive = repository.getLiveSessionState()
            assertEquals("São Paulo Grand Prix", saoPauloLive.grandPrixName)
            assertEquals("FP1", saoPauloLive.sessionName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCancellationExceptionNotSwallowedInSuspendFunctions() {
        val tempDir = Files.createTempDirectory("f1_cancel_test").toFile()
        try {
            val (repository, _) = createRepository(tempDir)

            val job = kotlinx.coroutines.Job()
            job.cancel()
            try {
                runBlocking(job) {
                    repository.getDriverStandings(2026)
                }
                org.junit.Assert.fail("Expected CancellationException to be thrown")
            } catch (e: CancellationException) {
                assertTrue(true)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 16. Jolpica & OpenF1 DTO Deserialization Resilience Tests
    // ========================================================================
    @Test
    fun testJolpicaAndOpenF1DtoDeserializationWithMissingAndDoubleFields() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // Test 1: Jolpica empty payload deserializes safely with all defaults
        val emptyJolpica = json.decodeFromString<JolpicaResponse<RaceTableWrapper>>("{}")
        assertNotNull(emptyJolpica.mrData)
        assertNull(emptyJolpica.mrData.raceTable)

        // Test 2: Jolpica RaceDto with missing nested and nullable fields
        val minimalRaceJson = """
            {
                "season": "2026",
                "round": "1",
                "raceName": "Australian Grand Prix",
                "date": "2026-03-15"
            }
        """.trimIndent()
        val parsedRace = json.decodeFromString<RaceDto>(minimalRaceJson)
        assertEquals("2026", parsedRace.season)
        assertEquals("1", parsedRace.round)
        assertEquals("Australian Grand Prix", parsedRace.raceName)
        assertEquals("2026-03-15", parsedRace.date)
        assertNull(parsedRace.time)
        assertTrue(parsedRace.results.isEmpty())
        assertEquals("", parsedRace.circuit.circuitId)

        // Test 3: Jolpica DriverStandingDto with missing constructors & wins
        val sparseDriverJson = """
            {
                "position": "1",
                "points": "25",
                "Driver": {
                    "driverId": "hamilton",
                    "givenName": "Lewis",
                    "familyName": "Hamilton"
                }
            }
        """.trimIndent()
        val parsedDriverStanding = json.decodeFromString<DriverStandingDto>(sparseDriverJson)
        assertEquals("1", parsedDriverStanding.position)
        assertEquals("25", parsedDriverStanding.points)
        assertEquals("Lewis", parsedDriverStanding.driver.givenName)
        assertEquals("Hamilton", parsedDriverStanding.driver.familyName)
        assertTrue(parsedDriverStanding.constructors.isEmpty())

        // Test 4: OpenF1 WeatherDto with Double rainfall and Double wind_direction
        val weatherJson = """
            {
                "session_key": 9158,
                "air_temperature": 24.5,
                "track_temperature": 32.1,
                "humidity": 65.0,
                "pressure": 1012.3,
                "rainfall": 1.5,
                "wind_direction": 180.5,
                "wind_speed": 12.3,
                "date": "2026-11-06T15:30:00Z"
            }
        """.trimIndent()
        val weather = json.decodeFromString<OpenF1WeatherDto>(weatherJson)
        assertEquals(9158, weather.sessionKey)
        assertEquals(24.5, weather.airTemperature, 0.001)
        assertEquals(32.1, weather.trackTemperature, 0.001)
        assertEquals(1.5, weather.rainfall, 0.001)
        assertEquals(180.5, weather.windDirection, 0.001)
        assertEquals(12.3, weather.windSpeed, 0.001)
    }

    // ========================================================================
    // 17. F1Repository with Fake JolpicaApiService Integration Tests
    // ========================================================================
    @Test
    fun testF1RepositoryWithFakeJolpicaApiServiceMidSeasonSwitchAndUtcTime() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_fake_api_test").toFile()
        try {
            val fakeJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int): JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper> =
                    JolpicaResponse()

                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> {
                    val race = RaceDto(
                        season = season,
                        round = "20",
                        raceName = "Brazilian Grand Prix",
                        circuit = CircuitDto(
                            circuitId = "interlagos",
                            circuitName = "Autódromo José Carlos Pace",
                            location = LocationDto(locality = "São Paulo", country = "Brazil")
                        ),
                        date = "2026-11-08",
                        time = "17:00:00Z",
                        firstPractice = SessionDto(date = "2026-11-06", time = "15:30:00Z"),
                        secondPractice = SessionDto(date = "2026-11-06", time = "19:00:00Z"),
                        qualifying = SessionDto(date = "2026-11-07", time = "18:00:00Z")
                    )
                    return JolpicaResponse(
                        mrData = MRData(
                            raceTable = RaceTableWrapper(
                                season = season,
                                races = listOf(race)
                            )
                        )
                    )
                }

                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> {
                    // Driver 1: Hamilton mid-season team switch (Mercedes -> Ferrari)
                    val ham = DriverStandingDto(
                        position = "1",
                        points = "100.5",
                        wins = "4",
                        driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton", code = "HAM", nationality = "British"),
                        constructors = listOf(
                            ConstructorDto(constructorId = "mercedes", name = "Mercedes"),
                            ConstructorDto(constructorId = "ferrari", name = "Ferrari")
                        )
                    )
                    // Driver 2: Tied on points with leader (diff == 0.0)
                    val tieDriver = DriverStandingDto(
                        position = "2",
                        points = "100.5",
                        wins = "3",
                        driver = DriverDto(driverId = "russell", givenName = "George", familyName = "Russell", code = "RUS", nationality = "British"),
                        constructors = listOf(ConstructorDto(constructorId = "mercedes", name = "Mercedes"))
                    )
                    return JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "12",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "12",
                                        driverStandings = listOf(ham, tieDriver)
                                    )
                                )
                            )
                        )
                    )
                }

                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> {
                    val mclaren = ConstructorStandingDto(
                        position = "1",
                        points = "250.0",
                        wins = "6",
                        constructor = ConstructorDto(constructorId = "mclaren", name = "McLaren")
                    )
                    return JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "12",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "12",
                                        constructorStandings = listOf(mclaren)
                                    )
                                )
                            )
                        )
                    )
                }

                override suspend fun getRaceResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = JolpicaResponse()
                override suspend fun getQualifyingResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = JolpicaResponse()
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> = JolpicaResponse()
            }

            val (repository, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)

            // Test 1: Mid-season team switch uses constructors.lastOrNull()
            val standings = repository.getDriverStandings(2026, forceRefresh = true)
            assertEquals(2, standings.size)
            assertEquals("Lewis Hamilton", standings[0].fullName)
            assertEquals(F1TeamId.FERRARI, standings[0].team)
            // Half-points preserved in summary string
            assertTrue(standings[0].last3Summary.contains("100.5 pts"))
            // Driver tied with leader formats gapToLeader as null (no "-0 to leader")
            assertNull(standings[1].gapToLeader)

            // Test 2: Season schedule parsing with UTC to local timezone conversion and circuit specs
            val schedule = repository.getSeasonSchedule(2026)
            assertEquals(1, schedule.size)
            val weekend = schedule[0]
            assertEquals(2026, weekend.season)
            assertEquals(20, weekend.round)
            assertEquals("INTERLAGOS", weekend.circuitId)
            assertEquals("4.309", weekend.circuitLengthKm)
            assertEquals(71, weekend.totalLaps)
            assertEquals(15, weekend.totalCorners)

            // Sessions sorted chronologically
            assertEquals(4, weekend.sessions.size)
            assertEquals("FP1", weekend.sessions[0].shortName)
            assertEquals("FP2", weekend.sessions[1].shortName)
            assertEquals("Quali", weekend.sessions[2].shortName)
            assertEquals("Race", weekend.sessions[3].shortName)

            // Time converted to local time
            val expectedRaceLocalTime = ZonedDateTime.of(
                LocalDate.of(2026, 11, 8),
                LocalTime.of(17, 0),
                ZoneOffset.UTC
            ).withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
            assertEquals(expectedRaceLocalTime, weekend.sessions[3].localTime)

            // Date range formatted from first session to race session in local timezone
            val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
            val expectedStartDay = Instant.parse("2026-11-06T15:30:00Z")
                .atZone(ZoneId.systemDefault())
                .format(dayFormatter)
                .uppercase(Locale.ENGLISH)
            val expectedEndDay = Instant.parse("2026-11-08T17:00:00Z")
                .atZone(ZoneId.systemDefault())
                .format(dayFormatter)
                .uppercase(Locale.ENGLISH)
            assertTrue(weekend.dateRangeText.contains(expectedStartDay))
            assertTrue(weekend.dateRangeText.contains(expectedEndDay))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 18. Expired Cache Fallback Test
    // ========================================================================
    @Test
    fun testExpiredScheduleAndStandingsFallback() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_expired_cache_test").toFile()
        try {
            val db = createDatabaseInstance(tempDir)
            val prefs = createPreferencesStore(tempDir)

            // Insert expired schedule into DB
            val expiredScheduleJson = """
                [
                    {
                        "season": 2026,
                        "round": 1,
                        "grandPrixName": "Expired Australian GP",
                        "circuitName": "Albert Park",
                        "location": "Melbourne, Australia",
                        "countryFlagEmoji": "🇦🇺",
                        "dateRangeText": "FRI 13 MAR - SUN 15 MAR",
                        "daysToGo": 0,
                        "circuitLengthKm": "5.278",
                        "totalLaps": 58,
                        "totalCorners": 14,
                        "circuitType": "MELBOURNE",
                        "circuitId": "MELBOURNE",
                        "sessions": []
                    }
                ]
            """.trimIndent()
            db.f1Dao().insertSchedule(
                CachedScheduleEntity(
                    season = 2026,
                    payloadJson = expiredScheduleJson,
                    lastUpdatedMillis = System.currentTimeMillis() - 2 * DEFAULT_TTL_MS
                )
            )

            // API throws network exception
            val failingJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int): JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper> = throw IOException("Offline")
                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> = throw IOException("Offline")
                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> = throw IOException("Offline")
                override suspend fun getRaceResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getQualifyingResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
            }

            val repository = F1Repository(
                context = null,
                jolpicaApi = failingJolpica,
                openF1Api = null,
                database = db,
                preferencesStore = prefs
            )

            // Must cleanly fall back to the expired cached schedule rather than crashing or throwing
            val schedule = repository.getSeasonSchedule(2026)
            assertEquals(1, schedule.size)
            assertEquals("Expired Australian GP", schedule[0].grandPrixName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 19. F1Database Double-Checked Locking & Atomic Write Concurrency Test
    // ========================================================================
    @Test
    fun testF1DatabaseAtomicWriteAndDoubleCheckedLocking() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_db_lock_test").toFile()
        try {
            F1Database.resetInstanceForTesting()
            val cacheDir = File(tempDir, "concurrent_f1_cache")

            // Launch 20 concurrent coroutines attempting to obtain the database instance
            val instances = (1..20).map {
                async(Dispatchers.IO) {
                    F1Database.getInstance(cacheDir)
                }
            }.awaitAll()

            val firstInstance = instances.first()
            assertTrue("Double-checked locking must return the exact same singleton instance across all threads",
                instances.all { it === firstInstance }
            )

            // Test atomic write with concurrent writers
            val dao = firstInstance.f1Dao()
            (1..10).map { i ->
                async(Dispatchers.IO) {
                    dao.insertStandings(
                        CachedStandingsEntity(
                            seasonKey = "concurrent_key_$i",
                            season = 2026,
                            type = "DRIVERS",
                            payloadJson = """{"iteration": $i}"""
                        )
                    )
                }
            }.awaitAll()

            // Verify no .tmp files leak
            val tmpFiles = cacheDir.listFiles()?.filter { it.name.endsWith(".tmp") } ?: emptyList()
            assertTrue(tmpFiles.isEmpty())
        } finally {
            F1Database.resetInstanceForTesting()
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 20. UserPreferencesStore Single Transaction Update Test
    // ========================================================================
    @Test
    fun testUserPreferencesStoreSingleTransactionUpdateAllPreferences() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_prefs_test").toFile()
        try {
            val prefsStore = createPreferencesStore(tempDir)
            val updatedState = UserPreferencesState(
                favoriteTeam = F1TeamId.ASTON_MARTIN,
                favoriteDriverCode = "ALO",
                themeMode = "LIGHT",
                useTeamColorAccent = false,
                pitRadioAlertsEnabled = false,
                sessionRemindersEnabled = false,
                lapRecordAlertsEnabled = false,
                offlineCacheEnabled = true
            )

            prefsStore.updateAllPreferences(updatedState)
            val result = prefsStore.preferencesFlow.first()

            assertEquals(F1TeamId.ASTON_MARTIN, result.favoriteTeam)
            assertEquals("ALO", result.favoriteDriverCode)
            assertEquals("LIGHT", result.themeMode)
            assertFalse(result.useTeamColorAccent)
            assertFalse(result.pitRadioAlertsEnabled)
            assertFalse(result.sessionRemindersEnabled)
            assertFalse(result.lapRecordAlertsEnabled)
            assertTrue(result.offlineCacheEnabled)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 21. OpenF1 "latest" Session Key Mapping & ISO-8601 Timestamp Parsing
    // ========================================================================
    @Test
    fun testLiveSessionStateMapsSyntheticKeyToLatestAndParsesIso8601Timestamps() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_openf1_latest_test").toFile()
        try {
            val capturedWeatherKeys = mutableListOf<String>()
            val capturedRaceControlKeys = mutableListOf<String>()

            val fakeOpenF1 = createFakeOpenF1Api(
                onGetWeather = { key ->
                    capturedWeatherKeys.add(key)
                    listOf(
                        OpenF1WeatherDto(
                            airTemperature = 27.5,
                            trackTemperature = 41.2,
                            humidity = 58.0,
                            pressure = 1015.0,
                            rainfall = 0.0,
                            sessionKey = 9693,
                            windDirection = 120.0,
                            windSpeed = 9.4
                        )
                    )
                },
                onGetRaceControl = { key ->
                    capturedRaceControlKeys.add(key)
                    listOf(
                        OpenF1RaceControlDto(
                            sessionKey = 9693,
                            date = "2025-11-02T17:15:30+00:00",
                            message = "GREEN LIGHT - PIT EXIT OPEN",
                            category = "Flag",
                            flag = "GREEN"
                        ),
                        OpenF1RaceControlDto(
                            sessionKey = 9693,
                            date = "2025-11-02T17:16:45Z",
                            message = "DRS ENABLED",
                            category = "Drs"
                        ),
                        OpenF1RaceControlDto(
                            sessionKey = 9693,
                            date = "",
                            message = "BLANK DATE FALLBACK",
                            category = "Other"
                        )
                    )
                }
            )

            val (repository, _) = createRepository(tempDir, openF1Api = fakeOpenF1)

            // Synthetic/blank fixture keys ("SAO_PAULO", "BAHRAIN", "  ") stay pure offline fixtures without hitting OpenF1
            val saoPauloFixture = repository.getLiveSessionState("SAO_PAULO")
            assertEquals("São Paulo Grand Prix", saoPauloFixture.grandPrixName)
            val bahrainFixture = repository.getLiveSessionState("BAHRAIN")
            assertEquals("Bahrain Grand Prix", bahrainFixture.grandPrixName)
            repository.getLiveSessionState("  ")
            assertTrue(capturedWeatherKeys.isEmpty())
            assertTrue(capturedRaceControlKeys.isEmpty())

            // "latest" queries OpenF1 and enriches weather & race control timestamps
            val liveState = repository.getLiveSessionState("latest")
            assertEquals(listOf("latest"), capturedWeatherKeys)
            assertEquals(listOf("latest"), capturedRaceControlKeys)
            assertEquals(41.2, liveState.weather.trackTempC, 0.01)
            assertEquals(27.5, liveState.weather.airTempC, 0.01)

            // takeLast(10).reversed() puts newest first: index 0 is blank-date, index 1 is Z, index 2 is +00:00
            assertEquals(3, liveState.raceControlMessages.size)
            assertEquals("LIVE", liveState.raceControlMessages[0].timestamp)

            val expectedZ = Instant.parse("2025-11-02T17:16:45Z")
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            assertEquals(expectedZ, liveState.raceControlMessages[1].timestamp)

            val expectedOffset = OffsetDateTime.parse("2025-11-02T17:15:30+00:00")
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            assertEquals(expectedOffset, liveState.raceControlMessages[2].timestamp)
            assertFalse(liveState.raceControlMessages[2].timestamp.contains("+"))

            // Numeric sessionKey ("9693") is passed through unchanged
            repository.getLiveSessionState("9693")
            assertEquals("9693", capturedWeatherKeys.last())
            assertEquals("9693", capturedRaceControlKeys.last())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 22. OpenF1 CancellationException Rethrow Test
    // ========================================================================
    @Test
    fun testGetLiveSessionStateRethrowsCancellationException() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_openf1_cancel_test").toFile()
        try {
            val cancellingWeatherApi = createFakeOpenF1Api(
                onGetWeather = { throw CancellationException("Cancelled weather request") }
            )
            val (repo1, _) = createRepository(tempDir, openF1Api = cancellingWeatherApi)

            var weatherCancelled = false
            try {
                repo1.getLiveSessionState("latest")
            } catch (_: CancellationException) {
                weatherCancelled = true
            }
            assertTrue("CancellationException from openF1Api.getWeather must be rethrown", weatherCancelled)

            val cancellingRcApi = createFakeOpenF1Api(
                onGetWeather = { emptyList() },
                onGetRaceControl = { throw CancellationException("Cancelled race control request") }
            )
            val (repo2, _) = createRepository(tempDir, openF1Api = cancellingRcApi)

            var rcCancelled = false
            try {
                repo2.getLiveSessionState("9693")
            } catch (_: CancellationException) {
                rcCancelled = true
            }
            assertTrue("CancellationException from openF1Api.getRaceControl must be rethrown", rcCancelled)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 23. offlineCacheEnabled == false Bypasses Read & Skips Write
    // ========================================================================
    @Test
    fun testOfflineCacheDisabledBypassesReadsAndSkipsWrites() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_offline_cache_pref_test").toFile()
        try {
            val db = createDatabaseInstance(tempDir)
            val prefs = createPreferencesStore(tempDir)

            // Seed DB with stale/custom entries that would otherwise be returned when cache is enabled
            db.f1Dao().insertSchedule(
                CachedScheduleEntity(
                    season = 2026,
                    payloadJson = """[{"season":2026,"round":99,"grandPrixName":"Cached Only GP","circuitName":"Cached Circuit","location":"Nowhere","countryFlagEmoji":"🏁","dateRangeText":"TBC","daysToGo":0,"circuitLengthKm":"5.000","totalLaps":50,"totalCorners":15,"sessions":[]}]""",
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
            db.f1Dao().insertStandings(
                CachedStandingsEntity(
                    seasonKey = "2026_drivers",
                    season = 2026,
                    type = "DRIVERS",
                    payloadJson = """[{"position":1,"driverId":"cached_driver","code":"CAC","firstName":"Cached","lastName":"Driver","fullName":"Cached Driver","nationality":"Test","team":"FERRARI","points":999.0,"wins":10,"podiums":10,"poles":10,"avgPtsPerRace":50.0,"racesCount":20,"gapToLeader":null,"gapToPrev":null,"last3Summary":"Cached","recentRaces":[]}]""",
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
            db.f1Dao().insertStandings(
                CachedStandingsEntity(
                    seasonKey = "2026_constructors",
                    season = 2026,
                    type = "CONSTRUCTORS",
                    payloadJson = """[{"position":1,"team":"HAAS","points":888.0,"wins":9,"gapToP1":null,"gapToPrev":null,"drivers":[],"seasonWins":[],"recentTeamForm":[]}]""",
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )

            // Disable offline cache
            prefs.updateAllPreferences(UserPreferencesState(offlineCacheEnabled = false))

            val fakeJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int) = JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper>()
                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        raceTable = RaceTableWrapper(
                            season = season,
                            races = listOf(
                                RaceDto(
                                    season = season,
                                    round = "1",
                                    raceName = "Network Australian Grand Prix",
                                    circuit = CircuitDto("albert_park", circuitName = "Albert Park", location = LocationDto(locality = "Melbourne", country = "Australia")),
                                    date = "2026-03-15",
                                    time = "05:00:00Z"
                                )
                            )
                        )
                    )
                )
                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        standingsTable = StandingsTableWrapper(
                            season = season,
                            round = "5",
                            standingsLists = listOf(
                                StandingsListDto(
                                    season = season,
                                    round = "5",
                                    driverStandings = listOf(
                                        DriverStandingDto(
                                            position = "1",
                                            points = "112",
                                            wins = "3",
                                            driver = DriverDto("leclerc", code = "LEC", givenName = "Charles", familyName = "Leclerc", nationality = "Monégasque"),
                                            constructors = listOf(ConstructorDto("ferrari", name = "Ferrari"))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        standingsTable = StandingsTableWrapper(
                            season = season,
                            round = "5",
                            standingsLists = listOf(
                                StandingsListDto(
                                    season = season,
                                    round = "5",
                                    constructorStandings = listOf(
                                        ConstructorStandingDto(
                                            position = "1",
                                            points = "210",
                                            wins = "4",
                                            constructor = ConstructorDto("ferrari", name = "Ferrari")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getRaceResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getQualifyingResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
            }

            val networkRepo = F1Repository(
                context = null,
                jolpicaApi = fakeJolpica,
                openF1Api = null,
                database = db,
                preferencesStore = prefs
            )

            // 1. Must bypass cached DB entries and fetch from network
            val schedule = networkRepo.getSeasonSchedule(2026)
            assertEquals(1, schedule.size)
            assertEquals("Network Australian Grand Prix", schedule[0].grandPrixName)

            val drivers = networkRepo.getDriverStandings(2026)
            assertEquals(1, drivers.size)
            assertEquals("Charles Leclerc", drivers[0].fullName)

            val constructors = networkRepo.getConstructorStandings(2026)
            assertEquals(1, constructors.size)
            assertEquals(F1TeamId.FERRARI, constructors[0].team)

            // 2. DB must NOT have been overwritten by the network responses
            assertTrue(db.f1Dao().getSchedule(2026)!!.payloadJson.contains("Cached Only GP"))
            assertTrue(db.f1Dao().getStandings("2026_drivers")!!.payloadJson.contains("Cached Driver"))
            assertTrue(db.f1Dao().getStandings("2026_constructors")!!.payloadJson.contains("HAAS"))

            // 3. With clean DB and offlineCacheEnabled = false and failing network, falls back to SeedDataProvider and writes nothing to DB
            db.f1Dao().clearSchedule()
            db.f1Dao().clearStandings()
            val offlineRepo = F1Repository(
                context = null,
                jolpicaApi = null,
                openF1Api = null,
                database = db,
                preferencesStore = prefs
            )

            val seedSchedule = offlineRepo.getSeasonSchedule(2026)
            assertFalse(seedSchedule.isEmpty())
            assertNull(db.f1Dao().getSchedule(2026))

            val seedDrivers = offlineRepo.getDriverStandings(2026)
            assertFalse(seedDrivers.isEmpty())
            assertNull(db.f1Dao().getStandings("2026_drivers"))

            val seedConstructors = offlineRepo.getConstructorStandings(2026)
            assertFalse(seedConstructors.isEmpty())
            assertNull(db.f1Dao().getStandings("2026_constructors"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 24. Latest Completed Race Helper Test
    // ========================================================================
    @Test
    fun testGetLatestCompletedRaceUsesNetworkResultsAndFallsBackToSeed() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_latest_race_test").toFile()
        try {
            // 1. Offline fallback for 2026 returns a 2026 weekend (never 2024 São Paulo results)
            val (offlineRepo, _) = createRepository(tempDir)
            val fallbackLatest = offlineRepo.getLatestCompletedRace(2026)
            assertEquals(2026, fallbackLatest.season)
            assertTrue(fallbackLatest.results.isEmpty())

            // 2. Network Jolpica schedule + results returns completed race with podium
            val todayUtc = LocalDate.now(ZoneOffset.UTC)
            val pastRaceDate = todayUtc.minusDays(5)
            val futureRaceDate = todayUtc.plusDays(14)
            val testSeason = pastRaceDate.year
            val scheduleRaces = buildList {
                add(
                    RaceDto(
                        season = testSeason.toString(),
                        round = "21",
                        raceName = "São Paulo Grand Prix",
                        circuit = CircuitDto("interlagos", circuitName = "Interlagos", location = LocationDto(locality = "São Paulo", country = "Brazil")),
                        date = pastRaceDate.toString(),
                        time = "17:00:00Z"
                    )
                )
                if (futureRaceDate.year == testSeason) {
                    add(
                        RaceDto(
                            season = testSeason.toString(),
                            round = "22",
                            raceName = "Las Vegas Grand Prix",
                            circuit = CircuitDto("vegas", circuitName = "Las Vegas Strip Circuit", location = LocationDto(locality = "Las Vegas", country = "USA")),
                            date = futureRaceDate.toString(),
                            time = "06:00:00Z"
                        )
                    )
                }
            }
            val fakeJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int) = JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper>()
                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        raceTable = RaceTableWrapper(
                            season = season,
                            races = scheduleRaces
                        )
                    )
                )
                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int) = JolpicaResponse<StandingsTableWrapper>()
                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int) = JolpicaResponse<StandingsTableWrapper>()
                override suspend fun getRaceResults(season: String, round: String) = JolpicaResponse(
                    mrData = MRData(
                        raceTable = RaceTableWrapper(
                            season = season,
                            round = round,
                            races = listOf(
                                RaceDto(
                                    season = season,
                                    round = round,
                                    raceName = "São Paulo Grand Prix",
                                    results = listOf(
                                        ResultDto(
                                            position = "1",
                                            points = "25",
                                            driver = DriverDto(driverId = "leclerc", code = "LEC", givenName = "Charles", familyName = "Leclerc"),
                                            constructor = ConstructorDto(constructorId = "ferrari", name = "Ferrari"),
                                            laps = "71",
                                            status = "Finished",
                                            time = TimeDto(time = "1:32:10.123")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getQualifyingResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
            }
            val (networkRepo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val latestNetwork = networkRepo.getLatestCompletedRace(testSeason)
            assertEquals("São Paulo Grand Prix", latestNetwork.grandPrixName)
            assertEquals(21, latestNetwork.round)
            assertEquals(1, latestNetwork.results.size)
            assertEquals("Charles Leclerc", latestNetwork.results[0].driverFullName)
            assertEquals(F1TeamId.FERRARI, latestNetwork.results[0].team)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 25. RaceWeekend Default circuitId & Seed Circuit Consistency Test
    // ========================================================================
    @Test
    fun testRaceWeekendDefaultCircuitIdDoesNotShadowCircuitType() {
        val weekend = RaceWeekend(
            season = 2026,
            round = 20,
            grandPrixName = "Brazilian Grand Prix",
            circuitName = "Autódromo José Carlos Pace",
            location = "São Paulo, Brazil",
            countryFlagEmoji = "🇧🇷",
            dateRangeText = "FRI 6 NOV - SUN 8 NOV",
            daysToGo = 10,
            circuitLengthKm = "4.309",
            totalLaps = 71,
            totalCorners = 15,
            circuitType = "INTERLAGOS",
            sessions = emptyList()
        )
        assertEquals("", weekend.circuitId)
        assertEquals("INTERLAGOS", weekend.circuitId.ifBlank { weekend.circuitType })

        val nextRace = SeedDataProvider.getNextRaceWeekend()
        assertEquals("SEPANG", nextRace.circuitType)
        assertEquals("SEPANG", nextRace.circuitId)

        val brazil2026 = SeedDataProvider.getBrazilianGpWeekend2026()
        assertEquals("INTERLAGOS", brazil2026.circuitType)
        assertEquals("INTERLAGOS", brazil2026.circuitId)

        val saoPaulo2024 = SeedDataProvider.getSaoPaulo2024Results()
        assertEquals("INTERLAGOS", saoPaulo2024.circuitType)
        assertEquals("INTERLAGOS", saoPaulo2024.circuitId)

        val abuDhabi2023 = SeedDataProvider.getAbuDhabi2023Results()
        assertEquals("YAS_MARINA", abuDhabi2023.circuitType)
        assertEquals("YAS_MARINA", abuDhabi2023.circuitId)

        val melbourne2025 = SeedDataProvider.getSeasonSchedule(2025).first()
        assertEquals("MELBOURNE", melbourne2025.circuitType)
        assertEquals("MELBOURNE", melbourne2025.circuitId)

        val abuDhabi2024 = SeedDataProvider.getSeasonSchedule(2024).last()
        assertEquals("YAS_MARINA", abuDhabi2024.circuitType)
        assertEquals("YAS_MARINA", abuDhabi2024.circuitId)
    }

    // ========================================================================
    // 26. Constructor Standings Driver Lineup Population Test
    // ========================================================================
    @Test
    fun testGetConstructorStandingsPopulatesDriversFromDriverStandings() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_constructor_drivers_test").toFile()
        try {
            val fakeJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int) = JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper>()
                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        standingsTable = StandingsTableWrapper(
                            season = season,
                            round = "10",
                            standingsLists = listOf(
                                StandingsListDto(
                                    season = season,
                                    round = "10",
                                    driverStandings = listOf(
                                        DriverStandingDto(
                                            position = "1",
                                            points = "180",
                                            wins = "4",
                                            driver = DriverDto(driverId = "leclerc", givenName = "Charles", familyName = "Leclerc", code = "LEC"),
                                            constructors = listOf(ConstructorDto(constructorId = "ferrari", name = "Ferrari"))
                                        ),
                                        DriverStandingDto(
                                            position = "2",
                                            points = "155",
                                            wins = "2",
                                            driver = DriverDto(driverId = "hamilton", givenName = "Lewis", familyName = "Hamilton", code = "HAM"),
                                            constructors = listOf(ConstructorDto(constructorId = "ferrari", name = "Ferrari"))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        standingsTable = StandingsTableWrapper(
                            season = season,
                            round = "10",
                            standingsLists = listOf(
                                StandingsListDto(
                                    season = season,
                                    round = "10",
                                    constructorStandings = listOf(
                                        ConstructorStandingDto(
                                            position = "1",
                                            points = "335",
                                            wins = "6",
                                            constructor = ConstructorDto(constructorId = "ferrari", name = "Ferrari")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getRaceResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getQualifyingResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
            }
            val (repo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val constructors = repo.getConstructorStandings(2026, forceRefresh = true)
            assertEquals(1, constructors.size)
            val ferrari = constructors[0]
            assertEquals(F1TeamId.FERRARI, ferrari.team)
            assertEquals(2, ferrari.drivers.size)
            assertEquals("Charles Leclerc", ferrari.drivers[0].fullName)
            assertEquals(180.0, ferrari.drivers[0].points, 0.001)
            assertEquals("Lewis Hamilton", ferrari.drivers[1].fullName)
            assertEquals(155.0, ferrari.drivers[1].points, 0.001)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testGetConstructorStandingsDoesNotMixSeedDriversWhenDriverStandingsFails() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_constructor_partial_failure_test").toFile()
        try {
            val fakeJolpica = object : JolpicaApiService {
                override suspend fun getSeasons(limit: Int, offset: Int) =
                    JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper>()
                override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getDriverStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> {
                    throw IOException("Driver standings endpoint unavailable")
                }
                override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int) = JolpicaResponse(
                    mrData = MRData(
                        standingsTable = StandingsTableWrapper(
                            season = season,
                            round = "12",
                            standingsLists = listOf(
                                StandingsListDto(
                                    season = season,
                                    round = "12",
                                    constructorStandings = listOf(
                                        ConstructorStandingDto(
                                            position = "1",
                                            points = "410",
                                            wins = "7",
                                            constructor = ConstructorDto(constructorId = "mclaren", name = "McLaren")
                                        ),
                                        ConstructorStandingDto(
                                            position = "2",
                                            points = "380",
                                            wins = "5",
                                            constructor = ConstructorDto(constructorId = "ferrari", name = "Ferrari")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                override suspend fun getRaceResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getQualifyingResults(season: String, round: String) = JolpicaResponse<RaceTableWrapper>()
                override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int) = JolpicaResponse<RaceTableWrapper>()
            }
            val (repo, db) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val constructors = repo.getConstructorStandings(2026, forceRefresh = true)

            assertEquals(2, constructors.size)
            assertEquals(F1TeamId.MCLAREN, constructors[0].team)
            assertEquals(410.0, constructors[0].points, 0.001)
            assertTrue(constructors[0].drivers.isEmpty())
            assertEquals(F1TeamId.FERRARI, constructors[1].team)
            assertEquals(380.0, constructors[1].points, 0.001)
            assertTrue(constructors[1].drivers.isEmpty())

            // Verify cached constructor standings do not contain seed driver points either
            val cachedEntity = db.f1Dao().getStandings("2026_constructors")
            assertNotNull(cachedEntity)
            val cachedFromRepo = repo.getConstructorStandings(2026, forceRefresh = false)
            assertEquals(2, cachedFromRepo.size)
            assertTrue(cachedFromRepo.all { it.drivers.isEmpty() })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 27. A1: Historical F1TeamId Mapping, Ambiguous Fallback & Constructor Grouping
    // ========================================================================
    @Test
    fun testF1TeamIdHistoricalIdsAndAmbiguousFallbacks() {
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("toro_rosso"))
        assertEquals(F1TeamId.AUDI_SAUBER, F1TeamId.fromName("alfa_romeo"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("force_india"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("racing_point"))
        assertEquals(F1TeamId.ASTON_MARTIN, F1TeamId.fromName("aston_martin"))
        assertEquals(F1TeamId.RED_BULL, F1TeamId.fromName("red_bull"))
        assertEquals(F1TeamId.RACING_BULLS, F1TeamId.fromName("rb"))

        // Ambiguous historical constructors removed from heuristic mapping must fall back to OTHER
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("lotus"))
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("Team Lotus"))
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("tyrrell"))
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("Tyrrell Racing"))
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("brabham"))
        assertEquals(F1TeamId.OTHER, F1TeamId.fromName("Brabham"))
    }

    @Test
    fun testConstructorGroupingByIdAndNameWithDistinctStableKeys() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_constructor_grouping_test").toFile()
        try {
            val fakeJolpica = createFakeJolpicaApi(
                onGetDriverStandings = { season, _, _ ->
                    JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "8",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "8",
                                        driverStandings = listOf(
                                            DriverStandingDto(
                                                position = "1",
                                                points = "50",
                                                wins = "2",
                                                driver = DriverDto("peterson", code = "PET", givenName = "Ronnie", familyName = "Peterson"),
                                                constructors = listOf(ConstructorDto(constructorId = "lotus_f1", name = "Lotus"))
                                            ),
                                            DriverStandingDto(
                                                position = "2",
                                                points = "44",
                                                wins = "1",
                                                driver = DriverDto("stewart", code = "STE", givenName = "Jackie", familyName = "Stewart"),
                                                constructors = listOf(ConstructorDto(constructorId = "tyrrell_racing", name = "Tyrrell"))
                                            ),
                                            DriverStandingDto(
                                                position = "3",
                                                points = "30",
                                                wins = "0",
                                                driver = DriverDto("depailler", code = "DEP", givenName = "Patrick", familyName = "Depailler"),
                                                constructors = listOf(ConstructorDto(constructorId = "", name = "Tyrrell"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                },
                onGetConstructorStandings = { season, _, _ ->
                    JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "8",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "8",
                                        constructorStandings = listOf(
                                            ConstructorStandingDto(
                                                position = "1",
                                                points = "50",
                                                wins = "2",
                                                constructor = ConstructorDto(constructorId = "lotus_f1", name = "Lotus")
                                            ),
                                            ConstructorStandingDto(
                                                position = "2",
                                                points = "74",
                                                wins = "1",
                                                constructor = ConstructorDto(constructorId = "", name = "Tyrrell")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
            )

            val (repo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val constructors = repo.getConstructorStandings(1973, forceRefresh = true)
            assertEquals(2, constructors.size)

            val lotus = constructors[0]
            assertEquals(F1TeamId.OTHER, lotus.team)
            assertEquals("lotus_f1", lotus.constructorId)
            assertEquals("Lotus", lotus.resolvedConstructorName)
            assertEquals("lotus_f1", lotus.stableKey)
            assertEquals(listOf("Ronnie Peterson"), lotus.drivers.map { it.fullName })

            val tyrrell = constructors[1]
            assertEquals(F1TeamId.OTHER, tyrrell.team)
            assertEquals("", tyrrell.constructorId)
            assertEquals("Tyrrell", tyrrell.resolvedConstructorName)
            assertEquals("other_2", tyrrell.stableKey)
            assertEquals(listOf("Jackie Stewart", "Patrick Depailler"), tyrrell.drivers.map { it.fullName })
            assertTrue(lotus.stableKey != tyrrell.stableKey)

            // Blank constructorName falls back to team.displayName
            val fallbackStanding = ConstructorStanding(
                position = 3,
                team = F1TeamId.FERRARI,
                points = 10.0,
                wins = 0,
                gapToP1 = null,
                gapToPrev = null,
                drivers = emptyList(),
                seasonWins = emptyList()
            )
            assertEquals("Ferrari", fallbackStanding.resolvedConstructorName)
            assertEquals("ferrari_3", fallbackStanding.stableKey)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 28. A4: No Cache Poisoning on Seed Fallback
    // ========================================================================
    @Test
    fun testSeedFallbackDoesNotPoisonCacheAndSubsequentOnlineRequestHitsApi() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_no_cache_poisoning_test").toFile()
        try {
            val db = createDatabaseInstance(tempDir)
            val prefs = createPreferencesStore(tempDir)

            val failingJolpica = createFakeJolpicaApi(
                onGetDriverStandings = { _, _, _ -> throw IOException("Network timeout") },
                onGetConstructorStandings = { _, _, _ -> throw IOException("Network timeout") }
            )
            val offlineRepo = F1Repository(
                context = null,
                jolpicaApi = failingJolpica,
                openF1Api = null,
                database = db,
                preferencesStore = prefs
            )

            // Falling back to 2026 seed data must return seed results without writing to f1_offline_cache
            val fallbackDrivers = offlineRepo.getDriverStandings(2026)
            assertFalse(fallbackDrivers.isEmpty())
            assertEquals("Kimi Antonelli", fallbackDrivers[0].fullName)
            assertNull(db.f1Dao().getStandings("2026_drivers"))

            val fallbackConstructors = offlineRepo.getConstructorStandings(2026)
            assertFalse(fallbackConstructors.isEmpty())
            assertEquals(F1TeamId.MERCEDES, fallbackConstructors[0].team)
            assertNull(db.f1Dao().getStandings("2026_constructors"))

            // Subsequent online request (even with forceRefresh = false) must still hit the API
            var driverApiCalls = 0
            var constructorApiCalls = 0
            val onlineJolpica = createFakeJolpicaApi(
                onGetDriverStandings = { season, _, _ ->
                    driverApiCalls++
                    JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "1",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "1",
                                        driverStandings = listOf(
                                            DriverStandingDto(
                                                position = "1",
                                                points = "25",
                                                wins = "1",
                                                driver = DriverDto("piastri", code = "PIA", givenName = "Oscar", familyName = "Piastri"),
                                                constructors = listOf(ConstructorDto("mclaren", name = "McLaren"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                },
                onGetConstructorStandings = { season, _, _ ->
                    constructorApiCalls++
                    JolpicaResponse(
                        mrData = MRData(
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "1",
                                standingsLists = listOf(
                                    StandingsListDto(
                                        season = season,
                                        round = "1",
                                        constructorStandings = listOf(
                                            ConstructorStandingDto(
                                                position = "1",
                                                points = "25",
                                                wins = "1",
                                                constructor = ConstructorDto("mclaren", name = "McLaren")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
            )
            val onlineRepo = F1Repository(
                context = null,
                jolpicaApi = onlineJolpica,
                openF1Api = null,
                database = db,
                preferencesStore = prefs
            )

            val liveDrivers = onlineRepo.getDriverStandings(2026, forceRefresh = false)
            assertEquals(1, driverApiCalls)
            assertEquals("Oscar Piastri", liveDrivers[0].fullName)
            assertNotNull(db.f1Dao().getStandings("2026_drivers"))

            val liveConstructors = onlineRepo.getConstructorStandings(2026, forceRefresh = false)
            assertEquals(1, constructorApiCalls)
            assertEquals(F1TeamId.MCLAREN, liveConstructors[0].team)
            assertNotNull(db.f1Dao().getStandings("2026_constructors"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========================================================================
    // 29. A5: OpenF1 Live Enrichment Conversions, Deterministic IDs & Nullable Stint Compound
    // ========================================================================
    @Test
    fun testOpenF1LiveEnrichmentConversionsAndFixtureIsolation() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_openf1_enrichment_test").toFile()
        try {
            var openF1Invocations = 0
            var currentWindDir = 225.0
            var currentWindSpeed = 5.55 // 5.55 * 3.6 = 19.98 -> 20.0 km/h
            var currentRainfall = 0.8

            val fakeOpenF1 = createFakeOpenF1Api(
                onGetWeather = { _ ->
                    openF1Invocations++
                    listOf(
                        OpenF1WeatherDto(
                            airTemperature = 22.4,
                            trackTemperature = 31.8,
                            humidity = 74.6,
                            pressure = 1009.2,
                            rainfall = currentRainfall,
                            sessionKey = 9801,
                            windDirection = currentWindDir,
                            windSpeed = currentWindSpeed
                        )
                    )
                },
                onGetRaceControl = { _ ->
                    openF1Invocations++
                    listOf(
                        OpenF1RaceControlDto(
                            sessionKey = 9801,
                            date = "2026-09-06T13:05:10Z",
                            message = "DOUBLE YELLOW IN SECTOR 2",
                            category = "Flag",
                            flag = "DOUBLE YELLOW"
                        ),
                        OpenF1RaceControlDto(
                            sessionKey = 9801,
                            date = "2026-09-06T13:07:00Z",
                            message = "",
                            category = null,
                            flag = "RED"
                        )
                    )
                }
            )
            val (repo, _) = createRepository(tempDir, openF1Api = fakeOpenF1)

            // Named fixtures never call OpenF1
            val saoPaulo = repo.getLiveSessionState("SAO_PAULO")
            val bahrain = repo.getLiveSessionState("BAHRAIN")
            assertEquals("São Paulo Grand Prix", saoPaulo.grandPrixName)
            assertEquals("Bahrain Grand Prix", bahrain.grandPrixName)
            assertEquals(0, openF1Invocations)

            // Numeric session key enriches weather & race control with deterministic IDs
            val firstLive = repo.getLiveSessionState("9801")
            assertEquals(2, openF1Invocations)
            assertEquals(20.0, firstLive.weather.windSpeedKmh, 0.001)
            assertEquals("SW", firstLive.weather.windDirection)
            assertEquals(100, firstLive.weather.rainfallPercent)
            assertEquals(75, firstLive.weather.humidityPercent)

            assertEquals(2, firstLive.raceControlMessages.size)
            assertEquals("RED", firstLive.raceControlMessages[0].title)
            assertEquals("INFO", firstLive.raceControlMessages[0].category)
            assertEquals("rc_9801_2026-09-06T13:07:00Z_0", firstLive.raceControlMessages[0].id)
            assertEquals("DOUBLE YELLOW IN SECTOR 2", firstLive.raceControlMessages[1].title)
            assertEquals(
                "rc_9801_2026-09-06T13:05:10Z_${"DOUBLE YELLOW IN SECTOR 2".hashCode()}",
                firstLive.raceControlMessages[1].id
            )

            // Calling again produces identical deterministic RC IDs
            val secondLive = repo.getLiveSessionState("9801")
            assertEquals(firstLive.raceControlMessages.map { it.id }, secondLive.raceControlMessages.map { it.id })

            // Dry track (rainfall == 0.0) & compass cardinal check (0.0 -> N, 90.0 -> E)
            currentRainfall = 0.0
            currentWindDir = 0.0
            currentWindSpeed = 10.0
            val dryNorth = repo.getLiveSessionState("latest")
            assertEquals(0, dryNorth.weather.rainfallPercent)
            assertEquals("N", dryNorth.weather.windDirection)
            assertEquals(36.0, dryNorth.weather.windSpeedKmh, 0.001)

            currentWindDir = 90.0
            val eastWind = repo.getLiveSessionState("LATEST")
            assertEquals("E", eastWind.weather.windDirection)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testOpenF1StintDtoNullableCompoundDeserialization() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val nullCompoundJson = """{"driver_number":1,"stint_number":1,"lap_start":1,"compound":null}"""
        val omittedCompoundJson = """{"driver_number":44,"stint_number":2,"lap_start":18}"""

        val stintNull = json.decodeFromString<OpenF1StintDto>(nullCompoundJson)
        assertNull(stintNull.compound)
        assertEquals(1, stintNull.driverNumber)

        val stintOmitted = json.decodeFromString<OpenF1StintDto>(omittedCompoundJson)
        assertNull(stintOmitted.compound)
        assertEquals(44, stintOmitted.driverNumber)
        assertEquals(2, stintOmitted.stintNumber)
    }

    // ========================================================================
    // 30. A6: getLatestCompletedRace Filtering & Double Half-Points Formatting
    // ========================================================================
    @Test
    fun testGetLatestCompletedRace2026NeverReturns2024SaoPauloAndFiltersInProgressOrFutureRaces() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_completed_race_filter_test").toFile()
        try {
            // 1. Offline 2026 fallback must return a 2026 weekend, never 2024 São Paulo
            val (offlineRepo, _) = createRepository(tempDir)
            val offline2026 = offlineRepo.getLatestCompletedRace(2026)
            assertEquals(2026, offline2026.season)
            assertTrue(offline2026.results.isEmpty())

            // Historical 2024 & 2023 still resolve their historical seeds
            val offline2024 = offlineRepo.getLatestCompletedRace(2024)
            assertEquals(2024, offline2024.season)
            assertEquals("São Paulo Grand Prix", offline2024.grandPrixName)

            val offline2023 = offlineRepo.getLatestCompletedRace(2023)
            assertEquals(2023, offline2023.season)
            assertEquals("Yas Marina Circuit", offline2023.circuitName)

            // 2. Completion filtering skips in-progress (1h after start < 3h buffer) and future races
            val nowInstant = Instant.now()
            val completedStart = nowInstant.minusSeconds(2 * 24 * 3600L)
            val inProgressStart = nowInstant.minusSeconds(3600L)
            val futureStart = nowInstant.plusSeconds(7 * 24 * 3600L)

            val testSeason = completedStart.atZone(ZoneOffset.UTC).year
            val requestedRounds = mutableListOf<String>()

            val fakeJolpica = createFakeJolpicaApi(
                onGetSeasonSchedule = { season, _, _ ->
                    val races = buildList {
                        add(
                            RaceDto(
                                season = season,
                                round = "10",
                                raceName = "Belgian Grand Prix",
                                circuit = CircuitDto("spa", circuitName = "Circuit de Spa-Francorchamps", location = LocationDto("Spa", "Belgium")),
                                date = completedStart.atZone(ZoneOffset.UTC).toLocalDate().toString(),
                                time = DateTimeFormatter.ISO_LOCAL_TIME.format(completedStart.atZone(ZoneOffset.UTC).toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)) + "Z"
                            )
                        )
                        if (inProgressStart.atZone(ZoneOffset.UTC).year == testSeason) {
                            add(
                                RaceDto(
                                    season = season,
                                    round = "11",
                                    raceName = "Dutch Grand Prix",
                                    circuit = CircuitDto("zandvoort", circuitName = "Circuit Zandvoort", location = LocationDto("Zandvoort", "Netherlands")),
                                    date = inProgressStart.atZone(ZoneOffset.UTC).toLocalDate().toString(),
                                    time = DateTimeFormatter.ISO_LOCAL_TIME.format(inProgressStart.atZone(ZoneOffset.UTC).toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)) + "Z"
                                )
                            )
                        }
                        if (futureStart.atZone(ZoneOffset.UTC).year == testSeason) {
                            add(
                                RaceDto(
                                    season = season,
                                    round = "12",
                                    raceName = "Italian Grand Prix",
                                    circuit = CircuitDto("monza", circuitName = "Autodromo Nazionale Monza", location = LocationDto("Monza", "Italy")),
                                    date = futureStart.atZone(ZoneOffset.UTC).toLocalDate().toString(),
                                    time = DateTimeFormatter.ISO_LOCAL_TIME.format(futureStart.atZone(ZoneOffset.UTC).toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)) + "Z"
                                )
                            )
                        }
                    }
                    JolpicaResponse(mrData = MRData(raceTable = RaceTableWrapper(season = season, races = races)))
                },
                onGetRaceResults = { season, round ->
                    requestedRounds.add(round)
                    JolpicaResponse(
                        mrData = MRData(
                            raceTable = RaceTableWrapper(
                                season = season,
                                round = round,
                                races = listOf(
                                    RaceDto(
                                        season = season,
                                        round = round,
                                        raceName = "Belgian Grand Prix",
                                        results = listOf(
                                            ResultDto(
                                                position = "1",
                                                points = "12.5",
                                                driver = DriverDto("verstappen", code = "VER", givenName = "Max", familyName = "Verstappen"),
                                                constructor = ConstructorDto("red_bull", name = "Red Bull"),
                                                laps = "1",
                                                status = "Finished"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
            )

            val (networkRepo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val completedRace = networkRepo.getLatestCompletedRace(testSeason)
            assertEquals(listOf("10"), requestedRounds)
            assertEquals(10, completedRace.round)
            assertEquals("Belgian Grand Prix", completedRace.grandPrixName)
            assertEquals(12.5, completedRace.results[0].points, 0.001)
            assertEquals("12.5", completedRace.results[0].formattedPoints)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testRaceDriverResultDoubleHalfPointsFormatting() {
        val halfPoints = RaceDriverResult(
            position = 1,
            driverCode = "VER",
            driverFullName = "Max Verstappen",
            team = F1TeamId.RED_BULL,
            finishTimeOrGap = "0:03:27.071",
            points = 12.5
        )
        assertEquals(12.5, halfPoints.points, 0.001)
        assertEquals("12.5", halfPoints.formattedPoints)

        val wholePoints = RaceDriverResult(
            position = 1,
            driverCode = "LEC",
            driverFullName = "Charles Leclerc",
            team = F1TeamId.FERRARI,
            finishTimeOrGap = "1:30:00.000",
            points = 25.0
        )
        assertEquals("25", wholePoints.formattedPoints)

        val intSecondary = RaceDriverResult(
            position = 2,
            driverCode = "HAM",
            driverFullName = "Lewis Hamilton",
            team = F1TeamId.FERRARI,
            finishTimeOrGap = "+1.200",
            points = 18
        )
        assertEquals(18.0, intSecondary.points, 0.001)
        assertEquals("18", intSecondary.formattedPoints)
    }

    // ========================================================================
    // 31. A7 & A18 & A15: Pagination, Non-Z UTC Offsets, SprintQualifying, Circuit Specs & Months
    // ========================================================================
    @Test
    fun testJolpicaPaginationFetchesAllPagesUntilTotal() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_pagination_test").toFile()
        try {
            val scheduleOffsets = mutableListOf<Int>()
            val driverOffsets = mutableListOf<Int>()
            val constructorOffsets = mutableListOf<Int>()

            val fakeJolpica = createFakeJolpicaApi(
                onGetSeasonSchedule = { season, _, offset ->
                    scheduleOffsets.add(offset)
                    val races = when (offset) {
                        0 -> listOf(
                            RaceDto(season = season, round = "1", raceName = "Round 1 GP", date = "2026-03-15", time = "05:00:00Z"),
                            RaceDto(season = season, round = "2", raceName = "Round 2 GP", date = "2026-03-22", time = "07:00:00Z")
                        )
                        2 -> listOf(
                            RaceDto(season = season, round = "3", raceName = "Round 3 GP", date = "2026-04-05", time = "06:00:00Z")
                        )
                        else -> emptyList()
                    }
                    JolpicaResponse(
                        mrData = MRData(
                            limit = "2",
                            offset = offset.toString(),
                            total = "3",
                            raceTable = RaceTableWrapper(season = season, races = races)
                        )
                    )
                },
                onGetDriverStandings = { season, _, offset ->
                    driverOffsets.add(offset)
                    val drivers = when (offset) {
                        0 -> listOf(
                            DriverStandingDto("1", points = "50", wins = "2", driver = DriverDto("ant", code = "ANT", givenName = "Kimi", familyName = "Antonelli"), constructors = listOf(ConstructorDto("mercedes", name = "Mercedes"))),
                            DriverStandingDto("2", points = "36", wins = "0", driver = DriverDto("rus", code = "RUS", givenName = "George", familyName = "Russell"), constructors = listOf(ConstructorDto("mercedes", name = "Mercedes")))
                        )
                        2 -> listOf(
                            DriverStandingDto("3", points = "25", wins = "0", driver = DriverDto("ham", code = "HAM", givenName = "Lewis", familyName = "Hamilton"), constructors = listOf(ConstructorDto("ferrari", name = "Ferrari")))
                        )
                        else -> emptyList()
                    }
                    JolpicaResponse(
                        mrData = MRData(
                            limit = "2",
                            offset = offset.toString(),
                            total = "3",
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "2",
                                standingsLists = listOf(StandingsListDto(season = season, round = "2", driverStandings = drivers))
                            )
                        )
                    )
                },
                onGetConstructorStandings = { season, _, offset ->
                    constructorOffsets.add(offset)
                    val constructors = when (offset) {
                        0 -> listOf(ConstructorStandingDto("1", points = "86", wins = "2", constructor = ConstructorDto("mercedes", name = "Mercedes")))
                        1 -> listOf(ConstructorStandingDto("2", points = "25", wins = "0", constructor = ConstructorDto("ferrari", name = "Ferrari")))
                        else -> emptyList()
                    }
                    JolpicaResponse(
                        mrData = MRData(
                            limit = "1",
                            offset = offset.toString(),
                            total = "2",
                            standingsTable = StandingsTableWrapper(
                                season = season,
                                round = "2",
                                standingsLists = listOf(StandingsListDto(season = season, round = "2", constructorStandings = constructors))
                            )
                        )
                    )
                }
            )

            val (repo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)

            val schedule = repo.getSeasonSchedule(2026, forceRefresh = true)
            assertEquals(listOf(0, 2), scheduleOffsets)
            assertEquals(listOf(1, 2, 3), schedule.map { it.round })

            val drivers = repo.getDriverStandings(2026, forceRefresh = true)
            assertEquals(listOf(0, 2), driverOffsets)
            assertEquals(listOf("ANT", "RUS", "HAM"), drivers.map { it.code })

            val constructors = repo.getConstructorStandings(2026, forceRefresh = true)
            assertEquals(listOf(0, 1), constructorOffsets)
            assertEquals(listOf(F1TeamId.MERCEDES, F1TeamId.FERRARI), constructors.map { it.team })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testNonUtcOffsetAndSprintQualifyingMapping() = runBlocking {
        val tempDir = Files.createTempDirectory("f1_offset_sprint_test").toFile()
        try {
            val fakeJolpica = createFakeJolpicaApi(
                onGetSeasonSchedule = { season, _, _ ->
                    val raceWithSprintQuali = RaceDto(
                        season = season,
                        round = "11",
                        raceName = "Austrian Grand Prix",
                        circuit = CircuitDto("red_bull_ring", circuitName = "Red Bull Ring", location = LocationDto("Spielberg", "Austria")),
                        date = "2026-07-05",
                        time = "15:00:00+02:00",
                        firstPractice = SessionDto(date = "2026-07-03", time = "12:30:00+02:00"),
                        sprintQualifying = SessionDto(date = "2026-07-03", time = "16:30:00+02:00"),
                        sprint = SessionDto(date = "2026-07-04", time = "12:00:00+02:00"),
                        qualifying = SessionDto(date = "2026-07-04", time = "16:00:00+02:00")
                    )
                    val raceWithSprintShootout = RaceDto(
                        season = season,
                        round = "22",
                        raceName = "São Paulo Grand Prix",
                        circuit = CircuitDto("interlagos", circuitName = "Autódromo José Carlos Pace", location = LocationDto("São Paulo", "Brazil")),
                        date = "2026-11-08",
                        time = "14:00:00-03:00",
                        firstPractice = SessionDto(date = "2026-11-06", time = "11:30:00-03:00"),
                        sprintShootout = SessionDto(date = "2026-11-06", time = "15:30:00-03:00")
                    )
                    JolpicaResponse(
                        mrData = MRData(
                            raceTable = RaceTableWrapper(season = season, races = listOf(raceWithSprintQuali, raceWithSprintShootout))
                        )
                    )
                }
            )

            val (repo, _) = createRepository(tempDir, jolpicaApi = fakeJolpica)
            val schedule = repo.getSeasonSchedule(2026, forceRefresh = true)
            assertEquals(2, schedule.size)

            val austria = schedule[0]
            assertTrue(austria.isSprintWeekend)
            assertEquals(5, austria.sessions.size)
            assertEquals(listOf("FP1", "SQ", "Sprint", "Quali", "Race"), austria.sessions.map { it.shortName })

            val sq = austria.sessions[1]
            assertEquals("Sprint Qualifying", sq.fullName)
            assertEquals(
                OffsetDateTime.parse("2026-07-03T16:30:00+02:00").toInstant().toEpochMilli(),
                sq.utcEpochMillis
            )

            val austriaRace = austria.sessions.last()
            val expectedAustriaInstant = OffsetDateTime.parse("2026-07-05T15:00:00+02:00").toInstant()
            assertEquals(expectedAustriaInstant.toEpochMilli(), austriaRace.utcEpochMillis)
            assertEquals(
                expectedAustriaInstant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                austriaRace.localTime
            )

            val brazil = schedule[1]
            assertTrue(brazil.isSprintWeekend)
            assertEquals(listOf("FP1", "SQ", "Race"), brazil.sessions.map { it.shortName })
            val brazilRace = brazil.sessions.last()
            assertEquals(
                OffsetDateTime.parse("2026-11-08T14:00:00-03:00").toInstant().toEpochMilli(),
                brazilRace.utcEpochMillis
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLookupCircuitSpecsDistinguishesCatalunyaAndSpa() {
        val catalunya = F1Repository.lookupCircuitSpecs("catalunya", "Circuit de Barcelona-Catalunya")
        assertEquals("CATALUNYA", catalunya.circuitId)
        assertEquals("4.657", catalunya.lengthKm)
        assertEquals(66, catalunya.laps)
        assertEquals(14, catalunya.corners)

        val spa = F1Repository.lookupCircuitSpecs("spa", "Circuit de Spa-Francorchamps")
        assertEquals("SPA", spa.circuitId)
        assertEquals("7.004", spa.lengthKm)
        assertEquals(44, spa.laps)
        assertEquals(19, spa.corners)

        // Word-boundary check: "spain" must not false-match "spa"
        val spainFallback = F1Repository.lookupCircuitSpecs("spain", "Spanish Grand Prix")
        assertEquals("SPAIN", spainFallback.circuitId)
        assertFalse(spainFallback.circuitId == "SPA")
    }

    @Test
    fun testUpdateWeekendCountdownsParsesAllMonthNames() {
        val pastSeason = LocalDate.now().year - 1
        val monthTokens = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Sept", "October", "Nov", "Dec")
        val sessions = monthTokens.mapIndexed { idx, month ->
            SessionScheduleItem(
                shortName = if (idx == monthTokens.lastIndex) "Race" else "S$idx",
                fullName = if (idx == monthTokens.lastIndex) "Race" else "Session $idx",
                dayOfWeek = "Sun",
                fullDayName = "Sunday",
                dateDay = "10",
                dateMonth = month,
                localTime = "14:00",
                countdownBadge = "Placeholder",
                isRace = idx == monthTokens.lastIndex
            )
        }

        val weekend = RaceWeekend(
            season = pastSeason,
            round = 1,
            grandPrixName = "Test GP",
            circuitName = "Test Circuit",
            location = "Test",
            countryFlagEmoji = "🏁",
            dateRangeText = "TEST",
            daysToGo = 99,
            circuitLengthKm = "5.000",
            totalLaps = 50,
            totalCorners = 15,
            sessions = sessions
        )

        val updated = F1Repository.updateWeekendCountdowns(weekend)
        assertEquals(0, updated.daysToGo)
        updated.sessions.dropLast(1).forEach {
            assertEquals("Completed", it.countdownBadge)
        }
        assertEquals("Final", updated.sessions.last().countdownBadge)

        // Future season computes positive daysToGo and "In N days"
        val futureSeason = LocalDate.now().year + 1
        val futureWeekend = weekend.copy(
            season = futureSeason,
            sessions = listOf(
                SessionScheduleItem(
                    shortName = "Race",
                    fullName = "Race",
                    dayOfWeek = "Sun",
                    fullDayName = "Sunday",
                    dateDay = "15",
                    dateMonth = "December",
                    localTime = "14:00",
                    countdownBadge = "Placeholder",
                    isRace = true
                )
            )
        )
        val updatedFuture = F1Repository.updateWeekendCountdowns(futureWeekend)
        assertTrue(updatedFuture.daysToGo > 0)
        assertEquals("In ${updatedFuture.daysToGo} days", updatedFuture.sessions[0].countdownBadge)
    }

    // ========================================================================
    // 32. A24: F1Database Singleton Per Canonical Path & Reset Isolation
    // ========================================================================
    @Test
    fun testF1DatabaseSingletonPerCanonicalPathAndReset() {
        val tempDir = Files.createTempDirectory("f1_db_singleton_test").toFile()
        try {
            F1Database.resetInstanceForTesting()
            val dirA = File(tempDir, "cache_a")
            val dirB = File(tempDir, "cache_b")

            val instanceA1 = F1Database.getInstance(dirA)
            val instanceA2 = F1Database.getInstance(File(tempDir, "./cache_a"))
            val instanceB = F1Database.getInstance(dirB)

            assertTrue("Same canonical path must return exact singleton instance", instanceA1 === instanceA2)
            assertFalse("Different canonical paths must maintain separate instances", instanceA1 === instanceB)

            F1Database.resetInstanceForTesting()
            val instanceA3 = F1Database.getInstance(dirA)
            assertFalse("resetInstanceForTesting must clear cached instances", instanceA1 === instanceA3)
        } finally {
            F1Database.resetInstanceForTesting()
            tempDir.deleteRecursively()
        }
    }

    companion object {
        private fun createFakeJolpicaApi(
            onGetSeasonSchedule: suspend (season: String, limit: Int, offset: Int) -> JolpicaResponse<RaceTableWrapper> = { _, _, _ -> JolpicaResponse() },
            onGetDriverStandings: suspend (season: String, limit: Int, offset: Int) -> JolpicaResponse<StandingsTableWrapper> = { _, _, _ -> JolpicaResponse() },
            onGetConstructorStandings: suspend (season: String, limit: Int, offset: Int) -> JolpicaResponse<StandingsTableWrapper> = { _, _, _ -> JolpicaResponse() },
            onGetRaceResults: suspend (season: String, round: String) -> JolpicaResponse<RaceTableWrapper> = { _, _ -> JolpicaResponse() },
            onGetQualifyingResults: suspend (season: String, round: String) -> JolpicaResponse<RaceTableWrapper> = { _, _ -> JolpicaResponse() }
        ): JolpicaApiService = object : JolpicaApiService {
            override suspend fun getSeasons(limit: Int, offset: Int): JolpicaResponse<com.reva2473.f1widget.data.remote.SeasonTableWrapper> =
                JolpicaResponse()
            override suspend fun getSeasonSchedule(season: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> =
                onGetSeasonSchedule(season, limit, offset)
            override suspend fun getDriverStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> =
                onGetDriverStandings(season, limit, offset)
            override suspend fun getConstructorStandings(season: String, limit: Int, offset: Int): JolpicaResponse<StandingsTableWrapper> =
                onGetConstructorStandings(season, limit, offset)
            override suspend fun getRaceResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> =
                onGetRaceResults(season, round)
            override suspend fun getQualifyingResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> =
                onGetQualifyingResults(season, round)
            override suspend fun getPitStops(season: String, round: String, limit: Int, offset: Int): JolpicaResponse<RaceTableWrapper> =
                JolpicaResponse()
        }

        private fun createFakeOpenF1Api(
            onGetWeather: suspend (String) -> List<OpenF1WeatherDto> = { emptyList() },
            onGetRaceControl: suspend (String) -> List<OpenF1RaceControlDto> = { emptyList() }
        ): OpenF1ApiService = object : OpenF1ApiService {
            override suspend fun getSessions(sessionKey: String?, sessionName: String?, sessionType: String?, year: Int?, countryName: String?, circuitShortName: String?): List<OpenF1SessionDto> = emptyList()
            override suspend fun getPositions(sessionKey: String, driverNumber: Int?): List<OpenF1PositionDto> = emptyList()
            override suspend fun getIntervals(sessionKey: String, driverNumber: Int?): List<OpenF1IntervalDto> = emptyList()
            override suspend fun getLaps(sessionKey: String, driverNumber: Int?, lapNumber: Int?): List<OpenF1LapDto> = emptyList()
            override suspend fun getStints(sessionKey: String, driverNumber: Int?): List<OpenF1StintDto> = emptyList()
            override suspend fun getTeamRadio(sessionKey: String, driverNumber: Int?): List<OpenF1TeamRadioDto> = emptyList()
            override suspend fun getRaceControl(sessionKey: String, category: String?, flag: String?): List<OpenF1RaceControlDto> = onGetRaceControl(sessionKey)
            override suspend fun getOvertakes(sessionKey: String): List<OpenF1OvertakeDto> = emptyList()
            override suspend fun getWeather(sessionKey: String): List<OpenF1WeatherDto> = onGetWeather(sessionKey)
        }
        private fun createPreferencesStore(tempDir: File): UserPreferencesStore {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File(tempDir, "user_preferences_${System.nanoTime()}.preferences_pb") }
            )
            return UserPreferencesStore(dataStore)
        }

        private fun createDatabaseInstance(tempDir: File): F1Database {
            return F1Database(File(tempDir, "f1_offline_cache"))
        }

        private fun createRepository(
            tempDir: File,
            jolpicaApi: JolpicaApiService? = null,
            openF1Api: OpenF1ApiService? = null
        ): Pair<F1Repository, F1Database> {
            val db = createDatabaseInstance(tempDir)
            val prefs = createPreferencesStore(tempDir)
            val repo = F1Repository(
                context = null,
                jolpicaApi = jolpicaApi,
                openF1Api = openF1Api,
                database = db,
                preferencesStore = prefs
            )
            return Pair(repo, db)
        }
    }
}
