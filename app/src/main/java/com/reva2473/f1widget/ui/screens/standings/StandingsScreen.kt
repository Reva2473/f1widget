package com.reva2473.f1widget.ui.screens.standings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.ConstructorWinItem
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.RaceRecentResult
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.theme.P1TColors

enum class StandingsTab {
    DRIVERS, CONSTRUCTORS
}

@Composable
fun StandingsScreen(
    driverStandings: List<DriverStanding>? = null,
    constructorStandings: List<ConstructorStanding>? = null,
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StandingsTab.DRIVERS) }
    var selectedYear by remember { mutableIntStateOf(2026) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    // Mock/Sample Driver Standings matching driver-standings-1080.webp
    val sampleDrivers = remember {
        listOf(
            DriverStanding(
                position = 1,
                driverId = "antonelli",
                code = "ANT",
                firstName = "Kimi",
                lastName = "Antonelli",
                fullName = "Kimi Antonelli",
                nationality = "ITA",
                team = F1TeamId.MERCEDES,
                points = 302.0,
                wins = 8,
                podiums = 12,
                poles = 6,
                avgPtsPerRace = 18.88,
                racesCount = 16,
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
                nationality = "GBR",
                team = F1TeamId.MERCEDES,
                points = 236.0,
                wins = 3,
                podiums = 10,
                poles = 4,
                avgPtsPerRace = 14.75,
                racesCount = 16,
                gapToLeader = "-66 to leader",
                gapToPrev = "-66 to P1",
                last3Summary = "P3, P2, P2 (51 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P3", "(15pts)", 3),
                    RaceRecentResult("Spanish Grand Prix", "P2", "(18pts)", 2),
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
                nationality = "GBR",
                team = F1TeamId.FERRARI,
                points = 199.0,
                wins = 2,
                podiums = 7,
                poles = 2,
                avgPtsPerRace = 12.44,
                racesCount = 16,
                gapToLeader = "-103 to leader",
                gapToPrev = "-37 to P2",
                last3Summary = "P4, P3, P4 (36 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P4", "(12pts)", 4),
                    RaceRecentResult("Spanish Grand Prix", "P3", "(15pts)", 3),
                    RaceRecentResult("Italian Grand Prix", "P4", "(12pts)", 4)
                )
            ),
            DriverStanding(
                position = 4,
                driverId = "norris",
                code = "NOR",
                firstName = "Lando",
                lastName = "Norris",
                fullName = "Lando Norris",
                nationality = "GBR",
                team = F1TeamId.MCLAREN,
                points = 184.0,
                wins = 1,
                podiums = 6,
                poles = 3,
                avgPtsPerRace = 11.50,
                racesCount = 16,
                gapToLeader = "-118 to leader",
                gapToPrev = "-15 to P3",
                last3Summary = "P2, P4, P3 (45 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P2", "(18pts)", 2),
                    RaceRecentResult("Spanish Grand Prix", "P4", "(12pts)", 4),
                    RaceRecentResult("Italian Grand Prix", "P3", "(15pts)", 3)
                )
            ),
            DriverStanding(
                position = 5,
                driverId = "leclerc",
                code = "LEC",
                firstName = "Charles",
                lastName = "Leclerc",
                fullName = "Charles Leclerc",
                nationality = "MON",
                team = F1TeamId.FERRARI,
                points = 179.0,
                wins = 2,
                podiums = 5,
                poles = 4,
                avgPtsPerRace = 11.18,
                racesCount = 16,
                gapToLeader = "-123 to leader",
                gapToPrev = "-5 to P4",
                last3Summary = "P1, P5, DNF (35 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P1", "(25pts)", 1),
                    RaceRecentResult("Spanish Grand Prix", "P5", "(10pts)", 5),
                    RaceRecentResult("Italian Grand Prix", "DNF", "(0pts)", 19)
                )
            ),
            DriverStanding(
                position = 6,
                driverId = "verstappen",
                code = "VER",
                firstName = "Max",
                lastName = "Verstappen",
                fullName = "Max Verstappen",
                nationality = "NED",
                team = F1TeamId.RED_BULL,
                points = 165.0,
                wins = 3,
                podiums = 5,
                poles = 2,
                avgPtsPerRace = 10.31,
                racesCount = 16,
                gapToLeader = "-137 to leader",
                gapToPrev = "-14 to P5",
                last3Summary = "P6, P6, P5 (26 pts)",
                recentRaces = listOf(
                    RaceRecentResult("Azerbaijan Grand Prix", "P6", "(8pts)", 6),
                    RaceRecentResult("Spanish Grand Prix", "P6", "(8pts)", 6),
                    RaceRecentResult("Italian Grand Prix", "P5", "(10pts)", 5)
                )
            )
        )
    }

    // Mock/Sample Constructor Standings matching constructor-standings-1080.webp
    val sampleConstructors = remember {
        listOf(
            ConstructorStanding(
                position = 1,
                team = F1TeamId.MERCEDES,
                points = 538.0,
                wins = 11,
                gapToP1 = null,
                gapToPrev = null,
                drivers = listOf(
                    ConstructorDriverPoints("Kimi Antonelli", 302.0),
                    ConstructorDriverPoints("George Russell", 236.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Bahrain Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Saudi Arabian Grand Prix", "George Russell"),
                    ConstructorWinItem("Australian Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Japanese Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Chinese Grand Prix", "George Russell"),
                    ConstructorWinItem("Miami Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Monaco Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Canadian Grand Prix", "George Russell"),
                    ConstructorWinItem("Spanish Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Italian Grand Prix", "Kimi Antonelli"),
                    ConstructorWinItem("Singapore Grand Prix", "Kimi Antonelli")
                )
            ),
            ConstructorStanding(
                position = 2,
                team = F1TeamId.FERRARI,
                points = 378.0,
                wins = 4,
                gapToP1 = "160 to P1",
                gapToPrev = "160 to P1",
                drivers = listOf(
                    ConstructorDriverPoints("Lewis Hamilton", 199.0),
                    ConstructorDriverPoints("Charles Leclerc", 179.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Emilia Romagna Grand Prix", "Charles Leclerc"),
                    ConstructorWinItem("British Grand Prix", "Lewis Hamilton"),
                    ConstructorWinItem("Belgian Grand Prix", "Lewis Hamilton"),
                    ConstructorWinItem("Azerbaijan Grand Prix", "Charles Leclerc")
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
                    ConstructorDriverPoints("Lando Norris", 184.0),
                    ConstructorDriverPoints("Oscar Piastri", 122.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Hungarian Grand Prix", "Oscar Piastri"),
                    ConstructorWinItem("Dutch Grand Prix", "Lando Norris")
                )
            ),
            ConstructorStanding(
                position = 4,
                team = F1TeamId.RED_BULL,
                points = 210.0,
                wins = 3,
                gapToP1 = "328 to P1",
                gapToPrev = "96 to P3",
                drivers = listOf(
                    ConstructorDriverPoints("Max Verstappen", 165.0),
                    ConstructorDriverPoints("Isack Hadjar", 45.0)
                ),
                seasonWins = listOf(
                    ConstructorWinItem("Austrian Grand Prix", "Max Verstappen"),
                    ConstructorWinItem("São Paulo Grand Prix", "Max Verstappen"),
                    ConstructorWinItem("Las Vegas Grand Prix", "Max Verstappen")
                )
            )
        )
    }

    val activeDrivers = driverStandings ?: sampleDrivers
    val activeConstructors = constructorStandings ?: sampleConstructors

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HEADER BAR: <, Title & Year Picker, •••
            StandingsTopBar(
                selectedTab = selectedTab,
                selectedYear = selectedYear,
                yearDropdownExpanded = yearDropdownExpanded,
                onYearDropdownChange = { yearDropdownExpanded = it },
                onYearSelect = { selectedYear = it },
                onBackClick = onBackClick
            )

            // TOGGLE PILL: "Driver Standings" / "Constructor Standings"
            StandingsToggleBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LIST OF CARDS
            when (selectedTab) {
                StandingsTab.DRIVERS -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeDrivers, key = { it.driverId }) { driver ->
                            DriverStandingCard(driver = driver)
                        }
                        item {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
                StandingsTab.CONSTRUCTORS -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeConstructors, key = { it.team.constructorId }) { constructor ->
                            ConstructorStandingCard(constructor = constructor)
                        }
                        item {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }
        }

        // Floating Pill Bottom Bar
        FloatingPillBottomBar(
            currentTab = "Settings",
            onSelectTab = { tab ->
                when (tab) {
                    "Home" -> onBackClick()
                    "Season" -> onTabSelected("Season")
                    else -> onTabSelected(tab)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Header with <, "Driver Standings" / "Constructor Standings" title + "2026 ▾" year picker (1950–2026), and ••• button.
 */
@Composable
private fun StandingsTopBar(
    selectedTab: StandingsTab,
    selectedYear: Int,
    yearDropdownExpanded: Boolean,
    onYearDropdownChange: (Boolean) -> Unit,
    onYearSelect: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        CircularHeaderButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = P1TColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Year Selector Dropdown Pill
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color(0xFF383844), RoundedCornerShape(18.dp))
                    .clickable { onYearDropdownChange(true) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$selectedYear",
                    color = P1TColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Year",
                    tint = P1TColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = yearDropdownExpanded,
                onDismissRequest = { onYearDropdownChange(false) },
                modifier = Modifier.background(P1TColors.SurfaceElevated)
            ) {
                (2026 downTo 2018).forEach { year ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "$year",
                                color = if (year == selectedYear) P1TColors.AccentRed else P1TColors.TextPrimary,
                                fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onYearSelect(year)
                            onYearDropdownChange(false)
                        }
                    )
                }
            }
        }

        // More Options •••
        CircularHeaderButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = P1TColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Segmented Pill Tab Toggle for DRIVERS vs CONSTRUCTORS
 */
@Composable
private fun StandingsToggleBar(
    selectedTab: StandingsTab,
    onTabSelect: (StandingsTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(P1TColors.SurfaceCard)
                .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Driver Standings
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedTab == StandingsTab.DRIVERS) Color(0xFF282834) else Color.Transparent)
                    .clickable { onTabSelect(StandingsTab.DRIVERS) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Driver Standings",
                    color = if (selectedTab == StandingsTab.DRIVERS) P1TColors.TextPrimary else P1TColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == StandingsTab.DRIVERS) FontWeight.Bold else FontWeight.Medium
                )
            }

            // Constructor Standings
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selectedTab == StandingsTab.CONSTRUCTORS) Color(0xFF282834) else Color.Transparent)
                    .clickable { onTabSelect(StandingsTab.CONSTRUCTORS) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Constructor Standings",
                    color = if (selectedTab == StandingsTab.CONSTRUCTORS) P1TColors.TextPrimary else P1TColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == StandingsTab.CONSTRUCTORS) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Expandable Driver Standing card matching driver-standings-1080.webp:
 * Left team-color stripe, avatar + P1/P2/P3 pill, team name in team color, driver name, gap to leader/prev,
 * large points in team color (302 PTS), 🏆 8 Wins 📊 12 Podiums,
 * inner team-bordered box (Avg Per Race, expandable Last 3 Races with Azerbaijan P5 (10pts), etc., and Pole Positions).
 */
