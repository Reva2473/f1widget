package com.reva2473.f1widget.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class F1TeamId(
    val displayName: String,
    val constructorId: String,
    val colorHex: Long,
    val secondaryHex: Long,
    val shortCode: String
) {
    MERCEDES("Mercedes", "mercedes", 0xFF00D2BE, 0xFF0A2E2B, "MER"),
    FERRARI("Ferrari", "ferrari", 0xFFE8002D, 0xFF330A12, "FER"),
    MCLAREN("McLaren", "mclaren", 0xFFFF8000, 0xFF331D08, "MCL"),
    RED_BULL("Red Bull", "red_bull", 0xFF3671C6, 0xFF0E1D33, "RBR"),
    ASTON_MARTIN("Aston Martin", "aston_martin", 0xFF229971, 0xFF0A261D, "AMR"),
    ALPINE("Alpine", "alpine", 0xFFFF87BC, 0xFF331424, "ALP"),
    WILLIAMS("Williams", "williams", 0xFF64C4FF, 0xFF102433, "WIL"),
    RACING_BULLS("Racing Bulls", "rb", 0xFF6692FF, 0xFF121C33, "VRB"),
    HAAS("Haas", "haas", 0xFFB6BABD, 0xFF222426, "HAA"),
    AUDI_SAUBER("Kick Sauber", "sauber", 0xFF52E252, 0xFF102B10, "SAU"),
    CADILLAC("Cadillac", "cadillac", 0xFFD4AF37, 0xFF2B240D, "CAD"),
    OTHER("Other", "other", 0xFF8E8E99, 0xFF24242A, "F1");

    companion object {
        fun fromName(name: String): F1TeamId {
            val lower = name.lowercase().trim()
            return when {
                lower.contains("red bull") || lower.contains("red_bull") || lower == "rbr" -> RED_BULL
                lower.contains("racing bulls") || lower.contains("vcarb") || lower.contains("alphatauri") ||
                    lower.contains("alpha tauri") || lower.contains("toro rosso") || lower.contains("toro_rosso") ||
                    lower == "rb" || lower.contains("rb f1") || lower.contains("racing bull") ||
                    lower.contains("cash app") || Regex("""\brb\b""").containsMatchIn(lower) -> RACING_BULLS
                lower.contains("mercedes") || lower.contains("brawn") -> MERCEDES
                lower.contains("ferrari") -> FERRARI
                lower.contains("mclaren") -> MCLAREN
                lower.contains("aston martin") || lower.contains("aston_martin") || lower.contains("force india") ||
                    lower.contains("force_india") || lower.contains("racing point") || lower.contains("racing_point") ||
                    lower.contains("jordan") -> ASTON_MARTIN
                lower.contains("alpine") || lower.contains("renault") || lower.contains("benetton") || lower.contains("enstone") -> ALPINE
                lower.contains("williams") -> WILLIAMS
                lower.contains("haas") -> HAAS
                lower.contains("sauber") || lower.contains("audi") || lower.contains("alfa romeo") ||
                    lower.contains("alfa_romeo") || lower.contains("alfa") -> AUDI_SAUBER
                lower.contains("cadillac") || lower.contains("andretti") -> CADILLAC
                else -> entries.firstOrNull {
                    it != OTHER && (
                        lower.contains(it.displayName.lowercase()) ||
                            (it.constructorId != "rb" && lower.contains(it.constructorId))
                        )
                } ?: OTHER
            }
        }
    }
}

@Serializable
enum class TyreCompound(val code: String, val label: String, val colorHex: Long) {
    SOFT("S", "Soft", 0xFFE1251B),
    MEDIUM("M", "Medium", 0xFFF5C518),
    HARD("H", "Hard", 0xFFF0F0F5),
    INTERMEDIATE("I", "Inter", 0xFF2CE580),
    WET("W", "Wet", 0xFF00B4D8)
}

@Serializable
enum class SectorStatus(val colorHex: Long) {
    OVERALL_BEST(0xFFB855FF),   // Purple
    PERSONAL_BEST(0xFF2CE580),  // Green
    NORMAL(0xFFF5C518),         // Yellow
    INACTIVE(0xFF3A3A44)        // Grey
}

@Serializable
data class SessionScheduleItem(
    val shortName: String,      // e.g. "FP1", "FP2", "FP3", "Quali", "Race"
    val fullName: String,       // e.g. "Free Practice 1", "Qualifying", "Race"
    val dayOfWeek: String,      // e.g. "Fri", "Sat", "Sun"
    val fullDayName: String,    // e.g. "Friday", "Saturday", "Sunday"
    val dateDay: String,        // e.g. "06", "07", "08"
    val dateMonth: String,      // e.g. "Nov"
    val localTime: String,      // e.g. "05:30", "17:00"
    val countdownBadge: String, // e.g. "Race in 36 days", "35 days"
    val isRace: Boolean = false,
    val utcEpochMillis: Long? = null
)

@Serializable
data class RaceRecentResult(
    val grandPrixName: String,  // e.g. "Azerbaijan Grand Prix"
    val positionText: String,   // e.g. "P5", "P1"
    val pointsText: String,     // e.g. "(10pts)", "(25pts)"
    val position: Int
)

