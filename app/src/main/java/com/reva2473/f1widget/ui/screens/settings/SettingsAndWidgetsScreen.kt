package com.reva2473.f1widget.ui.screens.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceRecentResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.domain.model.WidgetStyleType
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.F1CarSideProfileCanvas
import com.reva2473.f1widget.ui.components.F1CarTopDownCanvas
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme

private fun countCompletedRounds(schedule: List<RaceWeekend>): Int {
    if (schedule.isEmpty()) return 0
    val now = System.currentTimeMillis()
    return schedule.count {
        it.results.isNotEmpty() ||
            (it.sessions.lastOrNull()?.utcEpochMillis?.let { ts -> ts < now } == true) ||
            it.sessions.any { s ->
                s.countdownBadge.equals("Completed", true) || s.countdownBadge.equals("Final", true)
            }
    }
}

private fun resolveCurrentDriverStandings(
    driverStandings: List<DriverStanding>,
    favoriteDriverCode: String
): List<DriverStanding> {
    val seed2026 = SeedDataProvider.getDriverStandings2026()
    if (driverStandings.isEmpty()) return seed2026
    val matchesCurrent = driverStandings.any {
        it.code.equals(favoriteDriverCode, ignoreCase = true)
    } || driverStandings.any { d ->
        seed2026.any { s -> s.code.equals(d.code, ignoreCase = true) }
    }
    return if (matchesCurrent) driverStandings else seed2026
}

private fun resolveCurrentConstructorStandings(
    constructorStandings: List<ConstructorStanding>,
    favoriteTeam: F1TeamId
): List<ConstructorStanding> {
    val seed2026 = SeedDataProvider.getConstructorStandings2026()
    if (constructorStandings.isEmpty()) return seed2026
    val matchesCurrent = constructorStandings.any {
        it.team == favoriteTeam
    } || constructorStandings.any { c ->
        c.team != F1TeamId.OTHER && seed2026.any { s -> s.team == c.team }
    }
    return if (matchesCurrent) constructorStandings else seed2026
}

private fun darkSurfaceReadableColor(color: Color): Color {
    return if (color.luminance() >= 0.18f) color else lerp(color, Color.White, 0.42f)
}

private fun requestPinWidget(context: Context, widgetType: WidgetStyleType) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        Toast.makeText(context, "Pinning widgets is not supported by your launcher", Toast.LENGTH_SHORT).show()
        return
    }
    val receiverClass = when (widgetType) {
        WidgetStyleType.NEXT_GP_COUNTDOWN -> com.reva2473.f1widget.widget.NextRaceCountdownWidgetReceiver::class.java
        WidgetStyleType.WEEKEND_SESSIONS -> com.reva2473.f1widget.widget.WeekendScheduleWidgetReceiver::class.java
        WidgetStyleType.WDC_STANDINGS -> com.reva2473.f1widget.widget.DriverStandingsWidgetReceiver::class.java
        WidgetStyleType.WCC_STANDINGS -> com.reva2473.f1widget.widget.ConstructorStandingsWidgetReceiver::class.java
        WidgetStyleType.FAVORITE_DRIVER -> com.reva2473.f1widget.widget.FavoriteDriverWidgetReceiver::class.java
        WidgetStyleType.FAVORITE_TEAM -> com.reva2473.f1widget.widget.FavoriteTeamWidgetReceiver::class.java
        WidgetStyleType.LAST_RACE_PODIUM -> com.reva2473.f1widget.widget.LastRacePodiumWidgetReceiver::class.java
        WidgetStyleType.LIVE_TIMING_BANNER -> com.reva2473.f1widget.widget.LiveTimingBannerWidgetReceiver::class.java
    }
    val myProvider = ComponentName(context, receiverClass)
    val callbackIntent = Intent(context, receiverClass).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val successCallback = PendingIntent.getBroadcast(
        context,
        widgetType.ordinal,
        callbackIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val success = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
    Toast.makeText(
        context,
        if (success) "Pin request sent for ${widgetType.title}" else "Could not request widget pin",
        Toast.LENGTH_SHORT
    ).show()
}

private fun readOptionalIntProp(target: Any, getterName: String): Int? {
    return runCatching {
        val method = target.javaClass.methods.firstOrNull {
            it.name == getterName && it.parameterCount == 0
        }
        (method?.invoke(target) as? Number)?.toInt()
    }.getOrNull()
}

private fun computeLiveSessionProgress(liveSession: LiveSessionState): Float {
    val currentLap = readOptionalIntProp(liveSession, "getCurrentLap")
    val totalLaps = readOptionalIntProp(liveSession, "getTotalLaps")
    if (currentLap != null && totalLaps != null && totalLaps > 0) {
        return (currentLap.toFloat() / totalLaps.toFloat()).coerceIn(0f, 1f)
    }

    val raw = liveSession.timeRemaining.trim()
    if (raw.equals("Final", ignoreCase = true) ||
        raw.equals("Finished", ignoreCase = true) ||
        raw.equals("Completed", ignoreCase = true) ||
        raw == "0:00"
    ) {
        return 1f
    }
    if (raw.equals("Starts soon", ignoreCase = true) || raw.isBlank()) {
        return 0f
    }

    val lapMatch = Regex("""(\d+)\s*/\s*(\d+)""").find(raw)
    if (lapMatch != null) {
        val cur = lapMatch.groupValues[1].toFloatOrNull() ?: 0f
        val tot = lapMatch.groupValues[2].toFloatOrNull() ?: 0f
        if (tot > 0f) return (cur / tot).coerceIn(0f, 1f)
    }

    val cleanedTime = raw.replace("left", "", ignoreCase = true).trim()
    val parts = cleanedTime.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size in 2..3) {
        val remainingSecs = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            else -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        }.coerceAtLeast(0)
        val code = liveSession.shortSessionCode.uppercase()
        val nominalTotalSecs = when {
            code.contains("Q1") -> 18 * 60
            code.contains("Q2") -> 15 * 60
            code.contains("Q3") -> 12 * 60
            code.contains("SQ") -> 12 * 60
            code.contains("SPR") -> 30 * 60
            code.contains("RACE") -> 90 * 60
            else -> 60 * 60
        }
        return (1f - (remainingSecs.toFloat() / nominalTotalSecs.toFloat())).coerceIn(0.05f, 0.98f)
    }

    return if (liveSession.isLive) 0.5f else 0f
}

