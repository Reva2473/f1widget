package com.reva2473.f1widget.ui.screens.live

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.*
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.MiniSectorBars
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.theme.P1TTheme
import kotlinx.coroutines.delay

/**
 * Screen 3: LiveTimingScreen
 * Exact 1:1 replica of live-timing-1080.webp and live-driver-720.webp
 */
private val LiveSessionState.sessionType: String
    get() = shortSessionCode.ifBlank { sessionName }

private val LiveSessionState.trackStatus: String
    get() = trackStatusText

@Composable
fun LiveTimingScreen(
    sessionState: LiveSessionState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDriverCode by rememberSaveable {
        mutableStateOf(sessionState.drivers.firstOrNull { it.code == "HAM" }?.code)
    }
    var selectedSubTab by rememberSaveable { mutableStateOf("Track") }
    var isIntervalMode by rememberSaveable { mutableStateOf(false) } // false = Leader, true = Interval
    var isAudioMuted by rememberSaveable { mutableStateOf(false) }
    var isRadioPlaying by remember { mutableStateOf(false) }
    var activeRadioClipId by remember { mutableStateOf<String?>(null) }
    var radioProgress by remember { mutableFloatStateOf(0f) }

    val validSelectedDriverCode = selectedDriverCode?.takeIf { code ->
        sessionState.drivers.any { it.code == code }
    }

    LaunchedEffect(activeRadioClipId, isRadioPlaying) {
        if (isRadioPlaying) {
            val clipId = activeRadioClipId ?: sessionState.latestRadio.id
            val currentRadio = if (clipId == sessionState.latestRadio.id) {
                sessionState.latestRadio
            } else {
                sessionState.radioClips.firstOrNull { it.id == clipId } ?: sessionState.latestRadio
            }
            val durationMs = (currentRadio.durationSeconds.coerceAtLeast(3)) * 1000L
            val initialOffsetMs = (radioProgress.coerceIn(0f, 1f) * durationMs).toLong()
            val startTime = System.currentTimeMillis() - initialOffsetMs
            while (isRadioPlaying) {
                val elapsed = System.currentTimeMillis() - startTime
                val p = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                radioProgress = p
                if (p >= 1f) {
                    isRadioPlaying = false
                    radioProgress = 0f
                    break
                }
                delay(16L)
            }
        }
    }

    val subTabs = listOf("Track", "Race control", "Radio", "Overtakes", "Weather")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val context = LocalContext.current
            LiveTimingTopBar(
                grandPrixName = sessionState.grandPrixName,
                sessionName = sessionState.sessionName,
                trackStatusText = sessionState.trackStatusText,
                isAudioMuted = isAudioMuted,
                onToggleMute = {
                    isAudioMuted = !isAudioMuted
                    Toast.makeText(
                        context,
                        if (isAudioMuted) "Team radio & audio muted" else "Team radio & audio unmuted",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onClose = onClose
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3-Column Stat Box: Q1 LEFT | BEST · VER | ON TRACK
            item {
                LiveStatThreeColumnBox(
                    shortSessionCode = sessionState.shortSessionCode,
                    timeRemaining = sessionState.timeRemaining,
                    bestLapDriverCode = sessionState.bestLapDriverCode,
                    bestLapTime = sessionState.bestLapTime,
                    carsOnTrack = sessionState.carsOnTrack
                )
            }

            // Sub-Tabs Bar: Track | Race control | Radio | Overtakes | Weather
            item {
                LiveTimingSubTabBar(
                    tabs = subTabs,
                    selectedTab = selectedSubTab,
                    onSelectTab = { selectedSubTab = it }
                )
            }

            // Tab Content
            when (selectedSubTab) {
                "Track" -> {
                    // Track Map Card
                    item {
                        TrackMapCard(
                            sessionState = sessionState,
                            selectedDriverCode = validSelectedDriverCode,
                            onDriverSelected = { code ->
                                selectedDriverCode = if (validSelectedDriverCode == code) null else code
                            }
                        )
                    }

                    // TEAM RADIO Card
                    item {
                        val topRadioId = sessionState.latestRadio.id
                        val isTopClipActive = activeRadioClipId == topRadioId
                        val isPlayingTop = isRadioPlaying && isTopClipActive
                        TeamRadioCard(
                            radio = sessionState.latestRadio,
                            isPlaying = isPlayingTop,
                            progress = if (isTopClipActive) radioProgress else 0f,
                            isMuted = isAudioMuted,
                            onTogglePlay = {
                                if (activeRadioClipId == topRadioId) {
                                    isRadioPlaying = !isRadioPlaying
                                } else {
                                    activeRadioClipId = topRadioId
                                    radioProgress = 0f
                                    isRadioPlaying = true
                                }
                            }
                        )
                    }

                    // Race Control Event Banner
                    item {
                        SessionStartedBannerCard(
                            messages = sessionState.raceControlMessages
                        )
                    }

                    // Timing Tower Header Row (POS | DRIVER | BEST · GAP | Segmented toggle)
                    item {
                        TimingTowerHeader(
                            isIntervalMode = isIntervalMode,
                            onToggleMode = { isIntervalMode = it }
                        )
                    }

                    // Timing Tower Driver Cards
                    itemsIndexed(
                        items = sessionState.drivers,
                        key = { index, driver -> "${index}_${driver.code}" }
                    ) { _, driver ->
                        TimingTowerDriverCard(
                            driver = driver,
                            isIntervalMode = isIntervalMode,
                            isSelected = driver.code == validSelectedDriverCode,
                            onClick = {
                                selectedDriverCode = if (validSelectedDriverCode == driver.code) null else driver.code
                            }
                        )
                    }
                }

                "Race control" -> {
                    item {
                        Text(
                            text = "RACE CONTROL LOG",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(
                        items = sessionState.raceControlMessages,
                        key = { index, msg -> "${index}_${msg.id}" }
                    ) { _, msg ->
                        RaceControlMessageCard(msg)
                    }
                }

                "Radio" -> {
                    item {
                        Text(
                            text = "TEAM RADIO TRANSMISSIONS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(
                        items = sessionState.radioClips,
                        key = { index, clip -> "${index}_${clip.id}" }
                    ) { _, clip ->
                        val isClipActive = activeRadioClipId == clip.id
                        val isClipPlaying = isRadioPlaying && isClipActive
                        TeamRadioClipItem(
                            clip = clip,
                            isPlaying = isClipPlaying,
                            progress = if (isClipActive) radioProgress else 0f,
                            isMuted = isAudioMuted,
                            onTogglePlay = {
                                if (activeRadioClipId == clip.id) {
                                    isRadioPlaying = !isRadioPlaying
                                } else {
                                    activeRadioClipId = clip.id
                                    radioProgress = 0f
                                    isRadioPlaying = true
                                }
                            }
                        )
                    }
                }

                "Overtakes" -> {
                    item {
                        Text(
                            text = "OVERTAKE TRACKER",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    if (sessionState.overtakes.isEmpty()) {
                        item {
                            val sessionDesc = sessionState.sessionName.ifBlank { sessionState.sessionType }.lowercase()
                            Text(
                                text = "No overtakes recorded in this $sessionDesc session.",
                                color = P1TTheme.textMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = sessionState.overtakes,
                            key = { index, it -> "${index}_${it.lap}_${it.overtakingDriver}_${it.overtakenDriver}_${it.positionGained}" }
                        ) { _, overtake ->
                            OvertakeEventCard(overtake)
                        }
                    }
                }

                "Weather" -> {
                    item {
                        WeatherConditionsCard(sessionState.weather)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

/**
 * Top Bar: Circular X close button, "Bahrain Grand Prix" + red dot "Qualifying · Q1",
 * speaker/mute icon, green bordered "🚩 Track Clear" pill.
 */
@Composable
private fun LiveTimingTopBar(
    grandPrixName: String,
    sessionName: String,
    trackStatusText: String,
    isAudioMuted: Boolean,
    onToggleMute: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button with >=48dp touch target
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Title & Session
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grandPrixName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(P1TTheme.accentRed)
                    )
                    Text(
                        text = sessionName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Audio mute button with >=48dp touch target
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAudioMuted) P1TTheme.redFlagBg else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                        tint = if (isAudioMuted) P1TTheme.accentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Dynamic track status pill
            val effectiveStatus = trackStatusText.ifBlank { "Track Clear" }
            val (statusBg, statusBorder, statusTint) = resolveTrackStatusColors(effectiveStatus)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(13.dp)
                    )
                    Text(
                        text = effectiveStatus,
                        color = statusTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 3-column Stat Box (#181820):
 * Q1 LEFT 8:21 | BEST · VER 1:36.477 (purple) | ON TRACK 21
 */
@Composable
private fun LiveStatThreeColumnBox(
    shortSessionCode: String,
    timeRemaining: String,
    bestLapDriverCode: String,
    bestLapTime: String,
    carsOnTrack: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Time Left
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val leftLabel = when {
                    shortSessionCode.isBlank() -> "LEFT"
                    shortSessionCode.endsWith("LEFT", ignoreCase = true) -> shortSessionCode
                    else -> "$shortSessionCode LEFT"
                }
                Text(
                    text = leftLabel,
                    color = P1TTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeRemaining,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // Column 2: Best Lap (Purple)
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BEST · $bestLapDriverCode",
                    color = P1TTheme.sectorPurple,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bestLapTime,
                    color = P1TTheme.sectorPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // Column 3: On Track
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ON TRACK",
                    color = P1TTheme.textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = carsOnTrack.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sub-Tabs Bar: Track | Race control | Radio | Overtakes | Weather
 */
@Composable
private fun LiveTimingSubTabBar(
    tabs: List<String>,
    selectedTab: String,
    onSelectTab: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else P1TTheme.textMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 26.dp else 0.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                )
            }
        }
    }
}

private fun formatSectorLength(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "—"
    val numericValue = trimmed.removeSuffix("km").removeSuffix("KM").trim().toDoubleOrNull()
    if (numericValue != null) {
        if (numericValue <= 0.0) return "—"
        return if (trimmed.endsWith("km", ignoreCase = true)) trimmed else "$trimmed km"
    }
    return trimmed
}

/**
 * Track Map Card:
 * Track Canvas + Sector lengths + Bottom status bar
 */
@Composable
private fun TrackMapCard(
    sessionState: LiveSessionState,
    selectedDriverCode: String?,
    onDriverSelected: (String) -> Unit
) {
    val circuitId = sessionState.circuitId.ifBlank { "INTERLAGOS" }
    val s1Desc = formatSectorLength(sessionState.sector1LengthKm)
    val s2Desc = formatSectorLength(sessionState.sector2LengthKm)
    val s3Desc = formatSectorLength(sessionState.sector3LengthKm)
    val hasValidSelection = selectedDriverCode != null &&
        sessionState.drivers.any { it.code == selectedDriverCode }

    val trackStatus = sessionState.trackStatus.ifBlank { "Track Clear" }
    val (_, _, statusColor) = resolveTrackStatusColors(trackStatus)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Track Map Canvas with live drivers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                LiveTrackMapCanvas(
                    drivers = sessionState.drivers,
                    selectedDriverCode = selectedDriverCode.takeIf { hasValidSelection },
                    onDriverTap = onDriverSelected,
                    circuitType = circuitId,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sector Lengths
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectorLengthItem(label = "S1", value = s1Desc)
                Text(
                    text = "·",
                    color = P1TTheme.textMuted
                )
                SectorLengthItem(label = "S2", value = s2Desc)
                Text(
                    text = "·",
                    color = P1TTheme.textMuted
                )
                SectorLengthItem(label = "S3", value = s3Desc)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom bar: Track Clear and Cars on Track / Tap again to clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(13.dp)
                    )
                    Text(
                        text = trackStatus,
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (hasValidSelection) "Tap again to clear" else "${sessionState.carsOnTrack} on track",
                    color = P1TTheme.textMuted,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun SectorLengthItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * TEAM RADIO Card (🎧 TEAM RADIO in red, driver, timestamp,
 * circular Play/Pause button ▶ with progress bar at bottom)
 */
@Composable
private fun TeamRadioCard(
    radio: TeamRadioClip,
    isPlaying: Boolean,
    progress: Float = 0f,
    isMuted: Boolean = false,
    onTogglePlay: () -> Unit
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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header in red
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = P1TTheme.accentRed,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(15.dp)
                    )
                    Text(
                        text = "TEAM RADIO",
                        color = P1TTheme.accentRed,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMuted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = P1TTheme.redFlagBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.redFlagBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = null,
                                    tint = P1TTheme.accentRed,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "MUTED",
                                    color = P1TTheme.accentRed,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = radio.timestamp,
                        color = P1TTheme.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogoBadge(team = radio.team, sizeDp = 28)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = radio.driverName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val teamName = if (radio.team == F1TeamId.AUDI_SAUBER) "Audi" else radio.team.displayName
                    Text(
                        text = "$teamName · ${radio.timestamp}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${radio.transcript}\"",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Circle Button with >=48dp touch target
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = when {
                                isPlaying -> "Pause radio"
                                isMuted -> "Play radio (muted)"
                                else -> "Play radio"
                            },
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Progress Bar (0f when idle/unstarted; preserves paused offset)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(P1TTheme.dividerSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isMuted) P1TTheme.textMuted else P1TTheme.accentRed)
                )
            }
        }
    }
}

/**
 * SESSION STARTED 09:00:00 Race Control Banner Card
 */
@Composable
private fun SessionStartedBannerCard(
    messages: List<RaceControlMessage>
) {
    val firstMsg = messages.firstOrNull() ?: RaceControlMessage("1", "SESSION STARTED", "09:00:00", "INFO")
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = P1TTheme.surfaceInnerCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(P1TTheme.sectorGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = firstMsg.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = firstMsg.timestamp,
                color = P1TTheme.textMuted,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Timing Tower Header: POS | DRIVER | BEST · GAP | Segmented toggle Interval / Leader
 */
@Composable
private fun TimingTowerHeader(
    isIntervalMode: Boolean,
    onToggleMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "POS",
            color = P1TTheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "DRIVER",
            color = P1TTheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Segmented Toggle: Interval / Leader
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = P1TTheme.surfaceInnerCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isIntervalMode) P1TTheme.pillSelectedBg else Color.Transparent,
                    modifier = Modifier.clickable { onToggleMode(true) }
                ) {
                    Text(
                        text = "Interval",
                        color = if (isIntervalMode) MaterialTheme.colorScheme.onSurface else P1TTheme.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isIntervalMode) P1TTheme.pillSelectedBg else Color.Transparent,
                    modifier = Modifier.clickable { onToggleMode(false) }
                ) {
                    Text(
                        text = "Leader",
                        color = if (!isIntervalMode) MaterialTheme.colorScheme.onSurface else P1TTheme.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Timing Tower Driver Card:
 * Position, team badge, 3-letter code + full name + blue IN PIT pill.
 * Second row: Tyre compound badge + tyre age + MiniSectorBars.
 * Right: Best lap time + gap.
 * When tapped: expands 2x3 telemetry grid.
 */
@Composable
private fun TimingTowerDriverCard(
    driver: LiveDriverTiming,
    isIntervalMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val teamColor = Color(driver.team.colorHex)
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val borderColor = if (isSelected) adaptiveTeamColor else MaterialTheme.colorScheme.outline

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Main Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position number
                Text(
                    text = driver.position.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(24.dp)
                )

                // Driver Avatar
                DriverPortraitAvatar(
                    driverCodeOrName = driver.code,
                    teamColor = if (driver.team == F1TeamId.OTHER) P1TTheme.textMuted else teamColor,
                    size = 28.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isOutLap = driver.isOutLap

                // Driver details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = driver.code,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = driver.fullName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (driver.isInPit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = P1TTheme.inPitText.copy(alpha = if (P1TTheme.isDark) 0.22f else 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.inPitText)
                            ) {
                                Text(
                                    text = "IN PIT",
                                    color = P1TTheme.inPitText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isOutLap) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = P1TTheme.pillBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.pillBorder)
                            ) {
                                Text(
                                    text = "OUT LAP",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Second row: Tyre + Age + MiniSectors
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TyreCompoundBadge(compound = driver.tyreCompound, sizeDp = 16)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${driver.tyreAgeLaps}",
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        MiniSectorBars(
                            s1 = driver.sector1Status,
                            s2 = driver.sector2Status,
                            s3 = driver.sector3Status
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Column: Best Lap Time + Gap
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = driver.bestLapTime,
                        color = if (driver.isOverallBest) P1TTheme.sectorPurple else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val gapText = if (isIntervalMode) driver.intervalToAhead else driver.gapToLeader
                    Text(
                        text = gapText,
                        color = P1TTheme.textMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Expanded Telemetry Grid (2×3)
            AnimatedVisibility(visible = isSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 1: Sectors 1, 2, 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryCell(label = "SECTOR 1", value = driver.sector1Time, status = driver.sector1Status, modifier = Modifier.weight(1f))
                        TelemetryCell(label = "SECTOR 2", value = driver.sector2Time, status = driver.sector2Status, modifier = Modifier.weight(1f))
                        TelemetryCell(label = "SECTOR 3", value = driver.sector3Time, status = driver.sector3Status, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Last Lap, Best Lap, Tyre Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryCell(label = "LAST LAP", value = driver.lastLapTime, modifier = Modifier.weight(1f))
                        TelemetryCell(
                            label = "BEST LAP",
                            value = driver.bestLapTime,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            label = "TYRE",
                            value = "(${driver.tyreCompound.code}) ${driver.tyreCompound.label} · ${driver.tyreAgeLaps}L",
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    status: SectorStatus? = null
) {
    val valueColor = when (status) {
        SectorStatus.OVERALL_BEST -> P1TTheme.sectorPurple
        SectorStatus.PERSONAL_BEST -> P1TTheme.sectorGreen
        SectorStatus.NORMAL -> P1TTheme.sectorYellow
        SectorStatus.INACTIVE -> P1TTheme.textMuted
        null -> MaterialTheme.colorScheme.onSurface
    }
    Column(modifier = modifier) {
        Text(
            text = label,
            color = P1TTheme.textMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RaceControlMessageCard(msg: RaceControlMessage) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (msg.category) {
                                "FLAG" -> P1TTheme.sectorYellow
                                "SAFETY_CAR" -> P1TTheme.adaptiveTeamText(Color(0xFFFF8000))
                                else -> P1TTheme.sectorGreen
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = msg.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = msg.timestamp,
                color = P1TTheme.textMuted,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TeamRadioClipItem(
    clip: TeamRadioClip,
    isPlaying: Boolean,
    progress: Float = 0f,
    isMuted: Boolean = false,
    onTogglePlay: () -> Unit
) {
    val teamColor = Color(clip.team.colorHex)
    val adaptiveTeam = P1TTheme.adaptiveTeamText(teamColor)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(teamColor.copy(alpha = 0.2f))
                            .border(1.dp, adaptiveTeam, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = when {
                                isPlaying -> "Pause"
                                isMuted -> "Play (muted)"
                                else -> "Play"
                            },
                            tint = adaptiveTeam,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                TeamLogoBadge(team = clip.team, sizeDp = 24)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${clip.driverName} (${clip.driverCode})",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isMuted) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Muted",
                                    tint = P1TTheme.accentRed,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = clip.timestamp,
                                color = P1TTheme.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${clip.transcript}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isPlaying || progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(P1TTheme.dividerSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (isMuted) P1TTheme.textMuted else adaptiveTeam)
                    )
                }
            }
        }
    }
}

@Composable
private fun OvertakeEventCard(event: OvertakeEvent) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = P1TTheme.pillBg
            ) {
                Text(
                    text = "LAP ${event.lap}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.overtakingDriver} passed ${event.overtakenDriver}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "For P${event.positionGained} at ${event.cornerText}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun WeatherConditionsCard(weather: WeatherSnapshot) {
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
                text = "TRACK & ATMOSPHERIC CONDITIONS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WeatherParam(label = "TRACK TEMP", value = "${weather.trackTempC}°C")
                WeatherParam(label = "AIR TEMP", value = "${weather.airTempC}°C")
                WeatherParam(label = "HUMIDITY", value = "${weather.humidityPercent}%")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WeatherParam(label = "WIND", value = "${weather.windSpeedKmh} km/h ${weather.windDirection}")
                WeatherParam(label = "RAINFALL", value = "${weather.rainfallPercent}%")
                WeatherParam(label = "PRESSURE", value = "${weather.pressureMbar} mbar")
            }
        }
    }
}

@Composable
private fun resolveTrackStatusColors(trackStatus: String): Triple<Color, Color, Color> = when {
    trackStatus.contains("Red", ignoreCase = true) ->
        Triple(P1TTheme.redFlagBg, P1TTheme.accentRed, P1TTheme.accentRed)
    trackStatus.contains("Clear", ignoreCase = true) ||
    trackStatus.contains("Green", ignoreCase = true) ->
        Triple(P1TTheme.trackClearBg, P1TTheme.sectorGreen, P1TTheme.sectorGreen)
    else ->
        Triple(P1TTheme.sectorYellowBg, P1TTheme.sectorYellow, P1TTheme.sectorYellow)
}

@Composable
private fun WeatherParam(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = P1TTheme.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
