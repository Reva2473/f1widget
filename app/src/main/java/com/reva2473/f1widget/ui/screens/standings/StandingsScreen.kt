package com.reva2473.f1widget.ui.screens.standings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.ui.components.ApexMotion
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.SegmentedPillToggle
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
private val STANDINGS_INDIGO_GLOW = Color(0xFF1E2952)

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

private fun formatOrdinalRank(position: Int): String {
    if (position <= 0) return "-"
    val mod100 = position % 100
    val suffix = if (mod100 in 11..13) {
        "th"
    } else {
        when (position % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$position$suffix"
}

private fun extractDriverSurname(fullName: String): String {
    val trimmed = fullName.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.substringAfterLast(' ').ifBlank { trimmed }
}

private fun resolveTeamColor(team: F1TeamId): Color =
    if (team != F1TeamId.OTHER) Color(team.colorHex) else NEUTRAL_OTHER_TEAM_COLOR

private fun formatConstructorGap(gapToP1: String): String {
    val trimmedGap = gapToP1.trim()
    return when {
        trimmedGap.equals("Leader", ignoreCase = true) ||
            trimmedGap.startsWith("-") ||
            trimmedGap.startsWith("+") -> trimmedGap
        else -> "-$trimmedGap"
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
    onTabSelected: (String) -> Unit = {},
    navigationToken: Int = 0,
    onRefresh: (() -> Unit)? = null
) {
    var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var lastAppliedInitialTab by rememberSaveable { mutableStateOf(initialTab) }
    var lastAppliedNavigationToken by rememberSaveable { mutableStateOf(navigationToken) }
    LaunchedEffect(initialTab, navigationToken) {
        if (initialTab != lastAppliedInitialTab || navigationToken != lastAppliedNavigationToken) {
            selectedTab = initialTab
            lastAppliedInitialTab = initialTab
            lastAppliedNavigationToken = navigationToken
        }
    }
    var yearPickerOpen by rememberSaveable { mutableStateOf(false) }

    // Preview/fallback data synced directly with SeedDataProvider
    val sampleDrivers = remember { SeedDataProvider.getDriverStandings2026() }
    val sampleConstructors = remember { SeedDataProvider.getConstructorStandings2026() }

    // Distinguish null (preview default for 2026) vs emptyList() (no data for selectedYear)
    val activeDrivers = driverStandings ?: if (selectedYear == 2026) sampleDrivers else emptyList()
    val activeConstructors = constructorStandings ?: if (selectedYear == 2026) sampleConstructors else emptyList()
    val listBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp
    val isDark = P1TTheme.isDark

    P1TRadialBackground(
        modifier = modifier,
        overrideGlowColor = if (isDark) STANDINGS_INDIGO_GLOW else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // EDITORIAL HEADER BAR: < Standings | Year Pill ▾ •••
            StandingsTopBar(
                selectedTab = selectedTab,
                selectedYear = selectedYear,
                onOpenYearPicker = { yearPickerOpen = true },
                onBackClick = onBackClick,
                onToggleTab = {
                    selectedTab = if (selectedTab == StandingsTab.DRIVERS) {
                        StandingsTab.CONSTRUCTORS
                    } else {
                        StandingsTab.DRIVERS
                    }
                },
                onRefresh = onRefresh
            )

            // SEGMENTED PILL TOGGLE: "Drivers" / "Constructors"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                SegmentedPillToggle(
                    options = listOf("Drivers", "Constructors"),
                    selectedIndex = if (selectedTab == StandingsTab.DRIVERS) 0 else 1,
                    onSelect = { index ->
                        selectedTab = if (index == 0) StandingsTab.DRIVERS else StandingsTab.CONSTRUCTORS
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        ApexMotion.tabContentTransform(
                            if (targetState == StandingsTab.CONSTRUCTORS) 1 else -1
                        )
                    },
                    label = "StandingsTabTransition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { tab ->
                    when (tab) {
                        StandingsTab.DRIVERS -> {
                            if (errorMessage != null || activeDrivers.isEmpty()) {
                                StandingsEmptyOrErrorCard(
                                    selectedYear = selectedYear,
                                    selectedTab = tab,
                                    errorMessage = errorMessage,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = listBottomPadding)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
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
                                    selectedTab = tab,
                                    errorMessage = errorMessage,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = listBottomPadding)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
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
 * Editorial top bar with Back button, bold "Standings" title, right-aligned "$selectedYear ▾" pill,
 * and MoreVert menu.
 */
@Composable
private fun StandingsTopBar(
    selectedTab: StandingsTab,
    selectedYear: Int,
    onOpenYearPicker: () -> Unit,
    onBackClick: () -> Unit,
    onToggleTab: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val isDark = P1TTheme.isDark
    val yearPillBg = if (isDark) Color(0x4D1E1E26) else P1TTheme.pillBg
    val yearPillBorder = if (isDark) Color.White.copy(alpha = 0.14f) else P1TTheme.pillBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularHeaderButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Standings",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Right-aligned Year Selector Pill (>= 48.dp touch target)
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
                        .background(yearPillBg)
                        .border(1.dp, yearPillBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$selectedYear ▾",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

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
                    if (onRefresh != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Refresh Standings",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                onRefresh()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (selectedTab == StandingsTab.DRIVERS) {
                                    "Switch to Constructors"
                                } else {
                                    "Switch to Drivers"
                                },
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            moreMenuExpanded = false
                            onToggleTab?.invoke()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Select Season Year",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            moreMenuExpanded = false
                            onOpenYearPicker()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Editorial Driver Standing card with obsidian surface, team accent bar, subtle #position watermark,
 * ordinal rank indicator, 54.dp portrait avatar, tabular points, and expandable car + last-3 races stage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverStandingCard(driver: DriverStanding, selectedYear: Int) {
    var isExpanded by rememberSaveable(selectedYear, driver.driverId, driver.position) {
        mutableStateOf(false)
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DriverChevronRotation"
    )
    val isDark = P1TTheme.isDark
    val teamColor = resolveTeamColor(driver.team)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val carRes = getTeamCarResOrNull(driver.team)
    val rankColor = when (driver.position) {
        1 -> P1TTheme.poleGold
        else -> P1TTheme.textMuted
    }

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

            Box(modifier = Modifier.weight(1f)) {
                // Giant subtle faded #position watermark on the right background
                Text(
                    text = "#${driver.position}",
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isDark) 0.06f else 0.045f
                    ),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Rank + Team, Full Name, Gap, Stats
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatOrdinalRank(driver.position),
                                    color = rankColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "·",
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = driver.team.displayName,
                                    color = adaptiveTeamColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = driver.fullName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
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

                            Spacer(modifier = Modifier.height(7.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatIconLabel(
                                    icon = Icons.Default.EmojiEvents,
                                    contentDescription = "Wins",
                                    label = "${driver.wins} Wins",
                                    emphasized = true
                                )
                                if (driver.podiums > 0 || driver.poles > 0) {
                                    StatIconLabel(
                                        icon = Icons.Default.Leaderboard,
                                        contentDescription = "Podiums",
                                        label = "${driver.podiums} Podiums",
                                        emphasized = true
                                    )
                                    StatIconLabel(
                                        icon = Icons.Default.History,
                                        contentDescription = "Poles",
                                        label = "${driver.poles} Poles",
                                        emphasized = false
                                    )
                                } else if (driver.racesCount > 0) {
                                    StatIconLabel(
                                        icon = Icons.Default.Flag,
                                        contentDescription = "Races",
                                        label = "${driver.racesCount} Races",
                                        emphasized = false
                                    )
                                }
                            }
                        }

                        // Right: 54.dp Driver Portrait + Tabular Points + Expand Chevron
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DriverPortraitAvatar(
                                driverCodeOrName = driver.fullName.ifBlank { driver.code },
                                teamColor = teamColor,
                                size = 54.dp,
                                driverCode = driver.code.takeIf { it.isNotBlank() }
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatStandingsPoints(driver.points),
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "PTS",
                                    color = P1TTheme.textMuted,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = P1TTheme.textMuted,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }
                        }
                    }

                    // Expandable Car Showcase + Average Points + Last 3 Races Stage
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = ApexMotion.cardExpandEnter(),
                        exit = ApexMotion.cardExpandExit()
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
                            if (carRes != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    teamColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                                                    teamColor.copy(alpha = if (isDark) 0.06f else 0.03f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TeamCarShowcaseImage(
                                        team = driver.team,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (driver.racesCount > 0 || driver.avgPtsPerRace > 0.0) {
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
                                        style = TextStyle(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (driver.recentRaces.isNotEmpty()) {
                                if (driver.racesCount > 0 || driver.avgPtsPerRace > 0.0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Text(
                                    text = "Last 3 races (${driver.last3Summary})",
                                    color = adaptiveTeamColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    driver.recentRaces.forEach { race ->
                                        val isWinner = race.position == 1
                                        val posBadgeBg = when (race.position) {
                                            1 -> P1TColors.GoldTrophy
                                            2 -> P1TColors.SilverTrophy
                                            3 -> P1TColors.BronzeTrophy
                                            else -> P1TTheme.pillBg
                                        }
                                        val posBadgeText = if (race.position in 1..3) {
                                            P1TTheme.onColor(posBadgeBg)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.03f)
                                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isWinner) P1TTheme.poleGold.copy(alpha = 0.35f)
                                                    else P1TTheme.borderInnerCard,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(posBadgeBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = race.positionText,
                                                        color = posBadgeText,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                                Text(
                                                    text = race.grandPrixName,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = race.pointsText,
                                                style = TextStyle(fontFeatureSettings = "tnum"),
                                                color = if (isWinner) {
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
                                }
                            } else if (driver.last3Summary.isNotBlank()) {
                                if (driver.racesCount > 0 || driver.avgPtsPerRace > 0.0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = "Season form (${driver.last3Summary})",
                                    color = adaptiveTeamColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                if (driver.racesCount > 0 || driver.avgPtsPerRace > 0.0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = "Detailed race-by-race breakdown unavailable",
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatIconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    label: String,
    emphasized: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = P1TTheme.textMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Editorial Constructor Standing card with dedicated header row, dramatic full-width car stage
 * with team watermark, compact teammate pill chips, and expandable season wins list.
 */
@Composable
private fun ConstructorStandingCard(constructor: ConstructorStanding, selectedYear: Int) {
    var isExpanded by rememberSaveable(selectedYear, constructor.stableKey) {
        mutableStateOf(false)
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ConstructorChevronRotation"
    )
    val isDark = P1TTheme.isDark
    val teamColor = resolveTeamColor(constructor.team)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val carRes = getTeamCarResOrNull(constructor.team)

    val badgeColor = when (constructor.position) {
        1 -> P1TColors.GoldTrophy
        2 -> P1TColors.SilverTrophy
        3 -> P1TColors.BronzeTrophy
        else -> P1TTheme.pillBg
    }
    val badgeTextColor = if (constructor.position <= 3) {
        P1TTheme.onColor(badgeColor)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

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
                // Clean Top Header Row: Rank Badge + Name + Gap + Points + Chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P${constructor.position}",
                                color = badgeTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = constructor.resolvedConstructorName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val subtitleParts = buildList {
                                if (!constructor.gapToP1.isNullOrBlank()) {
                                    add(formatConstructorGap(constructor.gapToP1))
                                }
                                if (constructor.wins > 0) {
                                    add("${constructor.wins} Win${if (constructor.wins == 1) "" else "s"}")
                                }
                            }
                            if (subtitleParts.isNotEmpty()) {
                                Text(
                                    text = subtitleParts.joinToString(" · "),
                                    color = P1TTheme.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatStandingsPoints(constructor.points),
                                style = TextStyle(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "PTS",
                                color = P1TTheme.textMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = P1TTheme.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }
                }

                // Full-width Car Showcase Stage with giant faded team watermark
                if (carRes != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        teamColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                                        teamColor.copy(alpha = if (isDark) 0.06f else 0.03f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        teamColor.copy(alpha = if (isDark) 0.12f else 0.07f),
                                        Color.Transparent,
                                        teamColor.copy(alpha = if (isDark) 0.10f else 0.06f)
                                    )
                                )
                            )
                            .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = constructor.resolvedConstructorName.uppercase(),
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isDark) 0.06f else 0.05f
                            ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        TeamCarShowcaseImage(
                            team = constructor.team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(horizontal = 14.dp)
                        )
                    }
                }

                // Teammate Pill Chips (24.dp avatar + surname + points)
                if (constructor.drivers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        constructor.drivers.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { driver ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = P1TTheme.surfaceInnerCard,
                                        border = BorderStroke(1.dp, P1TTheme.borderInnerCard),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DriverPortraitAvatar(
                                                driverCodeOrName = driver.fullName,
                                                teamColor = teamColor,
                                                size = 24.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = extractDriverSurname(driver.fullName),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = formatStandingsPoints(driver.points),
                                                style = TextStyle(fontFeatureSettings = "tnum"),
                                                color = adaptiveTeamColor,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Expandable Season Race Wins Container
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = ApexMotion.cardExpandEnter(),
                    exit = ApexMotion.cardExpandExit()
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
                        if (constructor.seasonWins.isNotEmpty()) {
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
                                Text(
                                    text = "Season race wins (${constructor.seasonWins.size} wins)",
                                    color = adaptiveTeamColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

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
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = win.winnerFullName,
                                        color = adaptiveTeamColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else if (constructor.wins > 0) {
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
                                Text(
                                    text = "${constructor.wins} Season Win${if (constructor.wins == 1) "" else "s"}",
                                    color = adaptiveTeamColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Detailed race-by-race breakdown unavailable",
                                color = P1TTheme.textMuted,
                                fontSize = 11.5.sp
                            )
                        } else {
                            Text(
                                text = "Detailed race-by-race breakdown unavailable",
                                color = P1TTheme.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
