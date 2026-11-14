package com.reva2473.f1widget.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.CircuitOutlineCanvas
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.theme.P1TTheme
import kotlin.math.roundToInt

private val RaceWeekend.countdownBadge: String
    get() = sessions.firstOrNull { it.isRace }?.countdownBadge ?: ""

private val LiveSessionState.sessionType: String
    get() = shortSessionCode.ifBlank { sessionName }

private val LiveSessionState.trackStatus: String
    get() = trackStatusText

private fun formatPointsValue(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    weekend: RaceWeekend? = null,
    liveSession: LiveSessionState? = null,
    driverStandings: List<DriverStanding>? = null,
    constructorStandings: List<ConstructorStanding>? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    lastCompletedRace: RaceWeekend? = null,
    showBottomBar: Boolean = true,
    onNavigateToLiveTiming: () -> Unit = {},
    onNavigateToStandings: () -> Unit = {},
    onNavigateToWeekend: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Default sample data matching home-1080.webp if not provided
    val defaultSessions = remember {
        listOf(
            SessionScheduleItem("FP1", "Free Practice 1", "Fri", "Friday", "06", "Nov", "05:30", "38 days"),
            SessionScheduleItem("FP2", "Free Practice 2", "Fri", "Friday", "06", "Nov", "09:00", "38 days"),
            SessionScheduleItem("FP3", "Free Practice 3", "Sat", "Saturday", "07", "Nov", "05:30", "37 days"),
            SessionScheduleItem("Quali", "Qualifying", "Sat", "Saturday", "07", "Nov", "09:00", "37 days"),
            SessionScheduleItem("Race", "Race", "Sun", "Sunday", "08", "Nov", "08:00", "Race in 1 day", isRace = true)
        )
    }

    val sampleLiveDrivers = remember {
        listOf(
            LiveDriverTiming(1, "VER", "Max Verstappen", F1TeamId.RED_BULL, "1:36.477", "1:36.477", "Leader", "Leader", trackProgress = 0.88f),
            LiveDriverTiming(2, "HAD", "Isack Hadjar", F1TeamId.RED_BULL, "1:36.898", "1:36.898", "+0.421", "+0.421", trackProgress = 0.52f),
            LiveDriverTiming(3, "ANT", "Kimi Antonelli", F1TeamId.MERCEDES, "1:37.041", "1:37.041", "+0.564", "+0.143", trackProgress = 0.35f),
            LiveDriverTiming(4, "LEC", "Charles Leclerc", F1TeamId.FERRARI, "1:37.120", "1:37.120", "+0.643", "+0.079", trackProgress = 0.22f),
            LiveDriverTiming(5, "HAM", "Lewis Hamilton", F1TeamId.FERRARI, "1:37.195", "1:37.195", "+0.718", "+0.075", trackProgress = 0.72f),
            LiveDriverTiming(6, "NOR", "Lando Norris", F1TeamId.MCLAREN, "1:37.310", "1:37.310", "+0.833", "+0.115", trackProgress = 0.12f)
        )
    }

    val activeLiveDrivers = liveSession?.drivers ?: sampleLiveDrivers

    val defaultWeekend = remember(defaultSessions) {
        RaceWeekend(
            season = 2026,
            round = 16,
            grandPrixName = "Bahrain Grand Prix in Malaysia",
            circuitName = "Sepang International Circuit",
            location = "Kuala Lumpur, Malaysia",
            countryFlagEmoji = "🇲🇾",
            dateRangeText = "FRI 6 NOV - SUN 8 NOV",
            daysToGo = 1,
            circuitLengthKm = "5.543",
            totalLaps = 56,
            totalCorners = 15,
            circuitType = "SEPANG",
            circuitId = "SEPANG",
            sessions = defaultSessions
        )
    }
    val activeWeekend = weekend ?: defaultWeekend

    // Strictly bind completed race results to lastCompletedRace (falling back to explicit lastRaceResults only if lastCompletedRace == null)
    val effectiveLastRaceResults = if (lastCompletedRace != null) {
        lastCompletedRace.results
    } else {
        lastRaceResults.orEmpty()
    }

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. TOP BAR
            HomeTopBar(
                onNavigateToSettings = onNavigateToSettings
            )

            // SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // 2. HERO HORIZONTAL PAGER (5 slides)
                HomeHeroPager(
                    weekend = activeWeekend,
                    sessions = activeWeekend.sessions,
                    driverLeader = driverStandings?.firstOrNull(),
                    constructorLeader = constructorStandings?.firstOrNull(),
                    lastCompletedRace = lastCompletedRace,
                    lastRaceResults = effectiveLastRaceResults,
                    onSlideTap = onNavigateToWeekend,
                    onStandingsTap = onNavigateToStandings
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. EXPLORE F1 STATS CARD
                ExploreF1StatsCard(onClick = onNavigateToExplore)

                Spacer(modifier = Modifier.height(18.dp))

                // 4. LIVE SESSION TRACK MAP CARD
                LiveTrackSummaryCard(
                    liveSession = liveSession,
                    weekend = activeWeekend,
                    drivers = activeLiveDrivers,
                    onOpenLiveTiming = onNavigateToLiveTiming
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5. BENTO RACE WEEKEND SUMMARY CARD
                HomeBentoCard(
                    weekend = activeWeekend,
                    driverStandings = driverStandings,
                    liveSession = liveSession,
                    onClick = onNavigateToWeekend
                )

                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(96.dp)
                )
            }
        }

        if (showBottomBar) {
            // FLOATING PILL BOTTOM BAR
            FloatingPillBottomBar(
                currentTab = "Home",
                onSelectTab = { tab ->
                    when (tab) {
                        "Season" -> onNavigateToWeekend()
                        "Standings" -> onNavigateToStandings()
                        "Settings" -> onNavigateToSettings()
                        else -> onTabSelected(tab)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Top bar: Left circular green verified shield badge button (Settings), centered "Home" title, right circular settings gear button.
 */
@Composable
private fun HomeTopBar(
    onNavigateToSettings: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shield badge button with green check wired to Settings
        CircularHeaderButton(onClick = onNavigateToSettings) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Settings & Widgets",
                    tint = P1TTheme.sectorGreen,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Centered Title
        Text(
            text = "Home",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Settings Gear Icon
        CircularHeaderButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 5-slide HorizontalPager Hero with 5-dot pager indicator (active dot elongated into pill)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeroPager(
    weekend: RaceWeekend,
    sessions: List<SessionScheduleItem>,
    driverLeader: DriverStanding? = null,
    constructorLeader: ConstructorStanding? = null,
    lastCompletedRace: RaceWeekend? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onSlideTap: () -> Unit,
    onStandingsTap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> HeroSlideNextRace(weekend = weekend, sessions = sessions, onClick = onSlideTap)
                1 -> HeroSlideApexGridWelcome(onClick = onSlideTap)
                2 -> HeroSlideWdcLeader(leader = driverLeader, onClick = onStandingsTap)
                3 -> HeroSlideWccLeader(leader = constructorLeader, onClick = onStandingsTap)
                4 -> HeroSlideLastRaceWinner(
                    lastCompletedRace = lastCompletedRace,
                    lastRaceResults = lastRaceResults,
                    onClick = onSlideTap
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pager Indicator Dots (active dot elongated into pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(if (isSelected) 22.dp else 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) primaryAccent else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

/**
 * Slide 0 (home-1080.webp):
 * "ROUND 16 · IN 1 DAY" in red, "Bahrain Grand Prix in Malaysia", "Sepang International Circuit",
 * left session schedule table beside SepangOutlineCanvas, and bottom 4-column stat strip.
 */
@Composable
private fun HeroSlideNextRace(
    weekend: RaceWeekend,
    sessions: List<SessionScheduleItem> = weekend.sessions,
    onClick: () -> Unit
) {
    val countdownText = if (weekend.countdownBadge.isNotBlank()) {
        weekend.countdownBadge.uppercase()
    } else {
        "IN ${weekend.daysToGo} DAY${if (weekend.daysToGo == 1) "" else "S"}"
    }
    val topBadge = "ROUND ${weekend.round}  ·  $countdownText"
    val rawCircuitId = weekend.circuitId.ifBlank { weekend.circuitType }
    val circuitId = when {
        rawCircuitId.isNotBlank() && !rawCircuitId.equals("SEPANG", ignoreCase = true) -> rawCircuitId
        weekend.circuitName.contains("Pace", ignoreCase = true) ||
            weekend.circuitName.contains("Interlagos", ignoreCase = true) -> "INTERLAGOS"
        weekend.circuitName.contains("Yas", ignoreCase = true) -> "YAS_MARINA"
        rawCircuitId.isNotBlank() -> rawCircuitId
        else -> "SEPANG"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent
            val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

            // "ROUND X · IN Y DAYS"
            Text(
                text = topBadge,
                color = adaptiveAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Grand Prix Name
            Text(
                text = weekend.grandPrixName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            // Circuit & Location
            Text(
                text = weekend.circuitName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = weekend.location,
                color = P1TTheme.textMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Left session schedule table beside Circuit Outline Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session Table
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sessions.take(5).forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.shortName,
                                color = if (session.isRace) adaptiveAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.width(42.dp)
                            )
                            Text(
                                text = "${session.dayOfWeek} ${session.localTime}",
                                color = if (session.isRace) adaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Circuit Outline Canvas on right
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircuitOutlineCanvas(
                        circuitType = circuitId,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom 4-column stat strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(P1TTheme.surfaceInnerCard)
                    .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPillColumn(
                    label = weekend.daysToGo.toString(),
                    sub = if (weekend.daysToGo == 1) "DAY TO GO" else "DAYS TO GO"
                )
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(P1TTheme.dividerSubtle))
                StatPillColumn(label = weekend.circuitLengthKm, sub = "KM")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(P1TTheme.dividerSubtle))
                StatPillColumn(label = weekend.totalLaps.toString(), sub = "LAPS")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(P1TTheme.dividerSubtle))
                StatPillColumn(label = weekend.totalCorners.toString(), sub = "CORNERS")
            }
        }
    }
}

@Composable
private fun StatPillColumn(label: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = sub,
            color = P1TTheme.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Slide 1 (home-welcome-1080.webp):
 * "FORMULA 1 DATA", bold "ApexGrid" title, red accent bar, description, F1 car graphic, and "Join Community ↗" pill.
 */
@Composable
private fun HeroSlideApexGridWelcome(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        val primaryAccent = LocalF1Accent.current.primaryAccent
        val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "FORMULA 1 DATA",
                color = adaptiveAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ApexGrid",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Accent Bar
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 10.dp)
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(primaryAccent)
                    )

                    Text(
                        text = "Dive deep into Formula 1 history with comprehensive race data, statistics, and exclusive insights.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                // F1 Car Graphic Icon Representation
                val carPath = remember { Path() }
                val wheelColor = if (P1TTheme.isDark) Color(0xFF2A2A34) else Color(0xFF3A3A46)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Stylized F1 car silhouette path
                        carPath.reset()
                        carPath.moveTo(w * 0.1f, h * 0.6f)
                        carPath.lineTo(w * 0.3f, h * 0.55f)
                        carPath.lineTo(w * 0.45f, h * 0.35f)
                        carPath.lineTo(w * 0.7f, h * 0.35f)
                        carPath.lineTo(w * 0.85f, h * 0.5f)
                        carPath.lineTo(w * 0.95f, h * 0.52f)
                        carPath.lineTo(w * 0.95f, h * 0.65f)
                        carPath.lineTo(w * 0.75f, h * 0.65f)
                        carPath.lineTo(w * 0.65f, h * 0.7f)
                        carPath.lineTo(w * 0.35f, h * 0.7f)
                        carPath.lineTo(w * 0.25f, h * 0.65f)
                        carPath.close()
                        drawPath(carPath, color = primaryAccent.copy(alpha = 0.85f))
                        // Wheels
                        drawCircle(wheelColor, radius = 10.dp.toPx(), center = Offset(w * 0.3f, h * 0.68f))
                        drawCircle(wheelColor, radius = 10.dp.toPx(), center = Offset(w * 0.8f, h * 0.68f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Join Community ↗" Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(P1TTheme.pillBg)
                    .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/r/formula1"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            onClick()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Join Community",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "↗",
                    color = adaptiveAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Slide 2: WDC Leader (Dynamically bound to driverStandings.firstOrNull())
 */
@Composable
private fun HeroSlideWdcLeader(
    leader: DriverStanding? = null,
    onClick: () -> Unit = {}
) {
    val teamColor = leader?.team?.let { Color(it.colorHex) } ?: Color(F1TeamId.MERCEDES.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val driverName = leader?.fullName ?: "Kimi Antonelli"
    val teamName = leader?.team?.displayName ?: "Mercedes AMG Petronas"
    val positionText = "P${leader?.position ?: 1}"
    val pointsText = if (leader != null) "${formatPointsValue(leader.points)} PTS" else "302 PTS"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "WORLD DRIVERS' CHAMPIONSHIP",
                color = adaptiveTeamColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DriverPortraitAvatar(
                        driverCodeOrName = leader?.code ?: "ANT",
                        teamColor = teamColor,
                        size = 50.dp,
                        positionBadge = positionText
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = driverName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$teamName · $positionText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = pointsText,
                    color = adaptiveTeamColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = P1TTheme.textMuted, modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.wins ?: 8} Wins", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = P1TTheme.textMuted, modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.podiums ?: 12} Podiums", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = P1TTheme.textMuted, modifier = Modifier.size(15.dp))
                    Text(text = if (leader != null) "${leader.poles} Poles" else "+66 Lead", color = P1TTheme.sectorGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Slide 3: WCC Leader (Dynamically bound to constructorStandings.firstOrNull())
 */
@Composable
private fun HeroSlideWccLeader(
    leader: ConstructorStanding? = null,
    onClick: () -> Unit = {}
) {
    val teamColor = leader?.team?.let { Color(it.colorHex) } ?: Color(F1TeamId.MERCEDES.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val teamName = leader?.resolvedConstructorName ?: "Mercedes"
    val driversText = if (leader != null && leader.drivers.isNotEmpty()) {
        leader.drivers.joinToString(" & ") { it.fullName.substringAfterLast(" ") }
    } else {
        "Antonelli & Russell"
    }
    val positionText = "P${leader?.position ?: 1}"
    val pointsText = if (leader != null) "${formatPointsValue(leader.points)} PTS" else "538 PTS"
    val rawGap = leader?.gapToP1?.trim()?.takeIf { it.isNotBlank() }
    val gapText = when {
        rawGap == null -> "$positionText Leader"
        rawGap.startsWith("-") -> rawGap
        else -> "-$rawGap"
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "CONSTRUCTORS' CHAMPIONSHIP",
                color = adaptiveTeamColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teamName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = driversText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = pointsText,
                    color = adaptiveTeamColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TeamCarShowcaseImage(
                team = leader?.team ?: F1TeamId.MERCEDES,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = P1TTheme.textMuted, modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.wins ?: 11} Wins", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(text = gapText, color = P1TTheme.textMuted, fontSize = 12.sp)
                Text(text = "Championship $positionText", color = P1TTheme.sectorGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide 4: Last Race Winner (strictly bound to lastCompletedRace / explicit lastRaceResults)
 */
@Composable
private fun HeroSlideLastRaceWinner(
    lastCompletedRace: RaceWeekend? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onClick: () -> Unit = {}
) {
    val results = if (lastCompletedRace != null) {
        lastCompletedRace.results
    } else {
        lastRaceResults.orEmpty()
    }
    val winner = results.firstOrNull { it.position == 1 } ?: results.firstOrNull()

    if (winner == null) {
        val primaryAccent = LocalF1Accent.current.primaryAccent
        val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
        val pendingSubtitle = lastCompletedRace?.grandPrixName?.takeIf { it.isNotBlank() }
            ?.let { "$it · Classification pending" }
            ?: "Season opener / next results pending"

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = "LAST GRAND PRIX WINNER",
                    color = adaptiveAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Awaiting Race Result",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pendingSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = P1TTheme.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Official podium & fastest lap appear after the chequered flag",
                    color = P1TTheme.textMuted,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    val winnerName = winner.driverFullName
    val winnerTeam = winner.team
    val raceName = lastCompletedRace?.grandPrixName?.takeIf { it.isNotBlank() }.orEmpty()
    val teamSubtitle = if (raceName.isNotBlank()) {
        "${winnerTeam.displayName} · $raceName"
    } else {
        winnerTeam.displayName
    }
    val finishTime = winner.finishTimeOrGap.ifBlank { "P${winner.position}" }
    val fastestLapResult = results.firstOrNull { it.fastestLapPosition.equals("P1", ignoreCase = true) }
        ?: results.firstOrNull { it.fastestLapTime.isNotBlank() }
    val fastestLapTime = fastestLapResult?.fastestLapTime?.takeIf { it.isNotBlank() }
    val fastestLapLabel = if (fastestLapTime != null) {
        val codeSuffix = fastestLapResult.driverCode.takeIf {
            it.isNotBlank() && !it.equals(winner.driverCode, ignoreCase = true)
        }?.let { " ($it)" }.orEmpty()
        "⚡ Fastest Lap: $fastestLapTime$codeSuffix"
    } else if (winner.totalLaps > 0) {
        "${winner.totalLaps} Laps completed"
    } else {
        "Race Winner"
    }
    val pointsText = "${winner.formattedPoints} PTS"
    val teamColor = Color(winnerTeam.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "LAST GRAND PRIX WINNER",
                color = adaptiveTeamColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = winnerName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = teamSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = finishTime,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fastestLapLabel,
                    color = P1TTheme.sectorPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = pointsText,
                    color = adaptiveTeamColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * 3. "Explore F1 Stats" card (Results, standings and records since 1950)
 * with 3 overlapping circular icons on left and chevron > on right.
 */
@Composable
private fun ExploreF1StatsCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3 Overlapping Driver Avatars
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Driver 1 (VER)
                Box(modifier = Modifier.size(28.dp)) {
                    DriverPortraitAvatar("VER", Color(F1TeamId.RED_BULL.colorHex), size = 28.dp)
                }
                // Driver 2 (HAM)
                Box(modifier = Modifier.padding(start = 16.dp).size(28.dp)) {
                    DriverPortraitAvatar("HAM", Color(F1TeamId.FERRARI.colorHex), size = 28.dp)
                }
                // Driver 3 (NOR)
                Box(modifier = Modifier.padding(start = 32.dp).size(28.dp)) {
                    DriverPortraitAvatar("NOR", Color(F1TeamId.MCLAREN.colorHex), size = 28.dp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Explore F1 Stats",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Results, standings and records since 1950",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Chevron >
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Explore",
                tint = P1TTheme.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 4. Live Session Track Map card:
 * Red dot LIVE, bold Q1 · 9:56 left, green "Track Clear" pill + >,
 * left column top 3 (1 VER 1:36.477, 2 HAD +0.421, 3 ANT +0.564), right LiveTrackMapCanvas
 */
@Composable
private fun LiveTrackSummaryCard(
    liveSession: LiveSessionState? = null,
    weekend: RaceWeekend? = null,
    drivers: List<LiveDriverTiming> = liveSession?.drivers.orEmpty(),
    onOpenLiveTiming: () -> Unit
) {
    val sessionBadge = liveSession?.sessionType?.ifBlank { "FP1" } ?: "FP1"
    val timeRemaining = liveSession?.timeRemaining.orEmpty()
    val timeRemainingText = if (timeRemaining.isBlank()) {
        "· 9:56 left"
    } else if (timeRemaining.contains("left", ignoreCase = true)) {
        "· $timeRemaining"
    } else {
        "· $timeRemaining left"
    }
    val trackStatus = liveSession?.trackStatus?.ifBlank { "Track Clear" } ?: "Track Clear"
    val (statusColor, statusBg, statusBorder) = when {
        trackStatus.contains("Red", ignoreCase = true) ->
            Triple(P1TTheme.accentRed, P1TTheme.redFlagBg, P1TTheme.redFlagBorder)
        trackStatus.contains("Clear", ignoreCase = true) ||
            trackStatus.contains("Green", ignoreCase = true) ->
            Triple(P1TTheme.sectorGreen, P1TTheme.trackClearBg, P1TTheme.trackClearBorder)
        else ->
            Triple(P1TTheme.sectorYellow, P1TTheme.sectorYellowBg, P1TTheme.sectorYellowBorder)
    }

    val circuitId = liveSession?.circuitId?.ifBlank { null }
        ?: weekend?.circuitId?.ifBlank { weekend.circuitType }?.ifBlank { null }
        ?: "INTERLAGOS"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onOpenLiveTiming() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Live Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Red flashing LIVE dot
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(P1TTheme.accentRed)
                    )
                    Text(
                        text = "LIVE",
                        color = P1TTheme.accentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "·",
                        color = P1TTheme.textMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = sessionBadge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = timeRemainingText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Track Clear Pill + >
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .border(1.dp, statusBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = trackStatus,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Left top 3 column | Right LiveTrackMapCanvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top 3 Drivers Column
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    drivers.take(3).forEach { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Position
                            Text(
                                text = "${d.position}",
                                color = P1TTheme.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(16.dp)
                            )
                            // Team Stripe
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color(d.team.colorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Code
                            Text(
                                text = d.code,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(36.dp)
                            )
                            // Time / Gap
                            Text(
                                text = if (d.position == 1) d.bestLapTime else d.gapToLeader,
                                color = if (d.position == 1) P1TTheme.sectorPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Live Track Map Canvas
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                ) {
                    LiveTrackMapCanvas(
                        drivers = drivers,
                        circuitType = circuitId,
                        onDriverTap = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 5. 3-column Bento card:
 * (WEEKEND: LEC fastest in FP2 with red top border | TITLE: +66 Antonelli leads | RACE: 08:00 Sun)
 * + bottom headline "Leclerc sets the pace in FP2" and "🇲🇾 Bahrain GP in Malaysia · Round 16 · so far".
 */
@Composable
private fun HomeBentoCard(
    weekend: RaceWeekend? = null,
    driverStandings: List<DriverStanding>? = null,
    liveSession: LiveSessionState? = null,
    onClick: () -> Unit = {}
) {
    // Column 1: WEEKEND — only label "fastest" when a driver genuinely holds the fastest lap
    val fastestLiveDriver = liveSession?.drivers?.firstOrNull { it.isOverallBest }
        ?: liveSession?.bestLapDriverCode?.takeIf { it.isNotBlank() }?.let { code ->
            liveSession.drivers.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }
    val p1LiveDriver = liveSession?.drivers?.minByOrNull { it.position }
    val sessionName = liveSession?.shortSessionCode?.ifBlank { liveSession.sessionName }?.ifBlank { "Session" } ?: "Session"
    val weekendColText = when {
        fastestLiveDriver != null -> "${fastestLiveDriver.code} fastest in $sessionName"
        p1LiveDriver != null -> "${p1LiveDriver.code} leads $sessionName"
        else -> "Weekend schedule ready"
    }

    // Column 2: TITLE (e.g. P1 leader gap over P2, handling ties cleanly without "+0")
    val p1 = driverStandings?.getOrNull(0)
    val p2 = driverStandings?.getOrNull(1)
    val titleColText = if (p1 != null && p2 != null) {
        val gap = (p1.points - p2.points).coerceAtLeast(0.0)
        val roundedGap = (gap * 10.0).roundToInt() / 10.0
        if (roundedGap <= 0.0) {
            "Level · ${p1.lastName} (0 pts)"
        } else {
            "+${formatPointsValue(roundedGap)} ${p1.lastName} leads"
        }
    } else if (p1 != null) {
        "${p1.lastName} leads"
    } else {
        "Standings pending"
    }

    // Column 3: RACE
    val raceSession = weekend?.sessions?.firstOrNull { it.isRace } ?: weekend?.sessions?.lastOrNull()
    val raceColText = if (raceSession != null) {
        "${raceSession.localTime} ${raceSession.dayOfWeek}"
    } else {
        "Race TBD"
    }

    // Bottom Headline & Subheadline
    val headline = when {
        fastestLiveDriver != null -> "${fastestLiveDriver.fullName} sets the pace in $sessionName"
        p1LiveDriver != null -> "${p1LiveDriver.fullName} leads $sessionName"
        else -> "${weekend?.grandPrixName?.takeIf { it.isNotBlank() } ?: "Grand Prix"} weekend ahead"
    }

    val flag = weekend?.countryFlagEmoji?.takeIf { it.isNotBlank() } ?: "🏁"
    val gpName = weekend?.grandPrixName?.takeIf { it.isNotBlank() } ?: "Grand Prix"
    val roundText = if (weekend != null && weekend.round > 0) "Round ${weekend.round}" else "Season"
    val subHeadline = if (liveSession != null) "$flag $gpName · $roundText · so far" else "$flag $gpName · $roundText"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 3 Bento Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val primaryAccent = LocalF1Accent.current.primaryAccent
                val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

                // Column 1: WEEKEND
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TTheme.surfaceInnerCard)
                        .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                ) {
                    // Accent top border line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(primaryAccent)
                    )
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "WEEKEND",
                            color = adaptiveAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weekendColText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Column 2: TITLE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TTheme.surfaceInnerCard)
                        .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "TITLE",
                            color = P1TTheme.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = titleColText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Column 3: RACE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TTheme.surfaceInnerCard)
                        .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "RACE",
                            color = P1TTheme.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = raceColText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Headline
            Text(
                text = headline,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subHeadline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
