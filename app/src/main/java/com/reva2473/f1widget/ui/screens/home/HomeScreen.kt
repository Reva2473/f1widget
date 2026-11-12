package com.reva2473.f1widget.ui.screens.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.SepangOutlineCanvas
import com.reva2473.f1widget.ui.theme.P1TColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    weekend: RaceWeekend? = null,
    liveSession: LiveSessionState? = null,
    onNavigateToLiveTiming: () -> Unit = {},
    onNavigateToStandings: () -> Unit = {},
    onNavigateToWeekend: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
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

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. TOP BAR
            HomeTopBar()

            // SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 90.dp)
            ) {
                // 2. HERO HORIZONTAL PAGER (5 slides)
                HomeHeroPager(
                    sessions = weekend?.sessions ?: defaultSessions,
                    onSlideTap = { onNavigateToWeekend() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. EXPLORE F1 STATS CARD
                ExploreF1StatsCard(onClick = onNavigateToExplore)

                Spacer(modifier = Modifier.height(18.dp))

                // 4. LIVE SESSION TRACK MAP CARD
                LiveTrackSummaryCard(
                    drivers = activeLiveDrivers,
                    onOpenLiveTiming = onNavigateToLiveTiming
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5. BENTO RACE WEEKEND SUMMARY CARD
                HomeBentoCard(onClick = onNavigateToWeekend)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // FLOATING PILL BOTTOM BAR
        FloatingPillBottomBar(
            currentTab = "Home",
            onSelectTab = { tab ->
                when (tab) {
                    "Season" -> onNavigateToWeekend()
                    "Settings", "Standings" -> onNavigateToStandings()
                    else -> onTabSelected(tab)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Top bar: Left circular green verified shield badge button, centered "Home" title, right circular notification bell button.
 */
@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shield badge button with green check
        CircularHeaderButton(onClick = {}) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Verified",
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

        // Notification Bell
        CircularHeaderButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
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
    sessions: List<SessionScheduleItem>,
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
                0 -> HeroSlideNextRace(sessions = sessions, onClick = onSlideTap)
                1 -> HeroSlideApexGridWelcome(onClick = onSlideTap)
                2 -> HeroSlideWdcLeader()
                3 -> HeroSlideWccLeader()
                4 -> HeroSlideLastRaceWinner()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pager Indicator Dots (active dot elongated into pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(if (isSelected) 22.dp else 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) P1TColors.AccentRed else Color(0xFF383844))
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
    sessions: List<SessionScheduleItem>,
    onClick: () -> Unit
) {
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
            // "ROUND 16 · IN 1 DAY"
            Text(
                text = "ROUND 16 · IN 1 DAY",
                color = P1TColors.AccentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Grand Prix Name
            Text(
                text = "Bahrain Grand Prix in Malaysia",
                color = P1TColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            // Circuit & Location
            Text(
                text = "Sepang International Circuit",
                color = P1TColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Kuala Lumpur, Malaysia",
                color = P1TColors.TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Left session schedule table beside SepangOutlineCanvas
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
                                color = if (session.isRace) P1TColors.AccentRed else P1TColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.width(42.dp)
                            )
                            Text(
                                text = "${session.dayOfWeek} ${session.localTime}",
                                color = if (session.isRace) P1TColors.AccentRed else P1TColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Sepang Outline Canvas on right
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    SepangOutlineCanvas(
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
                    .background(P1TColors.SurfaceInnerCard)
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPillColumn(label = "1 DAY", sub = "TO GO")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
                StatPillColumn(label = "5.543", sub = "KM")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
                StatPillColumn(label = "56", sub = "LAPS")
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF2E2E38)))
                StatPillColumn(label = "15", sub = "CORNERS")
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "FORMULA 1 DATA",
                color = P1TColors.AccentRed,
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

                    // Red Accent Bar
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 10.dp)
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(P1TColors.AccentRed)
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
                        drawPath(carPath, color = Color(0xFFE1251B).copy(alpha = 0.85f))
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
                    color = P1TColors.AccentRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Slide 2: WDC Leader (Kimi Antonelli 302 PTS)
 */
@Composable
private fun HeroSlideWdcLeader() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "WORLD DRIVERS' CHAMPIONSHIP",
                color = Color(F1TeamId.MERCEDES.colorHex),
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
                    Text(text = "Kimi Antonelli", color = P1TColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = "Mercedes AMG Petronas · P1", color = P1TColors.TextSecondary, fontSize = 13.sp)
                }
                Text(
                    text = "302 PTS",
                    color = Color(F1TeamId.MERCEDES.colorHex),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "🏆 8 Wins", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "📊 12 Podiums", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "⏱️ +66 Lead", color = Color(0xFF2CE580), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide 3: WCC Leader (Mercedes 538 PTS)
 */
@Composable
private fun HeroSlideWccLeader() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "CONSTRUCTORS' CHAMPIONSHIP",
                color = Color(F1TeamId.MERCEDES.colorHex),
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
                    Text(text = "Mercedes", color = P1TColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = "Antonelli & Russell", color = P1TColors.TextSecondary, fontSize = 13.sp)
                }
                Text(
                    text = "538 PTS",
                    color = Color(F1TeamId.MERCEDES.colorHex),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "🏆 11 Wins", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "🥈 Ferrari (378 PTS)", color = P1TColors.TextMuted, fontSize = 12.sp)
                Text(text = "+160 to P2", color = Color(0xFF2CE580), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide 4: Last Race Winner (Max Verstappen)
 */
@Composable
private fun HeroSlideLastRaceWinner() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "LAST GRAND PRIX WINNER",
                color = Color(F1TeamId.RED_BULL.colorHex),
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
                    Text(text = "Max Verstappen", color = P1TColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = "Red Bull Racing · São Paulo GP", color = P1TColors.TextSecondary, fontSize = 13.sp)
                }
                Text(
                    text = "2:06:54.430",
                    color = P1TColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "⚡ Fastest Lap: 1:26.993", color = P1TColors.SectorPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "26 PTS", color = Color(F1TeamId.RED_BULL.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Black)
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
            // 3 Overlapping Circular Icons
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Circle 3 (Red)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(P1TColors.AccentRed)
                        .border(2.dp, P1TColors.SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏎️", fontSize = 12.sp)
                }
                // Circle 2 (Cyan)
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00D2BE))
                        .border(2.dp, P1TColors.SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 12.sp)
                }
                // Circle 1 (Yellow)
                Box(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD100))
                        .border(2.dp, P1TColors.SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⏱️", fontSize = 12.sp)
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
                imageVector = Icons.Default.KeyboardArrowRight,
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
    drivers: List<LiveDriverTiming>,
    onOpenLiveTiming: () -> Unit
) {
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
                        text = "Q1",
                        color = P1TColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "· 9:56 left",
                        color = P1TColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Track Clear Pill + >
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TColors.TrackClearBg)
                        .border(1.dp, Color(0xFF1E5B37), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(P1TColors.SectorGreen)
                    )
                    Text(
                        text = "Track Clear",
                        color = P1TColors.SectorGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = P1TColors.SectorGreen,
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
private fun HomeBentoCard(onClick: () -> Unit) {
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
                // Column 1: WEEKEND
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(P1TColors.SurfaceInnerCard)
                        .border(1.dp, Color(0xFF24242C), RoundedCornerShape(12.dp))
                ) {
                    // Red top border accent line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(P1TColors.AccentRed)
                    )
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "WEEKEND",
                            color = P1TColors.AccentRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LEC fastest in FP2",
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
                            text = "+66 Antonelli leads",
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
                            text = "08:00 Sun",
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
                text = "Leclerc sets the pace in FP2",
                color = P1TColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🇲🇾 Bahrain GP in Malaysia · Round 16 · so far",
                color = P1TColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
