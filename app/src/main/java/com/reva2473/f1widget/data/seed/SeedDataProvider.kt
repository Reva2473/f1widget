package com.reva2473.f1widget.data.seed

import com.reva2473.f1widget.domain.model.CircuitCorner
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.ConstructorWinItem
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.OvertakeEvent
import com.reva2473.f1widget.domain.model.RaceControlMessage
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceRecentResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.TeamRaceFormColumn
import com.reva2473.f1widget.domain.model.TeamRadioClip
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.domain.model.TyreStint
import com.reva2473.f1widget.domain.model.WeatherSnapshot

/**
 * High-fidelity pre-seeded data provider for 1950–2026 Formula 1 seasons,
 * replicating exact screens from P1T mobile app.
 */
object SeedDataProvider {

    // ========================================================================
    // 1. Next Race Weekend (Bahrain Grand Prix in Malaysia - Sepang)
    // Matches home-1080.webp
    // ========================================================================
    fun getNextRaceWeekend(): RaceWeekend {
        return RaceWeekend(
            season = 2026,
            round = 16,
            grandPrixName = "Bahrain Grand Prix in Malaysia",
            circuitName = "Sepang International Circuit",
            location = "Kuala Lumpur, Malaysia",
            countryFlagEmoji = "🇲🇾",
            dateRangeText = "FRI 18 SEP - SUN 20 SEP",
            daysToGo = 1,
            circuitLengthKm = "5.543",
            totalLaps = 56,
            totalCorners = 15,
            speedTrapNormalizedX = 0.24f,
            speedTrapNormalizedY = 0.27f,
            circuitType = "SEPANG",
            sessions = listOf(
                SessionScheduleItem(
                    shortName = "FP1",
                    fullName = "Free Practice 1",
                    dayOfWeek = "Fri",
                    fullDayName = "Friday",
                    dateDay = "18",
                    dateMonth = "Sep",
                    localTime = "05:30",
                    countdownBadge = "Completed",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "FP2",
                    fullName = "Free Practice 2",
                    dayOfWeek = "Fri",
                    fullDayName = "Friday",
                    dateDay = "18",
                    dateMonth = "Sep",
                    localTime = "09:00",
                    countdownBadge = "Completed",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "FP3",
                    fullName = "Free Practice 3",
                    dayOfWeek = "Sat",
                    fullDayName = "Saturday",
                    dateDay = "19",
                    dateMonth = "Sep",
                    localTime = "05:30",
                    countdownBadge = "In 1 day",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "Quali",
                    fullName = "Qualifying",
                    dayOfWeek = "Sat",
                    fullDayName = "Saturday",
                    dateDay = "19",
                    dateMonth = "Sep",
                    localTime = "09:00",
                    countdownBadge = "In 1 day",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "Race",
                    fullName = "Race",
                    dayOfWeek = "Sun",
                    fullDayName = "Sunday",
                    dateDay = "20",
                    dateMonth = "Sep",
                    localTime = "08:00",
                    countdownBadge = "Race in 1 day",
                    isRace = true
                )
            ),
            results = emptyList(),
            isSprintWeekend = false
        )
    }

    // ========================================================================
    // 2. Brazilian Grand Prix 2026 Schedule
    // Matches race-weekend-1080.webp
    // ========================================================================
    fun getBrazilianGpWeekend2026(): RaceWeekend {
        return RaceWeekend(
            season = 2026,
            round = 20,
            grandPrixName = "Brazilian Grand Prix",
            circuitName = "Autódromo José Carlos Pace",
            location = "São Paulo, Brazil",
            countryFlagEmoji = "🇧🇷",
            dateRangeText = "FRI 6 NOV - SUN 8 NOV",
            daysToGo = 34,
            circuitLengthKm = "4.309",
            totalLaps = 71,
            totalCorners = 15,
            speedTrapNormalizedX = 0.22f,
            speedTrapNormalizedY = 0.26f,
            circuitType = "INTERLAGOS",
            sessions = listOf(
                SessionScheduleItem(
                    shortName = "FP1",
                    fullName = "Free Practice 1",
                    dayOfWeek = "Fri",
                    fullDayName = "Friday",
                    dateDay = "06",
                    dateMonth = "Nov",
                    localTime = "15:30",
                    countdownBadge = "34 days",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "FP2",
                    fullName = "Free Practice 2",
                    dayOfWeek = "Fri",
                    fullDayName = "Friday",
                    dateDay = "06",
                    dateMonth = "Nov",
                    localTime = "19:00",
                    countdownBadge = "34 days",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "FP3",
                    fullName = "Free Practice 3",
                    dayOfWeek = "Sat",
                    fullDayName = "Saturday",
                    dateDay = "07",
                    dateMonth = "Nov",
                    localTime = "14:30",
                    countdownBadge = "35 days",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "Quali",
                    fullName = "Qualifying",
                    dayOfWeek = "Sat",
                    fullDayName = "Saturday",
                    dateDay = "07",
                    dateMonth = "Nov",
                    localTime = "18:00",
                    countdownBadge = "35 days",
                    isRace = false
                ),
                SessionScheduleItem(
                    shortName = "Race",
                    fullName = "Race",
                    dayOfWeek = "Sun",
                    fullDayName = "Sunday",
                    dateDay = "08",
                    dateMonth = "Nov",
                    localTime = "17:00",
                    countdownBadge = "Race in 36 days",
                    isRace = true
                )
            ),
            results = emptyList(),
            isSprintWeekend = false
        )
    }

