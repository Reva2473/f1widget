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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.components.MultiSectorCircuitMap
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme

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
    modifier: Modifier = Modifier,
    weekend: RaceWeekend? = null,
    selectedGrandPrix: String = "",
    schedule: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026),
    initialTab: WeekendFilterTab = WeekendFilterTab.WEEKEND_SCHEDULE,
    reminderSessionKeys: Set<String> = emptySet(),
    showBottomBar: Boolean = true,
    onToggleSessionReminder: (sessionKey: String, enabled: Boolean) -> Unit = { _, _ -> },
    onSelectGrandPrix: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onTabSelected: (String) -> Unit = {}
) {
    var selectedFilter by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedFilter = initialTab
    }
    var currentSelectedGp by rememberSaveable(
        selectedGrandPrix,
        weekend?.season,
        weekend?.round,
        weekend?.grandPrixName
    ) {
        mutableStateOf(selectedGrandPrix)
    }
    LaunchedEffect(selectedGrandPrix, weekend?.season, weekend?.round, weekend?.grandPrixName) {
        currentSelectedGp = selectedGrandPrix
    }
    var gpDropdownExpanded by remember { mutableStateOf(false) }

    val yearRegex = remember { Regex("\\b(19\\d{2}|20\\d{2})\\b") }
    val formatGpLabel: (RaceWeekend) -> String = { rw ->
        if (rw.grandPrixName.contains(rw.season.toString())) rw.grandPrixName
        else "${rw.grandPrixName} ${rw.season}"
    }

    val gpOptions = remember(schedule, weekend) {
        buildList {
            weekend?.let { add(formatGpLabel(it)) }
            schedule.forEach { rw -> add(formatGpLabel(rw)) }
            add("São Paulo Grand Prix 2024")
            add("Abu Dhabi Grand Prix 2023")
        }.distinct()
    }

    val effectiveWeekend: RaceWeekend = remember(weekend, schedule, currentSelectedGp, selectedGrandPrix) {
        val target = currentSelectedGp.ifBlank { selectedGrandPrix }.trim()
        if (target.isBlank()) {
            weekend ?: schedule.firstOrNull() ?: SeedDataProvider.getBrazilianGpWeekend2026()
        } else {
            val explicitYear = yearRegex.find(target)?.groupValues?.get(1)?.toIntOrNull()
            val cleanTarget = target.replace(yearRegex, "").replace(Regex("\\s+"), " ").trim()

            fun isLocationCompatible(rw: RaceWeekend): Boolean {
                val mentionsMalaysia = cleanTarget.contains("Malaysia", ignoreCase = true) ||
                    cleanTarget.contains("Sepang", ignoreCase = true) ||
                    cleanTarget.contains("Kuala Lumpur", ignoreCase = true)
                val rwIsMalaysia = rw.grandPrixName.contains("Malaysia", ignoreCase = true) ||
                    rw.location.contains("Malaysia", ignoreCase = true) ||
                    rw.circuitName.contains("Sepang", ignoreCase = true) ||
                    rw.circuitType.equals("SEPANG", ignoreCase = true) ||
                    rw.circuitId.equals("SEPANG", ignoreCase = true)
                if (mentionsMalaysia && !rwIsMalaysia) return false

                val mentionsSakhir = cleanTarget.contains("Sakhir", ignoreCase = true) ||
                    cleanTarget.contains("Bahrain International", ignoreCase = true)
                if (mentionsSakhir && rwIsMalaysia) return false

                val onlyBahrainNoMalaysia = cleanTarget.contains("Bahrain", ignoreCase = true) && !mentionsMalaysia
                if (onlyBahrainNoMalaysia && rwIsMalaysia) return false

                return true
            }

            fun matchesExactly(rw: RaceWeekend): Boolean {
                if (explicitYear != null && rw.season != explicitYear) return false
                if (!isLocationCompatible(rw)) return false
                val label = formatGpLabel(rw)
                return label.equals(target, ignoreCase = true) ||
                    rw.grandPrixName.equals(target, ignoreCase = true) ||
                    (cleanTarget.isNotBlank() && rw.grandPrixName.equals(cleanTarget, ignoreCase = true)) ||
                    rw.circuitName.equals(target, ignoreCase = true) ||
                    (cleanTarget.isNotBlank() && rw.circuitName.equals(cleanTarget, ignoreCase = true)) ||
                    rw.location.equals(target, ignoreCase = true) ||
                    (cleanTarget.isNotBlank() && rw.location.equals(cleanTarget, ignoreCase = true))
            }

            fun matchesLoosely(rw: RaceWeekend): Boolean {
                if (explicitYear != null && rw.season != explicitYear) return false
                if (cleanTarget.isBlank()) return false
                if (!isLocationCompatible(rw)) return false
                return rw.grandPrixName.contains(cleanTarget, ignoreCase = true) ||
                    cleanTarget.contains(rw.grandPrixName, ignoreCase = true) ||
                    rw.circuitName.contains(cleanTarget, ignoreCase = true) ||
                    cleanTarget.contains(rw.circuitName, ignoreCase = true) ||
                    rw.location.contains(cleanTarget, ignoreCase = true) ||
                    cleanTarget.contains(rw.location, ignoreCase = true)
            }

            // 1. Exact match against schedule
            val exactFromSchedule = schedule.firstOrNull(::matchesExactly)
            if (exactFromSchedule != null) {
                if (weekend != null &&
                    weekend.season == exactFromSchedule.season &&
                    weekend.round == exactFromSchedule.round &&
                    weekend.grandPrixName.equals(exactFromSchedule.grandPrixName, ignoreCase = true)
                ) {
                    weekend
                } else {
                    exactFromSchedule
                }
            } else if (weekend != null && matchesExactly(weekend)) {
                // 2. Exact match against passed-in weekend
                weekend
            } else if (explicitYear != null && explicitYear != 2026) {
                // 3. Historical/year-specific lookup when explicitYear != 2026
                when {
                    explicitYear == 2023 && target.contains("Abu Dhabi", ignoreCase = true) ->
                        SeedDataProvider.getAbuDhabi2023Results()
                    explicitYear == 2024 && (
                        target.contains("São Paulo", ignoreCase = true) ||
                            target.contains("Sao Paulo", ignoreCase = true) ||
                            target.contains("Brazilian", ignoreCase = true)
                        ) -> SeedDataProvider.getSaoPaulo2024Results()
                    else -> {
                        val historical = SeedDataProvider.getSeasonSchedule(explicitYear)
                        historical.firstOrNull(::matchesExactly)
                            ?: historical.firstOrNull(::matchesLoosely)
                            ?: weekend?.takeIf(::matchesLoosely)
                            ?: when {
                                target.contains("São Paulo", ignoreCase = true) ||
                                    target.contains("Sao Paulo", ignoreCase = true) ||
                                    target.contains("Brazilian", ignoreCase = true) ->
                                    historical.firstOrNull { it.circuitType == "INTERLAGOS" }
                                        ?: SeedDataProvider.getSaoPaulo2024Results()
                                target.contains("Abu Dhabi", ignoreCase = true) ->
                                    historical.firstOrNull { it.circuitType == "YAS_MARINA" }
                                        ?: SeedDataProvider.getAbuDhabi2023Results()
                                else -> historical.firstOrNull() ?: SeedDataProvider.getBrazilianGpWeekend2026()
                            }
                    }
                }
            } else {
                // 4. Loose match against schedule (rejecting Sakhir vs Malaysia confusion) -> loose match against weekend -> safe fallbacks
                val looseFromSchedule = schedule.firstOrNull(::matchesLoosely)
                when {
                    looseFromSchedule != null && weekend != null &&
                        weekend.season == looseFromSchedule.season &&
                        weekend.round == looseFromSchedule.round &&
                        weekend.grandPrixName.equals(looseFromSchedule.grandPrixName, ignoreCase = true) -> weekend
                    looseFromSchedule != null -> looseFromSchedule
                    weekend != null && matchesLoosely(weekend) -> weekend
                    (target.contains("São Paulo", ignoreCase = true) || target.contains("Sao Paulo", ignoreCase = true)) ->
                        if (explicitYear == 2026) SeedDataProvider.getBrazilianGpWeekend2026()
                        else SeedDataProvider.getSaoPaulo2024Results()
                    target.contains("Abu Dhabi", ignoreCase = true) ->
                        if (explicitYear == 2026) {
                            SeedDataProvider.getSeasonSchedule(2026)
                                .firstOrNull { it.grandPrixName.contains("Abu Dhabi", ignoreCase = true) }
                                ?: SeedDataProvider.getAbuDhabi2023Results()
                        } else {
                            SeedDataProvider.getAbuDhabi2023Results()
                        }
                    target.contains("Brazilian", ignoreCase = true) -> SeedDataProvider.getBrazilianGpWeekend2026()
                    target.contains("Bahrain", ignoreCase = true) ||
                        target.contains("Sepang", ignoreCase = true) ||
                        target.contains("Malaysia", ignoreCase = true) -> {
                        if ((explicitYear == null || explicitYear == 2026) &&
                            target.contains("Bahrain", ignoreCase = true) &&
                            !target.contains("Malaysia", ignoreCase = true) &&
                            !target.contains("Sepang", ignoreCase = true)
                        ) {
                            SeedDataProvider.getSeasonSchedule(2026)
                                .firstOrNull { it.grandPrixName.equals("Bahrain Grand Prix", ignoreCase = true) }
                                ?: SeedDataProvider.getNextRaceWeekend()
                        } else {
                            SeedDataProvider.getNextRaceWeekend()
                        }
                    }
                    target.contains("British", ignoreCase = true) || target.contains("Silverstone", ignoreCase = true) ->
                        SeedDataProvider.getSeasonSchedule(explicitYear ?: 1950)
                            .firstOrNull { it.circuitType == "SILVERSTONE" }
                            ?: SeedDataProvider.getNextRaceWeekend()
                    target.contains("Monaco", ignoreCase = true) ->
                        SeedDataProvider.getSeasonSchedule(explicitYear ?: 1950)
                            .firstOrNull { it.circuitType == "MONACO" }
                            ?: SeedDataProvider.getNextRaceWeekend()
                    target.contains("Monza", ignoreCase = true) || target.contains("Italian", ignoreCase = true) ->
                        SeedDataProvider.getSeasonSchedule(explicitYear ?: 1950)
                            .firstOrNull { it.circuitType == "MONZA" }
                            ?: SeedDataProvider.getNextRaceWeekend()
                    else -> weekend ?: SeedDataProvider.getBrazilianGpWeekend2026()
                }
            }
        }
    }

    val selectedGpIndex = remember(gpOptions, effectiveWeekend.grandPrixName, effectiveWeekend.season, effectiveWeekend.circuitId) {
        val effectiveLabel = formatGpLabel(effectiveWeekend)
        val exactIdx = gpOptions.indexOfFirst { option ->
            option.equals(effectiveLabel, ignoreCase = true)
        }
        if (exactIdx >= 0) {
            exactIdx
        } else {
            val effectiveIsMalaysia = effectiveWeekend.grandPrixName.contains("Malaysia", ignoreCase = true) ||
                effectiveWeekend.circuitName.contains("Sepang", ignoreCase = true) ||
                effectiveWeekend.circuitType.equals("SEPANG", ignoreCase = true) ||
                effectiveWeekend.circuitId.equals("SEPANG", ignoreCase = true)
            gpOptions.indexOfFirst { option ->
                val optionIsMalaysia = option.contains("Malaysia", ignoreCase = true) ||
                    option.contains("Sepang", ignoreCase = true)
                option.contains(effectiveWeekend.season.toString()) &&
                    option.contains(effectiveWeekend.grandPrixName, ignoreCase = true) &&
                    optionIsMalaysia == effectiveIsMalaysia
            }
        }
    }

    val activeSessions = effectiveWeekend.sessions
    val activeResults = effectiveWeekend.results

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // HEADER WITH GP SELECTOR
            WeekendTopBar(
                currentGpName = gpOptions.getOrElse(selectedGpIndex) { formatGpLabel(effectiveWeekend) },
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
                        if (activeSessions.isEmpty()) {
                            item {
                                EmptyWeekendTabCard(
                                    title = "Session schedule unavailable",
                                    subtitle = "Session timetable is not available for ${effectiveWeekend.grandPrixName} (${effectiveWeekend.season})"
                                )
                            }
                        } else {
                            item {
                                WeekendScheduleSection(
                                    sessions = activeSessions,
                                    season = effectiveWeekend.season,
                                    round = effectiveWeekend.round,
                                    reminderSessionKeys = reminderSessionKeys,
                                    onToggleSessionReminder = onToggleSessionReminder
                                )
                            }
                        }
                    }
                    WeekendFilterTab.RACE_RESULTS -> {
                        if (activeResults.isEmpty()) {
                            item {
                                EmptyWeekendTabCard(
                                    title = "Race Results Pending",
                                    subtitle = "Race results available after the Grand Prix"
                                )
                            }
                        } else {
                            items(
                                items = activeResults,
                                key = { "${effectiveWeekend.season}_${effectiveWeekend.round}_${it.driverCode}" }
                            ) { result ->
                                RaceResultExpandedCard(
                                    result = result,
                                    season = effectiveWeekend.season,
                                    round = effectiveWeekend.round
                                )
                            }
                        }
                    }
                    WeekendFilterTab.PIT_STOPS -> {
                        if (activeResults.isEmpty()) {
                            item {
                                EmptyWeekendTabCard(
                                    title = "Pit Stop Data Pending",
                                    subtitle = "Pit stop and tyre stint data available after the Grand Prix"
                                )
                            }
                        } else {
                            items(
                                items = activeResults,
                                key = { "pit_${effectiveWeekend.season}_${effectiveWeekend.round}_${it.driverCode}" }
                            ) { result ->
                                PitStopsSummaryCard(result = result)
                            }
                        }
                    }
                    WeekendFilterTab.QUALIFYING -> {
                        if (activeResults.isEmpty()) {
                            item {
                                EmptyWeekendTabCard(
                                    title = "Qualifying Results Pending",
                                    subtitle = "Qualifying classification available after the qualifying session"
                                )
                            }
                        } else {
                            val sortedQuali = activeResults.sortedBy {
                                it.qualifyingPosition.removePrefix("P").toIntOrNull() ?: Int.MAX_VALUE
                            }
                            itemsIndexed(
                                items = sortedQuali,
                                key = { _, it -> "quali_${effectiveWeekend.season}_${effectiveWeekend.round}_${it.driverCode}" }
                            ) { index, result ->
                                QualifyingResultRow(result = result, rank = index + 1)
                            }
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
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(96.dp)
                    )
                }
            }
        }

        // Floating Pill Bottom Bar
        if (showBottomBar) {
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
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
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
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        // GP Picker Dropdown
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onExpandedChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(P1TTheme.pillBg)
                        .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentGpName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select GP",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == currentGpName) adaptiveAccent else MaterialTheme.colorScheme.onSurface,
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
                    text = { Text("Next Grand Prix ↻", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        moreMenuExpanded = false
                        val currentIdx = options.indexOf(currentGpName)
                        val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % options.size
                        onSelectOption(nextIdx)
                    }
                )
                if (onToggleView != null) {
                    DropdownMenuItem(
                        text = { Text("Toggle Circuit / Schedule", color = MaterialTheme.colorScheme.onSurface) },
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
    modifier: Modifier = Modifier,
    circuitType: String = "SEPANG",
    dateRangeText: String = "",
    circuitId: String = circuitType
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            val primaryAccent = LocalF1Accent.current.primaryAccent
            val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)

            // Badges row: ROUND 20 🚩 | RACE - 69 LAPS 🚩
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(P1TTheme.accentPillBg(primaryAccent))
                        .border(1.dp, primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = roundText,
                        color = adaptiveAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(P1TTheme.pillBg)
                        .border(1.dp, P1TTheme.pillBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = lapsText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grand Prix Name & Subtitle
            Text(
                text = grandPrixName,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-sector colored circuit map switched dynamically on circuitId (falling back to circuitType)
            val resolvedCircuitKey = circuitId.ifBlank { circuitType }.uppercase()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                when (resolvedCircuitKey) {
                    "YAS_MARINA", "YAS MARINA", "ABU_DHABI", "ABU DHABI" -> YasMarinaSectorMapCanvas(modifier = Modifier.fillMaxSize())
                    "INTERLAGOS", "BRAZIL", "SAO_PAULO", "SAO PAULO" -> InterlagosSectorMapCanvas(modifier = Modifier.fillMaxSize())
                    "SEPANG", "MALAYSIA" -> SepangSectorMapCanvas(modifier = Modifier.fillMaxSize())
                    else -> MultiSectorCircuitMap(circuitType = resolvedCircuitKey, modifier = Modifier.fillMaxSize())
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
    val onAccentColor = P1TTheme.onColor(primaryAccent)

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
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) primaryAccent else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) primaryAccent else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (isSelected) onAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWeekendTabCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Schedule list: 08 Nov Race Sunday · 17:00 with red 🚩 Race in 36 days pill + bell icon, etc. + Race History
 */
@Composable
private fun WeekendScheduleSection(
    sessions: List<SessionScheduleItem>,
    season: Int = 2026,
    round: Int = 0,
    reminderSessionKeys: Set<String> = emptySet(),
    onToggleSessionReminder: (sessionKey: String, enabled: Boolean) -> Unit = { _, _ -> }
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sessions.forEach { session ->
            val sessionCode = session.shortName.ifBlank { session.fullName }
            val sessionKey = "${season}_${round}_$sessionCode"
            val isReminderSet = sessionKey in reminderSessionKeys

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                        // Date Block
                        Column(
                            modifier = Modifier
                                .width(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(P1TTheme.pillBg)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = session.dateDay,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = session.dateMonth.uppercase(),
                                color = P1TTheme.textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Session Title
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.fullName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${session.fullDayName} · ${session.localTime}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Countdown Pill & Bell Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (session.isRace) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(P1TTheme.accentPillBg(primaryAccent))
                                    .border(1.dp, primaryAccent, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = session.countdownBadge,
                                    color = adaptiveAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Text(
                                text = session.countdownBadge,
                                color = P1TTheme.textMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextEnabled = !isReminderSet
                                onToggleSessionReminder(sessionKey, nextEnabled)
                                val msg = if (nextEnabled) {
                                    "Reminder set for ${session.fullName}"
                                } else {
                                    "Reminder removed for ${session.fullName}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isReminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = if (isReminderSet) {
                                    "Remove reminder for ${session.fullName}"
                                } else {
                                    "Set reminder for ${session.fullName}"
                                },
                                tint = if (isReminderSet || session.isRace) adaptiveAccent else P1TTheme.textMuted,
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
private fun RaceResultExpandedCard(
    result: RaceDriverResult,
    season: Int = 2026,
    round: Int = 0
) {
    var isExpanded by rememberSaveable(season, round, result.driverCode) { mutableStateOf(result.position == 1) }
    val teamColor = Color(result.team.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DriverPortraitAvatar(
                            driverCodeOrName = result.driverCode,
                            teamColor = teamColor,
                            size = 38.dp,
                            positionBadge = "P${result.position}"
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.driverFullName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.team.displayName,
                                color = adaptiveTeamColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${result.formattedPoints} PTS",
                                color = adaptiveTeamColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = P1TTheme.textMuted,
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
                            color = P1TTheme.textMuted,
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
                                        .background(P1TTheme.surfaceInnerCard)
                                        .border(1.dp, Color(stint.compound.colorHex).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TyreCompoundBadge(compound = stint.compound, sizeDp = 18)
                                    Text(
                                        text = "${stint.lapsCount}L",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Pit Stop connector badge if present
                                if (stint.pitStopDuration != null && stint.pitStopLap != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "⏱️ ${stint.pitStopDuration}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Lap ${stint.pitStopLap}",
                                            color = P1TTheme.textMuted,
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
                                .background(P1TTheme.surfaceInnerCard)
                                .border(1.dp, P1TTheme.borderInnerCard, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Qualifying Position & Time
                            val qualiPosText = result.qualifyingPosition.ifBlank { "—" }
                            val qualiValueText = if (result.qualifyingTime.isNotBlank()) {
                                "$qualiPosText (${result.qualifyingTime})"
                            } else {
                                qualiPosText
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Qualifying Position",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = qualiValueText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Row 2: Fastest Lap Position & Time
                            val fastestPosText = result.fastestLapPosition.ifBlank { "—" }
                            val fastestValueText = if (result.fastestLapTime.isNotBlank()) {
                                "$fastestPosText (${result.fastestLapTime})"
                            } else {
                                fastestPosText
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Fastest Lap Position",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = fastestValueText,
                                    color = if (result.fastestLapPosition.equals("P1", ignoreCase = true)) {
                                        P1TTheme.sectorPurple
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${result.totalLaps} Laps",
                                    color = MaterialTheme.colorScheme.onSurface,
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
    val teamColor = Color(result.team.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DriverPortraitAvatar(
                        driverCodeOrName = result.driverCode,
                        teamColor = teamColor,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${result.position}. ${result.driverFullName}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val pitStopsCount = result.stints.count { it.pitStopDuration != null }.takeIf { it > 0 }
                    ?: (result.stints.size - 1).coerceAtLeast(0)
                Text(
                    text = "$pitStopsCount Pit Stop(s)",
                    color = adaptiveTeamColor,
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
                    Text(text = "Stop at Lap ${s.pitStopLap}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "${s.pitStopDuration}", color = P1TTheme.sectorGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val teamColor = Color(result.team.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                DriverPortraitAvatar(
                    driverCodeOrName = result.driverCode,
                    teamColor = teamColor,
                    size = 28.dp,
                    positionBadge = result.qualifyingPosition.ifBlank { rank?.let { "P$it" } ?: "" }
                )
                Text(
                    text = result.driverFullName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = result.qualifyingTime.ifBlank { "—" },
                color = adaptiveTeamColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
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
    val laps = weekend.totalLaps.takeIf { it > 0 } ?: 0
    val lengthKm = weekend.circuitLengthKm.toDoubleOrNull() ?: 0.0
    val raceDistanceKm = if (laps > 0 && lengthKm > 0.0) {
        String.format(java.util.Locale.US, "%.3f km", laps * lengthKm)
    } else {
        "-"
    }

    val fastestResult = results.firstOrNull {
        it.fastestLapPosition.equals("P1", ignoreCase = true) && it.fastestLapTime.isNotBlank()
    }
    val fastestLapText = when {
        fastestResult != null -> "${fastestResult.fastestLapTime} (${fastestResult.driverFullName})"
        results.isEmpty() -> "Pending race session"
        else -> "Unavailable"
    }

    val qualiSession = weekend.sessions.firstOrNull {
        it.shortName.contains("Quali", ignoreCase = true) || it.fullName.contains("Qualifying", ignoreCase = true)
    }
    val raceSession = weekend.sessions.firstOrNull { it.isRace }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "RACE INFORMATION",
                    color = P1TTheme.adaptiveTeamText(LocalF1Accent.current.primaryAccent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Season & Round: ${weekend.season} · Round ${weekend.round}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "Weekend Format: ${if (weekend.isSprintWeekend) "Sprint Weekend" else "Traditional Grand Prix"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (qualiSession != null) {
                    Text(
                        text = "Qualifying: ${qualiSession.fullDayName} · ${qualiSession.localTime}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                if (raceSession != null) {
                    Text(
                        text = "Grand Prix Start: ${raceSession.fullDayName} · ${raceSession.localTime}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(text = "Number of Laps: $laps", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "Circuit Length: ${weekend.circuitLengthKm} km", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "Race Distance: $raceDistanceKm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "Fastest Lap: $fastestLapText", color = P1TTheme.sectorPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    val circuitCode = weekend.circuitId.ifBlank { weekend.circuitType }
    val accentCyan = if (P1TTheme.isDark) Color(0xFF00B4D8) else Color(0xFF0077A8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "CIRCUIT SPECIFICATIONS", color = accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Circuit: ${weekend.circuitName}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Location: ${weekend.countryFlagEmoji} ${weekend.location}".trim(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (weekend.dateRangeText.isNotBlank()) {
                    Text(
                        text = "Weekend Dates: ${weekend.dateRangeText}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(text = "Circuit Length: ${weekend.circuitLengthKm} km", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "Race Laps: ${weekend.totalLaps} laps", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "Number of Corners: ${weekend.totalCorners} corners", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (circuitCode.isNotBlank()) {
                    Text(text = "Track Code: ${circuitCode.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}