/**
 * Screen 4: SettingsAndWidgetsScreen
 * Featuring:
 * 1. In-app Widgets Preview Gallery displaying all 8 Home/Lock screen widget styles
 *    personalized with user's selected Favorite Driver & Favorite Team.
 * 2. Team Colour Accent Selector (all 11 F1 constructors — tapping immediately updates F1TeamId theme accent).
 * 3. Theme Mode (Dark, Light, System), Pit Radio Alerts, Offline Caching, and 1950–2026 Archive Browser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndWidgetsScreen(
    userPreferences: UserPreferencesState,
    onUpdatePreferences: (UserPreferencesState) -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier,
    driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState(),
    nextWeekend: RaceWeekend = SeedDataProvider.getNextRaceWeekend(),
    lastCompletedRace: RaceWeekend? = null,
    schedule: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026)
) {
    var selectedGalleryTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedWidgetIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDriverPicker by remember { mutableStateOf(false) }

    val resolvedDriverStandings = remember(driverStandings, userPreferences.favoriteDriverCode) {
        resolveCurrentDriverStandings(driverStandings, userPreferences.favoriteDriverCode)
    }
    val resolvedConstructorStandings = remember(constructorStandings, userPreferences.favoriteTeam) {
        resolveCurrentConstructorStandings(constructorStandings, userPreferences.favoriteTeam)
    }

    val galleryTabs = listOf(
        "2×2 WCC Cards",
        "Live Lock-Screen",
        "Constructors 4×4 & 4×2",
        "Team Form Matrix",
        "Custom Widget Configurator"
    )
    val widgetTypes = WidgetStyleType.entries

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Widgets",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: In-App Widgets Preview Gallery
            item {
                Text(
                    text = "HOME SCREEN WIDGETS GALLERY",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Gallery Category Tabs
            item {
                val primaryAccent = LocalF1Accent.current.primaryAccent
                val onAccentColor = P1TTheme.onColor(primaryAccent)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    galleryTabs.forEachIndexed { idx, tabTitle ->
                        val isSelected = idx == selectedGalleryTab
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedGalleryTab = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) primaryAccent else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) onAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Gallery Content
            item {
                val primaryAccent = LocalF1Accent.current.primaryAccent
                val onAccentColor = P1TTheme.onColor(primaryAccent)
                val context = LocalContext.current

                val quickPinWidgetType = when (selectedGalleryTab) {
                    0 -> WidgetStyleType.WCC_STANDINGS
                    1 -> WidgetStyleType.LIVE_TIMING_BANNER
                    2 -> WidgetStyleType.WCC_STANDINGS
                    3 -> WidgetStyleType.FAVORITE_TEAM
                    else -> null
                }

                when (selectedGalleryTab) {
                    0 -> WccSquareCardsShowcase(constructorStandings = resolvedConstructorStandings)
                    1 -> LiveSessionBannerWidgetShowcase(
                        liveSession = liveSession,
                        userPreferences = userPreferences
                    )
                    2 -> ConstructorsBarChartWidgetShowcase(
                        constructorStandings = resolvedConstructorStandings,
                        driverStandings = resolvedDriverStandings,
                        schedule = schedule
                    )
                    3 -> TeamFormAndTeammatesWidgetShowcase(
                        userPreferences = userPreferences,
                        constructorStandings = resolvedConstructorStandings,
                        driverStandings = resolvedDriverStandings,
                        lastCompletedRace = lastCompletedRace,
                        schedule = schedule
                    )
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sub-selector of the 8 standard customizable widget styles
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                widgetTypes.forEachIndexed { idx, type ->
                                    val isSelected = idx == selectedWidgetIndex
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedWidgetIndex = idx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) P1TTheme.pillSelectedBg else P1TTheme.pillBg,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, P1TTheme.adaptiveTeamText(primaryAccent)) else null
                                        ) {
                                            Text(
                                                text = type.title,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            val currentType = widgetTypes[selectedWidgetIndex]
                            WidgetLivePreviewCard(
                                widgetType = currentType,
                                preferences = userPreferences,
                                driverStandings = resolvedDriverStandings,
                                constructorStandings = resolvedConstructorStandings,
                                liveSession = liveSession,
                                nextWeekend = nextWeekend,
                                lastCompletedRace = lastCompletedRace,
                                schedule = schedule
                            )

                            // Pin Widget to Home Screen action button
                            Button(
                                onClick = { requestPinWidget(context, currentType) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryAccent,
                                    contentColor = onAccentColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pin ${currentType.title} to Home Screen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                if (quickPinWidgetType != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { requestPinWidget(context, quickPinWidgetType) },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            P1TTheme.adaptiveTeamText(primaryAccent).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = P1TTheme.adaptiveTeamText(primaryAccent),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pin Selected Widget (${quickPinWidgetType.title})",
                            color = P1TTheme.adaptiveTeamText(primaryAccent),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Section 2: Team Colour Accent Selector (all 11 F1 constructors)
            item {
                Text(
                    text = "TEAM ACCENT THEME",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Select Team Accent",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dynamically tints buttons, header glows, charts and widget badges.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // 11 Team Color Badges Grid/Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(F1TeamId.entries.toTypedArray(), key = { it.name }) { team ->
                                val isSelected = team == userPreferences.favoriteTeam
                                val teamColor = Color(team.colorHex)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            onUpdatePreferences(userPreferences.copy(favoriteTeam = team))
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(teamColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color(0x33888888),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = P1TTheme.onColor(teamColor),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = team.shortCode,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else P1TTheme.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        SettingsToggleRow(
                            title = "Use Team Accent Color",
                            subtitle = "Apply selected team livery color across app & widgets",
                            checked = userPreferences.useTeamColorAccent,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(useTeamColorAccent = it))
                            }
                        )
                    }
                }
            }

            // Section 3: Favourite Driver Personalization
            item {
                Text(
                    text = "FAVOURITE DRIVER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                val favDriver = resolvedDriverStandings.firstOrNull {
                    it.code.equals(userPreferences.favoriteDriverCode, ignoreCase = true)
                } ?: resolvedDriverStandings.firstOrNull()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDriverPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        favDriver?.let { drv ->
                            DriverPortraitAvatar(
                                driverCodeOrName = drv.fullName.ifBlank { drv.code },
                                teamColor = Color(drv.team.colorHex),
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = drv.fullName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${drv.team.displayName} · #${drv.position} in Championship",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change driver",
                            tint = P1TTheme.textMuted
                        )
                    }
                }
            }

            // Section 4: Display & Appearance
            item {
                Text(
                    text = "APPEARANCE & NOTIFICATIONS",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Theme mode toggle: Dark / Light / System (responsive on narrow screens)
                        val primaryAccent = LocalF1Accent.current.primaryAccent
                        val onAccentColor = P1TTheme.onColor(primaryAccent)

                        @Composable
                        fun ThemeModePills() {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                                    val isSelected = userPreferences.themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onUpdatePreferences(userPreferences.copy(themeMode = mode))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) primaryAccent else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = mode.lowercase().replaceFirstChar { it.uppercase() },
                                                color = if (isSelected) onAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth < 360.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Theme Mode",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    ThemeModePills()
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Theme Mode",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    ThemeModePills()
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        // Race Control & Pit Radio Alerts switch
                        SettingsToggleRow(
                            title = "Race Control & Pit Alerts",
                            subtitle = "Notify on flags, safety cars, and key pit radio calls",
                            checked = userPreferences.pitRadioAlertsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(pitRadioAlertsEnabled = it))
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        // Fastest Lap & Track Record Alerts switch
                        SettingsToggleRow(
                            title = "Lap Record Alerts",
                            subtitle = "Notify when a new fastest lap or track record is set",
                            checked = userPreferences.lapRecordAlertsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(lapRecordAlertsEnabled = it))
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        // Session Reminders switch
                        SettingsToggleRow(
                            title = "Session Reminders",
                            subtitle = "Notify 15 minutes before Practice, Quali & Grand Prix",
                            checked = userPreferences.sessionRemindersEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(sessionRemindersEnabled = it))
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        // Offline Cache
                        SettingsToggleRow(
                            title = "Offline Cache",
                            subtitle = "Keep weekend schedules and standings available offline",
                            checked = userPreferences.offlineCacheEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(offlineCacheEnabled = it))
                            }
                        )
                    }
                }
            }

            // Section 5: Historical Archive Link
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToArchive)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = P1TTheme.textMuted,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "F1 Historical Archive (1950–2026)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "77 seasons of champion drivers, constructors & race results",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open archive",
                            tint = P1TTheme.accentRed
                        )
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

    if (showDriverPicker) {
        Dialog(onDismissRequest = { showDriverPicker = false }) {
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
                            text = "Select Favourite Driver",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDriverPicker = false }, modifier = Modifier.size(48.dp)) {
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
                        itemsIndexed(
                            items = resolvedDriverStandings,
                            key = { idx, drv -> "${drv.driverId}_${drv.code}_$idx" }
                        ) { _, drv ->
                            val isSelected = drv.code.equals(userPreferences.favoriteDriverCode, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) P1TTheme.pillSelectedBg else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.dp, Color(drv.team.colorHex))
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable {
                                        onUpdatePreferences(userPreferences.copy(favoriteDriverCode = drv.code))
                                        showDriverPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DriverPortraitAvatar(
                                        driverCodeOrName = drv.fullName.ifBlank { drv.code },
                                        teamColor = Color(drv.team.colorHex),
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = drv.code,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = drv.fullName,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = drv.team.shortCode,
                                        color = P1TTheme.textMuted,
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
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = P1TTheme.onColor(LocalF1Accent.current.primaryAccent),
                checkedTrackColor = LocalF1Accent.current.primaryAccent,
                uncheckedThumbColor = P1TTheme.textMuted,
                uncheckedTrackColor = P1TTheme.pillBg
            )
        )
    }
}

/**
 * 2×2 Team WCC Square Cards Grid (Dynamic Top-6 Constructors Showcase)
 */
