package com.reva2473.f1widget.ui.screens.live

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.*
import com.reva2473.f1widget.ui.components.ApexMotion
import com.reva2473.f1widget.ui.components.BroadcastFlagBannerType
import com.reva2473.f1widget.ui.components.CircularHeaderButton
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.LiveTrackMapCanvas
import com.reva2473.f1widget.ui.components.MiniSectorBars
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.RaceControlStatusBanner
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.components.TyreCompoundBadge
import com.reva2473.f1widget.ui.components.resolveBroadcastFlagType
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Screen 3: LiveTimingScreen
 * Broadcast-grade live timing tower, track map, race control banner, and team radio.
 */
private val LiveSessionState.sessionType: String
    get() = shortSessionCode.ifBlank { sessionName }

private val LiveSessionState.trackStatus: String
    get() = trackStatusText

private fun TeamRadioClip.resolveRecordingUrl(): String? =
    recordingUrl?.trim()?.takeIf { it.isNotBlank() }

private fun TeamRadioClip.hasContent(): Boolean =
    id.isNotBlank() ||
        driverName.isNotBlank() ||
        driverCode.isNotBlank() ||
        transcript.isNotBlank() ||
        !recordingUrl.isNullOrBlank()

private fun extractDriverSurname(fullName: String, fallbackCode: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size > 1 -> parts.last()
        parts.size == 1 && !parts.first().equals(fallbackCode, ignoreCase = true) -> parts.first()
        else -> ""
    }
}

private fun <T> buildUniqueKeyedItems(
    items: List<T>,
    baseKey: (T) -> String
): List<Pair<String, T>> {
    val seen = HashMap<String, Int>(items.size)
    return items.map { item ->
        val raw = baseKey(item)
        val count = seen[raw] ?: 0
        seen[raw] = count + 1
        val uniqueKey = if (count == 0) raw else "${raw}_dup$count"
        uniqueKey to item
    }
}

