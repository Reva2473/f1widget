package com.reva2473.f1widget.ui.screens.drivers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme
import java.util.Locale

private fun formatCleanNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val formatted = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return if (formatted == "-0") "0" else formatted
}

private fun formatPercent(value: Double): String {
    val safe = if (value.isNaN() || value.isInfinite()) 0.0 else value.coerceAtLeast(0.0)
    return String.format(Locale.US, "%.1f%%", safe)
}

/**
 * Screen 1: DriverProfilesScreen
 * Exact 1:1 replica of driver-profiles-720.webp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfilesScreen(
    drivers: List<DriverStanding>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onDriverSelected: (DriverStanding) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showYearDialog by remember { mutableStateOf(false) }

    val yearsList = listOf(2026, 2025, 2024, 2023, 2022)

    val filteredDrivers = remember(drivers, searchQuery) {
        if (searchQuery.isBlank()) {
            drivers
        } else {
            val q = searchQuery.trim().lowercase()
            drivers.filter {
                it.fullName.lowercase().contains(q) ||
                        it.code.lowercase().contains(q) ||
                        it.team.displayName.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Driver Profiles",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCompare) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Compare Drivers",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search all drivers",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                )
            }

            // Browse by Year Header
            item {
                Text(
                    text = "Browse by Year",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }

            // Horizontal Year Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    yearsList.forEach { yr ->
                        val isSelected = yr == selectedYear
                        val label = if (yr == 2026) "2026 (Current)" else yr.toString()
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onYearSelected(yr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) P1TTheme.pillSelectedBg else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent))
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Red interactive row: "📅 View all years (1950-present)"
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(top = 2.dp)
                        .clickable { showYearDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = P1TTheme.accentRed,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View all years (1950-present)",
                                color = P1TTheme.accentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open archive",
                            tint = P1TTheme.accentRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // List Header: "2026 Drivers" and "23 drivers" count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedYear Drivers",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredDrivers.size} drivers",
                        color = P1TTheme.textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Drivers List Rows or Empty States
            if (drivers.isEmpty()) {
                item {
                    EmptyDriversStateCard(
                        title = "No driver data for $selectedYear",
                        subtitle = "Driver standings for $selectedYear are unavailable offline. Connect to the internet or select another season.",
                        actionLabel = if (selectedYear != 2026) "Switch to 2026" else null,
                        onAction = { onYearSelected(2026) }
                    )
                }
            } else if (filteredDrivers.isEmpty()) {
                item {
                    EmptyDriversStateCard(
                        title = "No matching drivers",
                        subtitle = "No drivers matched \"${searchQuery.trim()}\" in the $selectedYear season.",
                        actionLabel = "Clear search",
                        onAction = { searchQuery = "" }
                    )
                }
            } else {
                items(filteredDrivers, key = { "${selectedYear}_${it.driverId}_${it.position}" }) { driver ->
                    DriverRowCard(
                        driver = driver,
                        onClick = { onDriverSelected(driver) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
            }
        }
    }

    // 1950 - 2026 Archive Dialog
    if (showYearDialog) {
        YearSelectionDialog(
            currentYear = selectedYear,
            onSelectYear = { yr ->
                onYearSelected(yr)
                showYearDialog = false
            },
            onDismiss = { showYearDialog = false }
        )
    }
}

@Composable
private fun EmptyDriversStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = P1TTheme.textMuted,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Individual Driver Row Card matching driver-profiles-720.webp:
 * Circular portrait with initials in team color,
 * Top small label (e.g. "8 wins" or "3 wins · 66 pts to P1"),
 * Bold full name,
 * Vertical team color bar + team name,
 * Right side trophy icon (gold/silver/bronze) + P# and points.
 */