@Composable
private fun DriverStandingCard(driver: DriverStanding) {
    var isExpanded by remember { mutableStateOf(driver.position <= 2) }
    val teamColor = Color(driver.team.colorHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Team-Color Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (isExpanded) 220.dp else 100.dp)
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top Row: Avatar + Position Pill, Name & Team, Points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Position Pill
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (driver.position) {
                                        1 -> Color(0xFFF5C518)
                                        2 -> Color(0xFFC0C0C8)
                                        3 -> Color(0xFFD97B29)
                                        else -> Color(0xFF24242E)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P${driver.position}",
                                color = if (driver.position <= 3) Color.Black else P1TColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Driver Details
                        Column {
                            Text(
                                text = driver.team.displayName.uppercase(),
                                color = teamColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = driver.fullName,
                                color = P1TColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (driver.gapToLeader != null) {
                                Text(
                                    text = "${driver.gapToLeader} · ${driver.gapToPrev}",
                                    color = P1TColors.TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Large Points in team color
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${driver.points.toInt()} PTS",
                            color = teamColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = P1TColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Bar: 🏆 8 Wins  📊 12 Podiums
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 ${driver.wins} Wins",
                        color = P1TColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📊 ${driver.podiums} Podiums",
                        color = P1TColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⏱️ ${driver.poles} Poles",
                        color = P1TColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Inner Expandable Team-Bordered Box
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(P1TColors.SurfaceInnerCard)
                            .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        // Avg Per Race
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Avg Points Per Race",
                                color = P1TColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format("%.2f", driver.avgPtsPerRace),
                                color = P1TColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Last 3 Races Summary Header
                        Text(
                            text = "Last 3 Races (${driver.last3Summary})",
                            color = teamColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Individual Races
                        driver.recentRaces.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.grandPrixName,
                                    color = P1TColors.TextPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${r.positionText} ${r.pointsText}",
                                    color = if (r.position == 1) Color(0xFFF5C518) else P1TColors.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable Constructor Standing cards matching constructor-standings-1080.webp:
 * Mercedes 538 PTS, Ferrari 378 PTS, McLaren 306 PTS
 * with Team Drivers and expandable Race Wins (11 wins listing all 11 Grands Prix and winning driver in team color).
 */
@Composable
private fun ConstructorStandingCard(constructor: ConstructorStanding) {
    var isExpanded by remember { mutableStateOf(constructor.position <= 2) }
    val teamColor = Color(constructor.team.colorHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Team-Color Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (isExpanded) 240.dp else 100.dp)
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top Row: P1/P2/P3 Pill, Constructor Name, Points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (constructor.position) {
                                        1 -> Color(0xFFF5C518)
                                        2 -> Color(0xFFC0C0C8)
                                        3 -> Color(0xFFD97B29)
                                        else -> Color(0xFF24242E)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P${constructor.position}",
                                color = if (constructor.position <= 3) Color.Black else P1TColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column {
                            Text(
                                text = constructor.team.displayName,
                                color = P1TColors.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (constructor.gapToP1 != null) {
                                Text(
                                    text = "-${constructor.gapToP1}",
                                    color = P1TColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${constructor.points.toInt()} PTS",
                            color = teamColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = P1TColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Team Drivers Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    constructor.drivers.forEach { driver ->
                        Text(
                            text = "${driver.fullName} (${driver.points.toInt()} pts)",
                            color = P1TColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Inner Expandable Race Wins Box
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(P1TColors.SurfaceInnerCard)
                            .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Season Race Wins (${constructor.seasonWins.size} wins)",
                            color = teamColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        constructor.seasonWins.forEach { win ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 ${win.grandPrixName}",
                                    color = P1TColors.TextPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = win.winnerFullName,
                                    color = teamColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
