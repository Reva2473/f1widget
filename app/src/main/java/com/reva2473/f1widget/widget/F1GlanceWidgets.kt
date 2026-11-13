package com.reva2473.f1widget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.reva2473.f1widget.MainActivity
import com.reva2473.f1widget.data.local.UserPreferencesStore
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.ui.components.getDriverHeadshotRes
import com.reva2473.f1widget.ui.components.getTeamCarRes
import kotlinx.coroutines.flow.first

// Common widget colors & theme values
private val CarbonDark = Color(0xFF16161A)
private val ElevatedDark = Color(0xFF1E1E24)
private val BorderSubtle = Color(0xFF2A2A32)
private val CrimsonRed = Color(0xFFE1251B)
private val TextWhite = Color(0xFFF5F5F7)
private val TextGrey = Color(0xFF9A9AA6)
private val TextMuted = Color(0xFF686873)
private val GoldAccent = Color(0xFFF5C518)
private val SectorGreen = Color(0xFF2CE580)
private val PurpleRecord = Color(0xFFB855FF)

val DriverStanding.driverCode: String
    get() = code

private val TargetScreenActionKey = ActionParameters.Key<String>("EXTRA_TARGET_SCREEN")

fun launchAppAction(context: Context, targetScreen: String = "HOME"): Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("EXTRA_TARGET_SCREEN", targetScreen)
        data = Uri.parse("apexgrid://widget/$targetScreen")
    }
    return actionStartActivity(
        intent = intent,
        parameters = actionParametersOf(TargetScreenActionKey to targetScreen)
    )
}

private suspend fun loadUserPreferences(context: Context): UserPreferencesState {
    return try {
        UserPreferencesStore(context.applicationContext).preferencesFlow.first()
    } catch (_: Throwable) {
        UserPreferencesState()
    }
}