@Composable
private fun DriverRowCard(
    driver: DriverStanding,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real Driver Portrait Avatar
            DriverPortraitAvatar(
                driverCodeOrName = driver.code,
                teamColor = Color(driver.team.colorHex),
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                // Top subtitle / wins
                val rawGap = driver.gapToLeader?.trim().orEmpty()
                val subtitle = when {
                    driver.position == 1 ->
                        "${driver.wins} wins · Championship Leader"
                    rawGap.isBlank() || rawGap.equals("Leader", ignoreCase = true) ->
                        "${driver.wins} wins · —"
                    rawGap.contains("to leader", ignoreCase = true) || rawGap.contains("to P1", ignoreCase = true) ->
                        "${driver.wins} wins · $rawGap"
                    else -> {
                        val signedGap = if (rawGap.startsWith("-") || rawGap.startsWith("+")) rawGap else "-$rawGap"
                        "${driver.wins} wins · $signedGap to P1"
                    }
                }

                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Full Name
                Text(
                    text = driver.fullName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Team line with vertical team color bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(driver.team.colorHex))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = driver.team.displayName,
                        color = P1TTheme.textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: Trophy (P1/P2/P3) + Position & Points
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (driver.position <= 3) {
                        val trophyTint = when (driver.position) {
                            1 -> P1TColors.GoldTrophy
                            2 -> P1TColors.SilverTrophy
                            else -> P1TColors.BronzeTrophy
                        }
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Podium",
                            tint = trophyTint,
                            modifier = Modifier
                                .padding(end = 3.dp)
                                .size(16.dp)
                        )
                    }

                    Text(
                        text = "P${driver.position}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatCleanNumber(driver.points)} points",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 1950 - 2026 Archive Year Selection Grid Dialog
 */
@Composable
private fun YearSelectionDialog(
    currentYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val allYears = (2026 downTo 1950).toList()
    val accentColor = LocalF1Accent.current.primaryAccent
    val selectedTextColor = P1TTheme.onColor(accentColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "F1 Seasons (1950-2026)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(allYears, key = { it }) { yr ->
                        val isSelected = yr == currentYear
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable { onSelectYear(yr) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = yr.toString(),
                                    color = if (isSelected) selectedTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
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
 * Screen 2: CompareDriversScreen
 * Exact 1:1 replica of compare-drivers-1080.webp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareDriversScreen(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison,
    availableDrivers: List<DriverCareerComparison>,
    onSelectDriver1: (DriverCareerComparison) -> Unit,
    onSelectDriver2: (DriverCareerComparison) -> Unit,
    onSwapDrivers: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDriverPickerForSlot by remember { mutableStateOf<Int?>(null) }

    val allAvailableDrivers = remember(availableDrivers) {
        val existingCodes = availableDrivers.map { it.code.uppercase() }.toSet()
        val existingIds = availableDrivers.map { it.driverId.lowercase() }.toSet()
        val grid2026Extras = SeedDataProvider.getDriverStandings2026()
            .filter { standing ->
                standing.code.uppercase() !in existingCodes &&
                    standing.driverId.lowercase() !in existingIds
            }
            .map { standing ->
                val raceStarts = standing.racesCount.coerceAtLeast(0)
                val poles = standing.poles.coerceAtLeast(0)
                DriverCareerComparison(
                    driverId = standing.driverId,
                    code = standing.code,
                    firstName = standing.firstName,
                    lastName = standing.lastName,
                    nationality = standing.nationality,
                    team = standing.team,
                    championships = 0,
                    raceWins = standing.wins,
                    podiums = standing.podiums,
                    polePositions = poles,
                    fastestLaps = 0,
                    careerPoints = standing.points,
                    raceStarts = raceStarts,
                    dnfs = 0,
                    winRatePercent = if (raceStarts > 0) (standing.wins * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0,
                    podiumRatePercent = if (raceStarts > 0) (standing.podiums * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0,
                    poleRatePercent = if (raceStarts > 0) (poles * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0
                )
            }
        availableDrivers + grid2026Extras
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Drivers",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSwapDrivers) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Drivers",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Split Two-Color Hero Banner
            item {
                SplitHeroBanner(
                    driver1 = driver1,
                    driver2 = driver2,
                    onPickDriver1 = { showDriverPickerForSlot = 1 },
                    onPickDriver2 = { showDriverPickerForSlot = 2 }
                )
            }

            // CAREER STATISTICS Card
            item {
                CareerStatisticsCard(driver1 = driver1, driver2 = driver2)
            }

            // SUCCESS RATES Card
            item {
                SuccessRatesCard(driver1 = driver1, driver2 = driver2)
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Driver picker dialog
    showDriverPickerForSlot?.let { slot ->
        DriverPickerDialog(
            availableDrivers = allAvailableDrivers,
            currentSelectedCode = if (slot == 1) driver1.code else driver2.code,
            oppositeSelectedCode = if (slot == 1) driver2.code else driver1.code,
            onSelect = { selected ->
                if (slot == 1) onSelectDriver1(selected) else onSelectDriver2(selected)
                showDriverPickerForSlot = null
            },
            onDismiss = { showDriverPickerForSlot = null }
        )
    }
}

/**
 * Split Two-Color Hero Banner
 * Left half in Driver 1's team color, Right half in Driver 2's team color.
 * Centered dark circle with "VS".
 */
@Composable
private fun SplitHeroBanner(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison,
    onPickDriver1: () -> Unit,
    onPickDriver2: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = P1TTheme.isDark

    val d1TeamColor = Color(driver1.team.colorHex)
    val d1BgOuter = d1TeamColor.copy(alpha = if (isDark) 0.42f else 0.85f).compositeOver(surfaceColor)
    val d1BgInner = d1TeamColor.copy(alpha = if (isDark) 0.26f else 0.68f).compositeOver(surfaceColor)
    val d1TextColor = P1TTheme.onColor(d1BgInner)

    val d2TeamColor = Color(driver2.team.colorHex)
    val d2BgOuter = d2TeamColor.copy(alpha = if (isDark) 0.42f else 0.85f).compositeOver(surfaceColor)
    val d2BgInner = d2TeamColor.copy(alpha = if (isDark) 0.26f else 0.68f).compositeOver(surfaceColor)
    val d2TextColor = P1TTheme.onColor(d2BgInner)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left half: Driver 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(d1BgOuter, d1BgInner)))
                    .clickable(onClick = onPickDriver1)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DriverPortraitAvatar(
                        driverCodeOrName = driver1.code,
                        teamColor = d1TeamColor,
                        size = 46.dp,
                        useFullCutout = true
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 18.dp)
                    ) {
                        Text(
                            text = driver1.code,
                            color = d1TextColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${driver1.firstName} ${driver1.lastName.uppercase()}",
                            color = d1TextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = driver1.nationality,
                            color = d1TextColor.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to change ▾",
                            color = d1TextColor.copy(alpha = 0.82f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Right half: Driver 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(d2BgInner, d2BgOuter)))
                    .clickable(onClick = onPickDriver2)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp)
                    ) {
                        Text(
                            text = driver2.code,
                            color = d2TextColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${driver2.firstName} ${driver2.lastName.uppercase()}",
                            color = d2TextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = driver2.nationality,
                            color = d2TextColor.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "▾ Tap to change",
                            color = d2TextColor.copy(alpha = 0.82f),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    DriverPortraitAvatar(
                        driverCodeOrName = driver2.code,
                        teamColor = d2TeamColor,
                        size = 46.dp,
                        useFullCutout = true
                    )
                }
            }
        }

        // Center VS circle badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * CAREER STATISTICS Card with dual horizontal comparison bars facing center
 */
@Composable
private fun CareerStatisticsCard(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CAREER STATISTICS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            ComparisonStatRow(
                label = "Championships",
                val1 = driver1.championships.toDouble(),
                val2 = driver2.championships.toDouble(),
                text1 = driver1.championships.toString(),
                text2 = driver2.championships.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Race Wins",
                val1 = driver1.raceWins.toDouble(),
                val2 = driver2.raceWins.toDouble(),
                text1 = driver1.raceWins.toString(),
                text2 = driver2.raceWins.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Podiums",
                val1 = driver1.podiums.toDouble(),
                val2 = driver2.podiums.toDouble(),
                text1 = driver1.podiums.toString(),
                text2 = driver2.podiums.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Pole Positions",
                val1 = driver1.polePositions.toDouble(),
                val2 = driver2.polePositions.toDouble(),
                text1 = driver1.polePositions.toString(),
                text2 = driver2.polePositions.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Fastest Laps",
                val1 = driver1.fastestLaps.toDouble(),
                val2 = driver2.fastestLaps.toDouble(),
                text1 = driver1.fastestLaps.toString(),
                text2 = driver2.fastestLaps.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Career Points",
                val1 = driver1.careerPoints,
                val2 = driver2.careerPoints,
                text1 = formatCleanNumber(driver1.careerPoints),
                text2 = formatCleanNumber(driver2.careerPoints),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Race Starts",
                val1 = driver1.raceStarts.toDouble(),
                val2 = driver2.raceStarts.toDouble(),
                text1 = driver1.raceStarts.toString(),
                text2 = driver2.raceStarts.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            // For DNFs: lower value is better!
            ComparisonStatRow(
                label = "DNFs",
                val1 = driver1.dnfs.toDouble(),
                val2 = driver2.dnfs.toDouble(),
                text1 = driver1.dnfs.toString(),
                text2 = driver2.dnfs.toString(),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex),
                lowerIsBetter = true
            )
        }
    }
}

/**
 * SUCCESS RATES Card with dual center-split bars
 */
@Composable
private fun SuccessRatesCard(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SUCCESS RATES",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            ComparisonStatRow(
                label = "Win Rate",
                val1 = driver1.winRatePercent,
                val2 = driver2.winRatePercent,
                text1 = formatPercent(driver1.winRatePercent),
                text2 = formatPercent(driver2.winRatePercent),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Podium Rate",
                val1 = driver1.podiumRatePercent,
                val2 = driver2.podiumRatePercent,
                text1 = formatPercent(driver1.podiumRatePercent),
                text2 = formatPercent(driver2.podiumRatePercent),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Pole Rate",
                val1 = driver1.poleRatePercent,
                val2 = driver2.poleRatePercent,
                text1 = formatPercent(driver1.poleRatePercent),
                text2 = formatPercent(driver2.poleRatePercent),
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )
        }
    }
}

/**
 * Dual horizontal comparison bar facing center
 */
@Composable
private fun ComparisonStatRow(
    label: String,
    val1: Double,
    val2: Double,
    text1: String,
    text2: String,
    color1: Color,
    color2: Color,
    lowerIsBetter: Boolean = false
) {
    val clean1 = if (val1.isNaN() || val1.isInfinite()) 0.0 else val1.coerceAtLeast(0.0)
    val clean2 = if (val2.isNaN() || val2.isInfinite()) 0.0 else val2.coerceAtLeast(0.0)

    val (ratio1, ratio2) = when {
        clean1 == 0.0 && clean2 == 0.0 -> 0.5f to 0.5f
        clean1 == clean2 -> 0.5f to 0.5f
        lowerIsBetter -> {
            when {
                clean1 == 0.0 -> 1f to 0f
                clean2 == 0.0 -> 0f to 1f
                else -> {
                    val total = clean1 + clean2
                    val inv1 = (1f - (clean1 / total).toFloat()).coerceIn(0.08f, 0.92f)
                    val inv2 = (1f - (clean2 / total).toFloat()).coerceIn(0.08f, 0.92f)
                    inv1 to inv2
                }
            }
        }
        else -> {
            when {
                clean1 == 0.0 -> 0f to 1f
                clean2 == 0.0 -> 1f to 0f
                else -> {
                    val total = clean1 + clean2
                    (clean1 / total).toFloat().coerceIn(0.08f, 0.92f) to
                        (clean2 / total).toFloat().coerceIn(0.08f, 0.92f)
                }
            }
        }
    }

    val is1Better = if (lowerIsBetter) clean1 < clean2 else clean1 > clean2
    val is2Better = if (lowerIsBetter) clean2 < clean1 else clean2 > clean1
    val adaptiveColor1 = P1TTheme.adaptiveTeamText(color1)
    val adaptiveColor2 = P1TTheme.adaptiveTeamText(color2)

    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        // Values and Label Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text1,
                color = if (is1Better) adaptiveColor1 else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp,
                fontWeight = if (is1Better) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = label,
                color = P1TTheme.textMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text(
                text = text2,
                color = if (is2Better) adaptiveColor2 else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp,
                fontWeight = if (is2Better) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Dual center facing bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Bar (Driver 1) - aligns right towards center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio1)
                        .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                        .background(if (is1Better) color1 else color1.copy(alpha = 0.4f))
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right Bar (Driver 2) - aligns left away from center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio2)
                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(if (is2Better) color2 else color2.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/**
 * Driver picker modal list
 */
@Composable
private fun DriverPickerDialog(
    availableDrivers: List<DriverCareerComparison>,
    currentSelectedCode: String,
    oppositeSelectedCode: String? = null,
    onSelect: (DriverCareerComparison) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Driver",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDrivers, key = { it.driverId.ifBlank { it.code } }) { drv ->
                        val isCurrent = drv.code == currentSelectedCode
                        val isOpposite = drv.code == oppositeSelectedCode
                        val isEnabled = !isOpposite

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCurrent -> P1TTheme.pillSelectedBg
                                isOpposite -> P1TTheme.surfaceInnerCard
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = when {
                                isCurrent -> androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.adaptiveTeamText(Color(drv.team.colorHex)))
                                else -> null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(enabled = isEnabled) { onSelect(drv) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOpposite) Color(drv.team.colorHex).copy(alpha = 0.35f) else Color(drv.team.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = drv.code,
                                    color = if (isOpposite) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOpposite) "${drv.firstName} ${drv.lastName} (Opposite slot)" else "${drv.firstName} ${drv.lastName}",
                                    color = if (isOpposite) P1TTheme.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = drv.team.shortCode,
                                    color = if (isOpposite) P1TTheme.textMuted.copy(alpha = 0.5f) else P1TTheme.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