@Serializable
data class DriverStanding(
    val position: Int,
    val driverId: String,
    val code: String,           // e.g. "ANT", "RUS", "HAM", "NOR", "LEC", "VER"
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val nationality: String,
    val team: F1TeamId,
    val points: Double,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
    val avgPtsPerRace: Double,
    val racesCount: Int,
    val gapToLeader: String?,   // e.g. "-66 to leader"
    val gapToPrev: String?,     // e.g. "-37 to P2"
    val last3Summary: String,   // e.g. "P5, P1, P1 (60 pts)"
    val recentRaces: List<RaceRecentResult>
)

@Serializable
data class ConstructorWinItem(
    val grandPrixName: String,
    val winnerFullName: String
)

@Serializable
data class ConstructorDriverPoints(
    val fullName: String,
    val points: Double
)

@Serializable
data class TeamRaceFormColumn(
    val grandPrixName: String = "",
    val positionText: String = "",
    val pointsText: String = "",
    val position: Int = 1
)

@Serializable
data class ConstructorStanding(
    val position: Int,
    val team: F1TeamId,
    val points: Double,
    val wins: Int,
    val gapToP1: String?,       // e.g. "160 to P1"
    val gapToPrev: String?,     // e.g. "72 to P2"
    val drivers: List<ConstructorDriverPoints>,
    val seasonWins: List<ConstructorWinItem>,
    val recentTeamForm: List<TeamRaceFormColumn> = emptyList(),
    val constructorId: String = "",
    val constructorName: String = ""
) {
    val resolvedConstructorName: String
        get() = constructorName.ifBlank { team.displayName }

    val stableKey: String
        get() = constructorId.ifBlank { "${team.constructorId}_$position" }
}

@Serializable
data class TyreStint(
    val stintNumber: Int,
    val compound: TyreCompound,
    val lapsCount: Int,         // e.g. 16 -> "16L"
    val pitStopDuration: String? = null, // e.g. "21.6s"
    val pitStopLap: Int? = null          // e.g. 16 -> "Lap 16"
)

@Serializable
data class RaceDriverResult(
    val position: Int,
    val driverCode: String,
    val driverFullName: String,
    val team: F1TeamId,
    val finishTimeOrGap: String, // e.g. "2:06:54.430" or "+19.477"
    val points: Double,
    val qualifyingPosition: String = "",
    val qualifyingTime: String = "",
    val fastestLapPosition: String = "",
    val fastestLapTime: String = "",
    val totalLaps: Int = 58,
    val stints: List<TyreStint> = emptyList()
) {
    constructor(
        position: Int,
        driverCode: String,
        driverFullName: String,
        team: F1TeamId,
        finishTimeOrGap: String,
        points: Int,
        qualifyingPosition: String = "",
        qualifyingTime: String = "",
        fastestLapPosition: String = "",
        fastestLapTime: String = "",
        totalLaps: Int = 58,
        stints: List<TyreStint> = emptyList()
    ) : this(
        position = position,
        driverCode = driverCode,
        driverFullName = driverFullName,
        team = team,
        finishTimeOrGap = finishTimeOrGap,
        points = points.toDouble(),
        qualifyingPosition = qualifyingPosition,
        qualifyingTime = qualifyingTime,
        fastestLapPosition = fastestLapPosition,
        fastestLapTime = fastestLapTime,
        totalLaps = totalLaps,
        stints = stints
    )

    val formattedPoints: String
        get() = if (points % 1.0 == 0.0) points.toInt().toString() else points.toString()
}

@Serializable
data class CircuitCorner(
    val number: Int,
    val normalizedX: Float,
    val normalizedY: Float
)

@Serializable
data class RaceWeekend(
    val season: Int,
    val round: Int,
    val grandPrixName: String,      // e.g. "Bahrain Grand Prix in Malaysia" / "Brazilian Grand Prix"
    val circuitName: String,        // e.g. "Sepang International Circuit" / "Autódromo José Carlos Pace"
    val location: String,           // e.g. "Kuala Lumpur, Malaysia" / "São Paulo, Brazil"
    val countryFlagEmoji: String,   // e.g. "🇲🇾", "🇧🇷", "🇦🇪"
    val dateRangeText: String,      // e.g. "FRI 6 NOV - SUN 8 NOV"
    val daysToGo: Int,
    val circuitLengthKm: String,    // e.g. "5.543"
    val totalLaps: Int,             // e.g. 56, 69, 58
    val totalCorners: Int,          // e.g. 15, 16
    val speedTrapNormalizedX: Float = 0.24f,
    val speedTrapNormalizedY: Float = 0.27f,
    val circuitType: String = "SEPANG", // "SEPANG", "INTERLAGOS", "YAS_MARINA", "BAHRAIN"
    val circuitId: String = "",
    val sessions: List<SessionScheduleItem>,
    val results: List<RaceDriverResult> = emptyList(),
    val isSprintWeekend: Boolean = false
)

