package com.reva2473.f1widget

import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.TyreCompound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying SeedDataProvider integrity, standings calculations,
 * tyre stint structures, driver career comparisons, and live timing state.
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
        assertEquals("+23.265", p4.finishTimeOrGap)

        val p5 = results[4]
        assertEquals(5, p5.position)
        assertEquals("Charles Leclerc", p5.driverFullName)
        assertEquals(F1TeamId.FERRARI, p5.team)
        assertEquals("+30.112", p5.finishTimeOrGap)
    }

    @Test
    fun testAbuDhabi2023VerstappenStints() {
        val weekend = SeedDataProvider.getAbuDhabi2023Results()
        val p1 = weekend.results.first { it.position == 1 }

        assertEquals("Max Verstappen", p1.driverFullName)
        assertEquals("1:27:02.624", p1.finishTimeOrGap)
        assertEquals(26, p1.points)
        assertEquals(3, p1.stints.size)

        val stint1 = p1.stints[0]
        assertEquals(1, stint1.stintNumber)
        assertEquals(TyreCompound.MEDIUM, stint1.compound)
        assertEquals(16, stint1.lapsCount)
        assertEquals("21.6s", stint1.pitStopDuration)
        assertEquals(16, stint1.pitStopLap)

        val stint2 = p1.stints[1]
        assertEquals(2, stint2.stintNumber)
        assertEquals(TyreCompound.HARD, stint2.compound)
        assertEquals(27, stint2.lapsCount)
        assertEquals("21.2s", stint2.pitStopDuration)
        assertEquals(43, stint2.pitStopLap)

        val stint3 = p1.stints[2]
        assertEquals(3, stint3.stintNumber)
        assertEquals(TyreCompound.HARD, stint3.compound)
        assertEquals(15, stint3.lapsCount)
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

        // 4. Norris
        val p4 = standings[3]
        assertEquals(4, p4.position)
        assertEquals(186.0, p4.points, 0.01)
        assertEquals(2, p4.wins)

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
        val session = SeedDataProvider.getLiveSessionState()
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
}
