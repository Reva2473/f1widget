package com.reva2473.f1widget.ui.screens.live

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.*
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.MiniSectorBars
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
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
    var selectedDriverCode by remember { mutableStateOf<String?>("HAM") }
    var selectedSubTab by remember { mutableStateOf("Track") }
    var isIntervalMode by remember { mutableStateOf(false) } // false = Leader, true = Interval
    var isAudioMuted by remember { mutableStateOf(false) }
    var isRadioPlaying by remember { mutableStateOf(false) }
    var activeRadioClipId by remember { mutableStateOf<String?>(null) }
    var radioProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(activeRadioClipId, isRadioPlaying) {
        if (isRadioPlaying) {
            val clipId = activeRadioClipId ?: sessionState.latestRadio.id
            val currentRadio = if (clipId == sessionState.latestRadio.id) {
                sessionState.latestRadio
            } else {
                sessionState.radioClips.firstOrNull { it.id == clipId } ?: sessionState.latestRadio
            }
            val durationMs = (currentRadio.durationSeconds.coerceAtLeast(3)) * 1000L
            val startTime = System.currentTimeMillis()
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
        } else {
            radioProgress = 0f
        }
    }

    val subTabs = listOf("Track", "Race control", "Radio", "Overtakes", "Weather")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = P1TColors.BackgroundPrimary,
        topBar = {
            val context = LocalContext.current
            LiveTimingTopBar(
                grandPrixName = sessionState.grandPrixName,
                sessionName = sessionState.sessionName,
                trackStatusText = sessionState.trackStatusText,
                isAudioMuted = isAudioMuted,
                onToggleMute = {
                    isAudioMuted = !isAudioMuted
                    Toast.makeText(context, if (isAudioMuted) "Audio commentary muted" else "Audio commentary unmuted", Toast.LENGTH_SHORT).show()
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
                            selectedDriverCode = selectedDriverCode,
                            onDriverSelected = { code ->
                                selectedDriverCode = if (selectedDriverCode == code) null else code
                            }
                        )
                    }

                    // TEAM RADIO Card
                    item {
                        val isPlayingTop = isRadioPlaying && (activeRadioClipId == sessionState.latestRadio.id || activeRadioClipId == null)
                        TeamRadioCard(
                            radio = sessionState.latestRadio,
                            isPlaying = isPlayingTop,
                            progress = if (isPlayingTop) radioProgress else 0f,
                            onTogglePlay = {
                                if (activeRadioClipId == sessionState.latestRadio.id || (activeRadioClipId == null && isRadioPlaying)) {
                                    isRadioPlaying = !isRadioPlaying
                                } else {
                                    activeRadioClipId = sessionState.latestRadio.id
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
                    items(sessionState.drivers, key = { it.code }) { driver ->
                        TimingTowerDriverCard(
                            driver = driver,
                            isIntervalMode = isIntervalMode,
                            isSelected = driver.code == selectedDriverCode,
                            onClick = {
                                selectedDriverCode = if (selectedDriverCode == driver.code) null else driver.code
                            }
                        )
                    }
                }

                "Race control" -> {
                    item {
                        Text(
                            text = "RACE CONTROL LOG",
                            color = P1TColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(sessionState.raceControlMessages, key = { it.id }) { msg ->
                        RaceControlMessageCard(msg)
                    }
                }

                "Radio" -> {
                    item {
                        Text(
                            text = "TEAM RADIO TRANSMISSIONS",
                            color = P1TColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(sessionState.radioClips, key = { it.id }) { clip ->
                        val isClipPlaying = isRadioPlaying && activeRadioClipId == clip.id
                        TeamRadioClipItem(
                            clip = clip,
                            isPlaying = isClipPlaying,
                            progress = if (isClipPlaying) radioProgress else 0f,
                            onTogglePlay = {
                                if (activeRadioClipId == clip.id) {
                                    isRadioPlaying = !isRadioPlaying
                                } else {
                                    activeRadioClipId = clip.id
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
                            color = P1TColors.TextSecondary,
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
                                color = P1TColors.TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(sessionState.overtakes) { overtake ->
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
        color = P1TColors.BackgroundPrimary,
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
            // Close button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(P1TColors.SurfaceElevated)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Session
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grandPrixName,
                    color = Color.White,
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
                            .background(P1TColors.AccentRed)
                    )
                    Text(
                        text = sessionName,
                        color = P1TColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Audio mute button with >=40dp touch target
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                    tint = P1TColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Dynamic track status pill
            val (statusBg, statusBorder, statusTint) = when {
                trackStatusText.contains("Red", ignoreCase = true) ->
                    Triple(Color(0xFF3B1214), P1TColors.AccentRed, P1TColors.AccentRed)
                trackStatusText.contains("Yellow", ignoreCase = true) ||
                trackStatusText.contains("SC", ignoreCase = true) ||
                trackStatusText.contains("Safety", ignoreCase = true) ||
                trackStatusText.contains("VSC", ignoreCase = true) ->
                    Triple(Color(0xFF33290D), P1TColors.SectorYellow, P1TColors.SectorYellow)
                else ->
                    Triple(P1TColors.TrackClearBg, P1TColors.SectorGreen, P1TColors.SectorGreen)
            }

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
                        text = trackStatusText,
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
        color = Color(0xFF181820),
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                    color = P1TColors.TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeRemaining,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(P1TColors.BorderSubtle)
            )

            // Column 2: Best Lap (Purple)
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BEST · $bestLapDriverCode",
                    color = P1TColors.SectorPurple,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bestLapTime,
                    color = P1TColors.SectorPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(P1TColors.BorderSubtle)
            )

            // Column 3: On Track
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ON TRACK",
                    color = P1TColors.TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = carsOnTrack.toString(),
                    color = Color.White,
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) Color.White else P1TColors.TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 26.dp else 0.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                )
            }
        }
    }
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
    val isInterlagos = circuitId.contains("INTERLAGOS", ignoreCase = true) ||
        circuitId.contains("BRAZIL", ignoreCase = true) ||
        circuitId.contains("SAO_PAULO", ignoreCase = true)

    val (s1Desc, s2Desc, s3Desc) = if (isInterlagos) {
        Triple("T1-T3  1.2 km", "T4-T11  1.9 km", "T12-T15  1.1 km")
    } else {
        Triple(
            "T1-T5  ${sessionState.sector1LengthKm.ifBlank { "1.4 km" }}",
            "T6-T12  ${sessionState.sector2LengthKm.ifBlank { "1.8 km" }}",
            "T13-T15  ${sessionState.sector3LengthKm.ifBlank { "2.3 km" }}"
        )
    }

    val trackStatus = sessionState.trackStatus.ifBlank { "Track Clear" }
    val isClear = trackStatus.contains("Clear", ignoreCase = true)
    val statusColor = if (isClear) P1TColors.SectorGreen else P1TColors.SectorYellow

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                    selectedDriverCode = selectedDriverCode,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "S1",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s1Desc,
                        color = P1TColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "·",
                    color = P1TColors.TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "S2",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s2Desc,
                        color = P1TColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "·",
                    color = P1TColors.TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "S3",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s3Desc,
                        color = P1TColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
                    text = if (selectedDriverCode != null) "Tap again to clear" else "${sessionState.carsOnTrack} on track",
                    color = P1TColors.TextMuted,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

/**
 * TEAM RADIO Card (🎧 TEAM RADIO in teal, Fernando Alonso, Aston Martin · 08:55:53,
 * large white circular Play/Pause button ▶ with progress bar at bottom)
 */
@Composable
private fun TeamRadioCard(
    radio: TeamRadioClip,
    isPlaying: Boolean,
    progress: Float = 0f,
    onTogglePlay: () -> Unit
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
                        tint = Color(0xFFE1251B),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(15.dp)
                    )
                    Text(
                        text = "TEAM RADIO",
                        color = Color(0xFFE1251B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = radio.timestamp,
                    color = P1TColors.TextMuted,
                    fontSize = 11.sp
                )
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
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val teamName = if (radio.team == F1TeamId.AUDI_SAUBER) "Audi" else radio.team.displayName
                    Text(
                        text = "$teamName · ${radio.timestamp}",
                        color = P1TColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${radio.transcript}\"",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play radio",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF252530))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isPlaying) progress.coerceIn(0f, 1f) else 0.15f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE1251B))
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
        color = Color(0xFF14141A),
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(P1TColors.SectorGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = firstMsg.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = firstMsg.timestamp,
                color = P1TColors.TextMuted,
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
            color = P1TColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "DRIVER",
            color = P1TColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Segmented Toggle: Interval / Leader
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF16161E),
            border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle)
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isIntervalMode) Color(0xFF2C2C3A) else Color.Transparent,
                    modifier = Modifier.clickable { onToggleMode(true) }
                ) {
                    Text(
                        text = "Interval",
                        color = if (isIntervalMode) Color.White else P1TColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isIntervalMode) Color(0xFF2C2C3A) else Color.Transparent,
                    modifier = Modifier.clickable { onToggleMode(false) }
                ) {
                    Text(
                        text = "Leader",
                        color = if (!isIntervalMode) Color.White else P1TColors.TextMuted,
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
    val borderColor = if (isSelected) Color(driver.team.colorHex) else P1TColors.BorderSubtle

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = P1TColors.SurfaceCard,
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
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(24.dp)
                )

                // Driver Avatar
                DriverPortraitAvatar(
                    driverCodeOrName = driver.code,
                    teamColor = Color(driver.team.colorHex),
                    size = 28.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isOutLap = driver.isOutLap

                // Driver details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = driver.code,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = driver.fullName,
                            color = P1TColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (driver.isInPit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = P1TColors.InPitBlue.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.InPitBlue)
                            ) {
                                Text(
                                    text = "IN PIT",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isOutLap) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF202028),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF484858))
                            ) {
                                Text(
                                    text = "OUT LAP",
                                    color = Color(0xFFA0A0B0),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
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
                            color = P1TColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        MiniSectorBars(
                            s1 = driver.sector1Status,
                            s2 = driver.sector2Status,
                            s3 = driver.sector3Status
                        )
                    }
                }

                // Right Column: Best Lap Time + Gap
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = driver.bestLapTime,
                        color = if (driver.isOverallBest) P1TColors.SectorPurple else Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val gapText = if (isIntervalMode) driver.intervalToAhead else driver.gapToLeader
                    Text(
                        text = gapText,
                        color = P1TColors.TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
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
                    HorizontalDivider(color = P1TColors.BorderSubtle, thickness = 1.dp)
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
                            value = "${driver.bestLapTime} ${if (driver.gapToLeader != "Leader") driver.gapToLeader else ""}",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            label = "TYRE",
                            value = "(${driver.tyreCompound.code}) ${driver.tyreCompound.label} · ${driver.tyreAgeLaps}L",
                            modifier = Modifier.weight(1f)
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
    status: SectorStatus? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = P1TColors.TextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = status?.let { Color(it.colorHex) } ?: Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RaceControlMessageCard(msg: RaceControlMessage) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (msg.category) {
                                "FLAG" -> Color(0xFFF5C518)
                                "SAFETY_CAR" -> Color(0xFFFF8000)
                                else -> Color(0xFF2CE580)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = msg.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = msg.timestamp,
                color = P1TColors.TextMuted,
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
    onTogglePlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(clip.team.colorHex).copy(alpha = 0.2f))
                        .border(1.dp, Color(clip.team.colorHex), CircleShape)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(clip.team.colorHex),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                TeamLogoBadge(team = clip.team, sizeDp = 24)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${clip.driverName} (${clip.driverCode})",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = clip.timestamp,
                            color = P1TColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${clip.transcript}\"",
                        color = P1TColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF252530))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(clip.team.colorHex))
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
        color = P1TColors.SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                color = Color(0xFF1E1E28)
            ) {
                Text(
                    text = "LAP ${event.lap}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.overtakingDriver} passed ${event.overtakenDriver}",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "For P${event.positionGained} at ${event.cornerText}",
                    color = P1TColors.TextSecondary,
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
                text = "TRACK & ATMOSPHERIC CONDITIONS",
                color = P1TColors.TextSecondary,
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
private fun WeatherParam(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = P1TColors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
