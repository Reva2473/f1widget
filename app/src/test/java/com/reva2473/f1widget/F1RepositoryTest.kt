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
import com.reva2473.f1widget.data.remote.OpenF1PositionDto
import com.reva2473.f1widget.data.remote.OpenF1WeatherDto
import com.reva2473.f1widget.data.remote.RaceDto
import com.reva2473.f1widget.data.remote.RaceTableWrapper
import com.reva2473.f1widget.data.remote.ResultDto
import com.reva2473.f1widget.data.remote.SessionDto
import com.reva2473.f1widget.data.remote.StandingsListDto
import com.reva2473.f1widget.data.remote.StandingsTableWrapper
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.SectorStatus
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
import java.time.LocalDate
import java.time.LocalTime
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
        assertEquals(20, weekend.round)
        assertEquals("Brazilian Grand Prix", weekend.grandPrixName)
        assertEquals("Autódromo José Carlos Pace", weekend.circuitName)
        assertEquals("São Paulo, Brazil", weekend.location)
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
        assertEquals("Race in 36 days", race.countdownBadge)
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
        assertEquals(26, p1.points)

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
        assertEquals(16.8, p1.avgPtsPerRace, 0.01)
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

        // 4. Norris (points match recent races 18 + 15 + 12 = 45 pts)
        val p4 = standings[3]
        assertEquals(4, p4.position)
        assertEquals(186.0, p4.points, 0.01)
        assertEquals(2, p4.wins)
        assertEquals("P2, P3, P4 (45 pts)", p4.last3Summary)

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
        assertEquals("Andrea Kimi Antonelli", merc.drivers[0].fullName)
        assertEquals(302.0, merc.drivers[0].points, 0.01)
        assertEquals("George Russell", merc.drivers[1].fullName)
        assertEquals(236.0, merc.drivers[1].points, 0.01)

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
        assertEquals(SectorStatus.INACTIVE, p1.sector2Status)
        assertEquals(SectorStatus.INACTIVE, p1.sector3Status)
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
            assertEquals(20, brazil2026.round)

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

            // 5. Driver Standings 2026 & automatic offline cache insertion
            val driverStandings = repository.getDriverStandings(2026)
            assertTrue(driverStandings.size >= 8)
            assertEquals("Kimi Antonelli", driverStandings[0].fullName)
            assertEquals(F1TeamId.MERCEDES, driverStandings[0].team)
            val cachedDriverStandings = db.f1Dao().getStandings("2026_drivers")
            assertNotNull(cachedDriverStandings)
            assertTrue(cachedDriverStandings!!.payloadJson.contains("Kimi Antonelli"))

            // 6. Constructor Standings 2026 & automatic offline cache insertion
            val constructorStandings = repository.getConstructorStandings(2026)
            assertEquals(11, constructorStandings.size)
            assertEquals(F1TeamId.MERCEDES, constructorStandings[0].team)
            assertEquals(F1TeamId.FERRARI, constructorStandings[1].team)
            val cachedConstructors = db.f1Dao().getStandings("2026_constructors")
            assertNotNull(cachedConstructors)
            assertTrue(cachedConstructors!!.payloadJson.contains("MERCEDES"))

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

                override suspend fun getSeasonSchedule(season: String, limit: Int): JolpicaResponse<RaceTableWrapper> {
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

                override suspend fun getDriverStandings(season: String, limit: Int): JolpicaResponse<StandingsTableWrapper> {
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

                override suspend fun getConstructorStandings(season: String, limit: Int): JolpicaResponse<StandingsTableWrapper> {
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
                override suspend fun getPitStops(season: String, round: String, limit: Int): JolpicaResponse<RaceTableWrapper> = JolpicaResponse()
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

            // Date range formatted from first session to race session
            assertTrue(weekend.dateRangeText.contains("FRI"))
            assertTrue(weekend.dateRangeText.contains("SUN"))
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
                override suspend fun getSeasonSchedule(season: String, limit: Int): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getDriverStandings(season: String, limit: Int): JolpicaResponse<StandingsTableWrapper> = throw IOException("Offline")
                override suspend fun getConstructorStandings(season: String, limit: Int): JolpicaResponse<StandingsTableWrapper> = throw IOException("Offline")
                override suspend fun getRaceResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getQualifyingResults(season: String, round: String): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
                override suspend fun getPitStops(season: String, round: String, limit: Int): JolpicaResponse<RaceTableWrapper> = throw IOException("Offline")
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

    companion object {
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
