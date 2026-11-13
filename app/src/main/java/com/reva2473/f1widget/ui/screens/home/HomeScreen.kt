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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.reva2473.f1widget.ui.components.SepangOutlineCanvas
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.theme.P1TColors

private val RaceWeekend.countdownBadge: String
    get() = sessions.firstOrNull { it.isRace }?.countdownBadge ?: ""

private val LiveSessionState.sessionType: String
    get() = shortSessionCode.ifBlank { sessionName }

private val LiveSessionState.trackStatus: String
    get() = trackStatusText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    weekend: RaceWeekend? = null,
    liveSession: LiveSessionState? = null,
    driverStandings: List<DriverStanding>? = null,
    constructorStandings: List<ConstructorStanding>? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onNavigateToLiveTiming: () -> Unit = {},
    onNavigateToStandings: () -> Unit = {},
    onNavigateToWeekend: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Default sample data matching home-1080.webp if not provided
    val defaultSessions = listOf(
        SessionScheduleItem("FP1", "Free Practice 1", "Fri", "Friday", "06", "Nov", "05:30", "38 days"),
        SessionScheduleItem("FP2", "Free Practice 2", "Fri", "Friday", "06", "Nov", "09:00", "38 days"),
        SessionScheduleItem("FP3", "Free Practice 3", "Sat", "Saturday", "07", "Nov", "05:30", "37 days"),
        SessionScheduleItem("Quali", "Qualifying", "Sat", "Saturday", "07", "Nov", "09:00", "37 days"),
        SessionScheduleItem("Race", "Race", "Sun", "Sunday", "08", "Nov", "08:00", "Race in 1 day", isRace = true)
    )

    val sampleLiveDrivers = listOf(
        LiveDriverTiming(1, "VER", "Max Verstappen", F1TeamId.RED_BULL, "1:36.477", "1:36.477", "Leader", "Leader", trackProgress = 0.88f),
        LiveDriverTiming(2, "HAD", "Isack Hadjar", F1TeamId.RED_BULL, "1:36.898", "1:36.898", "+0.421", "+0.421", trackProgress = 0.52f),
        LiveDriverTiming(3, "ANT", "Kimi Antonelli", F1TeamId.MERCEDES, "1:37.041", "1:37.041", "+0.564", "+0.143", trackProgress = 0.35f),
        LiveDriverTiming(4, "LEC", "Charles Leclerc", F1TeamId.FERRARI, "1:37.120", "1:37.120", "+0.643", "+0.079", trackProgress = 0.22f),
        LiveDriverTiming(5, "HAM", "Lewis Hamilton", F1TeamId.FERRARI, "1:37.195", "1:37.195", "+0.718", "+0.075", trackProgress = 0.72f),
        LiveDriverTiming(6, "NOR", "Lando Norris", F1TeamId.MCLAREN, "1:37.310", "1:37.310", "+0.833", "+0.115", trackProgress = 0.12f)
    )

    val activeLiveDrivers = liveSession?.drivers ?: sampleLiveDrivers

    val defaultWeekend = RaceWeekend(
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
    val activeWeekend = weekend ?: defaultWeekend

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
                    .padding(bottom = 90.dp)
            ) {
                // 2. HERO HORIZONTAL PAGER (5 slides)
                HomeHeroPager(
                    weekend = activeWeekend,
                    sessions = activeWeekend.sessions,
                    driverLeader = driverStandings?.firstOrNull(),
                    constructorLeader = constructorStandings?.firstOrNull(),
                    driverStandings = driverStandings,
                    liveSession = liveSession,
                    lastRaceResults = lastRaceResults ?: activeWeekend.results,
                    onSlideTap = { onNavigateToWeekend() }
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
                    lastRaceResults = lastRaceResults ?: activeWeekend.results,
                    onClick = onNavigateToWeekend
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

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
                    tint = Color(0xFF2CE580),
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
                color = P1TColors.TextPrimary
            )
        )

        // Settings Gear Icon
        CircularHeaderButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFFF5F5F7),
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
    driverStandings: List<DriverStanding>? = null,
    liveSession: LiveSessionState? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onSlideTap: () -> Unit
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
                2 -> HeroSlideWdcLeader(leader = driverLeader, onClick = onSlideTap)
                3 -> HeroSlideWccLeader(leader = constructorLeader, onClick = onSlideTap)
                4 -> HeroSlideLastRaceWinner(
                    weekend = weekend,
                    driverStandings = driverStandings,
                    liveSession = liveSession,
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
                        .background(if (isSelected) primaryAccent else Color(0xFF383844))
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
    val circuitId = weekend.circuitId.ifBlank { weekend.circuitType }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent

            // "ROUND X · IN Y DAYS"
            Text(
                text = topBadge,
                color = primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Grand Prix Name
            Text(
                text = weekend.grandPrixName,
                color = P1TColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            // Circuit & Location
            Text(
                text = weekend.circuitName,
                color = P1TColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = weekend.location,
                color = P1TColors.TextMuted,
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
                        val primaryAccent = LocalF1Accent.current.primaryAccent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.shortName,
                                color = if (session.isRace) primaryAccent else P1TColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.width(42.dp)
                            )
                            Text(
                                text = "${session.dayOfWeek} ${session.localTime}",
                                color = if (session.isRace) primaryAccent else P1TColors.TextPrimary,
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
                    when {
                        circuitId.contains("INTERLAGOS", ignoreCase = true) ||
                            weekend.circuitName.contains("Pace", ignoreCase = true) -> {
                            CircuitOutlineCanvas(
                                circuitType = "INTERLAGOS",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                        circuitId.contains("YAS", ignoreCase = true) ||
                            weekend.circuitName.contains("Yas", ignoreCase = true) -> {
                            CircuitOutlineCanvas(
                                circuitType = "YAS_MARINA",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                        else -> {
                            SepangOutlineCanvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom 4-column stat strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(P1TColors.SurfaceInnerCard)
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPillColumn(
                    label = weekend.daysToGo.toString(),
                    sub = if (weekend.daysToGo == 1) "DAY TO GO" else "DAYS TO GO"
                )
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
                StatPillColumn(label = weekend.circuitLengthKm, sub = "KM")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
                StatPillColumn(label = weekend.totalLaps.toString(), sub = "LAPS")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
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
            color = P1TColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = sub,
            color = P1TColors.TextMuted,
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
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        val primaryAccent = LocalF1Accent.current.primaryAccent
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "FORMULA 1 DATA",
                color = primaryAccent,
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
                        color = P1TColors.TextPrimary,
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
                        color = P1TColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                // F1 Car Graphic Icon Representation
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
                        val carPath = Path().apply {
                            moveTo(w * 0.1f, h * 0.6f)
                            lineTo(w * 0.3f, h * 0.55f)
                            lineTo(w * 0.45f, h * 0.35f)
                            lineTo(w * 0.7f, h * 0.35f)
                            lineTo(w * 0.85f, h * 0.5f)
                            lineTo(w * 0.95f, h * 0.52f)
                            lineTo(w * 0.95f, h * 0.65f)
                            lineTo(w * 0.75f, h * 0.65f)
                            lineTo(w * 0.65f, h * 0.7f)
                            lineTo(w * 0.35f, h * 0.7f)
                            lineTo(w * 0.25f, h * 0.65f)
                            close()
                        }
                        drawPath(carPath, color = primaryAccent.copy(alpha = 0.85f))
                        // Wheels
                        drawCircle(Color(0xFF2A2A34), radius = 10.dp.toPx(), center = Offset(w * 0.3f, h * 0.68f))
                        drawCircle(Color(0xFF2A2A34), radius = 10.dp.toPx(), center = Offset(w * 0.8f, h * 0.68f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Join Community ↗" Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF22222A))
                    .border(1.dp, Color(0xFF383844), RoundedCornerShape(20.dp))
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
                    color = P1TColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "↗",
                    color = primaryAccent,
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
    val driverName = leader?.fullName ?: "Kimi Antonelli"
    val teamName = leader?.team?.displayName ?: "Mercedes AMG Petronas"
    val positionText = if (leader != null) "P${leader.position}" else "P1"
    val pointsText = if (leader != null) "${leader.points.toInt()} PTS" else "302 PTS"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "WORLD DRIVERS' CHAMPIONSHIP",
                color = teamColor,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DriverPortraitAvatar(
                        driverCodeOrName = leader?.code ?: "ANT",
                        teamColor = teamColor,
                        size = 50.dp,
                        positionBadge = "P1"
                    )
                    Column {
                        Text(text = driverName, color = P1TColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "$teamName · $positionText", color = P1TColors.TextSecondary, fontSize = 12.5.sp)
                    }
                }
                Text(
                    text = pointsText,
                    color = teamColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF85858B), modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.wins ?: 8} Wins", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = Color(0xFF85858B), modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.podiums ?: 12} Podiums", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF85858B), modifier = Modifier.size(15.dp))
                    Text(text = if (leader != null) "${leader.poles} Poles" else "+66 Lead", color = Color(0xFF2CE580), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    val teamName = leader?.team?.displayName ?: "Mercedes"
    val driversText = if (leader != null && leader.drivers.isNotEmpty()) {
        leader.drivers.joinToString(" & ") { it.fullName.substringAfterLast(" ") }
    } else {
        "Antonelli & Russell"
    }
    val pointsText = if (leader != null) "${leader.points.toInt()} PTS" else "538 PTS"
    val gapText = if (leader != null && leader.gapToP1 != null) "-${leader.gapToP1}" else "P1 Leader"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "CONSTRUCTORS' CHAMPIONSHIP",
                color = teamColor,
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
                    Text(text = teamName, color = P1TColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = driversText, color = P1TColors.TextSecondary, fontSize = 13.sp)
                }
                Text(
                    text = pointsText,
                    color = teamColor,
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
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF85858B), modifier = Modifier.size(15.dp))
                    Text(text = "${leader?.wins ?: 11} Wins", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(text = gapText, color = P1TColors.TextMuted, fontSize = 12.sp)
                Text(text = "Championship P1", color = Color(0xFF2CE580), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide 4: Last Race Winner
 */
@Composable
private fun HeroSlideLastRaceWinner(
    weekend: RaceWeekend? = null,
    driverStandings: List<DriverStanding>? = null,
    liveSession: LiveSessionState? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onClick: () -> Unit = {}
) {
    val results = lastRaceResults?.ifEmpty { null } ?: weekend?.results.orEmpty()
    val winner = results.firstOrNull { it.position == 1 } ?: results.firstOrNull()

    val winnerName = winner?.driverFullName ?: "Max Verstappen"
    val winnerTeam = winner?.team ?: F1TeamId.RED_BULL
    val raceName = if (weekend != null && weekend.grandPrixName.isNotBlank()) {
        weekend.grandPrixName
    } else {
        "São Paulo GP"
    }
    val teamSubtitle = "${winnerTeam.displayName} · $raceName"
    val finishTime = winner?.finishTimeOrGap?.ifBlank { "2:06:54.430" } ?: "2:06:54.430"
    val fastestLapTime = winner?.fastestLapTime?.ifBlank { "1:26.993" } ?: "1:26.993"
    val pointsText = "${winner?.points ?: 26} PTS"
    val teamColor = Color(winnerTeam.colorHex)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "LAST GRAND PRIX WINNER",
                color = teamColor,
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
                Column {
                    Text(
                        text = winnerName,
                        color = P1TColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = teamSubtitle,
                        color = P1TColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = finishTime,
                    color = P1TColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "⚡ Fastest Lap: $fastestLapTime",
                    color = P1TColors.SectorPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pointsText,
                    color = teamColor,
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
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(18.dp))
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
                    DriverPortraitAvatar("VER", Color(0xFF3671C6), size = 28.dp)
                }
                // Driver 2 (HAM)
                Box(modifier = Modifier.padding(start = 16.dp).size(28.dp)) {
                    DriverPortraitAvatar("HAM", Color(0xFFE8002D), size = 28.dp)
                }
                // Driver 3 (NOR)
                Box(modifier = Modifier.padding(start = 32.dp).size(28.dp)) {
                    DriverPortraitAvatar("NOR", Color(0xFFFF8000), size = 28.dp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Explore F1 Stats",
                    color = P1TColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Results, standings and records since 1950",
                    color = P1TColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Chevron >
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Explore",
                tint = P1TColors.TextMuted,
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
    val isClear = trackStatus.contains("Clear", ignoreCase = true)
    val statusColor = if (isClear) P1TColors.SectorGreen else P1TColors.SectorYellow
    val statusBg = if (isClear) P1TColors.TrackClearBg else Color(0xFF2E2A14)
    val statusBorder = if (isClear) Color(0xFF1E5B37) else Color(0xFF6B5A18)

    val circuitId = liveSession?.circuitId?.ifBlank {
        weekend?.circuitId?.ifBlank { "INTERLAGOS" } ?: "INTERLAGOS"
    } ?: weekend?.circuitId?.ifBlank { "INTERLAGOS" } ?: "INTERLAGOS"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
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
                            .background(P1TColors.AccentRed)
                    )
                    Text(
                        text = "LIVE",
                        color = P1TColors.AccentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "·",
                        color = P1TColors.TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = sessionBadge,
                        color = P1TColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = timeRemainingText,
                        color = P1TColors.TextSecondary,
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
                                color = P1TColors.TextMuted,
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
                                color = P1TColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(36.dp)
                            )
                            // Time / Gap
                            Text(
                                text = if (d.position == 1) d.bestLapTime else d.gapToLeader,
                                color = if (d.position == 1) P1TColors.SectorPurple else P1TColors.TextSecondary,
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
    lastRaceResults: List<RaceDriverResult>? = null,
    onClick: () -> Unit = {}
) {
    // Column 1: WEEKEND
    val fastestLiveDriver = liveSession?.drivers?.minByOrNull { it.position }
    val sessionName = liveSession?.shortSessionCode?.ifBlank { liveSession.sessionName }?.ifBlank { "FP2" } ?: "FP2"
    val weekendColText = if (fastestLiveDriver != null) {
        "${fastestLiveDriver.code} fastest in $sessionName"
    } else {
        "LEC fastest in FP2"
    }

    // Column 2: TITLE (e.g. P1 leader gap over P2: +${(p1.points - p2.points).toInt()} and "${p1.lastName} leads")
    val p1 = driverStandings?.getOrNull(0)
    val p2 = driverStandings?.getOrNull(1)
    val titleColText = if (p1 != null && p2 != null) {
        val gap = (p1.points - p2.points).toInt()
        "+$gap ${p1.lastName} leads"
    } else if (p1 != null) {
        "${p1.lastName} leads"
    } else {
        "+66 Antonelli leads"
    }

    // Column 3: RACE
    val raceSession = weekend?.sessions?.firstOrNull { it.isRace } ?: weekend?.sessions?.lastOrNull()
    val raceColText = if (raceSession != null) {
        "${raceSession.localTime} ${raceSession.dayOfWeek}"
    } else {
        "08:00 Sun"
    }

    // Bottom Headline & Subheadline
    val headline = if (fastestLiveDriver != null) {
        "${fastestLiveDriver.fullName} sets the pace in $sessionName"
    } else {
        "Leclerc sets the pace in FP2"
    }

    val flag = weekend?.countryFlagEmoji?.takeIf { it.isNotBlank() } ?: "🇲🇾"
    val gpName = weekend?.grandPrixName?.takeIf { it.isNotBlank() } ?: "Bahrain GP in Malaysia"
    val roundText = if (weekend != null && weekend.round > 0) "Round ${weekend.round}" else "Round 16"
    val subHeadline = "$flag $gpName · $roundText · so far"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
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

                // Column 1: WEEKEND
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TColors.SurfaceInnerCard)
                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
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
                            color = primaryAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weekendColText,
                            color = P1TColors.TextPrimary,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TColors.SurfaceInnerCard)
                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "TITLE",
                            color = P1TColors.TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = titleColText,
                            color = P1TColors.TextPrimary,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TColors.SurfaceInnerCard)
                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "RACE",
                            color = P1TColors.TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = raceColText,
                            color = P1TColors.TextPrimary,
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
                color = P1TColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subHeadline,
                color = P1TColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
