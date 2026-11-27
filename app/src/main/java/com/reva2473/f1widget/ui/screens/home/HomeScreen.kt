package com.reva2473.f1widget.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.ui.components.BroadcastFlagBannerType
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.CircuitOutlineCanvas
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.components.resolveBroadcastFlagType
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val HERO_MIN_HEIGHT = 248.dp

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

private fun parseLapTimeSeconds(time: String): Double? {
    val trimmed = time.trim()
    if (trimmed.isBlank()) return null
    val parts = trimmed.split(":")
    return when (parts.size) {
        1 -> parts[0].toDoubleOrNull()
        2 -> {
            val mins = parts[0].toDoubleOrNull() ?: return null
            val secs = parts[1].toDoubleOrNull() ?: return null
            mins * 60.0 + secs
        }
        3 -> {
            val hrs = parts[0].toDoubleOrNull() ?: return null
            val mins = parts[1].toDoubleOrNull() ?: return null
            val secs = parts[2].toDoubleOrNull() ?: return null
            hrs * 3600.0 + mins * 60.0 + secs
        }
        else -> null
    }
}

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
    onTabSelected: (String) -> Unit = {},
    onLastRaceTap: () -> Unit = onNavigateToWeekend,
    onArchiveTap: () -> Unit = onNavigateToStandings
) {
    val scrollState = rememberScrollState()
    val activeLiveDrivers = liveSession?.drivers.orEmpty()
    val activeWeekend = weekend ?: remember { SeedDataProvider.getNextRaceWeekend() }

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
                // 2. HERO HORIZONTAL PAGER (5 equalized slides)
                HomeHeroPager(
                    weekend = activeWeekend,
                    sessions = activeWeekend.sessions,
                    driverLeader = driverStandings?.firstOrNull(),
                    constructorLeader = constructorStandings?.firstOrNull(),
                    lastCompletedRace = lastCompletedRace,
                    lastRaceResults = effectiveLastRaceResults,
                    onSlideTap = onNavigateToWeekend,
                    onStandingsTap = onNavigateToStandings,
                    onLastRaceTap = onLastRaceTap,
                    onArchiveTap = onArchiveTap
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. EXPLORE F1 STATS CARD
                ExploreF1StatsCard(
                    driverStandings = driverStandings,
                    onClick = onNavigateToExplore
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. LIVE SESSION TRACK MAP CARD
                LiveTrackSummaryCard(
                    liveSession = liveSession,
                    weekend = activeWeekend,
                    drivers = activeLiveDrivers,
                    onOpenLiveTiming = onNavigateToLiveTiming
                )

                Spacer(modifier = Modifier.height(16.dp))

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
 * Editorial top bar: subtle ApexGrid emblem pill ("AG"), bold "ApexGrid" / "Home" title,
 * and right CircularHeaderButton for Settings.
 */
@Composable
private fun HomeTopBar(
    onNavigateToSettings: () -> Unit = {}
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subtle ApexGrid emblem pill balancing the right 48.dp Settings button
        Box(
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(P1TTheme.accentPillBg(primaryAccent))
                    .border(1.dp, primaryAccent.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                )
                Text(
                    text = "AG",
                    color = adaptiveAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Centered Editorial Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "APEXGRID",
                color = P1TTheme.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "Home",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

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
 * 5-slide HorizontalPager Hero with equalized slide heights and animated pill indicators.
 */
@Composable
private fun HomeHeroPager(
    weekend: RaceWeekend,
    sessions: List<SessionScheduleItem>,
    driverLeader: DriverStanding? = null,
    constructorLeader: ConstructorStanding? = null,
    lastCompletedRace: RaceWeekend? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onSlideTap: () -> Unit,
    onStandingsTap: () -> Unit,
    onLastRaceTap: () -> Unit = onSlideTap,
    onArchiveTap: () -> Unit = onStandingsTap
) {
    val pagerState = rememberPagerState(pageCount = { 5 })

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val fraction = 1f - pageOffset.coerceIn(0f, 1f)
            Box(
                modifier = Modifier.graphicsLayer {
                    val scale = lerp(0.95f, 1f, fraction)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(0.78f, 1f, fraction)
                }
            ) {
                when (page) {
                    0 -> HeroSlideNextRace(weekend = weekend, sessions = sessions, onClick = onSlideTap)
                    1 -> HeroSlideApexGridWelcome(onClick = onArchiveTap)
                    2 -> HeroSlideWdcLeader(leader = driverLeader, onClick = onStandingsTap)
                    3 -> HeroSlideWccLeader(leader = constructorLeader, onClick = onStandingsTap)
                    4 -> HeroSlideLastRaceWinner(
                        lastCompletedRace = lastCompletedRace,
                        lastRaceResults = lastRaceResults,
                        onClick = onLastRaceTap
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pager Indicator Dots (active dot animated into elongated pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 6.dp,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "HeroIndicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(pillWidth)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) primaryAccent else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

/**
 * Slide 0:
 * Atmospheric top pill ("NEXT GRAND PRIX · ROUND X"), countdown pill, bold GP title,
 * two-column layout (session schedule preview + glowing CircuitOutlineCanvas),
 * and crisp bottom 3-column motorsport spec bar (LENGTH | LAPS | TURNS).
 */
@Composable
private fun HeroSlideNextRace(
    weekend: RaceWeekend,
    sessions: List<SessionScheduleItem> = weekend.sessions,
    onClick: () -> Unit
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val isDark = P1TTheme.isDark

    val countdownText = when {
        weekend.countdownBadge.isNotBlank() -> weekend.countdownBadge.uppercase()
        weekend.daysToGo <= 0 -> "RACE WEEK"
        else -> "IN ${weekend.daysToGo} DAY${if (weekend.daysToGo == 1) "" else "S"}"
    }
    val topBadge = "NEXT GRAND PRIX · ROUND ${weekend.round}"

    val rawCircuitId = weekend.circuitId.ifBlank { weekend.circuitType }
    val circuitId = when {
        rawCircuitId.isNotBlank() -> rawCircuitId
        weekend.circuitName.contains("Pace", ignoreCase = true) ||
            weekend.circuitName.contains("Interlagos", ignoreCase = true) -> "INTERLAGOS"
        weekend.circuitName.contains("Yas", ignoreCase = true) -> "YAS_MARINA"
        else -> "MADRID"
    }

    val subtitleText = listOf(weekend.circuitName, weekend.location)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HERO_MIN_HEIGHT)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HERO_MIN_HEIGHT)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top atmospheric pill + countdown pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(50))
                            .background(P1TTheme.accentPillBg(primaryAccent))
                            .border(1.dp, primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = topBadge,
                            color = adaptiveAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(P1TTheme.pillBg)
                            .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = countdownText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bold GP Title
                Text(
                    text = weekend.grandPrixName.ifBlank { "Upcoming Grand Prix" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitleText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-column layout: session schedule preview on left + glowing CircuitOutlineCanvas on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 100.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.12f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (sessions.isEmpty()) {
                        Text(
                            text = "Session times pending",
                            color = P1TTheme.textMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        sessions.take(5).forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.shortName,
                                    color = if (session.isRace) adaptiveAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 40.dp)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = "${session.dayOfWeek} ${session.localTime}".trim(),
                                    color = if (session.isRace) adaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1.18f)
                        .heightIn(min = 100.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = if (isDark) 0.22f else 0.12f),
                                    Color.Transparent
                                )
                            )
                        ),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom 3-column motorsport spec bar separated by hairline dividers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(P1TTheme.surfaceInnerCard)
                    .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPillColumn(
                    label = if (weekend.circuitLengthKm.isNotBlank()) "${weekend.circuitLengthKm} KM" else "—",
                    sub = "LENGTH"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(P1TTheme.dividerSubtle)
                )
                StatPillColumn(
                    label = if (weekend.totalLaps > 0) weekend.totalLaps.toString() else "—",
                    sub = "LAPS"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(P1TTheme.dividerSubtle)
                )
                StatPillColumn(
                    label = if (weekend.totalCorners > 0) weekend.totalCorners.toString() else "—",
                    sub = "TURNS"
                )
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
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            color = P1TTheme.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

/**
 * Slide 1:
 * Formula 1 Data welcome showcase with TeamCarShowcaseImage over a subtle radial team spotlight
 * and an accessible 48.dp min-height "Join Community" pill.
 */
@Composable
private fun HeroSlideApexGridWelcome(onClick: () -> Unit) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val isDark = P1TTheme.isDark
    val context = LocalContext.current

    val accentTeam = remember(primaryAccent) {
        F1TeamId.entries.firstOrNull {
            it != F1TeamId.OTHER && Color(it.colorHex) == primaryAccent
        } ?: F1TeamId.FERRARI
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HERO_MIN_HEIGHT)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HERO_MIN_HEIGHT)
        ) {
            // Subtle radial team spotlight on the right
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = if (isDark) 0.18f else 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HERO_MIN_HEIGHT)
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(P1TTheme.accentPillBg(primaryAccent))
                            .border(1.dp, primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FORMULA 1 DATA & TELEMETRY",
                            color = adaptiveAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "ApexGrid",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Black
                            )

                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp, bottom = 9.dp)
                                    .width(36.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(primaryAccent)
                            )

                            Text(
                                text = "Explore Formula 1 standings, circuit telemetry, and race history since 1950.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryAccent.copy(alpha = if (isDark) 0.28f else 0.16f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TeamCarShowcaseImage(
                                team = accentTeam,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Join Community" Pill Button with 48.dp minimum height
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(P1TTheme.pillBg)
                        .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(24.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/r/formula1"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onClick()
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Join Community",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = adaptiveAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Slide 2: WDC Leader (Dynamically bound to driverStandings.firstOrNull(); clean pending state if null).
 */
@Composable
private fun HeroSlideWdcLeader(
    leader: DriverStanding? = null,
    onClick: () -> Unit = {}
) {
    val isDark = P1TTheme.isDark
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val teamColor = leader?.team?.let { Color(it.colorHex) } ?: primaryAccent
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HERO_MIN_HEIGHT)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HERO_MIN_HEIGHT)
        ) {
            // Giant faded #1 watermark on the right behind the portrait
            val watermarkText = if (leader != null && leader.position > 0) "#${leader.position}" else "#1"
            Text(
                text = watermarkText,
                color = if (leader != null) {
                    teamColor.copy(alpha = if (isDark) 0.14f else 0.12f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.07f)
                },
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-3).sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 16.dp)
            )

            if (leader == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HERO_MIN_HEIGHT)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "WORLD DRIVERS' CHAMPIONSHIP",
                            color = adaptiveTeamColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(teamColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Standings Pending",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Driver championship updates once official race points are scored.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(P1TTheme.surfaceInnerCard)
                                .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "— PTS",
                                color = P1TTheme.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HeroStatPill(
                            icon = Icons.Default.EmojiEvents,
                            label = "Open Standings"
                        )
                    }
                }
            } else {
                val driverName = leader.fullName.ifBlank { leader.code.ifBlank { "Championship Leader" } }
                val teamName = leader.team.displayName
                val positionText = "P${leader.position.coerceAtLeast(1)}"
                val pointsText = "${formatPointsValue(leader.points)} PTS"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HERO_MIN_HEIGHT)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WORLD DRIVERS' CHAMPIONSHIP",
                        color = adaptiveTeamColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Editorial showcase row: team accent bar + driver info on left, large portrait on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(teamColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = driverName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 21.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = teamName,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Points badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(P1TTheme.accentPillBg(teamColor))
                                    .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = pointsText,
                                    color = adaptiveTeamColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            teamColor.copy(alpha = if (isDark) 0.30f else 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            DriverPortraitAvatar(
                                driverCodeOrName = leader.fullName.ifBlank { leader.code },
                                driverCode = leader.code.takeIf { it.isNotBlank() },
                                teamColor = teamColor,
                                size = 80.dp,
                                positionBadge = positionText
                            )
                        }
                    }

                    // Bottom crisp stats pills (Wins, Podiums, Poles)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroStatPill(
                            icon = Icons.Default.EmojiEvents,
                            label = "${leader.wins} Wins"
                        )
                        if (leader.podiums > 0 || leader.poles > 0) {
                            HeroStatPill(
                                icon = Icons.Default.Leaderboard,
                                label = "${leader.podiums} Podiums"
                            )
                            HeroStatPill(
                                icon = Icons.Default.Flag,
                                label = "${leader.poles} Poles",
                                iconTint = adaptiveTeamColor
                            )
                        } else if (leader.racesCount > 0) {
                            HeroStatPill(
                                icon = Icons.Default.Flag,
                                label = "${leader.racesCount} Races"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = P1TTheme.textMuted
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(P1TTheme.surfaceInnerCard)
            .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Slide 3: WCC Leader (Dynamically bound to constructorStandings.firstOrNull(); clean pending state if null).
 */
@Composable
private fun HeroSlideWccLeader(
    leader: ConstructorStanding? = null,
    onClick: () -> Unit = {}
) {
    val isDark = P1TTheme.isDark
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val teamColor = leader?.team?.let { Color(it.colorHex) } ?: primaryAccent
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HERO_MIN_HEIGHT)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HERO_MIN_HEIGHT)
        ) {
            if (leader == null) {
                Text(
                    text = "WCC",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.05f else 0.06f),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HERO_MIN_HEIGHT)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CONSTRUCTORS' CHAMPIONSHIP",
                            color = adaptiveTeamColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Standings Pending",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Constructor classification and car showcase update after race scoring.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(P1TTheme.surfaceInnerCard)
                                .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "— PTS",
                                color = P1TTheme.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HeroStatPill(
                            icon = Icons.Default.EmojiEvents,
                            label = "Open Standings"
                        )
                    }
                }
            } else {
                val teamName = leader.resolvedConstructorName
                val watermarkLabel = leader.team.shortCode.takeIf { it.isNotBlank() && leader.team != F1TeamId.OTHER }
                    ?: teamName.take(3).uppercase()
                val driversText = if (leader.drivers.isNotEmpty()) {
                    leader.drivers.take(2).joinToString(" & ") {
                        it.fullName.substringAfterLast(" ").ifBlank { it.fullName }
                    }
                } else {
                    "Lineup Pending"
                }
                val position = leader.position.coerceAtLeast(1)
                val pointsText = "${formatPointsValue(leader.points)} PTS"
                val rawGap = leader.gapToP1?.trim()?.takeIf { it.isNotBlank() }
                val statusLabel = when {
                    position == 1 && (rawGap == null || rawGap.equals("Leader", ignoreCase = true)) ->
                        "Championship P1"
                    rawGap != null -> {
                        val formattedGap = if (rawGap.startsWith("-") || rawGap.startsWith("+")) rawGap else "-$rawGap"
                        "P$position · $formattedGap"
                    }
                    else -> "Championship P$position"
                }

                // Giant faded team watermark
                Text(
                    text = watermarkLabel,
                    color = teamColor.copy(alpha = if (isDark) 0.10f else 0.08f),
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HERO_MIN_HEIGHT)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CONSTRUCTORS' CHAMPIONSHIP",
                            color = adaptiveTeamColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = driversText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(P1TTheme.accentPillBg(teamColor))
                                    .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = pointsText,
                                    color = adaptiveTeamColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Prominent TeamCarShowcaseImage(height = 56.dp) over subtle team color glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        teamColor.copy(alpha = if (isDark) 0.24f else 0.14f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamCarShowcaseImage(
                            team = leader.team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }

                    // Bottom wins + clean Championship status / gap row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroStatPill(
                            icon = Icons.Default.EmojiEvents,
                            label = "${leader.wins} Wins"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(P1TTheme.surfaceInnerCard)
                                .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = if (position == 1) P1TTheme.sectorGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide 4: Last Race Winner (strictly bound to lastCompletedRace / explicit lastRaceResults).
 * Uses Material icons (no raw emojis), displays winner DriverPortraitAvatar + TeamCarShowcaseImage,
 * and shows the fastest lap badge only when fastestLapPosition == "P1" (or explicitly fastest valid time).
 */
@Composable
private fun HeroSlideLastRaceWinner(
    lastCompletedRace: RaceWeekend? = null,
    lastRaceResults: List<RaceDriverResult>? = null,
    onClick: () -> Unit = {}
) {
    val isDark = P1TTheme.isDark
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
                .heightIn(min = HERO_MIN_HEIGHT)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HERO_MIN_HEIGHT)
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LAST GRAND PRIX WINNER",
                        color = adaptiveAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Awaiting Race Result",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = pendingSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(P1TTheme.surfaceInnerCard)
                                .border(1.dp, P1TTheme.borderInnerCard, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = P1TTheme.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Official podium & fastest lap appear after the chequered flag",
                    color = P1TTheme.textMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    val winnerName = winner.driverFullName.ifBlank { winner.driverCode.ifBlank { "Race Winner" } }
    val winnerTeam = winner.team
    val raceName = lastCompletedRace?.grandPrixName?.takeIf { it.isNotBlank() }.orEmpty()
    val teamSubtitle = if (raceName.isNotBlank()) {
        "${winnerTeam.displayName} · $raceName"
    } else {
        winnerTeam.displayName
    }
    val finishTime = winner.finishTimeOrGap.ifBlank { "P${winner.position}" }

    val explicitFastest = results.firstOrNull {
        (it.fastestLapPosition.equals("P1", ignoreCase = true) ||
            it.fastestLapPosition.equals("1", ignoreCase = true)) &&
            it.fastestLapTime.isNotBlank()
    }
    val fastestLapResult = explicitFastest ?: if (results.none { it.fastestLapPosition.isNotBlank() }) {
        results.filter { it.fastestLapTime.isNotBlank() }
            .minByOrNull { parseLapTimeSeconds(it.fastestLapTime) ?: Double.MAX_VALUE }
    } else {
        null
    }
    val fastestLapTime = fastestLapResult?.fastestLapTime?.takeIf { it.isNotBlank() }
    val pointsText = "${winner.formattedPoints} PTS"
    val teamColor = Color(winnerTeam.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HERO_MIN_HEIGHT)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HERO_MIN_HEIGHT)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row: label + finish time pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAST GRAND PRIX WINNER",
                    color = adaptiveTeamColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(P1TTheme.surfaceInnerCard)
                        .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = finishTime,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            // Winner portrait + name/subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DriverPortraitAvatar(
                    driverCodeOrName = winner.driverFullName.ifBlank { winner.driverCode },
                    driverCode = winner.driverCode.takeIf { it.isNotBlank() },
                    teamColor = teamColor,
                    size = 54.dp,
                    positionBadge = "P${winner.position.coerceAtLeast(1)}"
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = winnerName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = teamSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Winning constructor car showcase over subtle glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                teamColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                TeamCarShowcaseImage(
                    team = winnerTeam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }

            // Bottom row: Fastest Lap (Bolt icon) / Laps completed (EmojiEvents icon) + Points pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fastestLapTime != null) {
                    val codeSuffix = fastestLapResult.driverCode.takeIf {
                        it.isNotBlank() && !it.equals(winner.driverCode, ignoreCase = true)
                    }?.let { " ($it)" }.orEmpty()
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(10.dp))
                            .background(P1TTheme.sectorPurple.copy(alpha = if (isDark) 0.16f else 0.10f))
                            .border(1.dp, P1TTheme.sectorPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Fastest Lap",
                            tint = P1TTheme.sectorPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Fastest Lap: $fastestLapTime$codeSuffix",
                            color = P1TTheme.sectorPurple,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    HeroStatPill(
                        icon = Icons.Default.EmojiEvents,
                        label = if (winner.totalLaps > 0) "${winner.totalLaps} Laps completed" else "Race Winner",
                        iconTint = adaptiveTeamColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(P1TTheme.accentPillBg(teamColor))
                        .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = pointsText,
                        color = adaptiveTeamColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 3. "Explore F1 Stats" card (Results, standings and records since 1950)
 * Dynamically renders the top 3 drivers from driverStandings, or a clean icon fallback if empty.
 */
@Composable
private fun ExploreF1StatsCard(
    driverStandings: List<DriverStanding>? = null,
    onClick: () -> Unit
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val topThree = driverStandings.orEmpty().take(3)

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
            if (topThree.isNotEmpty()) {
                val stackWidth = (28 + (topThree.size - 1) * 16).dp
                Box(
                    modifier = Modifier
                        .width(stackWidth)
                        .height(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    topThree.forEachIndexed { index, driver ->
                        Box(
                            modifier = Modifier
                                .padding(start = (index * 16).dp)
                                .size(28.dp)
                        ) {
                            DriverPortraitAvatar(
                                driverCodeOrName = driver.fullName.ifBlank { driver.code },
                                driverCode = driver.code.takeIf { it.isNotBlank() },
                                teamColor = Color(driver.team.colorHex),
                                size = 28.dp
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(P1TTheme.accentPillBg(primaryAccent))
                        .border(1.dp, primaryAccent.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = adaptiveAccent,
                        modifier = Modifier.size(18.dp)
                    )
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
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
 * Uncrowded LIVE / REPLAY / STANDBY header row, compact broadcast status strip,
 * top-3 live timing rows with vertical team stripe, 3-letter code, tabular monospace gap,
 * and TyreCompoundBadge beside LiveTrackMapCanvas (heightIn(min = 148.dp)).
 */
@Composable
private fun LiveTrackSummaryCard(
    liveSession: LiveSessionState? = null,
    weekend: RaceWeekend? = null,
    drivers: List<LiveDriverTiming> = liveSession?.drivers.orEmpty(),
    onOpenLiveTiming: () -> Unit
) {
    val isActuallyLive = liveSession?.isLive == true
    val hasSessionData = liveSession != null
    val sessionBadge = liveSession?.sessionType?.takeIf { it.isNotBlank() }.orEmpty()
    val timeRemaining = liveSession?.timeRemaining?.trim().orEmpty()
    val timeRemainingText = when {
        !hasSessionData || timeRemaining.isBlank() -> ""
        timeRemaining.contains("left", ignoreCase = true) ||
            timeRemaining.equals("Finished", ignoreCase = true) -> "· $timeRemaining"
        else -> "· $timeRemaining left"
    }
    val trackStatus = if (hasSessionData) {
        liveSession?.trackStatus?.ifBlank { "Track Clear" } ?: "Track Clear"
    } else {
        "Standby"
    }

    val flagType = if (hasSessionData) {
        resolveBroadcastFlagType(trackStatus)
    } else {
        BroadcastFlagBannerType.NEUTRAL
    }

    val (stripBg, stripFg, stripIcon) = when (flagType) {
        BroadcastFlagBannerType.GREEN_FLAG ->
            Triple(P1TColors.GreenFlagBg, Color(0xFF07160F), Icons.Default.Flag)
        BroadcastFlagBannerType.SAFETY_CAR ->
            Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Default.DirectionsCar)
        BroadcastFlagBannerType.VSC ->
            Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Default.Warning)
        BroadcastFlagBannerType.RED_FLAG ->
            Triple(P1TColors.RedFlagSolidBg, Color.White, Icons.Default.NotInterested)
        BroadcastFlagBannerType.FASTEST_LAP ->
            Triple(P1TColors.FastestLapPurple, Color.White, Icons.Default.Bolt)
        BroadcastFlagBannerType.NEUTRAL ->
            Triple(P1TTheme.surfaceInnerCard, P1TTheme.textMuted, Icons.Default.Flag)
    }

    val liveDotAlpha = if (isActuallyLive) {
        val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveDotAlpha"
        )
        alpha
    } else {
        1.0f
    }

    val circuitId = liveSession?.circuitId?.ifBlank { null }
        ?: weekend?.circuitId?.ifBlank { weekend.circuitType }?.ifBlank { null }
        ?: "MADRID"

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
            // Uncrowded Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusBadgeLabel = when {
                        isActuallyLive -> "LIVE"
                        hasSessionData -> "SESSION REPLAY"
                        else -> "NO LIVE SESSION"
                    }
                    val statusBadgeColor = if (isActuallyLive) P1TTheme.accentRed else P1TTheme.textMuted

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(liveDotAlpha)
                            .clip(CircleShape)
                            .background(statusBadgeColor)
                    )
                    Text(
                        text = statusBadgeLabel,
                        color = statusBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sessionBadge.isNotBlank()) {
                        Text(
                            text = "·",
                            color = P1TTheme.textMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = sessionBadge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (timeRemainingText.isNotBlank()) {
                        Text(
                            text = timeRemainingText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Live Timing",
                    tint = P1TTheme.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Broadcast Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(stripBg)
                    .then(
                        if (flagType == BroadcastFlagBannerType.NEUTRAL) {
                            Modifier.border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(10.dp))
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = stripIcon,
                        contentDescription = null,
                        tint = stripFg,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = trackStatus.uppercase(),
                        color = stripFg,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val trailingTelemetry = when {
                    hasSessionData && liveSession.carsOnTrack > 0 ->
                        "${liveSession.carsOnTrack} ON TRACK"
                    hasSessionData -> "TELEMETRY"
                    else -> "STANDBY"
                }
                Text(
                    text = trailingTelemetry,
                    color = stripFg.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Left top 3 column | Right LiveTrackMapCanvas (expands safely at large font scales)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 148.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (drivers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1.22f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = weekend?.grandPrixName?.takeIf { it.isNotBlank() } ?: "Track Telemetry",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No live timing session active. Tap to open Live Timing.",
                            color = P1TTheme.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1.22f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        drivers.take(3).forEach { d ->
                            val gapDisplay = if (d.position == 1) {
                                "Leader"
                            } else {
                                d.gapToLeader.takeIf { it.isNotBlank() } ?: "—"
                            }
                            val gapColor = if (d.position == 1) {
                                P1TTheme.sectorPurple
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position
                                Text(
                                    text = "${d.position}",
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(16.dp)
                                )
                                // Vertical Team Stripe
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color(d.team.colorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // 3-letter Driver Code
                                Text(
                                    text = d.code,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    modifier = Modifier.width(34.dp)
                                )
                                // Tabular Monospace Gap ("Leader" / "+0.785")
                                Text(
                                    text = gapDisplay,
                                    color = gapColor,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontFeatureSettings = "tnum",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (d.position == 1) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp)
                                )
                                // Tyre Compound Badge
                                TyreCompoundBadge(
                                    compound = d.tyreCompound,
                                    sizeDp = 18
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Live Track Map Canvas
                Box(
                    modifier = Modifier
                        .weight(1.18f)
                        .heightIn(min = 148.dp)
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
 * Aligned WEEKEND | TITLE | RACE tiles (non-overlapping top accent strip) +
 * bottom headline & Material-icon subheadline (zero raw emojis).
 */
@Composable
private fun HomeBentoCard(
    weekend: RaceWeekend? = null,
    driverStandings: List<DriverStanding>? = null,
    liveSession: LiveSessionState? = null,
    onClick: () -> Unit = {}
) {
    val weekendGpName = weekend?.grandPrixName?.trim().orEmpty()
    val liveGpName = liveSession?.grandPrixName?.trim().orEmpty()
    val isSameWeekendLive = liveSession != null && (
        liveGpName.isBlank() ||
            weekendGpName.isBlank() ||
            liveGpName.equals(weekendGpName, ignoreCase = true) ||
            liveGpName.contains(weekendGpName, ignoreCase = true) ||
            weekendGpName.contains(liveGpName, ignoreCase = true)
        )

    // Column 1: WEEKEND — only use liveSession timing when it belongs to the active weekend
    val fastestLiveDriver = if (isSameWeekendLive) {
        liveSession?.drivers?.firstOrNull { it.isOverallBest }
            ?: liveSession?.bestLapDriverCode?.takeIf { it.isNotBlank() }?.let { code ->
                liveSession.drivers.firstOrNull { it.code.equals(code, ignoreCase = true) }
            }
    } else {
        null
    }
    val p1LiveDriver = if (isSameWeekendLive) {
        liveSession?.drivers?.minByOrNull { it.position }
    } else {
        null
    }
    val sessionName = liveSession?.shortSessionCode?.ifBlank { liveSession.sessionName }?.ifBlank { "Session" } ?: "Session"
    val weekendColText = when {
        fastestLiveDriver != null -> "${fastestLiveDriver.code} fastest in $sessionName"
        p1LiveDriver != null -> "${p1LiveDriver.code} leads $sessionName"
        weekend != null && weekend.daysToGo > 0 ->
            "In ${weekend.daysToGo} day${if (weekend.daysToGo == 1) "" else "s"}"
        else -> "Weekend schedule ready"
    }

    // Column 2: TITLE
    val p1 = driverStandings?.getOrNull(0)
    val p2 = driverStandings?.getOrNull(1)
    val titleColText = if (p1 != null && p2 != null) {
        val gap = (p1.points - p2.points).coerceAtLeast(0.0)
        val roundedGap = (gap * 10.0).roundToInt() / 10.0
        if (roundedGap <= 0.0) {
            "Tied · ${formatPointsValue(p1.points)} pts"
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
        "${raceSession.localTime} ${raceSession.dayOfWeek}".trim()
    } else {
        "Race TBD"
    }

    // Bottom Headline & Subheadline (clean text without raw emojis)
    val gpName = weekendGpName.ifBlank { "Grand Prix" }
    val headline = when {
        fastestLiveDriver != null -> "${fastestLiveDriver.fullName} sets the pace in $sessionName"
        p1LiveDriver != null -> "${p1LiveDriver.fullName} leads $sessionName"
        else -> "$gpName weekend ahead"
    }

    val roundText = if (weekend != null && weekend.round > 0) "Round ${weekend.round}" else "Season"
    val subHeadline = when {
        isSameWeekendLive && liveSession?.isLive == true -> "$gpName · $roundText · Live"
        isSameWeekendLive -> "$gpName · $roundText · Session replay"
        !weekend?.dateRangeText.isNullOrBlank() -> "$gpName · $roundText · ${weekend?.dateRangeText}"
        else -> "$gpName · $roundText"
    }

    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

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
            // 3 Aligned Bento Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoSummaryTile(
                    title = "WEEKEND",
                    value = weekendColText,
                    titleColor = adaptiveAccent,
                    topStripColor = primaryAccent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                BentoSummaryTile(
                    title = "TITLE",
                    value = titleColText,
                    titleColor = P1TTheme.textMuted,
                    topStripColor = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                BentoSummaryTile(
                    title = "RACE",
                    value = raceColText,
                    titleColor = P1TTheme.textMuted,
                    topStripColor = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Headline
            Text(
                text = headline,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = adaptiveAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = subHeadline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BentoSummaryTile(
    title: String,
    value: String,
    titleColor: Color,
    topStripColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(P1TTheme.surfaceInnerCard)
            .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
    ) {
        // Dedicated non-overlapping top accent bar slot keeps all 3 tiles baseline-aligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(topStripColor)
        )
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