@Composable
private fun DriverAvatar(
    driverCode: String,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier
) {
    val headshotRes = getDriverHeadshotRes(driverCode)
    val radius = size / 2
    Box(
        modifier = modifier
            .size(size)
            .background(ColorProvider(Color(0xFF232325)))
            .cornerRadius(radius),
        contentAlignment = Alignment.Center
    ) {
        if (headshotRes != null) {
            Image(
                provider = ImageProvider(headshotRes),
                contentDescription = driverCode,
                modifier = GlanceModifier
                    .size(size)
                    .cornerRadius(radius)
            )
        } else {
            Text(
                text = driverCode.take(3).uppercase(),
                style = TextStyle(
                    color = ColorProvider(TextWhite),
                    fontSize = if (size >= 32.dp) 11.sp else 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ============================================================================
// 1. Next Race Countdown Widget
// ============================================================================
class NextRaceCountdownWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val race: RaceWeekend = try {
            repo.getNextRaceWeekend()
        } catch (_: Throwable) {
            SeedDataProvider.getNextRaceWeekend()
        }
        val raceSession = race.sessions.find {
            it.isRace || it.fullName.equals("Race", ignoreCase = true) || it.shortName.equals("Race", ignoreCase = true)
        } ?: race.sessions.lastOrNull()

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                        .clickable(launchAppAction(context, "RACE_WEEKEND"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header: Flag, Category, Days To Go Badge
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${race.countryFlagEmoji} NEXT GRAND PRIX",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "R${race.round} · in ${race.daysToGo}d",
                                    style = TextStyle(
                                        color = ColorProvider(TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Grand Prix Name
                        Text(
                            text = race.grandPrixName,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(TextWhite),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Circuit & Date Range
                        Text(
                            text = "${race.circuitName} · ${race.dateRangeText}",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(TextGrey),
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Footer stats bar: Laps, Length, Corners, Race Day
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(ElevatedDark))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "LAPS",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${race.totalLaps}",
                                    style = TextStyle(color = ColorProvider(TextWhite), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "CIRCUIT",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${race.circuitLengthKm} km",
                                    style = TextStyle(color = ColorProvider(TextWhite), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "CORNERS",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${race.totalCorners}",
                                    style = TextStyle(color = ColorProvider(TextWhite), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "RACE TIME",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = raceSession?.localTime ?: "17:00",
                                    style = TextStyle(color = ColorProvider(SectorGreen), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class NextRaceCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextRaceCountdownWidget()
}

// ============================================================================
// 2. Weekend Session Schedule Widget
// ============================================================================
class WeekendScheduleWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val race: RaceWeekend = try {
            repo.getNextRaceWeekend()
        } catch (_: Throwable) {
            SeedDataProvider.getNextRaceWeekend()
        }
        val sessions: List<SessionScheduleItem> = race.sessions.take(5)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                        .clickable(launchAppAction(context, "RACE_WEEKEND"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${race.countryFlagEmoji} ${race.grandPrixName}",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(TextWhite),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "WEEKEND SCHEDULE",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Sessions List
                        sessions.forEachIndexed { index, session ->
                            val isRace = session.isRace
                            val rowBg = if (isRace) ElevatedDark else CarbonDark
                            val labelColor = if (isRace) CrimsonRed else TextWhite

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(rowBg))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Short Name badge (FP1, FP2, Quali, Race)
                                Box(
                                    modifier = GlanceModifier
                                        .width(36.dp)
                                        .background(ColorProvider(if (isRace) CrimsonRed else BorderSubtle))
                                        .cornerRadius(4.dp)
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = session.shortName,
                                        style = TextStyle(
                                            color = ColorProvider(TextWhite),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Full Name
                                Text(
                                    text = session.fullName,
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style = TextStyle(
                                        color = ColorProvider(labelColor),
                                        fontSize = 10.sp,
                                        fontWeight = if (isRace) FontWeight.Bold else FontWeight.Normal
                                    )
                                )

                                // Day & Date
                                Text(
                                    text = "${session.dayOfWeek} ${session.dateDay} ${session.dateMonth}",
                                    style = TextStyle(
                                        color = ColorProvider(TextGrey),
                                        fontSize = 9.sp
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Local Time
                                Text(
                                    text = session.localTime,
                                    style = TextStyle(
                                        color = ColorProvider(if (isRace) GoldAccent else TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < sessions.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class WeekendScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekendScheduleWidget()
}

// ============================================================================
// 3. Driver Standings Widget (WDC)
// ============================================================================
class DriverStandingsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val standingsList = try {
            repo.getDriverStandings(2026)
        } catch (_: Throwable) {
            emptyList()
        }
        val standings: List<DriverStanding> = if (standingsList.isNotEmpty()) {
            standingsList.take(5)
        } else {
            SeedDataProvider.getDriverStandings2026().take(5)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "DRIVER_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DRIVERS CHAMPIONSHIP",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "WDC 2026",
                                style = TextStyle(
                                    color = ColorProvider(TextMuted),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Drivers List
                        standings.forEachIndexed { index, driver ->
                            val teamColor = Color(driver.team.colorHex)
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position
                                Text(
                                    text = "P${driver.position}",
                                    style = TextStyle(
                                        color = ColorProvider(if (driver.position == 1) GoldAccent else TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Team Color Strip
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(ColorProvider(teamColor))
                                        .cornerRadius(1.dp)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                DriverAvatar(driverCode = driver.code, size = 20.dp)

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Driver Name & Team
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = driver.fullName,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextWhite),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = driver.team.displayName,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextGrey),
                                            fontSize = 8.sp
                                        )
                                    )
                                }

                                // Wins badge
                                if (driver.wins > 0) {
                                    Text(
                                        text = "${driver.wins}W",
                                        style = TextStyle(
                                            color = ColorProvider(GoldAccent),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }

                                // Points
                                Text(
                                    text = "${driver.points.toInt()} PTS",
                                    style = TextStyle(
                                        color = ColorProvider(TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < standings.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class DriverStandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DriverStandingsWidget()
}

// ============================================================================
// 4. Constructor Standings Widget (WCC)
// ============================================================================
class ConstructorStandingsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val standingsList = try {
            repo.getConstructorStandings(2026)
        } catch (_: Throwable) {
            emptyList()
        }
        val standings: List<ConstructorStanding> = if (standingsList.isNotEmpty()) {
            standingsList.take(5)
        } else {
            SeedDataProvider.getConstructorStandings2026().take(5)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "CONSTRUCTOR_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONSTRUCTORS CHAMPIONSHIP",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "WCC 2026",
                                style = TextStyle(
                                    color = ColorProvider(TextMuted),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Constructors List
                        standings.forEachIndexed { index, constructor ->
                            val teamColor = Color(constructor.team.colorHex)
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "P${constructor.position}",
                                    style = TextStyle(
                                        color = ColorProvider(if (constructor.position == 1) GoldAccent else TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Team Bar
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(ColorProvider(teamColor))
                                        .cornerRadius(1.dp)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                val carRes = getTeamCarRes(constructor.team)
                                Image(
                                    provider = ImageProvider(carRes),
                                    contentDescription = constructor.team.displayName,
                                    modifier = GlanceModifier.width(32.dp).height(16.dp)
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                Text(
                                    text = constructor.team.displayName,
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style = TextStyle(
                                        color = ColorProvider(TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                val gap = constructor.gapToP1
                                if (gap != null) {
                                    Text(
                                        text = gap,
                                        style = TextStyle(
                                            color = ColorProvider(TextGrey),
                                            fontSize = 8.sp
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                }

                                Text(
                                    text = "${constructor.points.toInt()} PTS",
                                    style = TextStyle(
                                        color = ColorProvider(TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < standings.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class ConstructorStandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ConstructorStandingsWidget()
}

// ============================================================================
// 5. Favorite Driver Widget (2x2 Small)
// ============================================================================
class FavoriteDriverWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val standingsList = try {
            repo.getDriverStandings(2026)
        } catch (_: Throwable) {
            emptyList()
        }
        val standings = if (standingsList.isNotEmpty()) standingsList else SeedDataProvider.getDriverStandings2026()
        val targetCode = prefs.favoriteDriverCode
        val driver: DriverStanding = standings.firstOrNull {
            it.code.equals(targetCode, ignoreCase = true) ||
            it.driverId.equals(targetCode, ignoreCase = true) ||
            it.fullName.contains(targetCode, ignoreCase = true)
        } ?: standings.firstOrNull() ?: SeedDataProvider.getDriverStandings2026().first()
        val teamColor = Color(driver.team.colorHex)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "DRIVER_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header with Team Indicator
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(ColorProvider(teamColor))
                                    .cornerRadius(1.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = driver.team.displayName.uppercase(),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(teamColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "P${driver.position}",
                                style = TextStyle(
                                    color = ColorProvider(GoldAccent),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Driver Code & Name with Avatar
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = driver.code,
                                    style = TextStyle(
                                        color = ColorProvider(TextWhite),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = driver.fullName,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(TextGrey),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            DriverAvatar(driverCode = driver.code, size = 36.dp)
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Points and Wins row
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(ElevatedDark))
                                .cornerRadius(6.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "POINTS",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${driver.points.toInt()}",
                                    style = TextStyle(color = ColorProvider(TextWhite), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WINS",
                                    style = TextStyle(color = ColorProvider(TextMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${driver.wins}",
                                    style = TextStyle(color = ColorProvider(GoldAccent), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Recent form summary
                        Text(
                            text = driver.last3Summary,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(SectorGreen),
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class FavoriteDriverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoriteDriverWidget()
}

// ============================================================================
// 6. Favorite Team Widget (2x2 Small)
// ============================================================================
class FavoriteTeamWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val standingsList = try {
            repo.getConstructorStandings(2026)
        } catch (_: Throwable) {
            emptyList()
        }
        val standings = if (standingsList.isNotEmpty()) standingsList else SeedDataProvider.getConstructorStandings2026()
        val targetTeam = prefs.favoriteTeam
        val constructor: ConstructorStanding = standings.firstOrNull {
            it.team == targetTeam ||
            it.team.name.equals(targetTeam.name, ignoreCase = true) ||
            it.team.displayName.contains(targetTeam.displayName, ignoreCase = true)
        } ?: standings.firstOrNull() ?: SeedDataProvider.getConstructorStandings2026().first()
        val teamColor = Color(constructor.team.colorHex)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "CONSTRUCTOR_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(ColorProvider(teamColor))
                                    .cornerRadius(1.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "GARAGE",
                                style = TextStyle(
                                    color = ColorProvider(TextMuted),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "P${constructor.position}",
                                style = TextStyle(
                                    color = ColorProvider(GoldAccent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = constructor.team.displayName,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(TextWhite),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "${constructor.points.toInt()} PTS · ${constructor.wins} WINS",
                            style = TextStyle(
                                color = ColorProvider(teamColor),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Real Team Car Cutout
                        val carRes = getTeamCarRes(constructor.team)
                        Image(
                            provider = ImageProvider(carRes),
                            contentDescription = constructor.team.displayName,
                            modifier = GlanceModifier.fillMaxWidth().height(26.dp)
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Driver Pair
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(ElevatedDark))
                                .cornerRadius(6.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            constructor.drivers.take(2).forEachIndexed { i, drv ->
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = drv.fullName,
                                        maxLines = 1,
                                        modifier = GlanceModifier.defaultWeight(),
                                        style = TextStyle(color = ColorProvider(TextWhite), fontSize = 9.sp)
                                    )
                                    Text(
                                        text = "${drv.points.toInt()} pts",
                                        style = TextStyle(color = ColorProvider(TextGrey), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                                if (i == 0 && constructor.drivers.size > 1) {
                                    Spacer(modifier = GlanceModifier.height(1.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class FavoriteTeamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoriteTeamWidget()
}

// ============================================================================
// 7. Last Race Podium Widget (4x2 Medium)
// ============================================================================
class LastRacePodiumWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val resultsList = try {
            repo.getSaoPaulo2024Results().results
        } catch (_: Throwable) {
            emptyList()
        }
        val results: List<RaceDriverResult> = if (resultsList.isNotEmpty()) {
            resultsList.take(3)
        } else {
            SeedDataProvider.getSaoPaulo2024Results().results.take(3)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "RACE_WEEKEND"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LAST RACE PODIUM · SÃO PAULO",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "FINAL CLASSIFICATION",
                                style = TextStyle(
                                    color = ColorProvider(TextMuted),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Podium 3 Rows
                        results.forEachIndexed { index, driver ->
                            val posColor = when (driver.position) {
                                1 -> GoldAccent
                                2 -> Color(0xFFC0C0C8)
                                else -> Color(0xFFD97B29)
                            }
                            val teamColor = Color(driver.team.colorHex)

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Trophy position
                                Text(
                                    text = "P${driver.position}",
                                    style = TextStyle(
                                        color = ColorProvider(posColor),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Team line
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(ColorProvider(teamColor))
                                        .cornerRadius(1.dp)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                DriverAvatar(driverCode = driver.driverCode, size = 20.dp)

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                // Driver Name & Code
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = "${driver.driverCode} · ${driver.driverFullName}",
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextWhite),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = driver.team.displayName,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextGrey),
                                            fontSize = 8.sp
                                        )
                                    )
                                }

                                // Stints compound badges
                                if (driver.stints.isNotEmpty()) {
                                    val stintSummary = driver.stints.joinToString(" ➔ ") { "${it.compound.code}(${it.lapsCount})" }
                                    Text(
                                        text = stintSummary,
                                        style = TextStyle(
                                            color = ColorProvider(TextMuted),
                                            fontSize = 8.sp
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }

                                // Gap or Time
                                Text(
                                    text = driver.finishTimeOrGap,
                                    style = TextStyle(
                                        color = ColorProvider(if (driver.position == 1) SectorGreen else TextWhite),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < results.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class LastRacePodiumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LastRacePodiumWidget()
}

// ============================================================================
// 8. Live Timing Banner Widget
// ============================================================================
class LiveTimingBannerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val repo = F1Repository(context.applicationContext)
        val liveSession = try {
            repo.getLiveSessionState()
        } catch (_: Throwable) {
            SeedDataProvider.getSaoPauloLiveSessionState()
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(CarbonDark))
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                        .clickable(launchAppAction(context, "LIVE_TIMING"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE · ${liveSession.shortSessionCode}",
                                style = TextStyle(
                                    color = ColorProvider(CrimsonRed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(4.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${liveSession.timeRemaining} · ${liveSession.trackStatusText.uppercase()}",
                                    style = TextStyle(
                                        color = ColorProvider(SectorGreen),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = liveSession.grandPrixName,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(TextGrey),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Leader / P1 info
                        val leader = liveSession.drivers.firstOrNull()
                        if (leader != null) {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(ElevatedDark))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "P1",
                                    style = TextStyle(
                                        color = ColorProvider(GoldAccent),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                DriverAvatar(driverCode = leader.code, size = 20.dp)
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = "${leader.code} · ${leader.fullName}",
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextWhite),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "BEST: ${leader.bestLapTime} · LAST: ${leader.lastLapTime}",
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(TextGrey),
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class LiveTimingBannerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiveTimingBannerWidget()
}
