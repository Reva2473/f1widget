package com.reva2473.f1widget.ui.screens.drivers

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors

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
    var searchQuery by remember { mutableStateOf("") }
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
        containerColor = P1TColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Driver Profiles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCompare) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Compare Drivers",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = P1TColors.BackgroundPrimary
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
                            color = P1TColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = P1TColors.TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = P1TColors.SurfaceCard,
                        unfocusedContainerColor = P1TColors.SurfaceCard,
                        focusedBorderColor = P1TColors.BorderSubtle,
                        unfocusedBorderColor = P1TColors.BorderSubtle,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
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
                    color = P1TColors.TextSecondary,
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
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color(0xFF282834) else P1TColors.SurfaceCard,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.dp, LocalF1Accent.current.primaryAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle)
                            },
                            modifier = Modifier.clickable { onYearSelected(yr) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else P1TColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Red interactive row: "📅 View all years (1950-present)"
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = P1TColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
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
                            Text(
                                text = "📅",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "View all years (1950-present)",
                                color = P1TColors.AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open archive",
                            tint = P1TColors.AccentRed,
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredDrivers.size} drivers",
                        color = P1TColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Drivers List Rows
            items(filteredDrivers, key = { it.driverId }) { driver ->
                DriverRowCard(
                    driver = driver,
                    onClick = { onDriverSelected(driver) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
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
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
            // Driver Circle Portrait with team color ring
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(driver.team.secondaryHex))
                    .border(2.dp, Color(driver.team.colorHex), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = driver.code,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                // Top subtitle / wins
                val subtitle = when {
                    driver.position == 1 -> "${driver.wins} wins"
                    driver.gapToLeader != null -> "${driver.wins} wins · ${driver.gapToLeader.replace("-", "")} to P1"
                    else -> "${driver.wins} wins"
                }

                Text(
                    text = subtitle,
                    color = P1TColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Full Name
                Text(
                    text = driver.fullName,
                    color = Color.White,
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
                        color = P1TColors.TextMuted,
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
                                .size(16.dp)
                                .padding(end = 3.dp)
                        )
                    }

                    Text(
                        text = "P${driver.position}",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${driver.points.toInt()} points",
                    color = P1TColors.TextSecondary,
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = P1TColors.SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
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
                    items(allYears) { yr ->
                        val isSelected = yr == currentYear
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LocalF1Accent.current.primaryAccent else P1TColors.SurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectYear(yr) }
                        ) {
                            Text(
                                text = yr.toString(),
                                color = if (isSelected) Color.White else P1TColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = P1TColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Drivers",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSwapDrivers) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Drivers",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = P1TColors.BackgroundPrimary
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
            availableDrivers = availableDrivers,
            currentSelectedCode = if (slot == 1) driver1.code else driver2.code,
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(P1TColors.SurfaceCard)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half: Driver 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(driver1.team.secondaryHex),
                                Color(driver1.team.colorHex).copy(alpha = 0.85f)
                            )
                        )
                    )
                    .clickable(onClick = onPickDriver1)
                    .padding(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = driver1.code,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${driver1.firstName} ${driver1.lastName.uppercase()}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = driver1.nationality,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to change ▾",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }

            // Right half: Driver 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(driver2.team.colorHex).copy(alpha = 0.85f),
                                Color(driver2.team.secondaryHex)
                            )
                        )
                    )
                    .clickable(onClick = onPickDriver2)
                    .padding(14.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = driver2.code,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${driver2.firstName} ${driver2.lastName.uppercase()}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = driver2.nationality,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "▾ Tap to change",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Center VS circle badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color(0xFF0F0F14))
                .border(2.dp, Color(0xFF2A2A38), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = Color.White,
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
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CAREER STATISTICS",
                color = P1TColors.TextSecondary,
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
                text1 = driver1.careerPoints.toString(),
                text2 = driver2.careerPoints.toString(),
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
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SUCCESS RATES",
                color = P1TColors.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            ComparisonStatRow(
                label = "Win Rate",
                val1 = driver1.winRatePercent,
                val2 = driver2.winRatePercent,
                text1 = "${driver1.winRatePercent}%",
                text2 = "${driver2.winRatePercent}%",
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Podium Rate",
                val1 = driver1.podiumRatePercent,
                val2 = driver2.podiumRatePercent,
                text1 = "${driver1.podiumRatePercent}%",
                text2 = "${driver2.podiumRatePercent}%",
                color1 = Color(driver1.team.colorHex),
                color2 = Color(driver2.team.colorHex)
            )

            ComparisonStatRow(
                label = "Pole Rate",
                val1 = driver1.poleRatePercent,
                val2 = driver2.poleRatePercent,
                text1 = "${driver1.poleRatePercent}%",
                text2 = "${driver2.poleRatePercent}%",
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
    val total = if (val1 + val2 == 0.0) 1.0 else (val1 + val2)
    val ratio1 = (val1 / total).toFloat().coerceIn(0.08f, 0.92f)
    val ratio2 = (val2 / total).toFloat().coerceIn(0.08f, 0.92f)

    val is1Better = if (lowerIsBetter) val1 < val2 else val1 > val2
    val is2Better = if (lowerIsBetter) val2 < val1 else val2 > val1

    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        // Values and Label Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text1,
                color = if (is1Better) color1 else P1TColors.TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (is1Better) FontWeight.Bold else FontWeight.Medium
            )

            Text(
                text = label,
                color = P1TColors.TextMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = text2,
                color = if (is2Better) color2 else P1TColors.TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (is2Better) FontWeight.Bold else FontWeight.Medium
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
    onSelect: (DriverCareerComparison) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = P1TColors.SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDrivers) { drv ->
                        val isCurrent = drv.code == currentSelectedCode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) Color(0xFF282834) else P1TColors.SurfaceElevated,
                            border = if (isCurrent) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(drv.team.colorHex))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(drv) }
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
                                        .background(Color(drv.team.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = drv.code,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${drv.firstName} ${drv.lastName}",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = drv.team.shortCode,
                                    color = P1TColors.TextMuted,
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
