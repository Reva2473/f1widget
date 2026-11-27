package com.reva2473.f1widget.ui.screens.drivers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.getDriverCutoutRes
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

private fun resolveDriverFirstAndLastName(driver: DriverStanding): Pair<String, String> {
    val explicitFirst = driver.firstName.trim()
    val explicitLast = driver.lastName.trim()
    if (explicitFirst.isNotEmpty() || explicitLast.isNotEmpty()) {
        return explicitFirst to explicitLast.ifEmpty { driver.fullName.trim() }
    }
    val full = driver.fullName.trim()
    if (full.isEmpty()) return "" to driver.code
    val lastSpace = full.lastIndexOf(' ')
    return if (lastSpace > 0) {
        full.substring(0, lastSpace).trim() to full.substring(lastSpace + 1).trim()
    } else {
        "" to full
    }
}

/**
 * Screen 1: DriverProfilesScreen
 */
@Composable
fun DriverProfilesScreen(
    drivers: List<DriverStanding>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onDriverSelected: (DriverStanding) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showYearDialog by remember { mutableStateOf(false) }

    val adaptiveAccent = P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent)

    val yearsList = remember(selectedYear) {
        val base = listOf(2026, 2025, 2024, 2023, 2022)
        if (selectedYear in base) base else listOf(selectedYear) + base
    }

    val filteredDrivers = remember(drivers, searchQuery) {
        if (searchQuery.isBlank()) {
            drivers
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            drivers.filter {
                it.fullName.lowercase(Locale.ROOT).contains(q) ||
                    it.code.lowercase(Locale.ROOT).contains(q) ||
                    it.team.displayName.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    val spotlightLeader = remember(filteredDrivers, searchQuery, isLoading, errorMessage) {
        if (searchQuery.isBlank() && !isLoading && errorMessage.isNullOrBlank()) {
            filteredDrivers.firstOrNull { it.position == 1 } ?: filteredDrivers.firstOrNull()
        } else {
            null
        }
    }

    val listDrivers = remember(filteredDrivers, spotlightLeader) {
        if (spotlightLeader == null) {
            filteredDrivers
        } else {
            filteredDrivers.filterNot {
                it.driverId == spotlightLeader.driverId && it.position == spotlightLeader.position
            }
        }
    }

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            DriverProfilesHeaderBar(
                onNavigateBack = onNavigateBack,
                onNavigateToCompare = onNavigateToCompare
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
                            focusedBorderColor = adaptiveAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp)
                    )
                }

                // Browse by Year Header
                item {
                    Text(
                        text = "Browse by Year",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
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
                                        BorderStroke(1.dp, adaptiveAccent)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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

                // Archive row: "View all years (1950-present)"
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                                    tint = adaptiveAccent,
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "View all years (1950-present)",
                                    color = adaptiveAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open archive",
                                tint = adaptiveAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // List Header: "2026 Drivers" and count
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
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
                            text = when {
                                isLoading -> "Loading..."
                                !errorMessage.isNullOrBlank() -> "Unavailable"
                                else -> "${filteredDrivers.size} drivers"
                            },
                            color = P1TTheme.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // Featured Driver Spotlight Card for P1 Leader when search is empty
                if (spotlightLeader != null) {
                    item(key = "spotlight_${selectedYear}_${spotlightLeader.driverId}_${spotlightLeader.position}") {
                        FeaturedDriverSpotlightCard(
                            driver = spotlightLeader,
                            selectedYear = selectedYear,
                            onClick = { onDriverSelected(spotlightLeader) }
                        )
                    }
                }

                // Drivers List Rows or Loading / Error / Empty States
                if (isLoading) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = adaptiveAccent,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Loading $selectedYear drivers...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (!errorMessage.isNullOrBlank()) {
                    item {
                        EmptyDriversStateCard(
                            title = "Unable to load $selectedYear drivers",
                            subtitle = errorMessage,
                            actionLabel = "Retry",
                            onAction = onRetry
                        )
                    }
                } else if (drivers.isEmpty()) {
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
                    items(listDrivers, key = { "${selectedYear}_${it.driverId}_${it.position}" }) { driver ->
                        DriverRowCard(
                            driver = driver,
                            onClick = { onDriverSelected(driver) }
                        )
                    }
                }

                item {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(80.dp)
                    )
                }
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
private fun DriverProfilesHeaderBar(
    onNavigateBack: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            CircularHeaderButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Driver Profiles",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Labeled "Compare" pill button (>= 48.dp touch height)
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onNavigateToCompare),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = P1TTheme.accentPillBg(primaryAccent),
                border = BorderStroke(1.dp, adaptiveAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = adaptiveAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Compare",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedDriverSpotlightCard(
    driver: DriverStanding,
    selectedYear: Int,
    onClick: () -> Unit
) {
    val isDark = P1TTheme.isDark
    val teamColor = if (driver.team != F1TeamId.OTHER) {
        Color(driver.team.colorHex)
    } else {
        LocalF1Accent.current.primaryAccent
    }
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val (firstName, lastName) = remember(driver.firstName, driver.lastName, driver.fullName, driver.code) {
        resolveDriverFirstAndLastName(driver)
    }

    val cutoutRes = remember(driver.fullName, driver.code) {
        getDriverCutoutRes(driver.fullName.ifBlank { driver.code })
            ?: driver.code.takeIf { it.isNotBlank() }?.let(::getDriverCutoutRes)
    }

    val bgBrush = remember(teamColor, isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    teamColor.copy(alpha = 0.40f).compositeOver(Color(0xFF14141B)),
                    Color(0xFF111117),
                    Color(0xFF0A0A0F)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    teamColor.copy(alpha = 0.22f).compositeOver(Color.White),
                    teamColor.copy(alpha = 0.08f).compositeOver(Color.White),
                    Color(0xFFFFFFFF)
                )
            )
        }
    }

    val borderColor = if (isDark) {
        teamColor.copy(alpha = 0.42f)
    } else {
        teamColor.copy(alpha = 0.35f)
    }

    val watermarkText = driver.code.ifBlank { "P${driver.position}" }.uppercase(Locale.ROOT)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Giant faded driver rank/code watermark
        Text(
            text = watermarkText,
            color = teamColor.copy(alpha = if (isDark) 0.14f else 0.11f),
            fontSize = 84.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-3).sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 14.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp)
            ) {
                // Left Driver Identity Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = teamColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                        border = BorderStroke(1.dp, adaptiveTeamColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = P1TColors.GoldTrophy,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "P${driver.position} · $selectedYear LEADER",
                                color = adaptiveTeamColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (firstName.isNotBlank()) {
                        Text(
                            text = firstName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = lastName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(13.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(teamColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = driver.team.displayName,
                            color = adaptiveTeamColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Driver Cutout + Overlapping TeamCarShowcaseImage
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(164.dp)
                        .fillMaxHeight()
                        .padding(end = 6.dp)
                ) {
                    if (cutoutRes != null) {
                        Image(
                            painter = painterResource(id = cutoutRes),
                            contentDescription = driver.fullName,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(152.dp)
                                .width(126.dp)
                        )
                    } else {
                        DriverPortraitAvatar(
                            driverCodeOrName = driver.fullName.ifBlank { driver.code },
                            teamColor = teamColor,
                            size = 86.dp,
                            useFullCutout = true,
                            driverCode = driver.code.takeIf { it.isNotBlank() },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = 22.dp)
                        )
                    }

                    TeamCarShowcaseImage(
                        team = driver.team,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(148.dp)
                            .height(50.dp)
                            .padding(end = 4.dp)
                    )
                }
            }

            // Bottom 3-stat Pro Racing Stats bar (Points, Wins, Podiums)
            val statsBg = if (isDark) Color(0xCC111117) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            val statsBorder = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(statsBg)
                    .border(1.dp, statsBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotlightStatItem(
                    value = formatCleanNumber(driver.points),
                    label = "POINTS",
                    valueColor = adaptiveTeamColor,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(P1TTheme.dividerSubtle)
                )
                SpotlightStatItem(
                    value = driver.wins.toString(),
                    label = "WINS",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(P1TTheme.dividerSubtle)
                )
                SpotlightStatItem(
                    value = driver.podiums.toString(),
                    label = "PODIUMS",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpotlightStatItem(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            color = P1TTheme.textMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
 * Team-colored Driver Row Card:
 * Subtle team-color horizontal gradient wash, clean light-weight first name + bold black last name,
 * team name + rank/pts pill, and crisp 52.dp driver portrait on the right.
 */
@Composable
private fun DriverRowCard(
    driver: DriverStanding,
    onClick: () -> Unit
) {
    val isDark = P1TTheme.isDark
    val teamColor = if (driver.team != F1TeamId.OTHER) {
        Color(driver.team.colorHex)
    } else {
        MaterialTheme.colorScheme.outline
    }
    val adaptiveTeamColor = if (driver.team != F1TeamId.OTHER) {
        P1TTheme.adaptiveTeamText(teamColor)
    } else {
        P1TTheme.textMuted
    }

    val (firstName, lastName) = remember(driver.firstName, driver.lastName, driver.fullName, driver.code) {
        resolveDriverFirstAndLastName(driver)
    }

    val surfaceBase = P1TTheme.surfaceStandingCard
    val cardGradient = remember(teamColor, surfaceBase, isDark) {
        val washAlpha = if (driver.team != F1TeamId.OTHER) {
            if (isDark) 0.16f else 0.11f
        } else {
            0.04f
        }
        Brush.horizontalGradient(
            colors = listOf(
                surfaceBase,
                teamColor.copy(alpha = washAlpha).compositeOver(surfaceBase)
            )
        )
    }

    val winsLabel = if (driver.wins == 1) "1 win" else "${driver.wins} wins"
    val rawGap = driver.gapToLeader?.trim().orEmpty()
    val subtitle = when {
        driver.position == 1 ->
            "$winsLabel · Championship Leader"
        rawGap.isBlank() || rawGap.equals("Leader", ignoreCase = true) ->
            winsLabel
        rawGap.contains("to leader", ignoreCase = true) || rawGap.contains("to P1", ignoreCase = true) ->
            "$winsLabel · $rawGap"
        else -> {
            val signedGap = if (rawGap.startsWith("-") || rawGap.startsWith("+")) rawGap else "-$rawGap"
            "$winsLabel · $signedGap to P1"
        }
    }

    val displayName = buildAnnotatedString {
        if (firstName.isNotBlank()) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                append(firstName)
                append(" ")
            }
        }
        withStyle(
            SpanStyle(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        ) {
            append(lastName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient)
            .border(1.dp, P1TTheme.borderStandingCard, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(teamColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = driver.team.displayName,
                            color = adaptiveTeamColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Rank / Points pill
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = P1TTheme.pillBg,
                        border = BorderStroke(0.8.dp, P1TTheme.pillBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (driver.position <= 3) {
                                val trophyTint = when (driver.position) {
                                    1 -> P1TColors.GoldTrophy
                                    2 -> P1TColors.SilverTrophy
                                    else -> P1TColors.BronzeTrophy
                                }
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = trophyTint,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = "P${driver.position} · ${formatCleanNumber(driver.points)} pts",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Crisp 52.dp Driver Portrait Avatar
            DriverPortraitAvatar(
                driverCodeOrName = driver.fullName.ifBlank { driver.code },
                teamColor = teamColor,
                size = 52.dp,
                positionBadge = "P${driver.position}",
                driverCode = driver.code.takeIf { it.isNotBlank() }
            )
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
    val allYears = remember { (2026 downTo 1950).toList() }
    val gridState = rememberLazyGridState()
    val accentColor = LocalF1Accent.current.primaryAccent
    val selectedTextColor = P1TTheme.onColor(accentColor)

    LaunchedEffect(currentYear) {
        val targetIndex = allYears.indexOf(currentYear)
        if (targetIndex >= 0) {
            gridState.scrollToItem(targetIndex)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                    state = gridState,
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

private fun isCareerComparisonDriver(
    driver: DriverCareerComparison,
    careerDrivers: List<DriverCareerComparison>
): Boolean {
    return careerDrivers.any { career ->
        (driver.driverId.isNotBlank() && career.driverId.equals(driver.driverId, ignoreCase = true)) ||
            (career.firstName.equals(driver.firstName, ignoreCase = true) &&
                career.lastName.equals(driver.lastName, ignoreCase = true))
    }
}

private fun isSameDriverRecord(a: DriverCareerComparison, b: DriverCareerComparison): Boolean {
    if (a.driverId.isNotBlank() && b.driverId.isNotBlank()) {
        return a.driverId.equals(b.driverId, ignoreCase = true)
    }
    return a.firstName.equals(b.firstName, ignoreCase = true) &&
        a.lastName.equals(b.lastName, ignoreCase = true) &&
        a.code.equals(b.code, ignoreCase = true)
}

/**
 * Screen 2: CompareDriversScreen
 */
@Composable
fun CompareDriversScreen(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison,
    availableDrivers: List<DriverCareerComparison>,
    onSelectDriver1: (DriverCareerComparison) -> Unit,
    onSelectDriver2: (DriverCareerComparison) -> Unit,
    onSwapDrivers: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedYear: Int = 2026
) {
    var showDriverPickerForSlot by remember { mutableStateOf<Int?>(null) }

    val seedCareerDrivers = remember { SeedDataProvider.getDriverComparisons() }
    val isD1Career = remember(driver1, seedCareerDrivers) {
        isCareerComparisonDriver(driver1, seedCareerDrivers)
    }
    val isD2Career = remember(driver2, seedCareerDrivers) {
        isCareerComparisonDriver(driver2, seedCareerDrivers)
    }

    val allAvailableDrivers = remember(availableDrivers) {
        val existingCodes = availableDrivers.map { it.code.uppercase(Locale.ROOT) }.toSet()
        val existingIds = availableDrivers.map { it.driverId.lowercase(Locale.ROOT) }.toSet()
        val grid2026Extras = SeedDataProvider.getDriverStandings2026()
            .filter { standing ->
                standing.code.uppercase(Locale.ROOT) !in existingCodes &&
                    standing.driverId.lowercase(Locale.ROOT) !in existingIds
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

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CompareDriversHeaderBar(
                onNavigateBack = onNavigateBack,
                onSwapDrivers = onSwapDrivers
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Split Two-Card Hero Banner
                item {
                    SplitHeroBanner(
                        driver1 = driver1,
                        driver2 = driver2,
                        onPickDriver1 = { showDriverPickerForSlot = 1 },
                        onPickDriver2 = { showDriverPickerForSlot = 2 }
                    )
                }

                // CAREER / SEASON STATISTICS Card
                item {
                    CareerStatisticsCard(
                        driver1 = driver1,
                        driver2 = driver2,
                        isD1Career = isD1Career,
                        isD2Career = isD2Career,
                        selectedYear = selectedYear
                    )
                }

                // SUCCESS RATES Card
                item {
                    SuccessRatesCard(driver1 = driver1, driver2 = driver2)
                }

                item {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(72.dp)
                    )
                }
            }
        }
    }

    // Driver picker dialog
    showDriverPickerForSlot?.let { slot ->
        DriverPickerDialog(
            availableDrivers = allAvailableDrivers,
            currentSelectedDriver = if (slot == 1) driver1 else driver2,
            oppositeSelectedDriver = if (slot == 1) driver2 else driver1,
            onSelect = { selected ->
                if (slot == 1) onSelectDriver1(selected) else onSelectDriver2(selected)
                showDriverPickerForSlot = null
            },
            onDismiss = { showDriverPickerForSlot = null }
        )
    }
}

@Composable
private fun CompareDriversHeaderBar(
    onNavigateBack: () -> Unit,
    onSwapDrivers: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularHeaderButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Compare Drivers",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        CircularHeaderButton(onClick = onSwapDrivers) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Split Two-Card Hero Banner:
 * Two generous side-by-side team-colored vertical gradient cards with a centered floating "VS" badge.
 */
@Composable
private fun SplitHeroBanner(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison,
    onPickDriver1: () -> Unit,
    onPickDriver2: () -> Unit
) {
    val isDark = P1TTheme.isDark

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DriverCompareHeroCard(
                driver = driver1,
                onClick = onPickDriver1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            DriverCompareHeroCard(
                driver = driver2,
                onClick = onPickDriver2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Centered floating VS badge
        val vsBg = if (isDark) Color(0xFF14141B) else MaterialTheme.colorScheme.surface
        val vsBorder = if (isDark) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outline
        Box(
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.Center)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(vsBg)
                .border(1.5.dp, vsBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DriverCompareHeroCard(
    driver: DriverCareerComparison,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = P1TTheme.isDark
    val teamColor = Color(driver.team.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    val cardBrush = remember(teamColor, isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    teamColor.copy(alpha = 0.62f).compositeOver(Color(0xFF15151D)),
                    teamColor.copy(alpha = 0.22f).compositeOver(Color(0xFF0E0E14)),
                    Color(0xFF09090D)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    teamColor.copy(alpha = 0.26f).compositeOver(Color.White),
                    teamColor.copy(alpha = 0.08f).compositeOver(Color.White),
                    Color(0xFFF6F6FA)
                )
            )
        }
    }

    val borderColor = if (isDark) {
        teamColor.copy(alpha = 0.48f)
    } else {
        teamColor.copy(alpha = 0.36f)
    }

    val primaryTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
    val hintTextColor = if (isDark) Color.White.copy(alpha = 0.68f) else P1TTheme.textMuted
    val pillBg = if (isDark) Color.White.copy(alpha = 0.10f) else teamColor.copy(alpha = 0.12f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.18f) else adaptiveTeamColor.copy(alpha = 0.32f)

    val fullName = "${driver.firstName} ${driver.lastName}".trim().ifBlank { driver.code }
    val displayLastName = driver.lastName.trim().ifBlank { driver.firstName.trim().ifBlank { driver.code } }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBrush)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DriverPortraitAvatar(
                driverCodeOrName = fullName,
                teamColor = teamColor,
                size = 74.dp,
                useFullCutout = true,
                driverCode = driver.code.takeIf { it.isNotBlank() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = driver.code.ifBlank { "---" },
                color = primaryTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = displayLastName.uppercase(Locale.ROOT),
                color = secondaryTextColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = pillBg,
                border = BorderStroke(0.8.dp, pillBorder)
            ) {
                Text(
                    text = driver.team.displayName,
                    color = if (isDark) Color.White else adaptiveTeamColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to change ▾",
                color = hintTextColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * CAREER / SEASON STATISTICS Card with dual horizontal comparison bars facing center
 */
@Composable
private fun CareerStatisticsCard(
    driver1: DriverCareerComparison,
    driver2: DriverCareerComparison,
    isD1Career: Boolean = true,
    isD2Career: Boolean = true,
    selectedYear: Int = 2026
) {
    val sectionTitle = when {
        isD1Career && isD2Career -> "CAREER STATISTICS"
        !isD1Career && !isD2Career -> "$selectedYear SEASON STATISTICS"
        else -> "STATISTICS · CAREER VS $selectedYear SEASON"
    }
    val pointsLabel = when {
        isD1Career && isD2Career -> "Career Points"
        !isD1Career && !isD2Career -> "$selectedYear Season Points"
        else -> "Points"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = sectionTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (isD1Career != isD2Career) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${driver1.code}: ${if (isD1Career) "Career" else "$selectedYear Season"} · ${driver2.code}: ${if (isD2Career) "Career" else "$selectedYear Season"}",
                    color = P1TTheme.textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isD1Career || isD2Career) {
                ComparisonStatRow(
                    label = "Championships",
                    val1 = if (isD1Career) driver1.championships.toDouble() else 0.0,
                    val2 = if (isD2Career) driver2.championships.toDouble() else 0.0,
                    text1 = if (isD1Career) driver1.championships.toString() else "—",
                    text2 = if (isD2Career) driver2.championships.toString() else "—",
                    color1 = Color(driver1.team.colorHex),
                    color2 = Color(driver2.team.colorHex)
                )
            }

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

            if (isD1Career || isD2Career) {
                ComparisonStatRow(
                    label = "Fastest Laps",
                    val1 = if (isD1Career) driver1.fastestLaps.toDouble() else 0.0,
                    val2 = if (isD2Career) driver2.fastestLaps.toDouble() else 0.0,
                    text1 = if (isD1Career) driver1.fastestLaps.toString() else "—",
                    text2 = if (isD2Career) driver2.fastestLaps.toString() else "—",
                    color1 = Color(driver1.team.colorHex),
                    color2 = Color(driver2.team.colorHex)
                )
            }

            ComparisonStatRow(
                label = pointsLabel,
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

            if (isD1Career || isD2Career) {
                ComparisonStatRow(
                    label = "DNFs",
                    val1 = if (isD1Career) driver1.dnfs.toDouble() else 0.0,
                    val2 = if (isD2Career) driver2.dnfs.toDouble() else 0.0,
                    text1 = if (isD1Career) driver1.dnfs.toString() else "—",
                    text2 = if (isD2Career) driver2.dnfs.toString() else "—",
                    color1 = Color(driver1.team.colorHex),
                    color2 = Color(driver2.team.colorHex),
                    lowerIsBetter = true
                )
            }
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
    val hasMissing = text1 == "—" || text2 == "—"

    val (ratio1, ratio2) = when {
        hasMissing -> {
            val r1 = if (text1 == "—" || clean1 == 0.0) 0f else 0.5f
            val r2 = if (text2 == "—" || clean2 == 0.0) 0f else 0.5f
            r1 to r2
        }
        clean1 == 0.0 && clean2 == 0.0 -> if (lowerIsBetter) 0.5f to 0.5f else 0f to 0f
        clean1 == clean2 -> 0.5f to 0.5f
        lowerIsBetter -> when {
            clean1 == 0.0 -> 1f to 0f
            clean2 == 0.0 -> 0f to 1f
            else -> {
                val total = clean1 + clean2
                (clean2 / total).toFloat().coerceIn(0.08f, 0.92f) to
                    (clean1 / total).toFloat().coerceIn(0.08f, 0.92f)
            }
        }
        else -> when {
            clean1 == 0.0 -> 0f to 1f
            clean2 == 0.0 -> 1f to 0f
            else -> {
                val total = clean1 + clean2
                (clean1 / total).toFloat().coerceIn(0.08f, 0.92f) to
                    (clean2 / total).toFloat().coerceIn(0.08f, 0.92f)
            }
        }
    }

    val is1Better = !hasMissing && if (lowerIsBetter) clean1 < clean2 else clean1 > clean2
    val is2Better = !hasMissing && if (lowerIsBetter) clean2 < clean1 else clean2 > clean1
    val adaptiveColor1 = P1TTheme.adaptiveTeamText(color1)
    val adaptiveColor2 = P1TTheme.adaptiveTeamText(color2)

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
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
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = label,
                    color = P1TTheme.textMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                if (lowerIsBetter) {
                    Text(
                        text = "Lower is better",
                        color = P1TTheme.textMuted.copy(alpha = 0.78f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = text2,
                color = if (is2Better) adaptiveColor2 else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.5.sp,
                fontWeight = if (is2Better) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                    .background(P1TTheme.dividerSubtle.copy(alpha = 0.45f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (ratio1 > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio1)
                            .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                            .background(if (is1Better) color1 else color1.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right Bar (Driver 2) - aligns left away from center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(P1TTheme.dividerSubtle.copy(alpha = 0.45f)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (ratio2 > 0f) {
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
}

/**
 * Driver picker modal list with search, portrait avatars, and driverId-safe opposite exclusion.
 */
@Composable
private fun DriverPickerDialog(
    availableDrivers: List<DriverCareerComparison>,
    currentSelectedDriver: DriverCareerComparison,
    oppositeSelectedDriver: DriverCareerComparison? = null,
    onSelect: (DriverCareerComparison) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val adaptiveAccent = P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent)

    val filteredDrivers = remember(availableDrivers, searchQuery) {
        if (searchQuery.isBlank()) {
            availableDrivers
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            availableDrivers.filter { drv ->
                "${drv.firstName} ${drv.lastName}".lowercase(Locale.ROOT).contains(q) ||
                    drv.code.lowercase(Locale.ROOT).contains(q) ||
                    drv.team.displayName.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search driver or team",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = P1TTheme.surfaceInnerCard,
                        unfocusedContainerColor = P1TTheme.surfaceInnerCard,
                        focusedBorderColor = adaptiveAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredDrivers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching drivers",
                            color = P1TTheme.textMuted,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = filteredDrivers,
                            key = { index, it -> "${it.driverId}_${it.code}_$index" }
                        ) { _, drv ->
                            val isCurrent = isSameDriverRecord(drv, currentSelectedDriver)
                            val isOpposite = oppositeSelectedDriver != null &&
                                isSameDriverRecord(drv, oppositeSelectedDriver)
                            val isEnabled = !isOpposite
                            val teamColor = Color(drv.team.colorHex)
                            val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
                            val fullName = "${drv.firstName} ${drv.lastName}".trim().ifBlank { drv.code }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCurrent -> P1TTheme.pillSelectedBg
                                    isOpposite -> P1TTheme.surfaceInnerCard
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = when {
                                    isCurrent -> BorderStroke(1.dp, adaptiveTeamColor)
                                    else -> null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = isEnabled) { onSelect(drv) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DriverPortraitAvatar(
                                        driverCodeOrName = fullName,
                                        teamColor = teamColor,
                                        size = 36.dp,
                                        driverCode = drv.code.takeIf { it.isNotBlank() },
                                        modifier = Modifier.alpha(if (isOpposite) 0.45f else 1f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = drv.code,
                                        color = if (isOpposite) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isOpposite) "$fullName (Opposite slot)" else fullName,
                                        color = if (isOpposite) {
                                            P1TTheme.textMuted
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = drv.team.shortCode,
                                        color = if (isOpposite) {
                                            P1TTheme.textMuted.copy(alpha = 0.5f)
                                        } else {
                                            adaptiveTeamColor
                                        },
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
}