@Composable
private fun WccSquareCardsShowcase(
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026()
) {
    val standings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val sorted = standings.sortedBy { it.position }
    val leaderStanding = sorted.firstOrNull()
    val secondStanding = sorted.getOrNull(1)
    val leaderPts = leaderStanding?.points?.toInt() ?: 0
    val leaderNameUpper = leaderStanding?.resolvedConstructorName?.uppercase() ?: "P1"
    val topSix = sorted.take(6)

    fun gapLabel(teamPts: Int, isLeader: Boolean): String {
        return if (isLeader && secondStanding != null) {
            val lead = (teamPts - secondStanding.points.toInt()).coerceAtLeast(0)
            "+$lead ON ${secondStanding.resolvedConstructorName.uppercase()}"
        } else {
            val behind = (leaderPts - teamPts).coerceAtLeast(0)
            "-$behind TO $leaderNameUpper"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "2×2 CONSTRUCTORS (WCC) SQUARES (TOP ${topSix.size} SHOWCASE)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        topSix.chunked(2).forEachIndexed { rowIdx, pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEachIndexed { colIdx, standing ->
                    val cardIndex = rowIdx * 2 + colIdx
                    val isLeader = standing.position == (leaderStanding?.position ?: 1) &&
                        standing.team == leaderStanding?.team
                    WccSquareCardItem(
                        standing = standing,
                        gapText = gapLabel(standing.points.toInt(), isLeader),
                        cardVariant = cardIndex,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 188.dp)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WccSquareCardItem(
    standing: ConstructorStanding,
    gapText: String,
    cardVariant: Int,
    modifier: Modifier = Modifier
) {
    val team = standing.team
    val teamColor = Color(team.colorHex)
    val teamSecondary = Color(team.secondaryHex)
    val readableAccent = darkSurfaceReadableColor(teamColor)
    val pts = standing.points.toInt()
    val wins = standing.wins
    val pos = standing.position
    val teamNameUpper = standing.resolvedConstructorName.uppercase()

    when (cardVariant % 6) {
        0 -> {
            // Variant 0: Dark gradient with side-profile car + white stats bar
            val bgTop = lerp(teamSecondary, teamColor, 0.18f)
            val bgBottom = lerp(teamSecondary, Color.Black, 0.72f)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgBottom,
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            TeamLogoBadge(team = team, sizeDp = 22)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$pos",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 32.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "WCC",
                                    color = readableAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        F1CarSideProfileCanvas(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(14.dp)
                                        .background(teamColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$pts PTS  $wins WINS",
                                    color = Color.Black,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = gapText,
                            color = readableAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        1 -> {
            // Variant 1: Team dark gradient + horizontal top-down car
            val bgTop = lerp(teamSecondary, teamColor, 0.35f)
            val bgBottom = lerp(teamSecondary, Color.Black, 0.65f)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgBottom,
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                TeamLogoBadge(team = team, sizeDp = 20)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = teamNameUpper,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "P$pos",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "$pts PTS",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        F1CarTopDownCanvas(
                            team = team,
                            vertical = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )

                        Text(
                            text = gapText,
                            color = readableAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        2 -> {
            // Variant 2: Split column with vertical top-down car on right
            val bgTop = lerp(teamSecondary, teamColor, 0.22f)
            val bgBottom = lerp(teamSecondary, Color.Black, 0.7f)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgBottom,
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = teamNameUpper,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "P$pos",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$pts PTS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$wins WINS",
                                color = P1TColors.TextSecondary,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = gapText,
                                color = readableAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            F1CarTopDownCanvas(
                                team = team,
                                vertical = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }
                }
            }
        }
        3 -> {
            // Variant 3: Two-tone team livery top / white bottom
            val topTextColor = P1TTheme.onColor(teamColor)
            val lightAccent = if (teamColor.luminance() > 0.35f) lerp(teamColor, Color.Black, 0.42f) else teamColor
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14141A),
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(teamColor)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$pos",
                                        color = topTextColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "WCC",
                                        color = topTextColor.copy(alpha = 0.78f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TeamLogoBadge(team = team, sizeDp = 20)
                            }
                            Text(
                                text = "$pts PTS",
                                color = topTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                            .background(Color(0xFFF6F6F8))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            F1CarSideProfileCanvas(
                                team = team,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = teamNameUpper,
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$wins WINS",
                                        color = Color(0xFF555560),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = gapText,
                                    color = lightAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        4 -> {
            // Variant 4: Pit-box bracket card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16171E),
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.shortCode,
                                color = readableAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "WCC",
                                color = P1TColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(96.dp)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                F1CarTopDownCanvas(
                                    team = team,
                                    vertical = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$pos",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "$pts POINTS",
                                    color = readableAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "$wins WINS",
                                    color = P1TColors.TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Text(
                            text = gapText,
                            color = P1TColors.TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        else -> {
            // Variant 5: Dark gradient with side profile car + P# badge
            val bgTop = lerp(teamSecondary, teamColor, 0.25f)
            val bgBottom = lerp(teamSecondary, Color.Black, 0.72f)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgBottom,
                border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = teamNameUpper,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TeamLogoBadge(team = team, sizeDp = 20)
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "P$pos",
                                color = readableAccent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$pts PTS",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        F1CarSideProfileCanvas(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        )

                        Text(
                            text = "$wins WINS  ·  $gapText",
                            color = P1TColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live Session Lock-Screen / Banner Widget
 */
@Composable
private fun LiveSessionBannerWidgetShowcase(
    liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState(),
    userPreferences: UserPreferencesState = UserPreferencesState()
) {
    val trackStatusUpper = liveSession.trackStatusText.trim().uppercase()
    val isYellowStatus = trackStatusUpper.contains("YELLOW") || trackStatusUpper.contains("SC") || trackStatusUpper.contains("SAFETY")
    val isRedStatus = trackStatusUpper.contains("RED")
    val statusColor = when {
        isRedStatus -> P1TColors.AccentRed
        isYellowStatus -> P1TColors.SectorYellow
        else -> Color(0xFF2CE580)
    }
    val statusBg = when {
        isRedStatus -> Color(0xFF2F1215)
        isYellowStatus -> Color(0xFF2D2510)
        else -> Color(0xFF142E1F)
    }
    val circuitLabel = liveSession.circuitId.replace("_", " ").trim().uppercase()
    val headerTitle = if (circuitLabel.isNotBlank() && !liveSession.grandPrixName.uppercase().contains(circuitLabel)) {
        "${liveSession.grandPrixName.uppercase()} · $circuitLabel"
    } else {
        liveSession.grandPrixName.uppercase()
    }
    val timeRaw = liveSession.timeRemaining.trim()
    val timeLabel = when {
        timeRaw.isBlank() -> ""
        timeRaw.equals("Final", ignoreCase = true) ||
            timeRaw.equals("Finished", ignoreCase = true) ||
            timeRaw.equals("Starts soon", ignoreCase = true) -> timeRaw.uppercase()
        timeRaw.contains("left", ignoreCase = true) -> timeRaw.uppercase()
        else -> "${timeRaw.uppercase()} LEFT"
    }
    val sessionProgress = computeLiveSessionProgress(liveSession)
    val topTimings = liveSession.drivers.take(5)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LIVE SESSION LOCK-SCREEN / BANNER WIDGET",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF13131A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282838)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (liveSession.isLive) Color(0xFFE1251B) else Color(0xFF2A2A36)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (liveSession.isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = liveSession.shortSessionCode.ifBlank {
                                        if (liveSession.isLive) "LIVE" else "SESSION"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = headerTitle,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (timeLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeLabel,
                            color = P1TColors.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status pill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trackStatusUpper.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = trackStatusUpper,
                                    color = statusColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF85858B),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = "${liveSession.sessionName.uppercase()} · ${liveSession.carsOnTrack} CARS",
                            color = P1TColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5-Row Live Timing List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topTimings.forEach { driver ->
                        val isHighlighted = driver.code.equals(userPreferences.favoriteDriverCode, ignoreCase = true) ||
                            driver.team == userPreferences.favoriteTeam
                        val accentColor = Color(driver.team.colorHex)
                        val readableAccent = darkSurfaceReadableColor(accentColor)
                        val displayTime = if (driver.position == 1) driver.bestLapTime else driver.gapToLeader
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isHighlighted) accentColor.copy(alpha = 0.14f) else Color(0xFF1A1A24),
                            border = if (isHighlighted) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = driver.position.toString(),
                                    color = if (isHighlighted) readableAccent else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.widthIn(min = 20.dp)
                                )
                                TeamLogoBadge(team = driver.team, sizeDp = 16)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = driver.code,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = displayTime,
                                    color = if (driver.isOverallBest || driver.position == 1) P1TColors.SectorPurple else P1TColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom session progress line (computed dynamically)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF262634))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(sessionProgress)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFFE1251B))
                    )
                }
            }
        }
    }
}

/**
 * Constructors 11-Team Horizontal Car Bar-Chart (4×4) & Split Leader Widget (4×2)
 */
@Composable
private fun ConstructorsBarChartWidgetShowcase(
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    schedule: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026)
) {
    val rawStandings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val standings = rawStandings.sortedBy { it.position }
    val teamsList = standings.map { Triple(it.position, it.team, it.points.toInt()) }
    val maxPts = teamsList.maxOfOrNull { it.third }?.takeIf { it > 0 }?.toFloat() ?: 1f

    val totalRounds = schedule.size.coerceAtLeast(1)
    val completedRounds = countCompletedRounds(schedule)

    val leader = standings.firstOrNull() ?: SeedDataProvider.getConstructorStandings2026().first()
    val leaderWins = leader.wins
    val leaderPts = leader.points.toInt()
    val leaderPos = leader.position
    val leaderTeam = leader.team
    val leaderAccent = Color(leaderTeam.colorHex)
    val leaderHighlight = if (leaderAccent.luminance() < 0.22f) {
        lerp(leaderAccent, Color.White, 0.45f)
    } else {
        leaderAccent
    }
    val leaderBg = lerp(Color(leaderTeam.secondaryHex), Color.Black, 0.35f)
    val leaderDriversText = leader.drivers
        .takeIf { it.isNotEmpty() }
        ?.take(2)
        ?.joinToString(" · ") { "${it.fullName.substringAfterLast(" ").uppercase()} ${it.points.toInt()}" }
        ?: driverStandings
            .filter { it.team == leaderTeam }
            .sortedBy { it.position }
            .take(2)
            .joinToString(" · ") { "${it.code} ${it.points.toInt()}" }

    val contenders = standings.drop(1).take(3).map { standing ->
        val gap = (leaderPts - standing.points.toInt()).coerceAtLeast(0)
        Triple(standing.position.toString(), standing.team, "${standing.points.toInt()}  -$gap")
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "CONSTRUCTORS ${teamsList.size}-TEAM HORIZONTAL BAR-CHART (4×4)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // 4x4 11-Team Bar Chart Widget
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF13131A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282838)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSTRUCTOR STANDINGS",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "ROUND $completedRounds / $totalRounds",
                        color = P1TColors.TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teamsList.forEach { (pos, team, pts) ->
                        val ratio = (pts / maxPts).coerceIn(0.04f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$pos",
                                color = P1TColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 20.dp)
                            )
                            TeamLogoBadge(team = team, sizeDp = 14)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = team.displayName,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(0.36f)
                                    .padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Horizontal track line with car offset
                            Box(
                                modifier = Modifier
                                    .weight(0.64f)
                                    .height(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFF282836))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(2.dp)
                                        .background(Color(team.colorHex).copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier.fillMaxWidth(ratio),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    F1CarTopDownCanvas(
                                        team = team,
                                        vertical = false,
                                        modifier = Modifier.size(20.dp, 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$pts",
                                color = darkSurfaceReadableColor(Color(team.colorHex)),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.widthIn(min = 38.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "SPLIT LEADER CONSTRUCTORS WIDGET (4×2)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Split 4x2 Widget
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14141B),
            border = androidx.compose.foundation.BorderStroke(1.dp, leaderAccent.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Left white side
                Box(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F5F7))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONSTRUCTORS R$completedRounds",
                            color = Color(0xFF555566),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        contenders.forEach { (p, t, info) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = p,
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = t.displayName.uppercase(),
                                        color = Color.Black,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                F1CarSideProfileCanvas(
                                    team = t,
                                    modifier = Modifier.size(34.dp, 12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = info,
                                    color = Color(0xFF444455),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Right dynamic P1 leader side
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .background(leaderBg)
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${leaderWins} WINS",
                            color = leaderHighlight,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$leaderPos",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 36.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = leaderTeam.displayName.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (leaderDriversText.isNotBlank()) {
                            Text(
                                text = leaderDriversText,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${leaderPts} PTS",
                            color = leaderHighlight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Favorite Team Form Matrix Widget (4×4) & Teammate Split Widget (4×2)
 */
@Composable
private fun TeamFormAndTeammatesWidgetShowcase(
    userPreferences: UserPreferencesState = UserPreferencesState(),
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    lastCompletedRace: RaceWeekend? = null,
    schedule: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026)
) {
    val standings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val sortedStandings = standings.sortedBy { it.position }
    val driversList = if (driverStandings.isNotEmpty()) driverStandings else SeedDataProvider.getDriverStandings2026()

    val favTeam = userPreferences.favoriteTeam
    val teamColor = Color(favTeam.colorHex)
    val teamSecondary = Color(favTeam.secondaryHex)
    val headerFg = P1TTheme.onColor(teamColor)
    val bodyAccent = if (teamColor.luminance() > 0.16f) lerp(teamColor, Color.Black, 0.45f) else teamColor
    val darkAccent = if (teamColor.luminance() < 0.22f) lerp(teamColor, Color.White, 0.45f) else teamColor

    val teamStanding = sortedStandings.firstOrNull { it.team == favTeam }

    val completedRounds = countCompletedRounds(schedule)
    val effectiveLastCompletedRace = lastCompletedRace ?: schedule.lastOrNull { it.results.isNotEmpty() }

    val teamPts = teamStanding?.points?.toInt() ?: 0
    val teamIndex = sortedStandings.indexOfFirst { it.team == favTeam }.takeIf { it >= 0 } ?: 0
    val teamPos = teamStanding?.position ?: (teamIndex + 1)
    val teamAhead = if (teamIndex > 0) sortedStandings.getOrNull(teamIndex - 1) else null
    val teamBehind = sortedStandings.getOrNull(teamIndex + 1)
    val gapAheadText = if (teamAhead != null) {
        "-${(teamAhead.points.toInt() - teamPts).coerceAtLeast(0)} TO ${teamAhead.team.displayName.uppercase()}"
    } else {
        "WCC LEADER"
    }
    val gapBehindText = if (teamBehind != null) {
        "+${(teamPts - teamBehind.points.toInt()).coerceAtLeast(0)} OVER ${teamBehind.team.displayName.uppercase()}"
    } else {
        "P$teamPos"
    }

    val teamDrivers = driversList.filter { it.team == favTeam }.sortedBy { it.position }.take(2)
    val d1 = teamDrivers.getOrNull(0)
    val d2 = teamDrivers.getOrNull(1)
    val standingD1 = teamStanding?.drivers?.getOrNull(0)
    val standingD2 = teamStanding?.drivers?.getOrNull(1)
    val d1Code = d1?.code ?: standingD1?.fullName?.substringAfterLast(" ")?.take(3)?.uppercase() ?: "D1"
    val d2Code = d2?.code ?: standingD2?.fullName?.substringAfterLast(" ")?.take(3)?.uppercase() ?: "D2"
    val d1Label = when {
        d1 != null -> "${d1.lastName.uppercase()} (P${d1.position} · ${d1.points.toInt()})"
        standingD1 != null -> "${standingD1.fullName.substringAfterLast(" ").uppercase()} (${standingD1.points.toInt()})"
        else -> "DRIVER 1"
    }
    val d2Label = when {
        d2 != null -> "${d2.lastName.uppercase()} (P${d2.position} · ${d2.points.toInt()})"
        standingD2 != null -> "${standingD2.fullName.substringAfterLast(" ").uppercase()} (${standingD2.points.toInt()})"
        else -> "DRIVER 2"
    }

    val raceCols = d1?.recentRaces?.takeLast(7)?.takeIf { it.isNotEmpty() }
        ?: d2?.recentRaces?.takeLast(7)?.takeIf { it.isNotEmpty() }
        ?: teamStanding?.recentTeamForm?.takeLast(7)?.map {
            RaceRecentResult(it.grandPrixName, it.positionText, it.pointsText, it.position)
        }?.takeIf { it.isNotEmpty() }
        ?: emptyList()
    val races = if (raceCols.isNotEmpty()) {
        raceCols.map { it.grandPrixName.replace(" Grand Prix", "").take(3).uppercase() }
    } else {
        listOf("R$completedRounds")
    }
    val colCount = races.size
    fun alignDriverResults(driver: DriverStanding?): List<String> {
        val recent = driver?.recentRaces?.takeLast(7).orEmpty()
        if (raceCols.isNotEmpty() && recent.isNotEmpty()) {
            val byGp = recent.associateBy { it.grandPrixName.trim().lowercase() }
            val aligned = raceCols.map { col ->
                byGp[col.grandPrixName.trim().lowercase()]
                    ?.positionText
                    ?.removePrefix("P")
                    ?.ifBlank { "—" }
                    ?: "—"
            }
            if (aligned.any { it != "—" }) {
                return aligned
            }
        }
        val raw = recent.takeLast(colCount).map {
            it.positionText.removePrefix("P").ifBlank { "—" }
        }
        return if (raw.size >= colCount) {
            raw.takeLast(colCount)
        } else {
            List(colCount - raw.size) { "—" } + raw
        }
    }
    val d1Results = alignDriverResults(d1)
    val d2Results = alignDriverResults(d2)

    val latestVenuePtsText = effectiveLastCompletedRace?.results
        ?.filter { it.team == favTeam }
        ?.takeIf { it.isNotEmpty() }
        ?.sumOf { it.points }
        ?.let { pts -> if (pts % 1.0 == 0.0) pts.toInt().toString() else pts.toString() }
        ?: teamStanding?.recentTeamForm?.lastOrNull()?.pointsText
            ?.filter { it.isDigit() || it == '.' }
            ?.ifBlank { null }
        ?: run {
            val d1Last = d1?.recentRaces?.lastOrNull()
            val d2Last = d2?.recentRaces?.lastOrNull()
            val referenceGp = d1Last?.grandPrixName ?: d2Last?.grandPrixName
            val combined = listOfNotNull(d1Last, d2Last)
                .filter { referenceGp == null || it.grandPrixName.equals(referenceGp, ignoreCase = true) }
                .sumOf { it.pointsText.filter { c -> c.isDigit() || c == '.' }.toDoubleOrNull() ?: 0.0 }
            if (combined % 1.0 == 0.0) combined.toInt().toString() else combined.toString()
        }
    val latestVenueName = (
        effectiveLastCompletedRace?.grandPrixName
            ?: d1?.recentRaces?.lastOrNull()?.grandPrixName
            ?: d2?.recentRaces?.lastOrNull()?.grandPrixName
            ?: teamStanding?.recentTeamForm?.lastOrNull()?.grandPrixName
            ?: "LAST GP"
        )
        .replace(" Grand Prix", "")
        .uppercase()

    val d1Points = d1?.points?.toInt()
        ?: standingD1?.points?.toInt()
        ?: 0
    val d2Points = d2?.points?.toInt()
        ?: standingD2?.points?.toInt()
        ?: 0
    val gapToPrevText = if (teamAhead != null) {
        "-${(teamAhead.points.toInt() - teamPts).coerceAtLeast(0)} TO P${teamAhead.position}"
    } else {
        "WCC LEADER"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${favTeam.displayName.uppercase()} 'LAST ${races.size}' TEAM FORM MATRIX WIDGET (4×4)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Favorite Team Form Matrix 4x4
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Team Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(teamColor)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogoBadge(team = favTeam, sizeDp = 20)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = favTeam.displayName.uppercase(),
                                    color = headerFg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "WCC · AFTER ROUND $completedRounds",
                                color = headerFg.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Driver badges with real portraits
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DriverPortraitAvatar(
                                driverCodeOrName = d1Code,
                                teamColor = teamColor,
                                size = 28.dp,
                                positionBadge = "P${d1?.position ?: teamPos}"
                            )
                            DriverPortraitAvatar(
                                driverCodeOrName = d2Code,
                                teamColor = teamColor,
                                size = 28.dp,
                                positionBadge = "P${d2?.position ?: (teamPos + 1)}"
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Huge Position & Points
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "$teamPos", color = headerFg, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                            Text(text = "$teamPts PTS", color = headerFg, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // White Body with Team Car & Last N Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFC))
                        .padding(12.dp)
                ) {
                    F1CarSideProfileCanvas(
                        team = favTeam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "LAST ${races.size} RACES FORM",
                        color = Color(0xFF666677),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Table with horizontal scroll for small screens (fixed column widths for clean vertical alignment)
                    val driverColWidth = 132.dp
                    val raceColWidth = 34.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Table headers
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DRIVER",
                                color = Color(0xFF888899),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(driverColWidth)
                            )
                            races.forEach { r ->
                                Text(
                                    text = r,
                                    color = Color(0xFF888899),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(raceColWidth),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4E4EC), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Driver 1 row
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = d1Label,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(driverColWidth).padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            d1Results.forEach { res ->
                                val isPodium = res == "3" || res == "2" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(raceColWidth)
                                ) {
                                    Text(text = res, color = if (res == "DNF") bodyAccent else Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(bodyAccent))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Driver 2 row
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = d2Label,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(driverColWidth).padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            d2Results.forEach { res ->
                                val isPodium = res == "2" || res == "3" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(raceColWidth)
                                ) {
                                    Text(text = res, color = if (res == "DNF") bodyAccent else Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(bodyAccent))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4E4EC), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        // 3-Column Footer
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "+$latestVenuePtsText IN $latestVenueName", color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = gapAheadText, color = bodyAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = gapBehindText, color = Color(0xFF1565C0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text(
            text = "${favTeam.displayName.uppercase()} TEAMMATE SPLIT WIDGET (4×2)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Favorite Team 4x2 Teammate Split Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = lerp(teamSecondary, Color.Black, 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                lerp(teamSecondary, teamColor, 0.25f),
                                lerp(teamSecondary, Color.Black, 0.5f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Logo and car
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        TeamLogoBadge(team = favTeam, sizeDp = 22)
                        F1CarSideProfileCanvas(
                            team = favTeam,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                        Text(
                            text = "${favTeam.shortCode} SPLIT",
                            color = darkAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Info and Teammate Split Bar
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = favTeam.displayName.uppercase(),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = gapToPrevText,
                                    color = darkAccent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "P$teamPos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
                                Text(text = "$teamPts PTS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Teammate Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "$d1Points $d1Code", color = darkAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "$d2Points $d2Code", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            val totalTeammatePts = d1Points + d2Points
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                if (totalTeammatePts > 0) {
                                    if (d1Points > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(d1Points.toFloat())
                                                .fillMaxHeight()
                                                .background(darkAccent)
                                        )
                                    }
                                    if (d2Points > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(d2Points.toFloat())
                                                .fillMaxHeight()
                                                .background(Color.White)
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
}

/**
 * Renders an exact in-app preview of any of the 8 widget styles
 */
@Composable
private fun WidgetLivePreviewCard(
    widgetType: WidgetStyleType,
    preferences: UserPreferencesState,
    driverStandings: List<DriverStanding> = emptyList(),
    constructorStandings: List<ConstructorStanding> = emptyList(),
    liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState(),
    nextWeekend: RaceWeekend = SeedDataProvider.getNextRaceWeekend(),
    lastCompletedRace: RaceWeekend? = null,
    schedule: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026)
) {
    val activeDrivers = resolveCurrentDriverStandings(driverStandings, preferences.favoriteDriverCode)
    val activeConstructors = resolveCurrentConstructorStandings(constructorStandings, preferences.favoriteTeam)

    val favDriver = activeDrivers.firstOrNull {
        it.code.equals(preferences.favoriteDriverCode, ignoreCase = true) ||
        it.fullName.equals(preferences.favoriteDriverCode, ignoreCase = true)
    } ?: activeDrivers.firstOrNull()

    val teamStanding = activeConstructors.firstOrNull {
        it.team == preferences.favoriteTeam ||
        it.team.displayName.equals(preferences.favoriteTeam.displayName, ignoreCase = true) ||
        it.team.name.equals(preferences.favoriteTeam.name, ignoreCase = true)
    } ?: activeConstructors.firstOrNull()

    val resolvedLastCompletedRace = lastCompletedRace ?: schedule.lastOrNull { it.results.isNotEmpty() }
    val podiumResults = resolvedLastCompletedRace?.results?.sortedBy { it.position }?.take(3).orEmpty()
    val previewAccent = if (preferences.useTeamColorAccent) Color(preferences.favoriteTeam.colorHex) else P1TColors.AccentRed
    val previewAccentReadable = darkSurfaceReadableColor(previewAccent)
    val previewSecondary = if (preferences.useTeamColorAccent) Color(preferences.favoriteTeam.secondaryHex) else P1TColors.HeaderCrimsonGlow

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Widget Header: Type and Size Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = widgetType.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = widgetType.subtitle,
                        color = P1TTheme.textMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = P1TTheme.pillBg
                ) {
                    Text(
                        text = widgetType.sizeLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Widget Content Box (intentional dark gradient preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                previewSecondary,
                                Color(0xFF101016)
                            )
                        )
                    )
                    .border(1.dp, previewAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                when (widgetType) {
                    WidgetStyleType.NEXT_GP_COUNTDOWN -> {
                        val countdownText = when {
                            nextWeekend.daysToGo > 1 -> "${nextWeekend.daysToGo} DAYS TO GO"
                            nextWeekend.daysToGo == 1 -> "1 DAY TO GO"
                            nextWeekend.daysToGo == 0 -> "RACE WEEKEND"
                            else -> "COMPLETED"
                        }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nextWeekend.grandPrixName.uppercase(),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Round ${nextWeekend.round}",
                                    color = P1TColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = countdownText,
                                color = previewAccentReadable,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${nextWeekend.circuitName} · ${nextWeekend.circuitLengthKm} km",
                                color = P1TColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WidgetStyleType.WEEKEND_SESSIONS -> {
                        val keySessions = nextWeekend.sessions.let { list ->
                            val filtered = list.filter {
                                it.isRace || it.shortName.contains("Quali", ignoreCase = true) || it.shortName.equals("FP1", ignoreCase = true)
                            }
                            if (filtered.isNotEmpty()) filtered.take(3) else list.take(3)
                        }
                        Column {
                            Text(
                                text = "${nextWeekend.grandPrixName.uppercase()} SCHEDULE",
                                color = P1TColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            keySessions.forEach { session ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${session.shortName} (${session.dayOfWeek})",
                                        color = if (session.isRace) previewAccentReadable else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.localTime,
                                        color = if (session.isRace) previewAccentReadable else P1TColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    WidgetStyleType.WDC_STANDINGS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeDrivers.take(4).forEach { d ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${d.position}",
                                        color = P1TColors.TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.widthIn(min = 22.dp)
                                    )
                                    TeamLogoBadge(team = d.team, sizeDp = 16)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = d.fullName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${d.points.toInt()} pts",
                                        color = P1TColors.TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    WidgetStyleType.WCC_STANDINGS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeConstructors.take(4).forEach { teamItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${teamItem.position}",
                                        color = P1TColors.TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.widthIn(min = 22.dp)
                                    )
                                    TeamLogoBadge(team = teamItem.team, sizeDp = 16)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = teamItem.team.displayName,
                                        color = darkSurfaceReadableColor(Color(teamItem.team.colorHex)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${teamItem.points.toInt()} pts",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    WidgetStyleType.FAVORITE_DRIVER -> {
                        val driverPos = favDriver?.position ?: 1
                        val driverPoints = favDriver?.points?.toInt() ?: 0
                        val driverWins = favDriver?.wins ?: 0
                        val driverPodiums = favDriver?.podiums ?: 0
                        val driverSummary = favDriver?.last3Summary?.takeIf { it.isNotBlank() } ?: "P1, P1, P1"
                        val driverTeam = favDriver?.team ?: preferences.favoriteTeam

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    favDriver?.let {
                                        DriverPortraitAvatar(
                                            driverCodeOrName = it.fullName.ifBlank { it.code },
                                            teamColor = Color(it.team.colorHex),
                                            size = 38.dp,
                                            positionBadge = "P${it.position}"
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(
                                            text = favDriver?.fullName ?: "Driver",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = driverTeam.displayName,
                                            color = P1TColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "P$driverPos",
                                    color = darkSurfaceReadableColor(Color(driverTeam.colorHex)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            favDriver?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                TeamCarShowcaseImage(
                                    team = driverTeam,
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$driverPoints pts · $driverWins wins · $driverPodiums podiums",
                                color = P1TColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Recent: $driverSummary",
                                color = P1TColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WidgetStyleType.FAVORITE_TEAM -> {
                        val displayTeam = teamStanding?.team ?: preferences.favoriteTeam
                        val pos = teamStanding?.position ?: 1
                        val pts = teamStanding?.points?.toInt() ?: 0
                        val wins = teamStanding?.wins ?: 0
                        val resolvedDrivers: List<ConstructorDriverPoints> =
                            teamStanding?.drivers?.takeIf { it.isNotEmpty() }?.take(2)
                                ?: driverStandings
                                    .filter { it.team == displayTeam }
                                    .take(2)
                                    .map { ConstructorDriverPoints(fullName = it.fullName, points = it.points) }
                                    .ifEmpty {
                                        SeedDataProvider.getDriverStandings2026()
                                            .filter { it.team == displayTeam }
                                            .take(2)
                                            .map { ConstructorDriverPoints(fullName = it.fullName, points = it.points) }
                                    }
                        val driversText = resolvedDrivers
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(" · ") {
                                "${it.fullName.substringAfterLast(" ")} ${it.points.toInt()}"
                            }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    TeamLogoBadge(team = displayTeam, sizeDp = 22)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayTeam.displayName,
                                        color = darkSurfaceReadableColor(Color(displayTeam.colorHex)),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WCC P$pos",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TeamCarShowcaseImage(
                                team = displayTeam,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Constructor Championship · $pts pts",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (driversText != null) "Season wins: $wins · $driversText" else "Season wins: $wins",
                                color = P1TColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WidgetStyleType.LAST_RACE_PODIUM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (resolvedLastCompletedRace != null) {
                                        "LAST RACE PODIUM · ${resolvedLastCompletedRace.grandPrixName.uppercase()}"
                                    } else {
                                        "LAST RACE PODIUM"
                                    },
                                    color = Color(0xFFFF8A80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (podiumResults.isNotEmpty()) "FINAL CLASSIFICATION" else "PENDING",
                                    color = P1TColors.TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (podiumResults.isEmpty()) {
                                Text(
                                    text = "Awaiting official race classification",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            } else {
                                podiumResults.forEach { res ->
                                    val posColor = when (res.position) {
                                        1 -> P1TColors.GoldTrophy
                                        2 -> P1TColors.SilverTrophy
                                        else -> P1TColors.BronzeTrophy
                                    }
                                    val teamColor = Color(res.team.colorHex)
                                    val stintSummary = res.stints
                                        .takeIf { it.isNotEmpty() }
                                        ?.joinToString(" ➔ ") { "${it.compound.code}(${it.lapsCount})" }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1A1A22),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "P${res.position}",
                                                color = posColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(16.dp)
                                                    .clip(RoundedCornerShape(1.5.dp))
                                                    .background(teamColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            DriverPortraitAvatar(
                                                driverCodeOrName = res.driverCode,
                                                teamColor = teamColor,
                                                size = 24.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${res.driverCode} · ${res.driverFullName}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (stintSummary != null) {
                                                        "${res.team.displayName} · $stintSummary"
                                                    } else {
                                                        res.team.displayName
                                                    },
                                                    color = P1TColors.TextSecondary,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = res.finishTimeOrGap,
                                                color = if (res.position == 1) P1TColors.SectorGreen else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WidgetStyleType.LIVE_TIMING_BANNER -> {
                        val trackUpper = liveSession.trackStatusText.trim().uppercase()
                        val badgeColor = when {
                            trackUpper.contains("RED") -> Color(0xFFFF8A80)
                            trackUpper.contains("YELLOW") || trackUpper.contains("SC") || trackUpper.contains("SAFETY") -> P1TColors.SectorYellow
                            else -> P1TColors.SectorGreen
                        }
                        val statusBadgeText = listOfNotNull(
                            liveSession.timeRemaining.trim().takeIf { it.isNotBlank() },
                            trackUpper.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        val sessionTag = liveSession.shortSessionCode.ifBlank {
                            if (liveSession.isLive) "LIVE" else "SESSION"
                        }
                        val headerTag = if (liveSession.isLive) "LIVE · $sessionTag" else "LAST · $sessionTag"
                        val leader = liveSession.drivers.firstOrNull()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerTag,
                                    color = if (liveSession.isLive) Color(0xFFFF8A80) else P1TColors.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (statusBadgeText.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1E1E26)
                                    ) {
                                        Text(
                                            text = statusBadgeText,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveSession.grandPrixName,
                                    color = P1TColors.TextSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (leader != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E1E26),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "P1",
                                            color = P1TColors.GoldTrophy,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        DriverPortraitAvatar(
                                            driverCodeOrName = leader.code,
                                            teamColor = Color(leader.team.colorHex),
                                            size = 24.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${leader.code} · ${leader.fullName}",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "BEST: ${leader.bestLapTime} · LAST: ${leader.lastLapTime}",
                                                color = P1TColors.TextSecondary,
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Awaiting live session timing",
                                    color = P1TColors.TextSecondary,
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
