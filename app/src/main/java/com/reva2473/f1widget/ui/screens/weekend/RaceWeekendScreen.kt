package com.reva2473.f1widget.ui.screens.weekend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.domain.model.TyreStint
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.MultiSectorCircuitMap
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors

enum class WeekendFilterTab(val label: String) {
    WEEKEND_SCHEDULE("Weekend Schedule"),
    RACE_RESULTS("Race Results"),
    QUALIFYING("Qualifying"),
    PIT_STOPS("Pit Stops"),
    RACE_INFO("Race Info"),
    CIRCUIT_INFO("Circuit Info")
}

@Composable
fun SepangSectorMapCanvas(modifier: Modifier = Modifier) {
    MultiSectorCircuitMap(circuitType = "SEPANG", modifier = modifier)
}

@Composable
fun InterlagosSectorMapCanvas(modifier: Modifier = Modifier) {
    MultiSectorCircuitMap(circuitType = "INTERLAGOS", modifier = modifier)
}

@Composable
fun YasMarinaSectorMapCanvas(modifier: Modifier = Modifier) {
    MultiSectorCircuitMap(circuitType = "YAS_MARINA", modifier = modifier)
}

@Composable
fun RaceWeekendScreen(
    weekend: RaceWeekend? = null,
    selectedGrandPrix: String = "",
    onSelectGrandPrix: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf(WeekendFilterTab.WEEKEND_SCHEDULE) }
    var currentSelectedGp by rememberSaveable { mutableStateOf(selectedGrandPrix) }
    var gpDropdownExpanded by remember { mutableStateOf(false) }

    val gpOptions = listOf(
        "Brazilian Grand Prix 2026",
        "São Paulo Grand Prix 2024",
        "Bahrain Grand Prix in Malaysia 2026",
        "Abu Dhabi Grand Prix 2023"
    )

    val effectiveWeekend: RaceWeekend = remember(weekend, currentSelectedGp, selectedGrandPrix) {
        val target = currentSelectedGp.ifBlank { selectedGrandPrix }
        if (target.isBlank() || (weekend != null && (target.contains(weekend.grandPrixName, ignoreCase = true) || weekend.grandPrixName.contains(target.substringBefore(" 20"), ignoreCase = true)))) {
            weekend ?: SeedDataProvider.getBrazilianGpWeekend2026()
        } else {
            when {
                target.contains("Abu Dhabi", ignoreCase = true) -> SeedDataProvider.getAbuDhabi2023Results()
                target.contains("São Paulo", ignoreCase = true) || target.contains("Sao Paulo", ignoreCase = true) -> SeedDataProvider.getSaoPaulo2024Results()
                target.contains("Brazilian", ignoreCase = true) -> SeedDataProvider.getBrazilianGpWeekend2026()
                target.contains("Bahrain", ignoreCase = true) || target.contains("Sepang", ignoreCase = true) -> SeedDataProvider.getNextRaceWeekend()
                else -> weekend ?: SeedDataProvider.getBrazilianGpWeekend2026()
            }
        }
    }

    val selectedGpIndex = remember(effectiveWeekend.grandPrixName) {
        val idx = gpOptions.indexOfFirst {
            it.contains(effectiveWeekend.grandPrixName, ignoreCase = true) ||
            effectiveWeekend.grandPrixName.contains(it.substringBefore(" 20"), ignoreCase = true)
        }
        if (idx >= 0) idx else 0
    }

    // Sample data matching race-weekend-1080.webp & race-stints-720.webp
    val defaultSessions = listOf(
        SessionScheduleItem("FP1", "Free Practice 1", "Fri", "Friday", "06", "Nov", "11:30", "38 days"),
        SessionScheduleItem("Quali", "Qualifying", "Fri", "Friday", "06", "Nov", "15:00", "38 days"),
        SessionScheduleItem("Sprint Shootout", "Sprint Shootout", "Sat", "Saturday", "07", "Nov", "11:00", "37 days"),
        SessionScheduleItem("Sprint", "Sprint Race", "Sat", "Saturday", "07", "Nov", "15:30", "37 days"),
        SessionScheduleItem("Race", "Race Sunday", "Sun", "Sunday", "08", "Nov", "17:00", "Race in 36 days", isRace = true)
    )

    val sampleRaceResults = listOf(
        RaceDriverResult(
            position = 1,
            driverCode = "VER",
            driverFullName = "Max Verstappen",
            team = F1TeamId.RED_BULL,
            finishTimeOrGap = "2:06:54.430",
            points = 26,
            qualifyingPosition = "P17",
            qualifyingTime = "1:23.445",
            fastestLapPosition = "P1",
            fastestLapTime = "1:26.993",
            totalLaps = 69,
            stints = listOf(
                TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 28, pitStopDuration = "23.4s", pitStopLap = 28),
                TyreStint(stintNumber = 2, compound = TyreCompound.WET, lapsCount = 15, pitStopDuration = "21.6s", pitStopLap = 43),
                TyreStint(stintNumber = 3, compound = TyreCompound.INTERMEDIATE, lapsCount = 26)
            )
        ),
        RaceDriverResult(
            position = 2,
            driverCode = "OCO",
            driverFullName = "Esteban Ocon",
            team = F1TeamId.ALPINE,
            finishTimeOrGap = "+19.477",
            points = 18,
            qualifyingPosition = "P4",
            qualifyingTime = "1:23.680",
            fastestLapPosition = "P5",
            fastestLapTime = "1:28.102",
            totalLaps = 69,
            stints = listOf(
                TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 43, pitStopDuration = "22.1s", pitStopLap = 43),
                TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 26)
            )
        ),
        RaceDriverResult(
            position = 3,
            driverCode = "GAS",
            driverFullName = "Pierre Gasly",
            team = F1TeamId.ALPINE,
            finishTimeOrGap = "+22.532",
            points = 15,
            qualifyingPosition = "P13",
            qualifyingTime = "1:24.012",
            fastestLapPosition = "P6",
            fastestLapTime = "1:28.341",
            totalLaps = 69,
            stints = listOf(
                TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 43, pitStopDuration = "21.8s", pitStopLap = 43),
                TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 26)
            )
        ),
        RaceDriverResult(
            position = 4,
            driverCode = "RUS",
            driverFullName = "George Russell",
            team = F1TeamId.MERCEDES,
            finishTimeOrGap = "+23.265",
            points = 12,
            qualifyingPosition = "P2",
            qualifyingTime = "1:23.510",
            fastestLapPosition = "P3",
            fastestLapTime = "1:27.420",
            totalLaps = 69,
            stints = listOf(
                TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 29, pitStopDuration = "22.5s", pitStopLap = 29),
                TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 40)
            )
        ),
        RaceDriverResult(
            position = 5,
            driverCode = "LEC",
            driverFullName = "Charles Leclerc",
            team = F1TeamId.FERRARI,
            finishTimeOrGap = "+30.177",
            points = 10,
            qualifyingPosition = "P6",
            qualifyingTime = "1:23.750",
            fastestLapPosition = "P4",
            fastestLapTime = "1:27.604",
            totalLaps = 69,
            stints = listOf(
                TyreStint(stintNumber = 1, compound = TyreCompound.INTERMEDIATE, lapsCount = 25, pitStopDuration = "21.4s", pitStopLap = 25),
                TyreStint(stintNumber = 2, compound = TyreCompound.INTERMEDIATE, lapsCount = 44)
            )
        )
    )

    val activeSessions = if (effectiveWeekend.sessions.isNotEmpty()) effectiveWeekend.sessions else defaultSessions
    val activeResults = if (effectiveWeekend.results.isNotEmpty()) effectiveWeekend.results else sampleRaceResults

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // HEADER WITH GP SELECTOR
            WeekendTopBar(
                currentGpName = gpOptions[selectedGpIndex],
                options = gpOptions,
                expanded = gpDropdownExpanded,
                onExpandedChange = { gpDropdownExpanded = it },
                onSelectOption = { index ->
                    val chosenGp = gpOptions[index]
                    currentSelectedGp = chosenGp
                    onSelectGrandPrix(chosenGp)
                },
                onBackClick = onBackClick,
                onToggleView = {
                    selectedFilter = if (selectedFilter == WeekendFilterTab.CIRCUIT_INFO) {
                        WeekendFilterTab.WEEKEND_SCHEDULE
                    } else {
                        WeekendFilterTab.CIRCUIT_INFO
                    }
                }
            )

            // SCROLLABLE CONTENT
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // CIRCUIT HERO CARD
                item {
                    WeekendCircuitHeaderCard(
                        grandPrixName = effectiveWeekend.grandPrixName,
                        circuitName = effectiveWeekend.circuitName,
                        location = effectiveWeekend.location,
                        roundText = "ROUND ${effectiveWeekend.round}",
                        lapsText = "RACE - ${effectiveWeekend.totalLaps} LAPS",
                        circuitType = effectiveWeekend.circuitType,
                        dateRangeText = effectiveWeekend.dateRangeText,
                        circuitId = effectiveWeekend.circuitId.ifBlank { effectiveWeekend.circuitType }
                    )
                }

                // FILTER CHIPS ROW
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    WeekendFilterChips(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // TAB CONTENT
                when (selectedFilter) {
                    WeekendFilterTab.WEEKEND_SCHEDULE -> {
                        item {
                            WeekendScheduleSection(sessions = activeSessions)
                        }
                    }
                    WeekendFilterTab.RACE_RESULTS -> {
                        items(activeResults, key = { it.driverCode }) { result ->
                            RaceResultExpandedCard(result = result)
                        }
                    }
                    WeekendFilterTab.PIT_STOPS -> {
                        items(activeResults, key = { "pit_${it.driverCode}" }) { result ->
                            PitStopsSummaryCard(result = result)
                        }
                    }
                    WeekendFilterTab.QUALIFYING -> {
                        val sortedQuali = activeResults.sortedBy {
                            it.qualifyingPosition.removePrefix("P").toIntOrNull() ?: Int.MAX_VALUE
                        }
                        itemsIndexed(sortedQuali, key = { _, it -> "quali_${it.driverCode}" }) { index, result ->
                            QualifyingResultRow(result = result, rank = index + 1)
                        }
                    }
                    WeekendFilterTab.RACE_INFO -> {
                        item {
                            RaceInfoSection(weekend = effectiveWeekend, results = activeResults)
                        }
                    }
                    WeekendFilterTab.CIRCUIT_INFO -> {
                        item {
                            CircuitInfoSection(weekend = effectiveWeekend)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(95.dp))
                }
            }
        }

        // Floating Pill Bottom Bar
        FloatingPillBottomBar(
            currentTab = "Season",
            onSelectTab = { tab ->
                when (tab) {
                    "Home" -> onBackClick()
                    "Season" -> Unit
                    "Standings" -> onTabSelected("Standings")
                    "Settings" -> onTabSelected("Settings")
                    else -> onTabSelected(tab)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Top bar with Back Button, Dropdown GP selector, and More Vert
 */
@Composable
private fun WeekendTopBar(
    currentGpName: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectOption: (Int) -> Unit,
    onBackClick: () -> Unit,
    onToggleView: (() -> Unit)? = null
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularHeaderButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = P1TColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // GP Picker Dropdown
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color(0xFF383844), RoundedCornerShape(20.dp))
                    .clickable { onExpandedChange(true) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentGpName,
                    color = P1TColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select GP",
                    tint = P1TColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(P1TColors.SurfaceElevated)
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == currentGpName) primaryAccent else P1TColors.TextPrimary,
                                fontWeight = if (option == currentGpName) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelectOption(index)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }

        Box {
            CircularHeaderButton(onClick = { moreMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = P1TColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false },
                modifier = Modifier.background(P1TColors.SurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Next Grand Prix ↻", color = P1TColors.TextPrimary) },
                    onClick = {
                        moreMenuExpanded = false
                        val currentIdx = options.indexOf(currentGpName)
                        val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % options.size
                        onSelectOption(nextIdx)
                    }
                )
                if (onToggleView != null) {
                    DropdownMenuItem(
                        text = { Text("Toggle Circuit / Schedule", color = P1TColors.TextPrimary) },
                        onClick = {
                            moreMenuExpanded = false
                            onToggleView()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Header card with circuit name, location, date, "ROUND 20 🚩" / "RACE - 69 LAPS 🚩" pill,
 * and MultiSectorCircuitMap.
 */
@Composable
fun WeekendCircuitHeaderCard(
    grandPrixName: String,
    circuitName: String,
    location: String,
    roundText: String,
    lapsText: String,
    circuitType: String = "SEPANG",
    dateRangeText: String = "",
    circuitId: String = circuitType
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent

            // Badges row: ROUND 20 🚩 | RACE - 69 LAPS 🚩
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF241416))
                        .border(1.dp, primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = roundText,
                        color = primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E28))
                        .border(1.dp, Color(0xFF383848), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = lapsText,
                        color = P1TColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grand Prix Name & Subtitle
            Text(
                text = grandPrixName,
                color = P1TColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            val subtitle = if (dateRangeText.isNotBlank()) {
                "$circuitName · $location · $dateRangeText"
            } else {
                "$circuitName · $location"
            }
            Text(
                text = subtitle,
                color = P1TColors.TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-sector colored circuit map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                when (circuitType.uppercase()) {
                    "YAS_MARINA", "YAS MARINA", "ABU_DHABI", "ABU DHABI" -> YasMarinaSectorMapCanvas(modifier = Modifier.fillMaxSize())
                    "INTERLAGOS", "BRAZIL", "SAO_PAULO", "SAO PAULO" -> InterlagosSectorMapCanvas(modifier = Modifier.fillMaxSize())
                    else -> SepangSectorMapCanvas(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Horizontal Filter Chips: Weekend Schedule, Race Results, Qualifying, Pit Stops, Race Info, Circuit Info
 */
@Composable
private fun WeekendFilterChips(
    selected: WeekendFilterTab,
    onSelect: (WeekendFilterTab) -> Unit
) {
    val scrollState = rememberScrollState()
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val onAccentColor = if (primaryAccent.luminance() > 0.45f) Color(0xFF0F0F10) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeekendFilterTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) primaryAccent else P1TColors.SurfaceCard)
                    .border(
                        1.dp,
                        if (isSelected) primaryAccent else P1TColors.BorderSubtle,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) onAccentColor else P1TColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Schedule list: 08 Nov Race Sunday · 17:00 with red 🚩 Race in 36 days pill + bell icon, etc. + Race History
 */
@Composable
private fun WeekendScheduleSection(sessions: List<SessionScheduleItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sessions.forEach { session ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Block
                        Column(
                            modifier = Modifier
                                .width(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E26))
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = session.dateDay,
                                color = P1TColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = session.dateMonth.uppercase(),
                                color = P1TColors.TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Session Title
                        Column {
                            Text(
                                text = session.fullName,
                                color = P1TColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${session.fullDayName} · ${session.localTime}",
                                color = P1TColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val primaryAccent = LocalF1Accent.current.primaryAccent
                    val context = LocalContext.current
                    var reminderSet by rememberSaveable(session.fullName) { mutableStateOf(false) }

                    // Countdown Pill & Bell Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (session.isRace) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2E1214))
                                    .border(1.dp, primaryAccent, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = session.countdownBadge,
                                    color = primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Text(
                                text = session.countdownBadge,
                                color = P1TColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                reminderSet = !reminderSet
                                val msg = if (reminderSet) "Reminder set for ${session.fullName}" else "Reminder removed for ${session.fullName}"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (reminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = if (reminderSet) "Reminder Set" else "Set Reminder",
                                tint = if (reminderSet || session.isRace) primaryAccent else P1TColors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable Race Result card tinted in team color matching race-stints-720.webp:
 * Max Verstappen 2:06:54.430 26 PTS
 * When expanded: horizontal Stints visual pills (16L yellow Medium -> 21.6s Lap 16 -> 27L white Hard -> ...),
 * Lap Times (58 ▾), and Driver Stats (Qualifying Position P1, Qualifying Time, Fastest Lap Position, Fastest Lap Time).
 */
@Composable
private fun RaceResultExpandedCard(result: RaceDriverResult) {
    var isExpanded by rememberSaveable(result.driverCode) { mutableStateOf(result.position == 1) }
    val teamColor = Color(result.team.colorHex)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Team Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Main Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DriverPortraitAvatar(
                            driverCodeOrName = result.driverCode,
                            teamColor = teamColor,
                            size = 38.dp,
                            positionBadge = "P${result.position}"
                        )

                        Column {
                            Text(
                                text = result.driverFullName,
                                color = P1TColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = result.team.displayName,
                                color = teamColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Time & Points
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = result.finishTimeOrGap,
                                color = P1TColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${result.points} PTS",
                                color = teamColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = P1TColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Expanded Section matching race-stints-720.webp
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        // SECTION 1: STINTS VISUAL PILLS
                        Text(
                            text = "STINTS",
                            color = P1TColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal Tyre Stints Flow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            result.stints.forEachIndexed { idx, stint ->
                                // Stint Box
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(P1TColors.SurfaceInnerCard)
                                        .border(1.dp, Color(stint.compound.colorHex).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TyreCompoundBadge(compound = stint.compound, sizeDp = 18)
                                    Text(
                                        text = "${stint.lapsCount}L",
                                        color = P1TColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Pit Stop connector badge if present
                                if (stint.pitStopDuration != null && stint.pitStopLap != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "⏱️ ${stint.pitStopDuration}",
                                            color = P1TColors.TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Lap ${stint.pitStopLap}",
                                            color = P1TColors.TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SECTION 2: DRIVER STATS GRID
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(P1TColors.SurfaceInnerCard)
                                .border(1.dp, Color(0xFF262630), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Qualifying Position & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Qualifying Position",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${result.qualifyingPosition} (${result.qualifyingTime})",
                                    color = P1TColors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Row 2: Fastest Lap Position & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Fastest Lap Position",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${result.fastestLapPosition} (${result.fastestLapTime})",
                                    color = if (result.fastestLapPosition == "P1") P1TColors.SectorPurple else P1TColors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Row 3: Total Laps Completed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Laps",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${result.totalLaps} Laps",
                                    color = P1TColors.TextPrimary,
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
 * Pit Stops Summary Card
 */
@Composable
private fun PitStopsSummaryCard(result: RaceDriverResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DriverPortraitAvatar(
                        driverCodeOrName = result.driverCode,
                        teamColor = Color(result.team.colorHex),
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${result.position}. ${result.driverFullName}",
                        color = P1TColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val pitStopsCount = result.stints.count { it.pitStopDuration != null }.takeIf { it > 0 }
                    ?: (result.stints.size - 1).coerceAtLeast(0)
                Text(
                    text = "$pitStopsCount Pit Stop(s)",
                    color = Color(result.team.colorHex),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            result.stints.filter { it.pitStopDuration != null }.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Stop at Lap ${s.pitStopLap}", color = P1TColors.TextSecondary, fontSize = 11.sp)
                    Text(text = "${s.pitStopDuration}", color = P1TColors.SectorGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Qualifying Result Row
 */
@Composable
private fun QualifyingResultRow(result: RaceDriverResult, rank: Int? = null) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriverPortraitAvatar(
                    driverCodeOrName = result.driverCode,
                    teamColor = Color(result.team.colorHex),
                    size = 28.dp,
                    positionBadge = rank?.let { "P$it" } ?: result.qualifyingPosition
                )
                Text(text = result.driverFullName, color = P1TColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = result.qualifyingTime, color = Color(result.team.colorHex), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Race Info Section
 */
@Composable
private fun RaceInfoSection(
    weekend: RaceWeekend,
    results: List<RaceDriverResult> = emptyList()
) {
    val laps = weekend.totalLaps.takeIf { it > 0 } ?: 71
    val lengthKm = weekend.circuitLengthKm.toDoubleOrNull() ?: 4.309
    val raceDistanceKm = String.format(java.util.Locale.US, "%.3f km", laps * lengthKm)

    val fastestResult = results.firstOrNull { it.fastestLapPosition == "P1" || it.fastestLapTime.isNotBlank() }
    val fastestLapText = if (fastestResult != null && fastestResult.fastestLapTime.isNotBlank()) {
        "${fastestResult.fastestLapTime} (${fastestResult.driverFullName})"
    } else {
        when {
            weekend.circuitName.contains("Sepang", ignoreCase = true) -> "1:34.012 (Lewis Hamilton, 2017)"
            weekend.circuitName.contains("Yas Marina", ignoreCase = true) -> "1:26.103 (Max Verstappen, 2021)"
            else -> "1:10.540 (Valtteri Bottas, 2018)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
            modifier = Modifier.fillMaxWidth().border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text(text = "RACE INFORMATION", color = P1TColors.AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val firstGpYear = when {
                    weekend.circuitName.contains("Sepang", ignoreCase = true) -> "1999"
                    weekend.circuitName.contains("Yas Marina", ignoreCase = true) -> "2009"
                    else -> "1973"
                }
                Text(text = "First Grand Prix: $firstGpYear", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Number of Laps: $laps", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Circuit Length: ${weekend.circuitLengthKm} km", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Race Distance: $raceDistanceKm", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Lap Record: $fastestLapText", color = P1TColors.SectorPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Circuit Info Section
 */
@Composable
private fun CircuitInfoSection(
    weekend: RaceWeekend
) {
    val direction = when {
        weekend.circuitName.contains("Sepang", ignoreCase = true) -> "Clockwise"
        weekend.circuitName.contains("Yas Marina", ignoreCase = true) -> "Anti-clockwise"
        else -> "Anti-clockwise"
    }
    val drsZones = when {
        weekend.circuitName.contains("Sepang", ignoreCase = true) -> "2"
        weekend.circuitName.contains("Yas Marina", ignoreCase = true) -> "2"
        else -> "2"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = P1TColors.SurfaceCard),
            modifier = Modifier.fillMaxWidth().border(1.dp, P1TColors.BorderSubtle, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text(text = "CIRCUIT SPECIFICATIONS", color = Color(0xFF00B4D8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Circuit: ${weekend.circuitName}", color = P1TColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Location: ${weekend.location}", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Circuit Length: ${weekend.circuitLengthKm} km", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Number of Corners: ${weekend.totalCorners} corners", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "Direction: $direction", color = P1TColors.TextSecondary, fontSize = 12.sp)
                Text(text = "DRS Zones: $drsZones", color = P1TColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