@Composable
fun LiveTimingScreen(
    sessionState: LiveSessionState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDriverCode by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSubTab by rememberSaveable { mutableStateOf("Track") }
    var isIntervalMode by rememberSaveable { mutableStateOf(false) } // false = Leader, true = Interval
    var isAudioMuted by rememberSaveable { mutableStateOf(false) }
    var activeRadioClipId by rememberSaveable { mutableStateOf<String?>(null) }
    var isRadioPlaying by remember { mutableStateOf(false) }
    var isPlayerPrepared by remember { mutableStateOf(false) }
    var radioProgress by remember { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var streamErrorClipId by remember { mutableStateOf<String?>(null) }

    val validSelectedDriverCode = selectedDriverCode?.takeIf { code ->
        sessionState.drivers.any { it.code == code }
    }

    val topRadio = remember(sessionState.latestRadio, sessionState.radioClips) {
        sessionState.latestRadio.takeIf { it.hasContent() }
            ?: sessionState.radioClips.firstOrNull { it.hasContent() }
    }

    val allRadioClips = remember(topRadio, sessionState.radioClips) {
        buildList {
            if (topRadio != null) add(topRadio)
            addAll(sessionState.radioClips.filter { it.hasContent() })
        }.distinctBy { it.id }
    }
    val activeRadioClip = remember(activeRadioClipId, allRadioClips) {
        activeRadioClipId?.let { id -> allRadioClips.firstOrNull { it.id == id } }
    }
    val activeRecordingUrl = activeRadioClip?.resolveRecordingUrl()

    val keyedDrivers = remember(sessionState.drivers) {
        buildUniqueKeyedItems(sessionState.drivers) { driver ->
            "driver_${driver.code.ifBlank { driver.fullName }}_${driver.team.name}"
        }
    }
    val keyedRaceControl = remember(sessionState.raceControlMessages) {
        buildUniqueKeyedItems(sessionState.raceControlMessages) { msg ->
            "rc_${msg.id.ifBlank { "${msg.timestamp}_${msg.title.hashCode()}" }}"
        }
    }
    val keyedRadioClips = remember(sessionState.radioClips) {
        buildUniqueKeyedItems(sessionState.radioClips) { clip ->
            "radio_${clip.id.ifBlank { "${clip.driverCode}_${clip.timestamp}" }}"
        }
    }
    val keyedOvertakes = remember(sessionState.overtakes) {
        buildUniqueKeyedItems(sessionState.overtakes) { overtake ->
            "ot_${overtake.lap}_${overtake.overtakingDriver}_${overtake.overtakenDriver}_${overtake.positionGained}_${overtake.cornerText}"
        }
    }

    DisposableEffect(activeRadioClipId, activeRecordingUrl) {
        val url = activeRecordingUrl
        isPlayerPrepared = false
        if (url.isNullOrBlank()) {
            mediaPlayerRef = null
            isRadioPlaying = false
            radioProgress = 0f
            onDispose { }
        } else {
            val player = MediaPlayer()
            var disposed = false
            mediaPlayerRef = player
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                player.setDataSource(url)
                player.setOnPreparedListener { mp ->
                    if (!disposed) {
                        isPlayerPrepared = true
                        val vol = if (isAudioMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (isRadioPlaying) {
                            mp.start()
                        }
                    }
                }
                player.setOnCompletionListener {
                    if (!disposed) {
                        isRadioPlaying = false
                        radioProgress = 0f
                    }
                }
                player.setOnErrorListener { _, _, _ ->
                    if (!disposed) {
                        isRadioPlaying = false
                        isPlayerPrepared = false
                        radioProgress = 0f
                        streamErrorClipId = activeRadioClipId
                    }
                    true
                }
                player.prepareAsync()
            } catch (_: Exception) {
                isRadioPlaying = false
                isPlayerPrepared = false
                radioProgress = 0f
                streamErrorClipId = activeRadioClipId
            }
            onDispose {
                disposed = true
                isPlayerPrepared = false
                mediaPlayerRef = null
                runCatching {
                    if (player.isPlaying) player.stop()
                }
                runCatching { player.release() }
            }
        }
    }

    LaunchedEffect(isAudioMuted, mediaPlayerRef, isPlayerPrepared) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        val vol = if (isAudioMuted) 0f else 1f
        runCatching { player.setVolume(vol, vol) }
    }

    LaunchedEffect(isRadioPlaying, mediaPlayerRef, isPlayerPrepared) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        if (!isPlayerPrepared) return@LaunchedEffect
        runCatching {
            if (isRadioPlaying && !player.isPlaying) {
                player.start()
            } else if (!isRadioPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }

    LaunchedEffect(isRadioPlaying, isPlayerPrepared, mediaPlayerRef, activeRecordingUrl) {
        val player = mediaPlayerRef ?: return@LaunchedEffect
        if (activeRecordingUrl.isNullOrBlank() || !isPlayerPrepared) return@LaunchedEffect
        while (isRadioPlaying) {
            val duration = runCatching { player.duration }.getOrDefault(0)
            val position = runCatching { player.currentPosition }.getOrDefault(0)
            if (duration > 0) {
                radioProgress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(100L)
        }
    }

    val handleRadioToggle: (TeamRadioClip) -> Unit = { clip ->
        val url = clip.resolveRecordingUrl()
        if (url.isNullOrBlank()) {
            activeRadioClipId = clip.id
            streamErrorClipId = clip.id
            isRadioPlaying = false
            radioProgress = 0f
            Toast.makeText(context, "Audio stream unavailable", Toast.LENGTH_SHORT).show()
        } else {
            streamErrorClipId = null
            if (activeRadioClipId == clip.id) {
                isRadioPlaying = !isRadioPlaying
            } else {
                activeRadioClipId = clip.id
                radioProgress = 0f
                isRadioPlaying = true
            }
        }
    }

    val subTabs = listOf("Track", "Race control", "Radio", "Overtakes", "Weather")

    P1TRadialBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                LiveTimingTopBar(
                    grandPrixName = sessionState.grandPrixName,
                    sessionName = sessionState.sessionName,
                    isLive = sessionState.isLive,
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
                // 3-Column Stat Box: Q1 LEFT | BEST · VER (Purple) | ON TRACK
                item(key = "stat_three_col_box") {
                    LiveStatThreeColumnBox(
                        shortSessionCode = sessionState.shortSessionCode,
                        timeRemaining = sessionState.timeRemaining,
                        bestLapDriverCode = sessionState.bestLapDriverCode,
                        bestLapTime = sessionState.bestLapTime,
                        carsOnTrack = sessionState.carsOnTrack
                    )
                }

                // Sub-Tabs Bar: Track | Race control | Radio | Overtakes | Weather
                item(key = "sub_tab_bar") {
                    LiveTimingSubTabBar(
                        tabs = subTabs,
                        selectedTab = selectedSubTab,
                        onSelectTab = { selectedSubTab = it }
                    )
                }

                // Tab Content
                when (selectedSubTab) {
                    "Track" -> {
                        // Broadcast Race Control Status Banner (Image 3 style)
                        item(key = "broadcast_rc_banner") {
                            LiveBroadcastRaceControlBanner(sessionState = sessionState)
                        }

                        // Track Map Card
                        item(key = "track_map_card") {
                            TrackMapCard(
                                sessionState = sessionState,
                                selectedDriverCode = validSelectedDriverCode,
                                onDriverSelected = { code ->
                                    selectedDriverCode = if (validSelectedDriverCode == code) null else code
                                }
                            )
                        }

                        // TEAM RADIO Card (only rendered when a radio clip exists)
                        if (topRadio != null) {
                            item(key = "latest_team_radio") {
                                val topRadioId = topRadio.id
                                val isTopClipActive = activeRadioClipId == topRadioId
                                val isPlayingTop = isRadioPlaying && isTopClipActive
                                val isUnavailable = topRadio.resolveRecordingUrl().isNullOrBlank() || streamErrorClipId == topRadioId
                                TeamRadioCard(
                                    radio = topRadio,
                                    isPlaying = isPlayingTop,
                                    progress = if (isTopClipActive) radioProgress else 0f,
                                    isMuted = isAudioMuted,
                                    isAudioUnavailable = isUnavailable,
                                    onTogglePlay = { handleRadioToggle(topRadio) }
                                )
                            }
                        }

                        // Timing Tower Header Row (POS | DRIVER | Segmented toggle)
                        item(key = "timing_tower_header") {
                            TimingTowerHeader(
                                isIntervalMode = isIntervalMode,
                                onToggleMode = { isIntervalMode = it }
                            )
                        }

                        // Timing Tower Driver Cards
                        items(
                            items = keyedDrivers,
                            key = { it.first }
                        ) { (_, driver) ->
                            TimingTowerDriverCard(
                                driver = driver,
                                isIntervalMode = isIntervalMode,
                                isSelected = driver.code == validSelectedDriverCode,
                                onClick = {
                                    selectedDriverCode = if (validSelectedDriverCode == driver.code) null else driver.code
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    "Race control" -> {
                        item(key = "rc_header") {
                            Text(
                                text = "RACE CONTROL LOG",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        if (keyedRaceControl.isEmpty()) {
                            item(key = "rc_empty") {
                                LiveTimingEmptyStateCard(
                                    icon = Icons.Default.Flag,
                                    title = "No race control messages",
                                    subtitle = "Steward notices, track status flags, and safety car updates will appear here."
                                )
                            }
                        } else {
                            items(
                                items = keyedRaceControl,
                                key = { it.first }
                            ) { (_, msg) ->
                                RaceControlMessageCard(msg)
                            }
                        }
                    }

                    "Radio" -> {
                        item(key = "radio_header") {
                            Text(
                                text = "TEAM RADIO TRANSMISSIONS",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        if (keyedRadioClips.isEmpty()) {
                            item(key = "radio_empty") {
                                LiveTimingEmptyStateCard(
                                    icon = Icons.Default.Headphones,
                                    title = "No team radio clips",
                                    subtitle = "Driver and pit wall radio transmissions will appear here when available."
                                )
                            }
                        } else {
                            items(
                                items = keyedRadioClips,
                                key = { it.first }
                            ) { (_, clip) ->
                                val isClipActive = activeRadioClipId == clip.id
                                val isClipPlaying = isRadioPlaying && isClipActive
                                val isUnavailable = clip.resolveRecordingUrl().isNullOrBlank() || streamErrorClipId == clip.id
                                TeamRadioClipItem(
                                    clip = clip,
                                    isPlaying = isClipPlaying,
                                    progress = if (isClipActive) radioProgress else 0f,
                                    isMuted = isAudioMuted,
                                    isAudioUnavailable = isUnavailable,
                                    onTogglePlay = { handleRadioToggle(clip) }
                                )
                            }
                        }
                    }

                    "Overtakes" -> {
                        item(key = "overtakes_header") {
                            Text(
                                text = "OVERTAKE TRACKER",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        if (keyedOvertakes.isEmpty()) {
                            item(key = "overtakes_empty") {
                                val sessionDesc = sessionState.sessionName.ifBlank { sessionState.sessionType }.lowercase()
                                LiveTimingEmptyStateCard(
                                    icon = Icons.Default.SwapHoriz,
                                    title = "No overtakes recorded",
                                    subtitle = "No overtakes recorded in this $sessionDesc session."
                                )
                            }
                        } else {
                            items(
                                items = keyedOvertakes,
                                key = { it.first }
                            ) { (_, overtake) ->
                                OvertakeEventCard(overtake)
                            }
                        }
                    }

                    "Weather" -> {
                        item(key = "weather_card") {
                            WeatherConditionsCard(sessionState.weather)
                        }
                    }
                }

                item(key = "bottom_nav_spacer") {
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(72.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top Bar: Compact CircularHeaderButton for Close and Mute, giving generous room on 360dp screens
 * for the Grand Prix title and LIVE / LAST SESSION + session name row.
 */
@Composable
private fun LiveTimingTopBar(
    grandPrixName: String,
    sessionName: String,
    isAudioMuted: Boolean,
    onToggleMute: () -> Unit,
    onClose: () -> Unit,
    isLive: Boolean = false
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_dot_pulse")
    val livePulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularHeaderButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }

        // Generous room for GP Title & Session Badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = grandPrixName.ifBlank { "Live Timing" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = P1TTheme.redFlagBg,
                        border = BorderStroke(1.dp, P1TTheme.redFlagBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .alpha(livePulseAlpha)
                                    .clip(CircleShape)
                                    .background(P1TTheme.accentRed)
                            )
                            Text(
                                text = "LIVE",
                                color = P1TTheme.accentRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = P1TTheme.pillBg,
                        border = BorderStroke(1.dp, P1TTheme.pillBorder)
                    ) {
                        Text(
                            text = "LAST SESSION",
                            color = P1TTheme.textMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Text(
                    text = sessionName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        CircularHeaderButton(onClick = onToggleMute) {
            Icon(
                imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                tint = if (isAudioMuted) P1TTheme.accentRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * 3-column Stat Box:
 * Q1 LEFT 8:21 | BEST · VER 1:36.477 (Motorsport Purple highlight) | ON TRACK 21
 */
@Composable
private fun LiveStatThreeColumnBox(
    shortSessionCode: String,
    timeRemaining: String,
    bestLapDriverCode: String,
    bestLapTime: String,
    carsOnTrack: Int
) {
    val isDark = LocalF1Accent.current.isDark
    val purpleBg = P1TColors.FastestLapPurple.copy(alpha = if (isDark) 0.20f else 0.11f)
    val purpleBorder = P1TTheme.sectorPurple.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, P1TTheme.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Time Left
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = timeRemaining.ifBlank { "—" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Column 2: Best Lap (Vivid Motorsport Purple Highlight)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = purpleBg,
                border = BorderStroke(1.dp, purpleBorder),
                modifier = Modifier.weight(1.35f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bestLabel = if (bestLapDriverCode.isNotBlank()) "BEST · $bestLapDriverCode" else "FASTEST LAP"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = P1TTheme.sectorPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = bestLabel,
                            color = P1TTheme.sectorPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bestLapTime.ifBlank { "—" },
                        color = if (isDark) Color.White else P1TColors.FastestLapPurpleDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Column 3: On Track
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ON TRACK",
                    color = P1TTheme.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = carsOnTrack.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                    maxLines = 1
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
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onBackground else P1TTheme.textMuted,
                animationSpec = tween(durationMillis = 180, easing = ApexMotion.FastEase),
                label = "live_subtab_text_color"
            )
            val indicatorColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                animationSpec = tween(durationMillis = 180, easing = ApexMotion.FastEase),
                label = "live_subtab_indicator_color"
            )
            val indicatorWidth by animateDpAsState(
                targetValue = if (isSelected) 26.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
                label = "live_subtab_indicator_width"
            )
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
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(indicatorColor)
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
    isAudioUnavailable: Boolean = false,
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
                        text = teamName,
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
                    if (isAudioUnavailable) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Audio stream unavailable",
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                            .background(
                                if (isAudioUnavailable) P1TTheme.pillBg else MaterialTheme.colorScheme.onSurface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = when {
                                isAudioUnavailable -> "Audio stream unavailable"
                                isPlaying -> "Pause radio"
                                isMuted -> "Play radio (muted)"
                                else -> "Play radio"
                            },
                            tint = if (isAudioUnavailable) P1TTheme.textMuted else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Progress Bar (0f when idle/unstarted; never simulated when muted or unavailable)
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
                        .fillMaxWidth(if (isAudioUnavailable) 0f else progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isMuted) P1TTheme.textMuted else P1TTheme.accentRed)
                )
            }
        }
    }
}

/**
 * Broadcast Race Control Status Banner (Image 3 style):
 * Vivid GREEN FLAG (Track Clear), SAFETY CAR (amber), RED FLAG (F1 red), or FASTEST LAP (purple chevron).
 */
@Composable
private fun LiveBroadcastRaceControlBanner(
    sessionState: LiveSessionState
) {
    val latestMsg = sessionState.raceControlMessages.firstOrNull()
    val effectiveTrackStatus = sessionState.trackStatusText.ifBlank { "Track Clear" }
    val trackType = resolveBroadcastFlagType(effectiveTrackStatus)
    val msgType = latestMsg?.let { resolveBroadcastFlagType("${it.category} ${it.title}") }
        ?: BroadcastFlagBannerType.NEUTRAL

    val bannerType = when {
        trackType == BroadcastFlagBannerType.RED_FLAG ||
            trackType == BroadcastFlagBannerType.SAFETY_CAR ||
            trackType == BroadcastFlagBannerType.VSC -> trackType
        msgType == BroadcastFlagBannerType.FASTEST_LAP -> BroadcastFlagBannerType.FASTEST_LAP
        trackType != BroadcastFlagBannerType.NEUTRAL -> trackType
        msgType != BroadcastFlagBannerType.NEUTRAL -> msgType
        else -> BroadcastFlagBannerType.GREEN_FLAG
    }

    val headline = when (bannerType) {
        BroadcastFlagBannerType.RED_FLAG -> "RED FLAG"
        BroadcastFlagBannerType.SAFETY_CAR ->
            if (effectiveTrackStatus.contains("YELLOW", ignoreCase = true)) "YELLOW FLAG" else "SAFETY CAR"
        BroadcastFlagBannerType.VSC -> "VIRTUAL SAFETY CAR"
        BroadcastFlagBannerType.FASTEST_LAP -> "FASTEST LAP"
        BroadcastFlagBannerType.GREEN_FLAG -> "GREEN FLAG"
        BroadcastFlagBannerType.NEUTRAL -> latestMsg?.title?.takeIf { it.isNotBlank() } ?: "RACE CONTROL"
    }

    val subtitle = when (bannerType) {
        BroadcastFlagBannerType.FASTEST_LAP -> {
            val driver = sessionState.bestLapDriverCode
            val lapTime = sessionState.bestLapTime
            if (driver.isNotBlank() && lapTime.isNotBlank() && lapTime != "—") {
                "$driver · $lapTime"
            } else {
                latestMsg?.title?.takeIf { it.isNotBlank() } ?: effectiveTrackStatus
            }
        }
        else -> {
            val msgTitle = latestMsg?.title?.takeIf { it.isNotBlank() }
            when {
                msgTitle != null &&
                    !msgTitle.equals(headline, ignoreCase = true) &&
                    !msgTitle.equals(effectiveTrackStatus, ignoreCase = true) ->
                    "$effectiveTrackStatus · $msgTitle"
                else -> effectiveTrackStatus
            }
        }
    }

    RaceControlStatusBanner(
        headline = headline,
        subtitle = subtitle,
        bannerType = bannerType,
        headerLabel = if (bannerType == BroadcastFlagBannerType.FASTEST_LAP) "FASTEST LAP" else "RACE CONTROL",
        trailingBadge = latestMsg?.timestamp?.takeIf { it.isNotBlank() }
    )
}

/**
 * Timing Tower Header: POS | DRIVER | Segmented toggle Interval / Leader
 */
@Composable
private fun TimingTowerHeader(
    isIntervalMode: Boolean,
    onToggleMode: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "POS",
            color = P1TTheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp)
        )
        Text(
            text = "DRIVER",
            color = P1TTheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Segmented Toggle: Interval / Leader (each option has >= 48.dp touch target height)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = P1TTheme.surfaceInnerCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(modifier = Modifier.padding(horizontal = 3.dp)) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleMode(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isIntervalMode) P1TTheme.pillSelectedBg else Color.Transparent
                    ) {
                        Text(
                            text = "Interval",
                            color = if (isIntervalMode) MaterialTheme.colorScheme.onSurface else P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleMode(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (!isIntervalMode) P1TTheme.pillSelectedBg else Color.Transparent
                    ) {
                        Text(
                            text = "Leader",
                            color = if (!isIntervalMode) MaterialTheme.colorScheme.onSurface else P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Timing Tower Driver Card (Image 3 style):
 * Aligned columns: Position + vertical team color bar, 30.dp DriverPortraitAvatar,
 * 3-letter bold code + muted surname + IN PIT/OUT LAP pill, TyreCompoundBadge + MiniSectorBars,
 * and tabular monospace gap + best lap time. Tap expands 2x3 telemetry grid.
 */
@Composable
private fun TimingTowerDriverCard(
    driver: LiveDriverTiming,
    isIntervalMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val teamColor = Color(driver.team.colorHex)
    val effectiveBarColor = if (driver.team == F1TeamId.OTHER) P1TTheme.textMuted else teamColor
    val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
    val borderColor = if (isSelected) adaptiveTeamColor else P1TTheme.borderSubtle
    val chevronRotation by animateFloatAsState(
        targetValue = if (isSelected) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium),
        label = "timing_tower_chevron_rotation"
    )
    val surname = remember(driver.fullName, driver.code) {
        extractDriverSurname(driver.fullName, driver.code)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
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
                // Position + vertical team color bar
                Text(
                    text = driver.position.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(22.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(effectiveBarColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 30.dp Driver Portrait Avatar
                DriverPortraitAvatar(
                    driverCodeOrName = driver.code,
                    teamColor = effectiveBarColor,
                    size = 30.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Driver Identity + Tyre & MiniSectors
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
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (surname.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = surname,
                                color = P1TTheme.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        if (driver.isInPit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = P1TTheme.inPitText.copy(alpha = if (P1TTheme.isDark) 0.22f else 0.12f),
                                border = BorderStroke(1.dp, P1TTheme.inPitText)
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
                        } else if (driver.isOutLap) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = P1TTheme.pillBg,
                                border = BorderStroke(1.dp, P1TTheme.pillBorder)
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

                    Spacer(modifier = Modifier.height(5.dp))

                    // Second row: Tyre compound badge + age + MiniSectorBars
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TyreCompoundBadge(compound = driver.tyreCompound, sizeDp = 16)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${driver.tyreAgeLaps}L",
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
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

                // Right Column: Tabular monospace Gap / Leader + Best Lap Time
                val gapText = if (isIntervalMode) driver.intervalToAhead else driver.gapToLeader
                val isLeaderGap = gapText.equals("Leader", ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.widthIn(min = 64.dp)
                ) {
                    Text(
                        text = gapText,
                        color = if (isLeaderGap) P1TTheme.sectorGreen else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = driver.bestLapTime,
                        color = if (driver.isOverallBest) P1TTheme.sectorPurple else P1TTheme.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (driver.isOverallBest) FontWeight.Bold else FontWeight.Medium,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isSelected) "Collapse telemetry" else "Expand telemetry",
                    tint = P1TTheme.textMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            // Expanded Telemetry Grid (2×3)
            AnimatedVisibility(
                visible = isSelected,
                enter = ApexMotion.cardExpandEnter(),
                exit = ApexMotion.cardExpandExit()
            ) {
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
                            status = if (driver.isOverallBest) SectorStatus.OVERALL_BEST else null,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            label = "TYRE",
                            value = "${driver.tyreCompound.label} · ${driver.tyreAgeLaps}L",
                            modifier = Modifier.weight(1.25f),
                            useMonospace = false,
                            maxLines = 2
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
    status: SectorStatus? = null,
    useMonospace: Boolean = true,
    maxLines: Int = 2
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
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            style = if (useMonospace) {
                TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")
            } else {
                TextStyle.Default
            },
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private enum class RaceControlVisualCategory(
    val badgeLabel: String,
    val icon: ImageVector
) {
    RED_FLAG("RED FLAG", Icons.Filled.NotInterested),
    SAFETY_CAR("SAFETY CAR", Icons.Filled.DirectionsCar),
    VSC("VSC", Icons.Filled.Warning),
    INVESTIGATION("INVESTIGATION", Icons.Filled.Gavel),
    FASTEST_LAP("FASTEST LAP", Icons.Filled.Bolt),
    GREEN_FLAG("CLEAR", Icons.Filled.Flag),
    INFO("RACE CONTROL", Icons.Filled.Info)
}

private fun classifyRaceControlMessage(msg: RaceControlMessage): RaceControlVisualCategory {
    val combined = "${msg.category} ${msg.title}".uppercase(Locale.ROOT)
    return when {
        combined.contains("RED_FLAG") ||
            combined.contains("RED FLAG") ||
            msg.title.trim().equals("RED", ignoreCase = true) ||
            combined.contains("SESSION SUSPENDED") -> RaceControlVisualCategory.RED_FLAG
        combined.contains("VSC") || combined.contains("VIRTUAL SAFETY") -> RaceControlVisualCategory.VSC
        combined.contains("SAFETY_CAR") ||
            combined.contains("SAFETY CAR") ||
            combined.contains("SC DEPLOYED") ||
            combined.contains("YELLOW") -> RaceControlVisualCategory.SAFETY_CAR
        combined.contains("INVESTIGATION") ||
            combined.contains("NOTED") ||
            combined.contains("PENALTY") ||
            combined.contains("STEWARD") ||
            combined.contains("TRACK LIMITS") -> RaceControlVisualCategory.INVESTIGATION
        combined.contains("FASTEST_LAP") ||
            combined.contains("FASTEST LAP") ||
            combined.contains("PURPLE") -> RaceControlVisualCategory.FASTEST_LAP
        combined.contains("GREEN_FLAG") ||
            combined.contains("GREEN FLAG") ||
            combined.contains("CLEAR") ||
            combined.contains("SESSION STARTED") -> RaceControlVisualCategory.GREEN_FLAG
        else -> RaceControlVisualCategory.INFO
    }
}

private data class RaceControlVisualStyle(
    val accent: Color,
    val badgeBg: Color,
    val badgeBorder: Color
)

@Composable
private fun resolveRaceControlStyle(category: RaceControlVisualCategory): RaceControlVisualStyle {
    val isDark = P1TTheme.isDark
    return when (category) {
        RaceControlVisualCategory.RED_FLAG -> RaceControlVisualStyle(
            accent = P1TTheme.accentRed,
            badgeBg = P1TTheme.redFlagBg,
            badgeBorder = P1TTheme.redFlagBorder
        )
        RaceControlVisualCategory.SAFETY_CAR -> RaceControlVisualStyle(
            accent = if (isDark) Color(0xFFFF9800) else Color(0xFFB25E00),
            badgeBg = P1TTheme.sectorYellowBg,
            badgeBorder = P1TTheme.sectorYellowBorder
        )
        RaceControlVisualCategory.VSC -> RaceControlVisualStyle(
            accent = P1TTheme.sectorYellow,
            badgeBg = P1TTheme.sectorYellowBg,
            badgeBorder = P1TTheme.sectorYellowBorder
        )
        RaceControlVisualCategory.INVESTIGATION -> RaceControlVisualStyle(
            accent = P1TTheme.inPitText,
            badgeBg = P1TTheme.inPitText.copy(alpha = if (isDark) 0.18f else 0.10f),
            badgeBorder = P1TTheme.inPitText.copy(alpha = 0.45f)
        )
        RaceControlVisualCategory.FASTEST_LAP -> RaceControlVisualStyle(
            accent = P1TTheme.sectorPurple,
            badgeBg = P1TColors.FastestLapPurple.copy(alpha = if (isDark) 0.22f else 0.12f),
            badgeBorder = P1TTheme.sectorPurple.copy(alpha = 0.50f)
        )
        RaceControlVisualCategory.GREEN_FLAG -> RaceControlVisualStyle(
            accent = P1TTheme.sectorGreen,
            badgeBg = P1TTheme.trackClearBg,
            badgeBorder = P1TTheme.trackClearBorder
        )
        RaceControlVisualCategory.INFO -> RaceControlVisualStyle(
            accent = P1TTheme.textMuted,
            badgeBg = P1TTheme.pillBg,
            badgeBorder = P1TTheme.pillBorder
        )
    }
}

@Composable
private fun RaceControlMessageCard(msg: RaceControlMessage) {
    val category = remember(msg.category, msg.title) { classifyRaceControlMessage(msg) }
    val style = resolveRaceControlStyle(category)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, P1TTheme.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(style.accent)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = style.badgeBg,
                        border = BorderStroke(1.dp, style.badgeBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = style.accent,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = category.badgeLabel,
                                color = style.accent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (msg.timestamp.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg.timestamp,
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = msg.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeamRadioClipItem(
    clip: TeamRadioClip,
    isPlaying: Boolean,
    progress: Float = 0f,
    isMuted: Boolean = false,
    isAudioUnavailable: Boolean = false,
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
                            .background(
                                if (isAudioUnavailable) P1TTheme.pillBg else teamColor.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isAudioUnavailable) P1TTheme.pillBorder else adaptiveTeam,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = when {
                                isAudioUnavailable -> "Audio stream unavailable"
                                isPlaying -> "Pause"
                                isMuted -> "Play (muted)"
                                else -> "Play"
                            },
                            tint = if (isAudioUnavailable) P1TTheme.textMuted else adaptiveTeam,
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
                    if (isAudioUnavailable) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Audio stream unavailable",
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!isAudioUnavailable && (isPlaying || progress > 0f)) {
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
private fun LiveTimingEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = P1TTheme.textMuted,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherParam(label = "TRACK TEMP", value = "${weather.trackTempC}°C", modifier = Modifier.weight(1f))
                WeatherParam(label = "AIR TEMP", value = "${weather.airTempC}°C", modifier = Modifier.weight(1f))
                WeatherParam(label = "HUMIDITY", value = "${weather.humidityPercent}%", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val windText = buildString {
                    append("${weather.windSpeedKmh} km/h")
                    if (weather.windDirection.isNotBlank()) {
                        append(" · ")
                        append(weather.windDirection.trim())
                    }
                }
                WeatherParam(label = "WIND", value = windText, modifier = Modifier.weight(1f))
                WeatherParam(label = "RAINFALL", value = "${weather.rainfallPercent}%", modifier = Modifier.weight(1f))
                WeatherParam(label = "PRESSURE", value = "${weather.pressureMbar} mbar", modifier = Modifier.weight(1f))
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
private fun WeatherParam(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = P1TTheme.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