    // ========================================================================
    // 3. São Paulo 2024 Race Results
    // Matches race-results-1080.webp
    // ========================================================================
    fun getSaoPaulo2024Results(): RaceWeekend {
        return RaceWeekend(
            season = 2024,
            round = 21,
            grandPrixName = "São Paulo Grand Prix",
            circuitName = "Autódromo José Carlos Pace",
            location = "São Paulo, Brazil",
            countryFlagEmoji = "🇧🇷",
            dateRangeText = "FRI 1 NOV - SUN 3 NOV",
            daysToGo = 0,
            circuitLengthKm = "4.309",
            totalLaps = 69,
            totalCorners = 15,
            speedTrapNormalizedX = 0.22f,
            speedTrapNormalizedY = 0.26f,
            circuitType = "INTERLAGOS",
            sessions = listOf(
                SessionScheduleItem(
                    shortName = "Race",
                    fullName = "Race",
                    dayOfWeek = "Sun",
                    fullDayName = "Sunday",
                    dateDay = "03",
                    dateMonth = "Nov",
                    localTime = "15:30",
                    countdownBadge = "Final",
                    isRace = true
                )
            ),
            results = listOf(
                RaceDriverResult(
                    position = 1,
                    driverCode = "VER",
                    driverFullName = "Max Verstappen",
                    team = F1TeamId.RED_BULL,
                    finishTimeOrGap = "2:06:54.430",
                    points = 26,
                    qualifyingPosition = "P17",
                    qualifyingTime = "1:27.771",
                    fastestLapPosition = "P1",
                    fastestLapTime = "1:20.472",
                    totalLaps = 69,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 28, pitStopDuration = "22.1s", pitStopLap = 28),
                        TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 41)
                    )
                ),
                RaceDriverResult(
                    position = 2,
                    driverCode = "OCO",
                    driverFullName = "Esteban Ocon",
                    team = F1TeamId.ALPINE,
                    finishTimeOrGap = "+19.477",
                    points = 18,
                    qualifyingPosition = "P4",
                    qualifyingTime = "1:24.475",
                    fastestLapPosition = "P4",
                    fastestLapTime = "1:21.846",
                    totalLaps = 69,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 31, pitStopDuration = "23.4s", pitStopLap = 31),
                        TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 38)
                    )
                ),
                RaceDriverResult(
                    position = 3,
                    driverCode = "GAS",
                    driverFullName = "Pierre Gasly",
                    team = F1TeamId.ALPINE,
                    finishTimeOrGap = "+22.532",
                    points = 15,
                    qualifyingPosition = "P13",
                    qualifyingTime = "1:26.016",
                    fastestLapPosition = "P6",
                    fastestLapTime = "1:22.031",
                    totalLaps = 69,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 29, pitStopDuration = "22.8s", pitStopLap = 29),
                        TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 40)
                    )
                ),
                RaceDriverResult(
                    position = 4,
                    driverCode = "RUS",
                    driverFullName = "George Russell",
                    team = F1TeamId.MERCEDES,
                    finishTimeOrGap = "+23.265",
                    points = 12,
                    qualifyingPosition = "P2",
                    qualifyingTime = "1:23.579",
                    fastestLapPosition = "P5",
                    fastestLapTime = "1:21.980",
                    totalLaps = 69,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 27, pitStopDuration = "23.1s", pitStopLap = 27),
                        TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 42)
                    )
                ),
                RaceDriverResult(
                    position = 5,
                    driverCode = "LEC",
                    driverFullName = "Charles Leclerc",
                    team = F1TeamId.FERRARI,
                    finishTimeOrGap = "+30.112",
                    points = 10,
                    qualifyingPosition = "P6",
                    qualifyingTime = "1:24.525",
                    fastestLapPosition = "P7",
                    fastestLapTime = "1:22.150",
                    totalLaps = 69,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 24, pitStopDuration = "24.5s", pitStopLap = 24),
                        TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 45)
                    )
                ),
                RaceDriverResult(
                    position = 6,
                    driverCode = "NOR",
                    driverFullName = "Lando Norris",
                    team = F1TeamId.MCLAREN,
                    finishTimeOrGap = "+31.372",
                    points = 8,
                    qualifyingPosition = "P1",
                    qualifyingTime = "1:23.405",
                    fastestLapPosition = "P3",
                    fastestLapTime = "1:21.517",
                    totalLaps = 69
                ),
                RaceDriverResult(
                    position = 7,
                    driverCode = "TSU",
                    driverFullName = "Yuki Tsunoda",
                    team = F1TeamId.RACING_BULLS,
                    finishTimeOrGap = "+42.056",
                    points = 6,
                    qualifyingPosition = "P3",
                    qualifyingTime = "1:24.111",
                    fastestLapPosition = "P7",
                    fastestLapTime = "1:22.500",
                    totalLaps = 69
                ),
                RaceDriverResult(
                    position = 8,
                    driverCode = "PIA",
                    driverFullName = "Oscar Piastri",
                    team = F1TeamId.MCLAREN,
                    finishTimeOrGap = "+44.943",
                    points = 4,
                    qualifyingPosition = "P8",
                    qualifyingTime = "1:24.686",
                    fastestLapPosition = "P5",
                    fastestLapTime = "1:21.900",
                    totalLaps = 69
                ),
                RaceDriverResult(
                    position = 9,
                    driverCode = "LAW",
                    driverFullName = "Liam Lawson",
                    team = F1TeamId.RACING_BULLS,
                    finishTimeOrGap = "+50.452",
                    points = 2,
                    qualifyingPosition = "P5",
                    qualifyingTime = "1:24.484",
                    fastestLapPosition = "P9",
                    fastestLapTime = "1:22.900",
                    totalLaps = 69
                ),
                RaceDriverResult(
                    position = 10,
                    driverCode = "HAM",
                    driverFullName = "Lewis Hamilton",
                    team = F1TeamId.MERCEDES,
                    finishTimeOrGap = "+50.753",
                    points = 1,
                    qualifyingPosition = "P14",
                    qualifyingTime = "1:26.110",
                    fastestLapPosition = "P8",
                    fastestLapTime = "1:22.800",
                    totalLaps = 69
                )
            ),
            isSprintWeekend = true
        )
    }

    // ========================================================================
    // 4. Abu Dhabi 2023 Results & Stints
    // Matches race-stints-720.webp
    // ========================================================================
    fun getAbuDhabi2023Results(): RaceWeekend {
        return RaceWeekend(
            season = 2023,
            round = 22,
            grandPrixName = "Abu Dhabi Grand Prix",
            circuitName = "Yas Marina Circuit",
            location = "Abu Dhabi, UAE",
            countryFlagEmoji = "🇦🇪",
            dateRangeText = "FRI 24 NOV - SUN 26 NOV",
            daysToGo = 0,
            circuitLengthKm = "5.281",
            totalLaps = 58,
            totalCorners = 16,
            speedTrapNormalizedX = 0.52f,
            speedTrapNormalizedY = 0.28f,
            circuitType = "YAS_MARINA",
            sessions = listOf(
                SessionScheduleItem(
                    shortName = "Race",
                    fullName = "Race",
                    dayOfWeek = "Sun",
                    fullDayName = "Sunday",
                    dateDay = "26",
                    dateMonth = "Nov",
                    localTime = "17:00",
                    countdownBadge = "Final",
                    isRace = true
                )
            ),
            results = listOf(
                RaceDriverResult(
                    position = 1,
                    driverCode = "VER",
                    driverFullName = "Max Verstappen",
                    team = F1TeamId.RED_BULL,
                    finishTimeOrGap = "1:27:02.624",
                    points = 26,
                    qualifyingPosition = "P1",
                    qualifyingTime = "1:23.445",
                    fastestLapPosition = "P1",
                    fastestLapTime = "1:26.993",
                    totalLaps = 58,
                    stints = listOf(
                        TyreStint(
                            stintNumber = 1,
                            compound = TyreCompound.MEDIUM,
                            lapsCount = 16,
                            pitStopDuration = "21.6s",
                            pitStopLap = 16
                        ),
                        TyreStint(
                            stintNumber = 2,
                            compound = TyreCompound.HARD,
                            lapsCount = 27,
                            pitStopDuration = "21.2s",
                            pitStopLap = 43
                        ),
                        TyreStint(
                            stintNumber = 3,
                            compound = TyreCompound.HARD,
                            lapsCount = 15,
                            pitStopDuration = null,
                            pitStopLap = null
                        )
                    )
                ),
                RaceDriverResult(
                    position = 2,
                    driverCode = "LEC",
                    driverFullName = "Charles Leclerc",
                    team = F1TeamId.FERRARI,
                    finishTimeOrGap = "+17.993",
                    points = 18,
                    qualifyingPosition = "P2",
                    qualifyingTime = "1:23.584",
                    fastestLapPosition = "P3",
                    fastestLapTime = "1:27.118",
                    totalLaps = 58,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.MEDIUM, lapsCount = 17, pitStopDuration = "22.3s", pitStopLap = 17),
                        TyreStint(stintNumber = 2, compound = TyreCompound.HARD, lapsCount = 26, pitStopDuration = "21.9s", pitStopLap = 43),
                        TyreStint(stintNumber = 3, compound = TyreCompound.HARD, lapsCount = 15)
                    )
                ),
                RaceDriverResult(
                    position = 3,
                    driverCode = "RUS",
                    driverFullName = "George Russell",
                    team = F1TeamId.MERCEDES,
                    finishTimeOrGap = "+20.328",
                    points = 15,
                    qualifyingPosition = "P4",
                    qualifyingTime = "1:23.788",
                    fastestLapPosition = "P4",
                    fastestLapTime = "1:27.240",
                    totalLaps = 58,
                    stints = listOf(
                        TyreStint(stintNumber = 1, compound = TyreCompound.MEDIUM, lapsCount = 15, pitStopDuration = "22.8s", pitStopLap = 15),
                        TyreStint(stintNumber = 2, compound = TyreCompound.HARD, lapsCount = 27, pitStopDuration = "21.5s", pitStopLap = 42),
                        TyreStint(stintNumber = 3, compound = TyreCompound.HARD, lapsCount = 16)
                    )
                )
            ),
            isSprintWeekend = false
        )
    }

    // ========================================================================
    // 5. Driver Standings 2026
    // Matches driver-standings-1080.webp & driver-profiles-720.webp
    // ========================================================================
    fun getDriverStandings2026(): List<DriverStanding> {
        return listOf(
            DriverStanding(
                position = 1,
                driverId = "antonelli",
                code = "ANT",
                firstName = "Andrea Kimi",
                lastName = "Antonelli",
                fullName = "Kimi Antonelli",
                nationality = "Italian",
                team = F1TeamId.MERCEDES,
                points = 302.0,
                wins = 8,
                podiums = 12,
                poles = 6,
                avgPtsPerRace = 16.8,
                racesCount = 18,
                gapToLeader = null,
                gapToPrev = null,
                last3Summary = "P5, P1, P1 (60 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P5", "(10pts)", 5),
                    RaceRecentResult("Spanish Grand Prix", "P1", "(25pts)", 1),
                    RaceRecentResult("Italian Grand Prix", "P1", "(25pts)", 1)
                )
            ),
            DriverStanding(
                position = 2,
                driverId = "russell",
                code = "RUS",
                firstName = "George",
                lastName = "Russell",
                fullName = "George Russell",
                nationality = "British",
                team = F1TeamId.MERCEDES,
                points = 236.0,
                wins = 3,
                podiums = 8,
                poles = 5,
                avgPtsPerRace = 13.1,
                racesCount = 18,
                gapToLeader = "-66 to leader",
                gapToPrev = "-66 to P1",
                last3Summary = "P1, P5, P2 (53 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P1", "(25pts)", 1),
                    RaceRecentResult("Spanish Grand Prix", "P5", "(10pts)", 5),
                    RaceRecentResult("Italian Grand Prix", "P2", "(18pts)", 2)
                )
            ),
            DriverStanding(
                position = 3,
                driverId = "hamilton",
                code = "HAM",
                firstName = "Lewis",
                lastName = "Hamilton",
                fullName = "Lewis Hamilton",
                nationality = "British",
                team = F1TeamId.FERRARI,
                points = 199.0,
                wins = 1,
                podiums = 5,
                poles = 3,
                avgPtsPerRace = 11.1,
                racesCount = 18,
                gapToLeader = "-103 to leader",
                gapToPrev = "-37 to P2",
                last3Summary = "P3, P4, P3 (42 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P3", "(15pts)", 3),
                    RaceRecentResult("Spanish Grand Prix", "P4", "(12pts)", 4),
                    RaceRecentResult("Italian Grand Prix", "P3", "(15pts)", 3)
                )
            ),
            DriverStanding(
                position = 4,
                driverId = "norris",
                code = "NOR",
                firstName = "Lando",
                lastName = "Norris",
                fullName = "Lando Norris",
                nationality = "British",
                team = F1TeamId.MCLAREN,
                points = 186.0,
                wins = 2,
                podiums = 6,
                poles = 4,
                avgPtsPerRace = 10.3,
                racesCount = 18,
                gapToLeader = "-116 to leader",
                gapToPrev = "13 pts to P3",
                last3Summary = "P2, P3, P4 (45 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Spanish Grand Prix", "P2", "(18pts)", 2),
                    RaceRecentResult("Italian Grand Prix", "P3", "(15pts)", 3),
                    RaceRecentResult("Azerbaijan Grand Prix", "P4", "(12pts)", 4)
                )
            ),
            DriverStanding(
                position = 5,
                driverId = "leclerc",
                code = "LEC",
                firstName = "Charles",
                lastName = "Leclerc",
                fullName = "Charles Leclerc",
                nationality = "Monegasque",
                team = F1TeamId.FERRARI,
                points = 179.0,
                wins = 1,
                podiums = 5,
                poles = 2,
                avgPtsPerRace = 9.9,
                racesCount = 18,
                gapToLeader = "-123 to leader",
                gapToPrev = "7 pts to P4",
                last3Summary = "P2, P3, P5 (43 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P2", "(18pts)", 2),
                    RaceRecentResult("Spanish Grand Prix", "P3", "(15pts)", 3),
                    RaceRecentResult("Italian Grand Prix", "P5", "(10pts)", 5)
                )
            ),
            DriverStanding(
                position = 6,
                driverId = "verstappen",
                code = "VER",
                firstName = "Max",
                lastName = "Verstappen",
                fullName = "Max Verstappen",
                nationality = "Dutch",
                team = F1TeamId.RED_BULL,
                points = 163.0,
                wins = 0,
                podiums = 4,
                poles = 1,
                avgPtsPerRace = 9.1,
                racesCount = 18,
                gapToLeader = "-139 to leader",
                gapToPrev = "16 pts to P5",
                last3Summary = "P6, P6, P6 (24 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P6", "(8pts)", 6),
                    RaceRecentResult("Spanish Grand Prix", "P6", "(8pts)", 6),
                    RaceRecentResult("Italian Grand Prix", "P6", "(8pts)", 6)
                )
            ),
            DriverStanding(
                position = 7,
                driverId = "piastri",
                code = "PIA",
                firstName = "Oscar",
                lastName = "Piastri",
                fullName = "Oscar Piastri",
                nationality = "Australian",
                team = F1TeamId.MCLAREN,
                points = 120.0,
                wins = 0,
                podiums = 2,
                poles = 0,
                avgPtsPerRace = 6.7,
                racesCount = 18,
                gapToLeader = "-182 to leader",
                gapToPrev = "43 pts to P6",
                last3Summary = "P7, P7, P7 (18 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P7", "(6pts)", 7),
                    RaceRecentResult("Spanish Grand Prix", "P7", "(6pts)", 7),
                    RaceRecentResult("Italian Grand Prix", "P7", "(6pts)", 7)
                )
            ),
            DriverStanding(
                position = 8,
                driverId = "hadjar",
                code = "HAD",
                firstName = "Isack",
                lastName = "Hadjar",
                fullName = "Isack Hadjar",
                nationality = "French",
                team = F1TeamId.RED_BULL,
                points = 86.0,
                wins = 0,
                podiums = 1,
                poles = 0,
                avgPtsPerRace = 4.8,
                racesCount = 18,
                gapToLeader = "-216 to leader",
                gapToPrev = "34 pts to P7",
                last3Summary = "P8, P8, P8 (12 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P8", "(4pts)", 8),
                    RaceRecentResult("Spanish Grand Prix", "P8", "(4pts)", 8),
                    RaceRecentResult("Italian Grand Prix", "P8", "(4pts)", 8)
                )
            ),
            DriverStanding(
                position = 9,
                driverId = "alonso",
                code = "ALO",
                firstName = "Fernando",
                lastName = "Alonso",
                fullName = "Fernando Alonso",
                nationality = "Spanish",
                team = F1TeamId.ASTON_MARTIN,
                points = 62.0,
                wins = 0,
                podiums = 1,
                poles = 0,
                avgPtsPerRace = 3.4,
                racesCount = 18,
                gapToLeader = "-240 to leader",
                gapToPrev = "24 pts to P8",
                last3Summary = "P9, P9, P9 (6 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P9", "(2pts)", 9),
                    RaceRecentResult("Spanish Grand Prix", "P9", "(2pts)", 9),
                    RaceRecentResult("Italian Grand Prix", "P9", "(2pts)", 9)
                )
            ),
            DriverStanding(
                position = 10,
                driverId = "gasly",
                code = "GAS",
                firstName = "Pierre",
                lastName = "Gasly",
                fullName = "Pierre Gasly",
                nationality = "French",
                team = F1TeamId.ALPINE,
                points = 48.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 2.7,
                racesCount = 18,
                gapToLeader = "-254 to leader",
                gapToPrev = "14 pts to P9",
                last3Summary = "P10, P10, P10 (3 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P10", "(1pt)", 10),
                    RaceRecentResult("Spanish Grand Prix", "P10", "(1pt)", 10),
                    RaceRecentResult("Italian Grand Prix", "P10", "(1pt)", 10)
                )
            ),
            DriverStanding(
                position = 11,
                driverId = "ocon",
                code = "OCO",
                firstName = "Esteban",
                lastName = "Ocon",
                fullName = "Esteban Ocon",
                nationality = "French",
                team = F1TeamId.HAAS,
                points = 36.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 2.0,
                racesCount = 18,
                gapToLeader = "-266 to leader",
                gapToPrev = "12 pts to P10",
                last3Summary = "P11, P12, P11 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 12,
                driverId = "albon",
                code = "ALB",
                firstName = "Alexander",
                lastName = "Albon",
                fullName = "Alex Albon",
                nationality = "Thai",
                team = F1TeamId.WILLIAMS,
                points = 32.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 1.8,
                racesCount = 18,
                gapToLeader = "-270 to leader",
                gapToPrev = "4 pts to P11",
                last3Summary = "P12, P11, P12 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 13,
                driverId = "sainz",
                code = "SAI",
                firstName = "Carlos",
                lastName = "Sainz",
                fullName = "Carlos Sainz",
                nationality = "Spanish",
                team = F1TeamId.WILLIAMS,
                points = 30.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 1.7,
                racesCount = 18,
                gapToLeader = "-272 to leader",
                gapToPrev = "2 pts to P12",
                last3Summary = "P13, P13, P13 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 14,
                driverId = "stroll",
                code = "STR",
                firstName = "Lance",
                lastName = "Stroll",
                fullName = "Lance Stroll",
                nationality = "Canadian",
                team = F1TeamId.ASTON_MARTIN,
                points = 24.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 1.3,
                racesCount = 18,
                gapToLeader = "-278 to leader",
                gapToPrev = "6 pts to P13",
                last3Summary = "P14, P14, P14 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 15,
                driverId = "lawson",
                code = "LAW",
                firstName = "Liam",
                lastName = "Lawson",
                fullName = "Liam Lawson",
                nationality = "New Zealander",
                team = F1TeamId.RACING_BULLS,
                points = 20.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 1.1,
                racesCount = 18,
                gapToLeader = "-282 to leader",
                gapToPrev = "4 pts to P14",
                last3Summary = "P15, P15, P15 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 16,
                driverId = "colapinto",
                code = "COL",
                firstName = "Franco",
                lastName = "Colapinto",
                fullName = "Franco Colapinto",
                nationality = "Argentine",
                team = F1TeamId.ALPINE,
                points = 16.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.9,
                racesCount = 18,
                gapToLeader = "-286 to leader",
                gapToPrev = "4 pts to P15",
                last3Summary = "P16, P16, P16 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 17,
                driverId = "bearman",
                code = "BEA",
                firstName = "Oliver",
                lastName = "Bearman",
                fullName = "Oliver Bearman",
                nationality = "British",
                team = F1TeamId.HAAS,
                points = 12.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.7,
                racesCount = 18,
                gapToLeader = "-290 to leader",
                gapToPrev = "4 pts to P16",
                last3Summary = "P17, P17, P17 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 18,
                driverId = "hulkenberg",
                code = "HUL",
                firstName = "Nico",
                lastName = "Hülkenberg",
                fullName = "Nico Hülkenberg",
                nationality = "German",
                team = F1TeamId.AUDI_SAUBER,
                points = 8.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.4,
                racesCount = 18,
                gapToLeader = "-294 to leader",
                gapToPrev = "4 pts to P17",
                last3Summary = "P18, P18, P18 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 19,
                driverId = "bottas",
                code = "BOT",
                firstName = "Valtteri",
                lastName = "Bottas",
                fullName = "Valtteri Bottas",
                nationality = "Finnish",
                team = F1TeamId.CADILLAC,
                points = 4.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.2,
                racesCount = 18,
                gapToLeader = "-298 to leader",
                gapToPrev = "4 pts to P18",
                last3Summary = "P19, P19, P19 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 20,
                driverId = "lindblad",
                code = "LIN",
                firstName = "Arvid",
                lastName = "Lindblad",
                fullName = "Arvid Lindblad",
                nationality = "British",
                team = F1TeamId.RACING_BULLS,
                points = 2.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.1,
                racesCount = 18,
                gapToLeader = "-300 to leader",
                gapToPrev = "2 pts to P19",
                last3Summary = "P20, P20, P20 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 21,
                driverId = "bortoleto",
                code = "BOR",
                firstName = "Gabriel",
                lastName = "Bortoleto",
                fullName = "Gabriel Bortoleto",
                nationality = "Brazilian",
                team = F1TeamId.AUDI_SAUBER,
                points = 1.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.05,
                racesCount = 18,
                gapToLeader = "-301 to leader",
                gapToPrev = "1 pt to P20",
                last3Summary = "P21, P21, P21 (0 pts)",
                recentRaces = emptyList()
            ),
            DriverStanding(
                position = 22,
                driverId = "perez",
                code = "PER",
                firstName = "Sergio",
                lastName = "Pérez",
                fullName = "Sergio Perez",
                nationality = "Mexican",
                team = F1TeamId.CADILLAC,
                points = 0.0,
                wins = 0,
                podiums = 0,
                poles = 0,
                avgPtsPerRace = 0.0,
                racesCount = 18,
                gapToLeader = "-302 to leader",
                gapToPrev = "1 pt to P21",
                last3Summary = "P22, P22, P22 (0 pts)",
                recentRaces = emptyList()
            )
        )
    }

    fun getHistoricalDriverStandings(season: Int): List<DriverStanding> {
        return when (season) {
            2026 -> getDriverStandings2026()
            else -> emptyList()
        }
    }

    // ========================================================================
    // 6. Constructor Standings 2026
    // Matches constructor-standings-1080.webp
    // ========================================================================
    fun getConstructorStandings2026(): List<ConstructorStanding> {
        return listOf(
            ConstructorStanding(
                position = 1,
                team = F1TeamId.MERCEDES,
                points = 538.0,
                wins = 11,
                gapToP1 = null,
                gapToPrev = null,
                drivers = listOf(
                    ConstructorDriverPoints("Andrea Kimi Antonelli", 302.0),
                    ConstructorDriverPoints("George Russell", 236.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Australian Grand Prix", "George Russell"),
                    ConstructorWinItem("Chinese Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Japanese Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Miami Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Canadian Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Monaco Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Austrian Grand Prix", "George Russell"),
                    ConstructorWinItem("Belgian Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Italian Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Spanish Grand Prix", "Andrea Kimi Antonelli"),
                    ConstructorWinItem("Azerbaijan Grand Prix", "George Russell")
                ),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P1", "25pts", 1),
                    TeamRaceFormColumn("Spanish GP", "P1", "25pts", 1),
                    TeamRaceFormColumn("Azerbaijan GP", "P1", "25pts", 1)
                )
            ),
            ConstructorStanding(
                position = 2,
                team = F1TeamId.FERRARI,
                points = 378.0,
                wins = 2,
                gapToP1 = "160 to P1",
                gapToPrev = "160 to P1",
                drivers = listOf(
                    ConstructorDriverPoints("Lewis Hamilton", 199.0),
                    ConstructorDriverPoints("Charles Leclerc", 179.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Bahrain Grand Prix", "Charles Leclerc"),
                    ConstructorWinItem("British Grand Prix", "Lewis Hamilton")
                ),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P2", "18pts", 2),
                    TeamRaceFormColumn("Spanish GP", "P3", "15pts", 3),
                    TeamRaceFormColumn("Azerbaijan GP", "P2", "18pts", 2)
                )
            ),
            ConstructorStanding(
                position = 3,
                team = F1TeamId.MCLAREN,
                points = 306.0,
                wins = 2,
                gapToP1 = "232 to P1",
                gapToPrev = "72 to P2",
                drivers = listOf(
                    ConstructorDriverPoints("Lando Norris", 186.0),
                    ConstructorDriverPoints("Oscar Piastri", 120.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Hungarian Grand Prix", "Lando Norris"),
                    ConstructorWinItem("Dutch Grand Prix", "Lando Norris")
                ),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P3", "15pts", 3),
                    TeamRaceFormColumn("Spanish GP", "P2", "18pts", 2),
                    TeamRaceFormColumn("Azerbaijan GP", "P4", "12pts", 4)
                )
            ),
            ConstructorStanding(
                position = 4,
                team = F1TeamId.RED_BULL,
                points = 249.0,
                wins = 0,
                gapToP1 = "289 to P1",
                gapToPrev = "57 to P3",
                drivers = listOf(
                    ConstructorDriverPoints("Max Verstappen", 163.0),
                    ConstructorDriverPoints("Isack Hadjar", 86.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P4", "12pts", 4),
                    TeamRaceFormColumn("Spanish GP", "P4", "12pts", 4),
                    TeamRaceFormColumn("Azerbaijan GP", "P3", "15pts", 3)
                )
            ),
            ConstructorStanding(
                position = 5,
                team = F1TeamId.ASTON_MARTIN,
                points = 86.0,
                wins = 0,
                gapToP1 = "452 to P1",
                gapToPrev = "163 to P4",
                drivers = listOf(
                    ConstructorDriverPoints("Fernando Alonso", 62.0),
                    ConstructorDriverPoints("Lance Stroll", 24.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P6", "8pts", 6),
                    TeamRaceFormColumn("Spanish GP", "P7", "6pts", 7),
                    TeamRaceFormColumn("Azerbaijan GP", "P5", "10pts", 5)
                )
            ),
            ConstructorStanding(
                position = 6,
                team = F1TeamId.ALPINE,
                points = 64.0,
                wins = 0,
                gapToP1 = "474 to P1",
                gapToPrev = "22 to P5",
                drivers = listOf(
                    ConstructorDriverPoints("Pierre Gasly", 48.0),
                    ConstructorDriverPoints("Franco Colapinto", 16.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P8", "4pts", 8),
                    TeamRaceFormColumn("Spanish GP", "P8", "4pts", 8),
                    TeamRaceFormColumn("Azerbaijan GP", "P7", "6pts", 7)
                )
            ),
            ConstructorStanding(
                position = 7,
                team = F1TeamId.WILLIAMS,
                points = 62.0,
                wins = 0,
                gapToP1 = "476 to P1",
                gapToPrev = "2 to P6",
                drivers = listOf(
                    ConstructorDriverPoints("Alex Albon", 32.0),
                    ConstructorDriverPoints("Carlos Sainz", 30.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P7", "6pts", 7),
                    TeamRaceFormColumn("Spanish GP", "P6", "8pts", 6),
                    TeamRaceFormColumn("Azerbaijan GP", "P8", "4pts", 8)
                )
            ),
            ConstructorStanding(
                position = 8,
                team = F1TeamId.HAAS,
                points = 48.0,
                wins = 0,
                gapToP1 = "490 to P1",
                gapToPrev = "14 to P7",
                drivers = listOf(
                    ConstructorDriverPoints("Esteban Ocon", 36.0),
                    ConstructorDriverPoints("Oliver Bearman", 12.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P9", "2pts", 9),
                    TeamRaceFormColumn("Spanish GP", "P9", "2pts", 9),
                    TeamRaceFormColumn("Azerbaijan GP", "P9", "2pts", 9)
                )
            ),
            ConstructorStanding(
                position = 9,
                team = F1TeamId.RACING_BULLS,
                points = 22.0,
                wins = 0,
                gapToP1 = "516 to P1",
                gapToPrev = "26 to P8",
                drivers = listOf(
                    ConstructorDriverPoints("Liam Lawson", 20.0),
                    ConstructorDriverPoints("Arvid Lindblad", 2.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P10", "1pt", 10),
                    TeamRaceFormColumn("Spanish GP", "P11", "0pts", 11),
                    TeamRaceFormColumn("Azerbaijan GP", "P10", "1pt", 10)
                )
            ),
            ConstructorStanding(
                position = 10,
                team = F1TeamId.AUDI_SAUBER,
                points = 9.0,
                wins = 0,
                gapToP1 = "529 to P1",
                gapToPrev = "13 to P9",
                drivers = listOf(
                    ConstructorDriverPoints("Nico Hülkenberg", 8.0),
                    ConstructorDriverPoints("Gabriel Bortoleto", 1.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P11", "0pts", 11),
                    TeamRaceFormColumn("Spanish GP", "P10", "1pt", 10),
                    TeamRaceFormColumn("Azerbaijan GP", "P12", "0pts", 12)
                )
            ),
            ConstructorStanding(
                position = 11,
                team = F1TeamId.CADILLAC,
                points = 4.0,
                wins = 0,
                gapToP1 = "534 to P1",
                gapToPrev = "5 to P10",
                drivers = listOf(
                    ConstructorDriverPoints("Valtteri Bottas", 4.0),
                    ConstructorDriverPoints("Sergio Perez", 0.0)
                ),
                seasonWins = emptyList(),
                recentTeamForm = listOf(
                    TeamRaceFormColumn("Italian GP", "P12", "0pts", 12),
                    TeamRaceFormColumn("Spanish GP", "P12", "0pts", 12),
                    TeamRaceFormColumn("Azerbaijan GP", "P11", "0pts", 11)
                )
            )
        )
    }

    fun getHistoricalConstructorStandings(season: Int): List<ConstructorStanding> {
        return when (season) {
            2026 -> getConstructorStandings2026()
            else -> emptyList()
        }
    }

    // ========================================================================
    // 7. Driver Career Comparisons
    // Matches compare-drivers-1080.webp
    // ========================================================================
    fun getDriverComparisons(): List<DriverCareerComparison> {
        return listOf(
            DriverCareerComparison(
                driverId = "verstappen",
                code = "VER",
                firstName = "Max",
                lastName = "Verstappen",
                nationality = "Dutch",
                team = F1TeamId.RED_BULL,
                championships = 4,
                raceWins = 71,
                podiums = 134,
                polePositions = 48,
                fastestLaps = 37,
                careerPoints = 3607.5,
                raceStarts = 248,
                dnfs = 37,
                winRatePercent = 28.6,
                podiumRatePercent = 54.0,
                poleRatePercent = 19.4
            ),
            DriverCareerComparison(
                driverId = "norris",
                code = "NOR",
                firstName = "Lando",
                lastName = "Norris",
                nationality = "British",
                team = F1TeamId.MCLAREN,
                championships = 1,
                raceWins = 13,
                podiums = 49,
                polePositions = 19,
                fastestLaps = 20,
                careerPoints = 1616.0,
                raceStarts = 167,
                dnfs = 18,
                winRatePercent = 7.8,
                podiumRatePercent = 29.3,
                poleRatePercent = 11.4
            ),
            DriverCareerComparison(
                driverId = "hamilton",
                code = "HAM",
                firstName = "Lewis",
                lastName = "Hamilton",
                nationality = "British",
                team = F1TeamId.FERRARI,
                championships = 7,
                raceWins = 105,
                podiums = 202,
                polePositions = 104,
                fastestLaps = 67,
                careerPoints = 4850.5,
                raceStarts = 356,
                dnfs = 33,
                winRatePercent = 29.5,
                podiumRatePercent = 56.7,
                poleRatePercent = 29.2
            ),
            DriverCareerComparison(
                driverId = "leclerc",
                code = "LEC",
                firstName = "Charles",
                lastName = "Leclerc",
                nationality = "Monegasque",
                team = F1TeamId.FERRARI,
                championships = 0,
                raceWins = 8,
                podiums = 42,
                polePositions = 26,
                fastestLaps = 10,
                careerPoints = 1380.0,
                raceStarts = 146,
                dnfs = 22,
                winRatePercent = 5.5,
                podiumRatePercent = 28.8,
                poleRatePercent = 17.8
            ),
            DriverCareerComparison(
                driverId = "alonso",
                code = "ALO",
                firstName = "Fernando",
                lastName = "Alonso",
                nationality = "Spanish",
                team = F1TeamId.ASTON_MARTIN,
                championships = 2,
                raceWins = 32,
                podiums = 106,
                polePositions = 22,
                fastestLaps = 26,
                careerPoints = 2329.0,
                raceStarts = 401,
                dnfs = 77,
                winRatePercent = 8.0,
                podiumRatePercent = 26.4,
                poleRatePercent = 5.5
            ),
            DriverCareerComparison(
                driverId = "antonelli",
                code = "ANT",
                firstName = "Andrea Kimi",
                lastName = "Antonelli",
                nationality = "Italian",
                team = F1TeamId.MERCEDES,
                championships = 0,
                raceWins = 8,
                podiums = 12,
                polePositions = 6,
                fastestLaps = 5,
                careerPoints = 302.0,
                raceStarts = 18,
                dnfs = 1,
                winRatePercent = 44.4,
                podiumRatePercent = 66.7,
                poleRatePercent = 33.3
            ),
            DriverCareerComparison(
                driverId = "russell",
                code = "RUS",
                firstName = "George",
                lastName = "Russell",
                nationality = "British",
                team = F1TeamId.MERCEDES,
                championships = 0,
                raceWins = 5,
                podiums = 19,
                polePositions = 8,
                fastestLaps = 9,
                careerPoints = 735.0,
                raceStarts = 128,
                dnfs = 14,
                winRatePercent = 3.9,
                podiumRatePercent = 14.8,
                poleRatePercent = 6.3
            ),
            DriverCareerComparison(
                driverId = "senna",
                code = "SEN",
                firstName = "Ayrton",
                lastName = "Senna",
                nationality = "Brazilian",
                team = F1TeamId.MCLAREN,
                championships = 3,
                raceWins = 41,
                podiums = 80,
                polePositions = 65,
                fastestLaps = 19,
                careerPoints = 614.0,
                raceStarts = 161,
                dnfs = 54,
                winRatePercent = 25.5,
                podiumRatePercent = 49.7,
                poleRatePercent = 40.4
            ),
            DriverCareerComparison(
                driverId = "schumacher",
                code = "MSC",
                firstName = "Michael",
                lastName = "Schumacher",
                nationality = "German",
                team = F1TeamId.FERRARI,
                championships = 7,
                raceWins = 91,
                podiums = 155,
                polePositions = 68,
                fastestLaps = 77,
                careerPoints = 1566.0,
                raceStarts = 306,
                dnfs = 68,
                winRatePercent = 29.7,
                podiumRatePercent = 50.7,
                poleRatePercent = 22.2
            ),
            DriverCareerComparison(
                driverId = "prost",
                code = "PRO",
                firstName = "Alain",
                lastName = "Prost",
                nationality = "French",
                team = F1TeamId.MCLAREN,
                championships = 4,
                raceWins = 51,
                podiums = 106,
                polePositions = 33,
                fastestLaps = 41,
                careerPoints = 798.5,
                raceStarts = 199,
                dnfs = 54,
                winRatePercent = 25.6,
                podiumRatePercent = 53.3,
                poleRatePercent = 16.6
            ),
            DriverCareerComparison(
                driverId = "farina",
                code = "FAR",
                firstName = "Nino",
                lastName = "Farina",
                nationality = "Italian",
                team = F1TeamId.FERRARI,
                championships = 1,
                raceWins = 5,
                podiums = 20,
                polePositions = 5,
                fastestLaps = 5,
                careerPoints = 127.3,
                raceStarts = 33,
                dnfs = 10,
                winRatePercent = 15.2,
                podiumRatePercent = 60.6,
                poleRatePercent = 15.2
            )
        )
    }

    // ========================================================================
    // 8. Live Session State
    // Default: São Paulo Grand Prix (FP1)
    // Archive/Test: Bahrain Grand Prix (Qualifying Q1)
    // ========================================================================
    fun getLiveSessionState(sessionKey: String = "SAO_PAULO"): LiveSessionState {
        return if (sessionKey.equals("BAHRAIN", ignoreCase = true) || sessionKey.equals("Q1", ignoreCase = true)) {
            getBahrainLiveSessionState()
        } else {
            getSaoPauloLiveSessionState()
        }
    }

    fun getBahrainQ1LiveSessionState(): LiveSessionState = getBahrainLiveSessionState()

    fun getSaoPauloLiveSessionState(): LiveSessionState {
        val radio1 = TeamRadioClip(
            id = "r1",
            driverCode = "SLA",
            driverName = "Freddie Slater",
            team = F1TeamId.AUDI_SAUBER,
            timestamp = "15:43:57",
            transcript = "Front grip is coming in nicely through Sector 2, balance feels good.",
            durationSeconds = 6
        )
        val timingTower = listOf(
                LiveDriverTiming(
                    position = 1,
                    code = "VER",
                    fullName = "Max Verstappen",
                    team = F1TeamId.RED_BULL,
                    bestLapTime = "1:12.959",
                    lastLapTime = "1:12.959",
                    gapToLeader = "Leader",
                    intervalToAhead = "Leader",
                    isOverallBest = true,
                    isInPit = true,
                    isOutLap = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 6,
                    sector1Status = SectorStatus.PERSONAL_BEST,
                    sector2Status = SectorStatus.INACTIVE,
                    sector3Status = SectorStatus.INACTIVE,
                    sector1Time = "18.421",
                    sector2Time = "35.120",
                    sector3Time = "19.418",
                    trackProgress = 0.0f
                ),
                LiveDriverTiming(
                    position = 2,
                    code = "ANT",
                    fullName = "Andrea Kimi Antonelli",
                    team = F1TeamId.MERCEDES,
                    bestLapTime = "1:13.044",
                    lastLapTime = "1:13.044",
                    gapToLeader = "+0.085",
                    intervalToAhead = "+0.085",
                    isOverallBest = false,
                    isInPit = false,
                    isOutLap = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 8,
                    sector1Status = SectorStatus.OVERALL_BEST,
                    sector2Status = SectorStatus.PERSONAL_BEST,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "18.390",
                    sector2Time = "35.210",
                    sector3Time = "19.444",
                    trackProgress = 0.65f
                ),
                LiveDriverTiming(
                    position = 3,
                    code = "HAM",
                    fullName = "Lewis Hamilton",
                    team = F1TeamId.FERRARI,
                    bestLapTime = "1:13.298",
                    lastLapTime = "1:13.298",
                    gapToLeader = "+0.339",
                    intervalToAhead = "+0.254",
                    isOverallBest = false,
                    isInPit = false,
                    isOutLap = true,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 7,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "18.510",
                    sector2Time = "35.320",
                    sector3Time = "19.468",
                    trackProgress = 0.15f
                ),
                LiveDriverTiming(
                    position = 4,
                    code = "NOR",
                    fullName = "Lando Norris",
                    team = F1TeamId.MCLAREN,
                    bestLapTime = "1:13.420",
                    lastLapTime = "1:13.420",
                    gapToLeader = "+0.461",
                    intervalToAhead = "+0.122",
                    isOverallBest = false,
                    isInPit = false,
                    isOutLap = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 8,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "18.550",
                    sector2Time = "35.390",
                    sector3Time = "19.480",
                    trackProgress = 0.45f
                ),
                LiveDriverTiming(
                    position = 5,
                    code = "RUS",
                    fullName = "George Russell",
                    team = F1TeamId.MERCEDES,
                    bestLapTime = "1:13.617",
                    lastLapTime = "1:13.617",
                    gapToLeader = "+0.658",
                    intervalToAhead = "+0.197",
                    isOverallBest = false,
                    isInPit = false,
                    isOutLap = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 7,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "18.610",
                    sector2Time = "35.480",
                    sector3Time = "19.527",
                    trackProgress = 0.55f
                ),
                LiveDriverTiming(
                    position = 6,
                    code = "HAD",
                    fullName = "Isack Hadjar",
                    team = F1TeamId.RED_BULL,
                    bestLapTime = "1:13.750",
                    lastLapTime = "1:13.750",
                    gapToLeader = "+0.791",
                    intervalToAhead = "+0.133",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 5,
                    trackProgress = 0.72f
                ),
                LiveDriverTiming(
                    position = 7,
                    code = "BOR",
                    fullName = "Gabriel Bortoleto",
                    team = F1TeamId.AUDI_SAUBER,
                    bestLapTime = "1:13.880",
                    lastLapTime = "1:13.880",
                    gapToLeader = "+0.921",
                    intervalToAhead = "+0.130",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 4,
                    trackProgress = 0.81f
                ),
                LiveDriverTiming(
                    position = 8,
                    code = "BRO",
                    fullName = "Luke Browning",
                    team = F1TeamId.WILLIAMS,
                    bestLapTime = "1:13.990",
                    lastLapTime = "1:13.990",
                    gapToLeader = "+1.031",
                    intervalToAhead = "+0.110",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 6,
                    trackProgress = 0.38f
                ),
                LiveDriverTiming(
                    position = 9,
                    code = "HER",
                    fullName = "Colton Herta",
                    team = F1TeamId.CADILLAC,
                    bestLapTime = "1:14.050",
                    lastLapTime = "1:14.050",
                    gapToLeader = "+1.091",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 5,
                    trackProgress = 0.92f
                ),
                LiveDriverTiming(
                    position = 10,
                    code = "ALO",
                    fullName = "Fernando Alonso",
                    team = F1TeamId.ASTON_MARTIN,
                    bestLapTime = "1:14.120",
                    lastLapTime = "1:14.120",
                    gapToLeader = "+1.161",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 9,
                    trackProgress = 0.40f
                ),
                LiveDriverTiming(
                    position = 11,
                    code = "LIN",
                    fullName = "Arvid Lindblad",
                    team = F1TeamId.RACING_BULLS,
                    bestLapTime = "1:14.190",
                    lastLapTime = "1:14.190",
                    gapToLeader = "+1.231",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 5,
                    trackProgress = 0.84f
                ),
                LiveDriverTiming(
                    position = 12,
                    code = "OCO",
                    fullName = "Esteban Ocon",
                    team = F1TeamId.HAAS,
                    bestLapTime = "1:14.280",
                    lastLapTime = "1:14.280",
                    gapToLeader = "+1.321",
                    intervalToAhead = "+0.090",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 7,
                    trackProgress = 0.28f
                ),
                LiveDriverTiming(
                    position = 13,
                    code = "STR",
                    fullName = "Lance Stroll",
                    team = F1TeamId.ASTON_MARTIN,
                    bestLapTime = "1:14.340",
                    lastLapTime = "1:14.340",
                    gapToLeader = "+1.381",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 6,
                    trackProgress = 0.48f
                ),
                LiveDriverTiming(
                    position = 14,
                    code = "LAW",
                    fullName = "Liam Lawson",
                    team = F1TeamId.RACING_BULLS,
                    bestLapTime = "1:14.410",
                    lastLapTime = "1:14.410",
                    gapToLeader = "+1.451",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 8,
                    trackProgress = 0.60f
                ),
                LiveDriverTiming(
                    position = 15,
                    code = "COL",
                    fullName = "Franco Colapinto",
                    team = F1TeamId.ALPINE,
                    bestLapTime = "1:14.490",
                    lastLapTime = "1:14.490",
                    gapToLeader = "+1.531",
                    intervalToAhead = "+0.080",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 4,
                    trackProgress = 0.76f
                ),
                LiveDriverTiming(
                    position = 16,
                    code = "BEA",
                    fullName = "Oliver Bearman",
                    team = F1TeamId.HAAS,
                    bestLapTime = "1:14.560",
                    lastLapTime = "1:14.560",
                    gapToLeader = "+1.601",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 7,
                    trackProgress = 0.32f
                ),
                LiveDriverTiming(
                    position = 17,
                    code = "ALB",
                    fullName = "Alex Albon",
                    team = F1TeamId.WILLIAMS,
                    bestLapTime = "1:14.620",
                    lastLapTime = "1:14.620",
                    gapToLeader = "+1.661",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 8,
                    trackProgress = 0.35f
                ),
                LiveDriverTiming(
                    position = 18,
                    code = "CAM",
                    fullName = "Rafael Camara",
                    team = F1TeamId.CADILLAC,
                    bestLapTime = "1:14.710",
                    lastLapTime = "1:14.710",
                    gapToLeader = "+1.751",
                    intervalToAhead = "+0.090",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 5,
                    trackProgress = 0.95f
                ),
                LiveDriverTiming(
                    position = 19,
                    code = "LEC",
                    fullName = "Charles Leclerc",
                    team = F1TeamId.FERRARI,
                    bestLapTime = "1:14.790",
                    lastLapTime = "1:14.790",
                    gapToLeader = "+1.831",
                    intervalToAhead = "+0.080",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 6,
                    trackProgress = 0.22f
                ),
                LiveDriverTiming(
                    position = 20,
                    code = "PIA",
                    fullName = "Oscar Piastri",
                    team = F1TeamId.MCLAREN,
                    bestLapTime = "1:14.850",
                    lastLapTime = "1:14.850",
                    gapToLeader = "+1.891",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 9,
                    trackProgress = 0.50f
                ),
                LiveDriverTiming(
                    position = 21,
                    code = "GAS",
                    fullName = "Pierre Gasly",
                    team = F1TeamId.ALPINE,
                    bestLapTime = "1:14.920",
                    lastLapTime = "1:14.920",
                    gapToLeader = "+1.961",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 7,
                    trackProgress = 0.70f
                ),
                LiveDriverTiming(
                    position = 22,
                    code = "SAI",
                    fullName = "Carlos Sainz",
                    team = F1TeamId.WILLIAMS,
                    bestLapTime = "1:15.010",
                    lastLapTime = "1:15.010",
                    gapToLeader = "+2.051",
                    intervalToAhead = "+0.090",
                    isOverallBest = false,
                    isInPit = true,
                    isOutLap = false,
                    tyreCompound = TyreCompound.HARD,
                    tyreAgeLaps = 8,
                    trackProgress = 0.0f
                )
        )
        return LiveSessionState(
            grandPrixName = "São Paulo Grand Prix",
            sessionName = "FP1",
            shortSessionCode = "FP1",
            timeRemaining = "38:29",
            bestLapDriverCode = "VER",
            bestLapTime = "1:12.959",
            carsOnTrack = timingTower.count { !it.isInPit },
            trackStatusText = "Track Clear",
            sector1LengthKm = "T1-T3  1.2 km",
            sector2LengthKm = "T4-T11  1.9 km",
            sector3LengthKm = "T12-T15  1.1 km",
            circuitId = "INTERLAGOS",
            drivers = timingTower,
            latestRadio = radio1,
            radioClips = listOf(
                radio1,
                TeamRadioClip(
                    id = "r2",
                    driverCode = "VER",
                    driverName = "Max Verstappen",
                    team = F1TeamId.RED_BULL,
                    timestamp = "15:41:20",
                    transcript = "Box this lap, balance is fine for first run.",
                    durationSeconds = 4
                ),
                TeamRadioClip(
                    id = "r3",
                    driverCode = "HAM",
                    driverName = "Lewis Hamilton",
                    team = F1TeamId.FERRARI,
                    timestamp = "15:39:05",
                    transcript = "Starting our push lap now.",
                    durationSeconds = 5
                )
            ),
            raceControlMessages = listOf(
                RaceControlMessage("rc_1", "SESSION STARTED", "15:30:00", "INFO"),
                RaceControlMessage("rc_2", "GREEN FLAG", "15:30:00", "FLAG"),
                RaceControlMessage("rc_3", "TRACK CLEAR", "15:32:10", "INFO")
            ),
            overtakes = emptyList(),
            weather = WeatherSnapshot(
                trackTempC = 41.2,
                airTempC = 26.5,
                humidityPercent = 62,
                windSpeedKmh = 14.8,
                windDirection = "SSE",
                rainfallPercent = 0,
                pressureMbar = 1018.0
            )
        )
    }

    fun getBahrainLiveSessionState(): LiveSessionState {
        return LiveSessionState(
            grandPrixName = "Bahrain Grand Prix",
            sessionName = "Qualifying · Q1",
            shortSessionCode = "Q1",
            timeRemaining = "8:21",
            bestLapDriverCode = "VER",
            bestLapTime = "1:36.477",
            carsOnTrack = 21,
            trackStatusText = "Track Clear",
            sector1LengthKm = "1.4 km",
            sector2LengthKm = "1.8 km",
            sector3LengthKm = "2.3 km",
            circuitId = "BAHRAIN",
            drivers = listOf(
                LiveDriverTiming(
                    position = 1,
                    code = "VER",
                    fullName = "Max Verstappen",
                    team = F1TeamId.RED_BULL,
                    bestLapTime = "1:36.477",
                    lastLapTime = "1:36.477",
                    gapToLeader = "Leader",
                    intervalToAhead = "Leader",
                    isOverallBest = true,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.OVERALL_BEST,
                    sector2Status = SectorStatus.OVERALL_BEST,
                    sector3Status = SectorStatus.OVERALL_BEST,
                    sector1Time = "31.210",
                    sector2Time = "43.720",
                    sector3Time = "21.547",
                    trackProgress = 0.58f
                ),
                LiveDriverTiming(
                    position = 2,
                    code = "HAM",
                    fullName = "Lewis Hamilton",
                    team = F1TeamId.FERRARI,
                    bestLapTime = "1:36.595",
                    lastLapTime = "1:36.595",
                    gapToLeader = "+0.118",
                    intervalToAhead = "+0.118",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.PERSONAL_BEST,
                    sector2Status = SectorStatus.PERSONAL_BEST,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.453",
                    sector2Time = "43.939",
                    sector3Time = "21.603",
                    trackProgress = 0.54f
                ),
                LiveDriverTiming(
                    position = 3,
                    code = "LEC",
                    fullName = "Charles Leclerc",
                    team = F1TeamId.FERRARI,
                    bestLapTime = "1:36.730",
                    lastLapTime = "1:36.730",
                    gapToLeader = "+0.135",
                    intervalToAhead = "+0.017",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.PERSONAL_BEST,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.520",
                    sector2Time = "44.010",
                    sector3Time = "21.600",
                    trackProgress = 0.48f
                ),
                LiveDriverTiming(
                    position = 4,
                    code = "HAD",
                    fullName = "Isack Hadjar",
                    team = F1TeamId.RED_BULL,
                    bestLapTime = "1:36.898",
                    lastLapTime = "1:36.898",
                    gapToLeader = "+0.421",
                    intervalToAhead = "+0.286",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 3,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.602",
                    sector2Time = "44.110",
                    sector3Time = "21.686",
                    trackProgress = 0.62f
                ),
                LiveDriverTiming(
                    position = 5,
                    code = "ANT",
                    fullName = "Andrea Kimi Antonelli",
                    team = F1TeamId.MERCEDES,
                    bestLapTime = "1:37.041",
                    lastLapTime = "1:37.041",
                    gapToLeader = "+0.564",
                    intervalToAhead = "+0.143",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 4,
                    sector1Status = SectorStatus.PERSONAL_BEST,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.580",
                    sector2Time = "44.200",
                    sector3Time = "21.761",
                    trackProgress = 0.82f
                ),
                LiveDriverTiming(
                    position = 6,
                    code = "NOR",
                    fullName = "Lando Norris",
                    team = F1TeamId.MCLAREN,
                    bestLapTime = "1:37.112",
                    lastLapTime = "1:37.112",
                    gapToLeader = "+0.635",
                    intervalToAhead = "+0.071",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.650",
                    sector2Time = "44.250",
                    sector3Time = "21.712",
                    trackProgress = 0.05f
                ),
                LiveDriverTiming(
                    position = 7,
                    code = "PIA",
                    fullName = "Oscar Piastri",
                    team = F1TeamId.MCLAREN,
                    bestLapTime = "1:37.240",
                    lastLapTime = "1:37.240",
                    gapToLeader = "+0.763",
                    intervalToAhead = "+0.128",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 3,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.710",
                    sector2Time = "44.310",
                    sector3Time = "21.720",
                    trackProgress = 0.65f
                ),
                LiveDriverTiming(
                    position = 8,
                    code = "RUS",
                    fullName = "George Russell",
                    team = F1TeamId.MERCEDES,
                    bestLapTime = "1:37.310",
                    lastLapTime = "1:37.310",
                    gapToLeader = "+0.833",
                    intervalToAhead = "+0.070",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 3,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.750",
                    sector2Time = "44.340",
                    sector3Time = "21.720",
                    trackProgress = 0.60f
                ),
                LiveDriverTiming(
                    position = 9,
                    code = "ALO",
                    fullName = "Fernando Alonso",
                    team = F1TeamId.ASTON_MARTIN,
                    bestLapTime = "1:37.450",
                    lastLapTime = "1:37.450",
                    gapToLeader = "+0.973",
                    intervalToAhead = "+0.140",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.810",
                    sector2Time = "44.400",
                    sector3Time = "21.740",
                    trackProgress = 0.42f
                ),
                LiveDriverTiming(
                    position = 10,
                    code = "GAS",
                    fullName = "Pierre Gasly",
                    team = F1TeamId.ALPINE,
                    bestLapTime = "1:37.560",
                    lastLapTime = "1:37.560",
                    gapToLeader = "+1.083",
                    intervalToAhead = "+0.110",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 3,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    sector1Time = "31.860",
                    sector2Time = "44.450",
                    sector3Time = "21.750",
                    trackProgress = 0.50f
                ),
                LiveDriverTiming(
                    position = 11,
                    code = "OCO",
                    fullName = "Esteban Ocon",
                    team = F1TeamId.HAAS,
                    bestLapTime = "1:37.620",
                    lastLapTime = "1:37.620",
                    gapToLeader = "+1.143",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    sector1Status = SectorStatus.NORMAL,
                    sector2Status = SectorStatus.NORMAL,
                    sector3Status = SectorStatus.NORMAL,
                    trackProgress = 0.52f
                ),
                LiveDriverTiming(
                    position = 12,
                    code = "ALB",
                    fullName = "Alex Albon",
                    team = F1TeamId.WILLIAMS,
                    bestLapTime = "1:37.710",
                    lastLapTime = "1:37.710",
                    gapToLeader = "+1.233",
                    intervalToAhead = "+0.090",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 4,
                    trackProgress = 0.72f
                ),
                LiveDriverTiming(
                    position = 13,
                    code = "SAI",
                    fullName = "Carlos Sainz",
                    team = F1TeamId.WILLIAMS,
                    bestLapTime = "1:37.790",
                    lastLapTime = "1:37.790",
                    gapToLeader = "+1.313",
                    intervalToAhead = "+0.080",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    trackProgress = 0.74f
                ),
                LiveDriverTiming(
                    position = 14,
                    code = "STR",
                    fullName = "Lance Stroll",
                    team = F1TeamId.ASTON_MARTIN,
                    bestLapTime = "1:37.850",
                    lastLapTime = "1:37.850",
                    gapToLeader = "+1.373",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 3,
                    trackProgress = 0.38f
                ),
                LiveDriverTiming(
                    position = 15,
                    code = "LAW",
                    fullName = "Liam Lawson",
                    team = F1TeamId.RACING_BULLS,
                    bestLapTime = "1:37.910",
                    lastLapTime = "1:37.910",
                    gapToLeader = "+1.433",
                    intervalToAhead = "+0.060",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    trackProgress = 0.68f
                ),
                LiveDriverTiming(
                    position = 16,
                    code = "COL",
                    fullName = "Franco Colapinto",
                    team = F1TeamId.ALPINE,
                    bestLapTime = "1:38.020",
                    lastLapTime = "1:38.020",
                    gapToLeader = "+1.543",
                    intervalToAhead = "+0.110",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 3,
                    trackProgress = 0.69f
                ),
                LiveDriverTiming(
                    position = 17,
                    code = "BEA",
                    fullName = "Oliver Bearman",
                    team = F1TeamId.HAAS,
                    bestLapTime = "1:38.150",
                    lastLapTime = "1:38.150",
                    gapToLeader = "+1.673",
                    intervalToAhead = "+0.130",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    trackProgress = 0.76f
                ),
                LiveDriverTiming(
                    position = 18,
                    code = "HUL",
                    fullName = "Nico Hülkenberg",
                    team = F1TeamId.AUDI_SAUBER,
                    bestLapTime = "1:38.280",
                    lastLapTime = "1:38.280",
                    gapToLeader = "+1.803",
                    intervalToAhead = "+0.130",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 3,
                    trackProgress = 0.88f
                ),
                LiveDriverTiming(
                    position = 19,
                    code = "BOT",
                    fullName = "Valtteri Bottas",
                    team = F1TeamId.CADILLAC,
                    bestLapTime = "1:38.410",
                    lastLapTime = "1:38.410",
                    gapToLeader = "+1.933",
                    intervalToAhead = "+0.130",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    trackProgress = 0.94f
                ),
                LiveDriverTiming(
                    position = 20,
                    code = "LIN",
                    fullName = "Arvid Lindblad",
                    team = F1TeamId.RACING_BULLS,
                    bestLapTime = "1:38.560",
                    lastLapTime = "1:38.560",
                    gapToLeader = "+2.083",
                    intervalToAhead = "+0.150",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 2,
                    trackProgress = 0.86f
                ),
                LiveDriverTiming(
                    position = 21,
                    code = "BOR",
                    fullName = "Gabriel Bortoleto",
                    team = F1TeamId.AUDI_SAUBER,
                    bestLapTime = "1:38.720",
                    lastLapTime = "1:38.720",
                    gapToLeader = "+2.243",
                    intervalToAhead = "+0.160",
                    isOverallBest = false,
                    isInPit = false,
                    tyreCompound = TyreCompound.SOFT,
                    tyreAgeLaps = 3,
                    trackProgress = 0.90f
                ),
                LiveDriverTiming(
                    position = 22,
                    code = "PER",
                    fullName = "Sergio Perez",
                    team = F1TeamId.CADILLAC,
                    bestLapTime = "1:38.890",
                    lastLapTime = "1:38.890",
                    gapToLeader = "+2.413",
                    intervalToAhead = "+0.170",
                    isOverallBest = false,
                    isInPit = true,
                    tyreCompound = TyreCompound.MEDIUM,
                    tyreAgeLaps = 2,
                    trackProgress = 0.0f
                )
            ),
            latestRadio = TeamRadioClip(
                id = "rad_alo_1",
                driverName = "Fernando Alonso",
                driverCode = "ALO",
                team = F1TeamId.ASTON_MARTIN,
                timestamp = "08:55:53",
                transcript = "Check the tyre temps, feeling good in sector 2. Box this lap.",
                durationSeconds = 8
            ),
            radioClips = listOf(
                TeamRadioClip(
                    id = "rad_alo_1",
                    driverName = "Fernando Alonso",
                    driverCode = "ALO",
                    team = F1TeamId.ASTON_MARTIN,
                    timestamp = "08:55:53",
                    transcript = "Check the tyre temps, feeling good in sector 2. Box this lap.",
                    durationSeconds = 8
                ),
                TeamRadioClip(
                    id = "rad_ham_1",
                    driverName = "Lewis Hamilton",
                    driverCode = "HAM",
                    team = F1TeamId.FERRARI,
                    timestamp = "08:58:12",
                    transcript = "Balance is much better than FP3. We are fast on the Mediums.",
                    durationSeconds = 6
                ),
                TeamRadioClip(
                    id = "rad_ver_1",
                    driverName = "Max Verstappen",
                    driverCode = "VER",
                    team = F1TeamId.RED_BULL,
                    timestamp = "09:02:45",
                    transcript = "Mate, car is on rails through turn 5 and 6.",
                    durationSeconds = 5
                )
            ),
            raceControlMessages = listOf(
                RaceControlMessage("rc_1", "SESSION STARTED", "09:00:00", "INFO"),
                RaceControlMessage("rc_2", "GREEN FLAG", "09:00:00", "FLAG"),
                RaceControlMessage("rc_3", "TRACK CLEAR", "09:01:20", "INFO")
            ),
            overtakes = listOf(
                OvertakeEvent(
                    lap = 3,
                    overtakingDriver = "HAM",
                    overtakingTeam = F1TeamId.FERRARI,
                    overtakenDriver = "LEC",
                    overtakenTeam = F1TeamId.FERRARI,
                    positionGained = 2,
                    cornerText = "Turn 1"
                ),
                OvertakeEvent(
                    lap = 4,
                    overtakingDriver = "VER",
                    overtakingTeam = F1TeamId.RED_BULL,
                    overtakenDriver = "HAD",
                    overtakenTeam = F1TeamId.RED_BULL,
                    positionGained = 1,
                    cornerText = "Turn 15"
                )
            ),
            weather = WeatherSnapshot(
                trackTempC = 38.5,
                airTempC = 28.2,
                humidityPercent = 54,
                windSpeedKmh = 12.4,
                windDirection = "NNW",
                rainfallPercent = 0,
                pressureMbar = 1014.2
            )
        )
    }

    // ========================================================================
    // 9. Full Season Schedule (1950–2026 Archive)
    // ========================================================================
    fun getSeasonSchedule(season: Int): List<RaceWeekend> {
        return when (season) {
            2026 -> get2026FullSchedule()
            2025 -> get2025Schedule()
            2024 -> get2024Schedule()
            2023 -> get2023Schedule()
            1950 -> get1950Schedule()
            else -> generateArchiveSchedule(season)
        }
    }

    private fun get2026FullSchedule(): List<RaceWeekend> {
        return listOf(
            RaceWeekend(
                season = 2026,
                round = 1,
                grandPrixName = "Australian Grand Prix",
                circuitName = "Albert Park Circuit",
                location = "Melbourne, Australia",
                countryFlagEmoji = "🇦🇺",
                dateRangeText = "FRI 13 MAR - SUN 15 MAR",
                daysToGo = 0,
                circuitLengthKm = "5.278",
                totalLaps = 58,
                totalCorners = 14,
                circuitType = "ALBERT_PARK",
                circuitId = "MELBOURNE",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "15", "Mar", "06:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 2,
                grandPrixName = "Chinese Grand Prix",
                circuitName = "Shanghai International Circuit",
                location = "Shanghai, China",
                countryFlagEmoji = "🇨🇳",
                dateRangeText = "FRI 27 MAR - SUN 29 MAR",
                daysToGo = 0,
                circuitLengthKm = "5.451",
                totalLaps = 56,
                totalCorners = 16,
                circuitType = "SHANGHAI",
                circuitId = "SHANGHAI",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "29", "Mar", "08:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 3,
                grandPrixName = "Japanese Grand Prix",
                circuitName = "Suzuka International Racing Course",
                location = "Suzuka, Japan",
                countryFlagEmoji = "🇯🇵",
                dateRangeText = "FRI 10 APR - SUN 12 APR",
                daysToGo = 0,
                circuitLengthKm = "5.807",
                totalLaps = 53,
                totalCorners = 18,
                circuitType = "SUZUKA",
                circuitId = "SUZUKA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "12", "Apr", "06:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 4,
                grandPrixName = "Bahrain Grand Prix",
                circuitName = "Bahrain International Circuit",
                location = "Sakhir, Bahrain",
                countryFlagEmoji = "🇧🇭",
                dateRangeText = "FRI 24 APR - SUN 26 APR",
                daysToGo = 0,
                circuitLengthKm = "5.412",
                totalLaps = 57,
                totalCorners = 15,
                circuitType = "BAHRAIN",
                circuitId = "BAHRAIN",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "26", "Apr", "16:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 5,
                grandPrixName = "Saudi Arabian Grand Prix",
                circuitName = "Jeddah Corniche Circuit",
                location = "Jeddah, Saudi Arabia",
                countryFlagEmoji = "🇸🇦",
                dateRangeText = "FRI 01 MAY - SUN 03 MAY",
                daysToGo = 0,
                circuitLengthKm = "6.174",
                totalLaps = 50,
                totalCorners = 27,
                circuitType = "JEDDAH",
                circuitId = "JEDDAH",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "03", "May", "18:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 6,
                grandPrixName = "Miami Grand Prix",
                circuitName = "Miami International Autodrome",
                location = "Miami, USA",
                countryFlagEmoji = "🇺🇸",
                dateRangeText = "FRI 08 MAY - SUN 10 MAY",
                daysToGo = 0,
                circuitLengthKm = "5.412",
                totalLaps = 57,
                totalCorners = 19,
                circuitType = "MIAMI",
                circuitId = "MIAMI",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "10", "May", "20:30", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 7,
                grandPrixName = "Emilia Romagna Grand Prix",
                circuitName = "Autodromo Enzo e Dino Ferrari",
                location = "Imola, Italy",
                countryFlagEmoji = "🇮🇹",
                dateRangeText = "FRI 15 MAY - SUN 17 MAY",
                daysToGo = 0,
                circuitLengthKm = "4.909",
                totalLaps = 63,
                totalCorners = 19,
                circuitType = "IMOLA",
                circuitId = "IMOLA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "17", "May", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 8,
                grandPrixName = "Monaco Grand Prix",
                circuitName = "Circuit de Monaco",
                location = "Monte Carlo, Monaco",
                countryFlagEmoji = "🇲🇨",
                dateRangeText = "FRI 22 MAY - SUN 24 MAY",
                daysToGo = 0,
                circuitLengthKm = "3.337",
                totalLaps = 78,
                totalCorners = 19,
                circuitType = "MONACO",
                circuitId = "MONACO",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "24", "May", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 9,
                grandPrixName = "Spanish Grand Prix",
                circuitName = "Circuit de Barcelona-Catalunya",
                location = "Barcelona, Spain",
                countryFlagEmoji = "🇪🇸",
                dateRangeText = "FRI 05 JUN - SUN 07 JUN",
                daysToGo = 0,
                circuitLengthKm = "4.657",
                totalLaps = 66,
                totalCorners = 14,
                circuitType = "BARCELONA",
                circuitId = "CATALUNYA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "07", "Jun", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 10,
                grandPrixName = "Canadian Grand Prix",
                circuitName = "Circuit Gilles Villeneuve",
                location = "Montreal, Canada",
                countryFlagEmoji = "🇨🇦",
                dateRangeText = "FRI 19 JUN - SUN 21 JUN",
                daysToGo = 0,
                circuitLengthKm = "4.361",
                totalLaps = 70,
                totalCorners = 14,
                circuitType = "MONTREAL",
                circuitId = "MONTREAL",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "21", "Jun", "19:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 11,
                grandPrixName = "Austrian Grand Prix",
                circuitName = "Red Bull Ring",
                location = "Spielberg, Austria",
                countryFlagEmoji = "🇦🇹",
                dateRangeText = "FRI 03 JUL - SUN 05 JUL",
                daysToGo = 0,
                circuitLengthKm = "4.318",
                totalLaps = 71,
                totalCorners = 10,
                circuitType = "RED_BULL_RING",
                circuitId = "SPIELBERG",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "05", "Jul", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 12,
                grandPrixName = "British Grand Prix",
                circuitName = "Silverstone Circuit",
                location = "Silverstone, UK",
                countryFlagEmoji = "🇬🇧",
                dateRangeText = "FRI 17 JUL - SUN 19 JUL",
                daysToGo = 0,
                circuitLengthKm = "5.891",
                totalLaps = 52,
                totalCorners = 18,
                circuitType = "SILVERSTONE",
                circuitId = "SILVERSTONE",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "19", "Jul", "15:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 13,
                grandPrixName = "Belgian Grand Prix",
                circuitName = "Circuit de Spa-Francorchamps",
                location = "Spa, Belgium",
                countryFlagEmoji = "🇧🇪",
                dateRangeText = "FRI 31 JUL - SUN 02 AUG",
                daysToGo = 0,
                circuitLengthKm = "7.004",
                totalLaps = 44,
                totalCorners = 19,
                circuitType = "SPA",
                circuitId = "SPA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "02", "Aug", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 14,
                grandPrixName = "Hungarian Grand Prix",
                circuitName = "Hungaroring",
                location = "Budapest, Hungary",
                countryFlagEmoji = "🇭🇺",
                dateRangeText = "FRI 14 AUG - SUN 16 AUG",
                daysToGo = 0,
                circuitLengthKm = "4.381",
                totalLaps = 70,
                totalCorners = 14,
                circuitType = "HUNGARORING",
                circuitId = "HUNGARORING",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "16", "Aug", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 15,
                grandPrixName = "Dutch Grand Prix",
                circuitName = "Circuit Zandvoort",
                location = "Zandvoort, Netherlands",
                countryFlagEmoji = "🇳🇱",
                dateRangeText = "FRI 28 AUG - SUN 30 AUG",
                daysToGo = 0,
                circuitLengthKm = "4.259",
                totalLaps = 72,
                totalCorners = 14,
                circuitType = "ZANDVOORT",
                circuitId = "ZANDVOORT",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "30", "Aug", "14:00", "Final", isRace = true)
                )
            ),
            getNextRaceWeekend(), // Round 16: Sepang
            RaceWeekend(
                season = 2026,
                round = 17,
                grandPrixName = "Italian Grand Prix",
                circuitName = "Autodromo Nazionale Monza",
                location = "Monza, Italy",
                countryFlagEmoji = "🇮🇹",
                dateRangeText = "FRI 04 SEP - SUN 06 SEP",
                daysToGo = 0,
                circuitLengthKm = "5.793",
                totalLaps = 53,
                totalCorners = 11,
                circuitType = "MONZA",
                circuitId = "MONZA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "06", "Sep", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 18,
                grandPrixName = "Azerbaijan Grand Prix",
                circuitName = "Baku City Circuit",
                location = "Baku, Azerbaijan",
                countryFlagEmoji = "🇦🇿",
                dateRangeText = "FRI 18 SEP - SUN 20 SEP",
                daysToGo = 0,
                circuitLengthKm = "6.003",
                totalLaps = 51,
                totalCorners = 20,
                circuitType = "BAKU",
                circuitId = "BAKU",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "20", "Sep", "12:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 19,
                grandPrixName = "Singapore Grand Prix",
                circuitName = "Marina Bay Street Circuit",
                location = "Marina Bay, Singapore",
                countryFlagEmoji = "🇸🇬",
                dateRangeText = "FRI 02 OCT - SUN 04 OCT",
                daysToGo = 0,
                circuitLengthKm = "4.940",
                totalLaps = 62,
                totalCorners = 19,
                circuitType = "SINGAPORE",
                circuitId = "SINGAPORE",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "04", "Oct", "13:00", "Final", isRace = true)
                )
            ),
            getBrazilianGpWeekend2026(), // Round 20: São Paulo
            RaceWeekend(
                season = 2026,
                round = 21,
                grandPrixName = "United States Grand Prix",
                circuitName = "Circuit of the Americas",
                location = "Austin, USA",
                countryFlagEmoji = "🇺🇸",
                dateRangeText = "FRI 23 OCT - SUN 25 OCT",
                daysToGo = 0,
                circuitLengthKm = "5.513",
                totalLaps = 56,
                totalCorners = 20,
                circuitType = "COTA",
                circuitId = "COTA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "25", "Oct", "19:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 22,
                grandPrixName = "Mexico City Grand Prix",
                circuitName = "Autódromo Hermanos Rodríguez",
                location = "Mexico City, Mexico",
                countryFlagEmoji = "🇲🇽",
                dateRangeText = "FRI 30 OCT - SUN 01 NOV",
                daysToGo = 0,
                circuitLengthKm = "4.304",
                totalLaps = 71,
                totalCorners = 17,
                circuitType = "MEXICO",
                circuitId = "MEXICO",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "01", "Nov", "20:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 23,
                grandPrixName = "Las Vegas Grand Prix",
                circuitName = "Las Vegas Strip Circuit",
                location = "Las Vegas, USA",
                countryFlagEmoji = "🇺🇸",
                dateRangeText = "THU 19 NOV - SAT 21 NOV",
                daysToGo = 49,
                circuitLengthKm = "6.201",
                totalLaps = 50,
                totalCorners = 17,
                circuitType = "LAS_VEGAS",
                circuitId = "LAS_VEGAS",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sat", "Saturday", "21", "Nov", "22:00", "In 49 days", isRace = true)
                )
            ),
            RaceWeekend(
                season = 2026,
                round = 24,
                grandPrixName = "Abu Dhabi Grand Prix",
                circuitName = "Yas Marina Circuit",
                location = "Abu Dhabi, UAE",
                countryFlagEmoji = "🇦🇪",
                dateRangeText = "FRI 27 NOV - SUN 29 NOV",
                daysToGo = 57,
                circuitLengthKm = "5.281",
                totalLaps = 58,
                totalCorners = 16,
                circuitType = "YAS_MARINA",
                circuitId = "YAS_MARINA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "29", "Nov", "16:00", "In 57 days", isRace = true)
                )
            )
        )
    }

    private fun get2025Schedule(): List<RaceWeekend> {
        return listOf(
            RaceWeekend(
                season = 2025,
                round = 1,
                grandPrixName = "Australian Grand Prix",
                circuitName = "Albert Park Circuit",
                location = "Melbourne, Australia",
                countryFlagEmoji = "🇦🇺",
                dateRangeText = "FRI 14 MAR - SUN 16 MAR",
                daysToGo = 0,
                circuitLengthKm = "5.278",
                totalLaps = 58,
                totalCorners = 14,
                sessions = emptyList()
            )
        )
    }

    private fun get2024Schedule(): List<RaceWeekend> {
        return listOf(
            getSaoPaulo2024Results(),
            RaceWeekend(
                season = 2024,
                round = 24,
                grandPrixName = "Abu Dhabi Grand Prix",
                circuitName = "Yas Marina Circuit",
                location = "Abu Dhabi, UAE",
                countryFlagEmoji = "🇦🇪",
                dateRangeText = "FRI 06 DEC - SUN 08 DEC",
                daysToGo = 0,
                circuitLengthKm = "5.281",
                totalLaps = 58,
                totalCorners = 16,
                sessions = emptyList()
            )
        )
    }

    private fun get2023Schedule(): List<RaceWeekend> {
        return listOf(
            getAbuDhabi2023Results()
        )
    }

    private fun get1950Schedule(): List<RaceWeekend> {
        return listOf(
            RaceWeekend(
                season = 1950,
                round = 1,
                grandPrixName = "British Grand Prix",
                circuitName = "Silverstone Circuit",
                location = "Silverstone, UK",
                countryFlagEmoji = "🇬🇧",
                dateRangeText = "SAT 13 MAY 1950",
                daysToGo = 0,
                circuitLengthKm = "4.649",
                totalLaps = 70,
                totalCorners = 8,
                circuitType = "SILVERSTONE",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sat", "Saturday", "13", "May", "14:30", "Final", isRace = true)
                ),
                results = listOf(
                    RaceDriverResult(
                        position = 1,
                        driverCode = "FAR",
                        driverFullName = "Nino Farina",
                        team = F1TeamId.FERRARI,
                        finishTimeOrGap = "2:13:23.6",
                        points = 9,
                        qualifyingPosition = "P1",
                        qualifyingTime = "1:50.8",
                        fastestLapPosition = "P1",
                        fastestLapTime = "1:50.6",
                        totalLaps = 70
                    )
                )
            ),
            RaceWeekend(
                season = 1950,
                round = 2,
                grandPrixName = "Monaco Grand Prix",
                circuitName = "Circuit de Monaco",
                location = "Monte Carlo, Monaco",
                countryFlagEmoji = "🇲🇨",
                dateRangeText = "SUN 21 MAY 1950",
                daysToGo = 0,
                circuitLengthKm = "3.180",
                totalLaps = 100,
                totalCorners = 14,
                circuitType = "MONACO",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "21", "May", "14:30", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = 1950,
                round = 7,
                grandPrixName = "Italian Grand Prix",
                circuitName = "Autodromo Nazionale Monza",
                location = "Monza, Italy",
                countryFlagEmoji = "🇮🇹",
                dateRangeText = "SUN 03 SEP 1950",
                daysToGo = 0,
                circuitLengthKm = "6.300",
                totalLaps = 80,
                totalCorners = 8,
                circuitType = "MONZA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "03", "Sep", "14:30", "Final", isRace = true)
                )
            )
        )
    }

    private fun generateArchiveSchedule(season: Int): List<RaceWeekend> {
        return listOf(
            RaceWeekend(
                season = season,
                round = 1,
                grandPrixName = "Monaco Grand Prix",
                circuitName = "Circuit de Monaco",
                location = "Monte Carlo, Monaco",
                countryFlagEmoji = "🇲🇨",
                dateRangeText = "MAY $season",
                daysToGo = 0,
                circuitLengthKm = "3.337",
                totalLaps = 78,
                totalCorners = 19,
                circuitType = "MONACO",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "25", "May", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = season,
                round = 2,
                grandPrixName = "British Grand Prix",
                circuitName = "Silverstone Circuit",
                location = "Silverstone, UK",
                countryFlagEmoji = "🇬🇧",
                dateRangeText = "JUL $season",
                daysToGo = 0,
                circuitLengthKm = "5.891",
                totalLaps = 52,
                totalCorners = 18,
                circuitType = "SILVERSTONE",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "15", "Jul", "14:00", "Final", isRace = true)
                )
            ),
            RaceWeekend(
                season = season,
                round = 3,
                grandPrixName = "Italian Grand Prix",
                circuitName = "Autodromo Nazionale Monza",
                location = "Monza, Italy",
                countryFlagEmoji = "🇮🇹",
                dateRangeText = "SEP $season",
                daysToGo = 0,
                circuitLengthKm = "5.793",
                totalLaps = 53,
                totalCorners = 11,
                circuitType = "MONZA",
                sessions = listOf(
                    SessionScheduleItem("Race", "Race", "Sun", "Sunday", "08", "Sep", "14:00", "Final", isRace = true)
                )
            )
        )
    }
}