@Serializable
data class DriverCareerComparison(
    val driverId: String,
    val code: String,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val team: F1TeamId,
    val championships: Int,
    val raceWins: Int,
    val podiums: Int,
    val polePositions: Int,
    val fastestLaps: Int,
    val careerPoints: Double,
    val raceStarts: Int,
    val dnfs: Int,
    val winRatePercent: Double,
    val podiumRatePercent: Double,
    val poleRatePercent: Double
)

@Serializable
data class LiveDriverTiming(
    val position: Int,
    val code: String,
    val fullName: String,
    val team: F1TeamId,
    val bestLapTime: String,        // e.g. "1:36.477"
    val lastLapTime: String,        // e.g. "1:36.595"
    val gapToLeader: String,        // e.g. "Leader" or "+0.118"
    val intervalToAhead: String,    // e.g. "Leader" or "+0.118"
    val isOverallBest: Boolean = false,
    val isInPit: Boolean = false,
    val isOutLap: Boolean = false,
    val tyreCompound: TyreCompound = TyreCompound.SOFT,
    val tyreAgeLaps: Int = 2,
    val sector1Status: SectorStatus = SectorStatus.NORMAL,
    val sector2Status: SectorStatus = SectorStatus.NORMAL,
    val sector3Status: SectorStatus = SectorStatus.INACTIVE,
    val sector1Time: String = "31.453",
    val sector2Time: String = "43.939",
    val sector3Time: String = "—",
    val trackProgress: Float = 0.0f // 0.0f..1.0f around circuit path
)

@Serializable
data class TeamRadioClip(
    val id: String,
    val driverName: String,
    val driverCode: String,
    val team: F1TeamId,
    val timestamp: String,          // e.g. "08:55:53"
    val transcript: String,
    val durationSeconds: Int = 8
)

@Serializable
data class RaceControlMessage(
    val id: String,
    val title: String,              // e.g. "SESSION STARTED"
    val timestamp: String,          // e.g. "09:00:00"
    val category: String = "INFO"   // "FLAG", "INFO", "SAFETY_CAR"
)

@Serializable
data class OvertakeEvent(
    val lap: Int,
    val overtakingDriver: String,
    val overtakingTeam: F1TeamId,
    val overtakenDriver: String,
    val overtakenTeam: F1TeamId,
    val positionGained: Int,
    val cornerText: String
)

@Serializable
data class WeatherSnapshot(
    val trackTempC: Double,
    val airTempC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirection: String,
    val rainfallPercent: Int,
    val pressureMbar: Double
)

@Serializable
data class LiveSessionState(
    val grandPrixName: String,
    val sessionName: String,        // e.g. "Qualifying · Q1"
    val shortSessionCode: String,   // e.g. "Q1"
    val timeRemaining: String,      // e.g. "8:21" / "9:56 left"
    val bestLapDriverCode: String,  // e.g. "VER"
    val bestLapTime: String,        // e.g. "1:36.477"
    val carsOnTrack: Int,           // e.g. 21
    val trackStatusText: String,    // e.g. "Track Clear"
    val sector1LengthKm: String = "1.4 km",
    val sector2LengthKm: String = "1.8 km",
    val sector3LengthKm: String = "2.3 km",
    val circuitId: String = "SEPANG",
    val drivers: List<LiveDriverTiming>,
    val latestRadio: TeamRadioClip,
    val radioClips: List<TeamRadioClip>,
    val raceControlMessages: List<RaceControlMessage>,
    val overtakes: List<OvertakeEvent>,
    val weather: WeatherSnapshot
)

@Serializable
enum class WidgetStyleType(val title: String, val subtitle: String, val sizeLabel: String) {
    NEXT_GP_COUNTDOWN("Next Grand Prix & Circuit", "Countdown, circuit outline & session schedule", "4×2 Medium"),
    WEEKEND_SESSIONS("Weekend Session Schedule", "FP1, FP2, FP3, Qualifying & Race in local time", "4×3 Large"),
    WDC_STANDINGS("Driver Standings (WDC)", "Top 5 championship contenders with team bars", "4×3 Large"),
    WCC_STANDINGS("Constructor Standings (WCC)", "Top constructors with points & gap to P1", "4×3 Large"),
    FAVORITE_DRIVER("Favourite Driver Card", "Live position, points, wins & recent 3-race form", "2×2 Small"),
    FAVORITE_TEAM("Favourite Team Garage", "Constructor points, drivers & season wins", "2×2 Small"),
    LAST_RACE_PODIUM("Last Race Podium & Stints", "P1–P3 finishers, winning time & fastest lap", "4×2 Medium"),
    LIVE_TIMING_BANNER("Live Timing Banner", "Live session status, time left & P1 telemetry", "4×2 Medium")
}

@Serializable
data class UserPreferencesState(
    val favoriteTeam: F1TeamId = F1TeamId.FERRARI,
    val favoriteDriverCode: String = "LEC",
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val useTeamColorAccent: Boolean = true,
    val pitRadioAlertsEnabled: Boolean = true,
    val sessionRemindersEnabled: Boolean = true,
    val lapRecordAlertsEnabled: Boolean = true,
    val offlineCacheEnabled: Boolean = true
)
