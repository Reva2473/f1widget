package com.reva2473.f1widget.ui.screens.standings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.ConstructorWinItem
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.RaceRecentResult
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.getTeamCarResOrNull
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme
import kotlin.math.abs
import kotlin.math.roundToLong

enum class StandingsTab {
    DRIVERS, CONSTRUCTORS
}

private val NEUTRAL_OTHER_TEAM_COLOR = Color(0xFF8E8E99)

private fun formatStandingsPoints(points: Double): String {
    val rounded = (points * 100.0).roundToLong() / 100.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        val oneDecimal = (points * 10.0).roundToLong() / 10.0
        if (abs(rounded - oneDecimal) < 1e-6) {
            String.format(java.util.Locale.US, "%.1f", oneDecimal)
        } else {
            String.format(java.util.Locale.US, "%.2f", rounded)
        }
    }
}

@Composable
fun StandingsScreen(
    modifier: Modifier = Modifier,
    driverStandings: List<DriverStanding>? = null,
    constructorStandings: List<ConstructorStanding>? = null,
    selectedYear: Int = 2026,
    initialTab: StandingsTab = StandingsTab.DRIVERS,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    showBottomBar: Boolean = true,
    onYearSelected: (Int) -> Unit = {},
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var lastAppliedInitialTab by rememberSaveable { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        if (initialTab != lastAppliedInitialTab) {
            selectedTab = initialTab
            lastAppliedInitialTab = initialTab
        }
    }
    var yearPickerOpen by rememberSaveable { mutableStateOf(false) }

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

    // Distinguish null (preview default for 2026) vs emptyList() (no data for selectedYear)
    val activeDrivers = driverStandings ?: if (selectedYear == 2026) sampleDrivers else emptyList()
    val activeConstructors = constructorStandings ?: if (selectedYear == 2026) sampleConstructors else emptyList()
    val listBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // HEADER BAR: <, Title & Year Picker, •••
            StandingsTopBar(
                selectedTab = selectedTab,
                selectedYear = selectedYear,
                onOpenYearPicker = { yearPickerOpen = true },
                onBackClick = onBackClick,
                onToggleTab = {
                    selectedTab = if (selectedTab == StandingsTab.DRIVERS) StandingsTab.CONSTRUCTORS else StandingsTab.DRIVERS
                }
            )

            // TOGGLE PILL: "Driver Standings" / "Constructor Standings"
            StandingsToggleBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LOADING, EMPTY/ERROR, OR LIST OF CARDS
            if (isLoading) {
                StandingsLoadingCard(
                    selectedYear = selectedYear,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = listBottomPadding)
                )
            } else {
                when (selectedTab) {
                    StandingsTab.DRIVERS -> {
                        if (errorMessage != null || activeDrivers.isEmpty()) {
                            StandingsEmptyOrErrorCard(
                                selectedYear = selectedYear,
                                selectedTab = selectedTab,
                                errorMessage = errorMessage,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = listBottomPadding)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = listBottomPadding
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeDrivers, key = { "${selectedYear}_${it.driverId}_${it.position}" }) { driver ->
                                    DriverStandingCard(driver = driver, selectedYear = selectedYear)
                                }
                            }
                        }
                    }
                    StandingsTab.CONSTRUCTORS -> {
                        if (errorMessage != null || activeConstructors.isEmpty()) {
                            StandingsEmptyOrErrorCard(
                                selectedYear = selectedYear,
                                selectedTab = selectedTab,
                                errorMessage = errorMessage,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = listBottomPadding)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = listBottomPadding
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeConstructors, key = { "${selectedYear}_${it.stableKey}" }) { constructor ->
                                    ConstructorStandingCard(constructor = constructor, selectedYear = selectedYear)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Virtualized Year Picker Dialog (1950–2026)
        if (yearPickerOpen) {
            StandingsYearPickerDialog(
                selectedYear = selectedYear,
                onYearSelected = { year ->
                    yearPickerOpen = false
                    onYearSelected(year)
                },
                onDismiss = { yearPickerOpen = false }
            )
        }

        // Floating Pill Bottom Bar (only when shown standalone; MainActivity provides shared pill)
        if (showBottomBar) {
            FloatingPillBottomBar(
                currentTab = "Standings",
                onSelectTab = { tab ->
                    when (tab) {
                        "Home" -> onTabSelected("Home")
                        "Season" -> onTabSelected("Season")
                        "Standings" -> Unit
                        "Settings" -> onTabSelected("Settings")
                        else -> onTabSelected(tab)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun StandingsLoadingCard(
    selectedYear: Int,
    modifier: Modifier = Modifier
) {
    val accentColor = P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = P1TTheme.surfaceStandingCard,
            border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Loading $selectedYear standings...",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StandingsEmptyOrErrorCard(
    selectedYear: Int,
    selectedTab: StandingsTab,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val isPreConstructorsEra = selectedTab == StandingsTab.CONSTRUCTORS && selectedYear < 1958 && errorMessage == null
    val title = when {
        errorMessage != null -> "Unable to Load $selectedYear Standings"
        isPreConstructorsEra -> "Constructors Championship Unavailable"
        else -> "No $selectedYear Standings Available"
    }
    val description = when {
        errorMessage != null -> errorMessage
        isPreConstructorsEra -> "The Formula 1 Constructors Championship began in 1958. Switch to Driver Standings or choose a 1958–2026 season."
        else -> "${if (selectedTab == StandingsTab.DRIVERS) "Driver" else "Constructor"} standings for $selectedYear are unavailable offline. Connect to the internet or select another season."
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = P1TTheme.surfaceStandingCard,
            border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (errorMessage != null) P1TTheme.accentRed else P1TTheme.textMuted,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    color = P1TTheme.textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StandingsYearPickerDialog(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember { (2026 downTo 1950).toList() }
    val initialIndex = remember(selectedYear) {
        (2026 - selectedYear).coerceIn(0, years.lastIndex)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val accentColor = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(accentColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Season (1950–2026)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(years, key = { it }) { year ->
                        val isSelected = year == selectedYear
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) P1TTheme.accentPillBg(accentColor) else Color.Transparent
                                )
                                .clickable { onYearSelected(year) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$year Season",
                                color = if (isSelected) adaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = adaptiveAccent,
                                    modifier = Modifier.size(18.dp)
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
 * Header with <, "2026 ▾" year picker (1950–2026), and ••• button.
 */
@Composable
private fun StandingsTopBar(
    selectedTab: StandingsTab,
    selectedYear: Int,
    onOpenYearPicker: () -> Unit,
    onBackClick: () -> Unit,
    onToggleTab: (() -> Unit)? = null
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        // Year Selector Pill (>= 48.dp touch target)
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenYearPicker),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(P1TTheme.pillBg)
                    .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$selectedYear",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Year",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // More Options •••
        Box {
            CircularHeaderButton(onClick = { moreMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (selectedTab == StandingsTab.DRIVERS) "Switch to Constructors" else "Switch to Drivers",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        moreMenuExpanded = false
                        onToggleTab?.invoke()
                    }
                )
            }
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
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Driver Standings
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == StandingsTab.DRIVERS) P1TTheme.pillSelectedBg else Color.Transparent)
                    .clickable { onTabSelect(StandingsTab.DRIVERS) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Driver Standings",
                    color = if (selectedTab == StandingsTab.DRIVERS) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == StandingsTab.DRIVERS) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Constructor Standings
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == StandingsTab.CONSTRUCTORS) P1TTheme.pillSelectedBg else Color.Transparent)
                    .clickable { onTabSelect(StandingsTab.CONSTRUCTORS) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Constructor Standings",
                    color = if (selectedTab == StandingsTab.CONSTRUCTORS) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == StandingsTab.CONSTRUCTORS) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Unslopped Driver Standing card:
 * Graphite surface ramp: #19191B outer card, 1.dp hairline border Color.White.copy(alpha = 0.07f),
 * 4.dp left team accent bar, real DriverPortraitAvatar with headshot and position badge,
 * TeamCarShowcaseImage, crisp Material Icons in #85858B tint, and #141415 inner stats container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverStandingCard(driver: DriverStanding, selectedYear: Int) {
    var isExpanded by rememberSaveable(selectedYear, driver.driverId, driver.position) {
        mutableStateOf(driver.position <= 2)
    }
    val teamColor = if (driver.team != F1TeamId.OTHER) {
        Color(driver.team.colorHex)
    } else {
        NEUTRAL_OTHER_TEAM_COLOR
    }
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val carRes = getTeamCarResOrNull(driver.team)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = P1TTheme.surfaceStandingCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TTheme.borderStandingCard, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 4.dp Left Team Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top Row: Driver Portrait Avatar + Details, Points & Expand Icon
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Real Driver Portrait Avatar with position badge
                        DriverPortraitAvatar(
                            driverCodeOrName = driver.fullName.ifBlank { driver.code },
                            teamColor = teamColor,
                            size = 52.dp,
                            positionBadge = "P${driver.position}",
                            driverCode = driver.code.takeIf { it.isNotBlank() }
                        )

                        // Driver Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = driver.team.displayName,
                                color = adaptiveTeamColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = driver.fullName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!driver.gapToLeader.isNullOrBlank()) {
                                val gapText = buildString {
                                    append(driver.gapToLeader)
                                    if (!driver.gapToPrev.isNullOrBlank()) {
                                        append(" · ").append(driver.gapToPrev)
                                    }
                                }
                                Text(
                                    text = gapText,
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Points in team color
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatStandingsPoints(driver.points)} PTS",
                            color = adaptiveTeamColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = P1TTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Bar: Crisp Material Icons in muted tint (NO emoji strings)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Wins",
                            tint = P1TTheme.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${driver.wins} Wins",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Podiums",
                            tint = P1TTheme.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${driver.podiums} Podiums",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Poles",
                            tint = P1TTheme.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${driver.poles} Poles",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Inner Expandable Stats Container
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
                            .background(P1TTheme.surfaceInnerCard)
                            .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        // Team Car Cutout Showcase (only when mapped to a known team asset)
                        if (carRes != null) {
                            TeamCarShowcaseImage(
                                team = driver.team,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .padding(vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Avg Points Per Race
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avg points per race",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f", driver.avgPtsPerRace),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (driver.recentRaces.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Last 3 Races Summary Header
                            Text(
                                text = "Last 3 races (${driver.last3Summary})",
                                color = adaptiveTeamColor,
                                fontSize = 11.5.sp,
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
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = P1TTheme.textMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = r.grandPrixName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${r.positionText} ${r.pointsText}",
                                        color = if (r.position == 1) {
                                            P1TTheme.poleGold
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else if (driver.last3Summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Season form (${driver.last3Summary})",
                                color = adaptiveTeamColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unslopped Constructor Standing card:
 * Graphite surface ramp: #19191B outer card, 1.dp hairline border Color.White.copy(alpha = 0.07f),
 * 4.dp left team accent bar, real TeamCarShowcaseImage cutout, DriverPortraitAvatar for teammates,
 * crisp Material Icons in #85858B tint, and #141415 inner stats container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConstructorStandingCard(constructor: ConstructorStanding, selectedYear: Int) {
    var isExpanded by rememberSaveable(selectedYear, constructor.stableKey) {
        mutableStateOf(constructor.position <= 2)
    }
    val teamColor = if (constructor.team != F1TeamId.OTHER) {
        Color(constructor.team.colorHex)
    } else {
        NEUTRAL_OTHER_TEAM_COLOR
    }
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val carRes = getTeamCarResOrNull(constructor.team)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = P1TTheme.surfaceStandingCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, P1TTheme.borderStandingCard, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 4.dp Left Team Accent Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top Row: P# Badge, Constructor Name, Real Car Cutout, Points
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (constructor.position) {
                                        1 -> P1TColors.GoldTrophy
                                        2 -> P1TColors.SilverTrophy
                                        3 -> P1TColors.BronzeTrophy
                                        else -> P1TTheme.pillBg
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P${constructor.position}",
                                color = if (constructor.position <= 3) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = constructor.resolvedConstructorName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!constructor.gapToP1.isNullOrBlank()) {
                                val formattedGap = if (constructor.gapToP1.startsWith("-")) constructor.gapToP1 else "-${constructor.gapToP1}"
                                Text(
                                    text = formattedGap,
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Car Cutout and Points
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (carRes != null) {
                            TeamCarShowcaseImage(
                                team = constructor.team,
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(34.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatStandingsPoints(constructor.points)} PTS",
                                color = adaptiveTeamColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = P1TTheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (constructor.drivers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Team Drivers Strip with real DriverPortraitAvatar
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        constructor.drivers.forEach { driver ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DriverPortraitAvatar(
                                    driverCodeOrName = driver.fullName,
                                    teamColor = teamColor,
                                    size = 26.dp
                                )
                                Text(
                                    text = "${driver.fullName} (${formatStandingsPoints(driver.points)} pts)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Inner Expandable Race Wins Container
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
                            .background(P1TTheme.surfaceInnerCard)
                            .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = P1TTheme.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            val winCount = if (constructor.seasonWins.isNotEmpty()) {
                                constructor.seasonWins.size
                            } else {
                                constructor.wins
                            }
                            Text(
                                text = "Season race wins ($winCount wins)",
                                color = adaptiveTeamColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (constructor.seasonWins.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            constructor.seasonWins.forEach { win ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = P1TTheme.textMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = win.grandPrixName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = win.winnerFullName,
                                        color = adaptiveTeamColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
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
