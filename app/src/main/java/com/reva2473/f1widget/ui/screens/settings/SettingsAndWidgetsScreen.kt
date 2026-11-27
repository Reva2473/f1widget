package com.reva2473.f1widget.ui.screens.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
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
import com.reva2473.f1widget.ui.components.ApexMotion
import com.reva2473.f1widget.ui.components.BroadcastFlagBannerType
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.P1TRadialBackground
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.components.resolveBroadcastFlagType
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
    driverStandings: List<DriverStanding>
): List<DriverStanding> = driverStandings.ifEmpty { SeedDataProvider.getDriverStandings2026() }

private fun resolveCurrentConstructorStandings(
    constructorStandings: List<ConstructorStanding>
): List<ConstructorStanding> = constructorStandings.ifEmpty { SeedDataProvider.getConstructorStandings2026() }

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

private fun computeLiveSessionProgress(liveSession: LiveSessionState): Float {
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

    val resolvedDriverStandings = remember(driverStandings) {
        resolveCurrentDriverStandings(driverStandings)
    }
    val resolvedConstructorStandings = remember(constructorStandings) {
        resolveCurrentConstructorStandings(constructorStandings)
    }

    val galleryTabs = listOf(
        "2×2 WCC Cards",
        "Live Lock-Screen",
        "Constructors 4×4 & 4×2",
        "Team Form Matrix",
        "Custom Widget Configurator"
    )
    val widgetTypes = WidgetStyleType.entries

    P1TRadialBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SettingsTopHeader(userPreferences = userPreferences)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
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
                                    color = if (isSelected) primaryAccent else P1TTheme.surfaceStandingCard,
                                    border = if (isSelected) null else BorderStroke(1.dp, P1TTheme.borderStandingCard)
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

                    AnimatedContent(
                        targetState = selectedGalleryTab,
                        transitionSpec = {
                            ApexMotion.tabContentTransform(if (targetState >= initialState) 1 else -1)
                        },
                        label = "WidgetCategoryTransition"
                    ) { activeGalleryTab ->
                        when (activeGalleryTab) {
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
                                                    border = if (isSelected) BorderStroke(1.dp, P1TTheme.adaptiveTeamText(primaryAccent)) else null
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
                                    AnimatedContent(
                                        targetState = currentType,
                                        transitionSpec = { ApexMotion.crossfadeTransform() },
                                        label = "WidgetPreviewTransition"
                                    ) { animatedType ->
                                        WidgetLivePreviewCard(
                                            widgetType = animatedType,
                                            preferences = userPreferences,
                                            driverStandings = resolvedDriverStandings,
                                            constructorStandings = resolvedConstructorStandings,
                                            liveSession = liveSession,
                                            nextWeekend = nextWeekend,
                                            lastCompletedRace = lastCompletedRace,
                                            schedule = schedule
                                        )
                                    }

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
                    }

                    if (quickPinWidgetType != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { requestPinWidget(context, quickPinWidgetType) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
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
                    val activeTeam = userPreferences.favoriteTeam
                    val activeTeamColor = Color(activeTeam.colorHex)
                    val activeTeamAdaptive = P1TTheme.adaptiveTeamText(activeTeamColor)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = P1TTheme.surfaceStandingCard,
                        border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Team Accent",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = P1TTheme.accentPillBg(activeTeamColor),
                                    border = BorderStroke(1.dp, activeTeamColor.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = activeTeam.displayName.uppercase(),
                                        color = activeTeamAdaptive,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Dynamically tints buttons, header glows, charts and widget badges.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
                            )

                            // 11 Team Color Swatches Row with glowing active ring + team name caption
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(F1TeamId.entries.toTypedArray(), key = { it.name }) { team ->
                                    val isSelected = team == userPreferences.favoriteTeam
                                    val teamColor = Color(team.colorHex)
                                    val ringColor = P1TTheme.adaptiveTeamText(teamColor)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onUpdatePreferences(userPreferences.copy(favoriteTeam = team))
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.shadow(
                                                            elevation = 12.dp,
                                                            shape = CircleShape,
                                                            spotColor = teamColor,
                                                            ambientColor = teamColor
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) teamColor.copy(alpha = 0.22f) else Color.Transparent
                                                )
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) ringColor else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(teamColor)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.surface
                                                        } else {
                                                            P1TTheme.borderStandingCard
                                                        },
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected ${team.displayName}",
                                                        tint = P1TTheme.onColor(teamColor),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        Text(
                                            text = team.shortCode,
                                            color = if (isSelected) ringColor else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = team.displayName,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else P1TTheme.textMuted,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = P1TTheme.borderStandingCard,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

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
                        shape = RoundedCornerShape(14.dp),
                        color = P1TTheme.surfaceStandingCard,
                        border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                        shape = RoundedCornerShape(16.dp),
                        color = P1TTheme.surfaceStandingCard,
                        border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Theme mode toggle: Dark / Light / System (responsive on narrow screens)
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
                                        ThemeModePills(
                                            selectedMode = userPreferences.themeMode,
                                            onSelectMode = { mode ->
                                                onUpdatePreferences(userPreferences.copy(themeMode = mode))
                                            }
                                        )
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
                                        ThemeModePills(
                                            selectedMode = userPreferences.themeMode,
                                            onSelectMode = { mode ->
                                                onUpdatePreferences(userPreferences.copy(themeMode = mode))
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = P1TTheme.borderStandingCard,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Race Control & Pit Radio Alerts switch
                            SettingsToggleRow(
                                title = "Race Control & Pit Alerts",
                                subtitle = "Notify on flags, safety cars, and key pit radio calls",
                                checked = userPreferences.pitRadioAlertsEnabled,
                                onCheckedChange = {
                                    onUpdatePreferences(userPreferences.copy(pitRadioAlertsEnabled = it))
                                }
                            )

                            HorizontalDivider(
                                color = P1TTheme.borderStandingCard,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Fastest Lap & Track Record Alerts switch
                            SettingsToggleRow(
                                title = "Lap Record Alerts",
                                subtitle = "Notify when a new fastest lap or track record is set",
                                checked = userPreferences.lapRecordAlertsEnabled,
                                onCheckedChange = {
                                    onUpdatePreferences(userPreferences.copy(lapRecordAlertsEnabled = it))
                                }
                            )

                            HorizontalDivider(
                                color = P1TTheme.borderStandingCard,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Session Reminders switch
                            SettingsToggleRow(
                                title = "Session Reminders",
                                subtitle = "Notify 15 minutes before Practice, Quali & Grand Prix",
                                checked = userPreferences.sessionRemindersEnabled,
                                onCheckedChange = {
                                    onUpdatePreferences(userPreferences.copy(sessionRemindersEnabled = it))
                                }
                            )

                            HorizontalDivider(
                                color = P1TTheme.borderStandingCard,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

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
                        shape = RoundedCornerShape(14.dp),
                        color = P1TTheme.surfaceStandingCard,
                        border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                                        .padding(end = 8.dp)
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
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
    }

    if (showDriverPicker) {
        Dialog(onDismissRequest = { showDriverPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = P1TTheme.surfaceStandingCard,
                border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                                color = if (isSelected) P1TTheme.pillSelectedBg else P1TTheme.surfaceInnerCard,
                                border = if (isSelected) {
                                    BorderStroke(1.dp, Color(drv.team.colorHex))
                                } else {
                                    BorderStroke(1.dp, P1TTheme.borderStandingCard.copy(alpha = 0.6f))
                                },
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
private fun SettingsTopHeader(
    userPreferences: UserPreferencesState
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val adaptiveAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val team = userPreferences.favoriteTeam

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Widgets & Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${team.displayName} · ${userPreferences.favoriteDriverCode.uppercase()}",
                color = P1TTheme.textMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = P1TTheme.accentPillBg(primaryAccent),
            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeamLogoBadge(team = team, sizeDp = 16)
                Text(
                    text = team.shortCode,
                    color = adaptiveAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ThemeModePills(
    selectedMode: String,
    onSelectMode: (String) -> Unit
) {
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val onAccentColor = P1TTheme.onColor(primaryAccent)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
            val isSelected = selectedMode == mode
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectMode(mode) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) primaryAccent else P1TTheme.pillBg,
                    border = if (isSelected) null else BorderStroke(1.dp, P1TTheme.borderStandingCard)
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
                            .heightIn(min = 196.dp)
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

                        TeamCarShowcaseImage(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            contentScale = ContentScale.Fit
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

                        TeamCarShowcaseImage(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentScale = ContentScale.Fit
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
            // Variant 2: Split header with full-width side-profile car stage
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 28.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$pts PTS",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$wins WINS",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        TeamCarShowcaseImage(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentScale = ContentScale.Fit
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
                }
            }
        }
        3 -> {
            // Variant 3: Two-tone team livery top / adaptive inner surface bottom
            val topTextColor = P1TTheme.onColor(teamColor)
            val bodyGapColor = P1TTheme.adaptiveTeamText(teamColor)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.95f)
                            .background(teamColor)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
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
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 22.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "WCC",
                                        color = topTextColor.copy(alpha = 0.82f),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                TeamLogoBadge(team = team, sizeDp = 20)
                            }
                            Text(
                                text = "$pts PTS",
                                color = topTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                            .background(P1TTheme.surfaceInnerCard)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            TeamCarShowcaseImage(
                                team = team,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = teamNameUpper,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$wins WINS",
                                        color = P1TTheme.textMuted,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = gapText,
                                    color = bodyGapColor,
                                    fontSize = 9.5.sp,
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
            // Variant 4: Pit-box carbon garage card
            val adaptiveTeamColor = P1TTheme.adaptiveTeamText(teamColor)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, teamColor.copy(alpha = 0.45f)),
                modifier = modifier
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(11.dp)
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
                                color = adaptiveTeamColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "WCC",
                                color = P1TTheme.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$pos",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 28.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "$pts PTS",
                                    color = adaptiveTeamColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$wins WINS",
                                    color = P1TTheme.textMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF121218))
                                .border(
                                    1.dp,
                                    teamColor.copy(alpha = 0.45f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TeamCarShowcaseImage(
                                team = team,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Text(
                            text = gapText,
                            color = P1TTheme.textMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
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

                        TeamCarShowcaseImage(
                            team = team,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentScale = ContentScale.Fit
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

@Composable
private fun BroadcastStatusPill(
    statusText: String,
    modifier: Modifier = Modifier,
    trailingTime: String? = null,
    darkCanvas: Boolean = false
) {
    val bannerType = resolveBroadcastFlagType(statusText)
    val (bgColor, fgColor, icon) = when (bannerType) {
        BroadcastFlagBannerType.GREEN_FLAG -> Triple(P1TColors.GreenFlagBg, Color(0xFF07160F), Icons.Filled.Flag)
        BroadcastFlagBannerType.SAFETY_CAR -> Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Filled.DirectionsCar)
        BroadcastFlagBannerType.VSC -> Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Filled.Warning)
        BroadcastFlagBannerType.RED_FLAG -> Triple(P1TColors.RedFlagSolidBg, Color.White, Icons.Filled.NotInterested)
        BroadcastFlagBannerType.FASTEST_LAP -> Triple(P1TColors.FastestLapPurple, Color.White, Icons.Filled.Bolt)
        BroadcastFlagBannerType.NEUTRAL -> Triple(
            if (darkCanvas) Color(0xFF23232E) else P1TTheme.pillBg,
            if (darkCanvas) Color.White else MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Flag
        )
    }
    val label = listOfNotNull(
        statusText.trim().uppercase().takeIf { it.isNotBlank() },
        trailingTime?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    if (label.isBlank()) return

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (bannerType == BroadcastFlagBannerType.NEUTRAL) {
            BorderStroke(1.dp, if (darkCanvas) Color.White.copy(alpha = 0.14f) else P1TTheme.borderStandingCard)
        } else {
            null
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = fgColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
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
    var animateProgress by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateProgress = true }
    val animatedSessionProgress by animateFloatAsState(
        targetValue = if (animateProgress) sessionProgress else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LiveSessionProgress"
    )
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
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                            color = if (liveSession.isLive) P1TColors.AccentRed else P1TTheme.pillBg
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
                                    color = if (liveSession.isLive) Color.White else MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                            color = P1TTheme.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Broadcast-grade RaceControlStatusBanner-style status pill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trackStatusUpper.isNotBlank()) {
                        BroadcastStatusPill(
                            statusText = trackStatusUpper,
                            darkCanvas = false
                        )
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
                            tint = P1TTheme.textMuted,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = "${liveSession.sessionName.uppercase()} · ${liveSession.carsOnTrack} CARS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        val adaptiveAccent = P1TTheme.adaptiveTeamText(accentColor)
                        val displayTime = if (driver.position == 1) driver.bestLapTime else driver.gapToLeader
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isHighlighted) P1TTheme.accentPillBg(accentColor) else P1TTheme.surfaceInnerCard,
                            border = BorderStroke(
                                1.dp,
                                if (isHighlighted) accentColor else P1TTheme.borderStandingCard
                            ),
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
                                    color = if (isHighlighted) adaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.widthIn(min = 20.dp)
                                )
                                TeamLogoBadge(team = driver.team, sizeDp = 16)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = driver.code,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = displayTime,
                                    color = if (driver.isOverallBest || driver.position == 1) {
                                        P1TTheme.sectorPurple
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
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
                        .background(P1TTheme.borderStandingCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedSessionProgress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(P1TTheme.accentRed)
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
    val leaderHighlight = darkSurfaceReadableColor(leaderAccent)
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
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROUND $completedRounds / $totalRounds",
                        color = P1TTheme.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                var animateBars by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { animateBars = true }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teamsList.forEach { (pos, team, pts) ->
                        val ratio = (pts / maxPts).coerceIn(0.04f, 1f)
                        val animatedRatio by animateFloatAsState(
                            targetValue = if (animateBars) ratio else 0f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "ConstructorBarWidth_${team.name}"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$pos",
                                color = P1TTheme.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 20.dp)
                            )
                            TeamLogoBadge(team = team, sizeDp = 14)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = team.displayName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(0.36f)
                                    .padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Horizontal track line with car offset
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(0.64f)
                                    .height(22.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val carWidth = 56.dp
                                val carHeight = 20.dp
                                val availableTravel = (maxWidth - carWidth).coerceAtLeast(0.dp)
                                val carOffsetX = availableTravel * animatedRatio.coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(P1TTheme.dividerSubtle)
                                )
                                Box(
                                    modifier = Modifier
                                        .width((carOffsetX + carWidth * 0.5f).coerceAtMost(maxWidth))
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color(team.colorHex).copy(alpha = 0.6f))
                                )
                                TeamCarShowcaseImage(
                                    team = team,
                                    modifier = Modifier
                                        .offset(x = carOffsetX)
                                        .width(carWidth)
                                        .height(carHeight),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$pts",
                                color = P1TTheme.adaptiveTeamText(Color(team.colorHex)),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
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

        // Split 4x2 Widget (balanced column weights & compact car image to avoid narrow-screen truncation)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, leaderAccent.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 134.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Left contenders list on theme-aware inner surface
                Box(
                    modifier = Modifier
                        .weight(1.42f)
                        .fillMaxHeight()
                        .background(P1TTheme.surfaceInnerCard)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONSTRUCTORS R$completedRounds",
                            color = P1TTheme.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = t.displayName.uppercase(),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TeamCarShowcaseImage(
                                    team = t,
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(14.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = info,
                                    color = P1TTheme.textMuted,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Right dynamic P1 leader side (intentional dark team livery card)
                Box(
                    modifier = Modifier
                        .weight(0.88f)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$leaderPos",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 30.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = leaderTeam.displayName.uppercase(),
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
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
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, teamColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Team Header (2-row layout prevents team name squeezing and avatar clipping on narrow screens)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(teamColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            TeamLogoBadge(team = favTeam, sizeDp = 22)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = favTeam.displayName.uppercase(),
                                    color = headerFg,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "WCC · AFTER ROUND $completedRounds",
                                    color = headerFg.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "P$teamPos",
                                color = headerFg,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 24.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$teamPts PTS",
                                color = headerFg.copy(alpha = 0.92f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Balanced Teammate Avatars Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = headerFg.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DriverPortraitAvatar(
                                    driverCodeOrName = d1?.fullName?.ifBlank { d1Code } ?: d1Code,
                                    driverCode = d1Code,
                                    teamColor = teamColor,
                                    size = 32.dp,
                                    positionBadge = "P${d1?.position ?: teamPos}"
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = d1Code,
                                        color = headerFg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "$d1Points PTS",
                                        color = headerFg.copy(alpha = 0.82f),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = headerFg.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DriverPortraitAvatar(
                                    driverCodeOrName = d2?.fullName?.ifBlank { d2Code } ?: d2Code,
                                    driverCode = d2Code,
                                    teamColor = teamColor,
                                    size = 32.dp,
                                    positionBadge = "P${d2?.position ?: (teamPos + 1)}"
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = d2Code,
                                        color = headerFg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "$d2Points PTS",
                                        color = headerFg.copy(alpha = 0.82f),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Inner Surface Body with Team Car & Last N Table
                val bodyAdaptiveAccent = P1TTheme.adaptiveTeamText(teamColor)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(P1TTheme.surfaceInnerCard)
                        .padding(12.dp)
                ) {
                    TeamCarShowcaseImage(
                        team = favTeam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "LAST ${races.size} RACES FORM",
                        color = P1TTheme.textMuted,
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
                                color = P1TTheme.textMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(driverColWidth)
                            )
                            races.forEach { r ->
                                Text(
                                    text = r,
                                    color = P1TTheme.textMuted,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(raceColWidth),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        HorizontalDivider(
                            color = P1TTheme.borderStandingCard,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Driver 1 row
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = d1Label,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                    Text(
                                        text = res,
                                        color = if (res == "DNF") bodyAdaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(bodyAdaptiveAccent))
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                                    Text(
                                        text = res,
                                        color = if (res == "DNF") bodyAdaptiveAccent else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(bodyAdaptiveAccent))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = P1TTheme.borderStandingCard,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // 3-Column Footer
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "+$latestVenuePtsText IN $latestVenueName", color = P1TTheme.sectorGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = gapAheadText, color = bodyAdaptiveAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = gapBehindText, color = P1TTheme.inPitText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                        TeamCarShowcaseImage(
                            team = favTeam,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentScale = ContentScale.Fit
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
    val activeDrivers = resolveCurrentDriverStandings(driverStandings)
    val activeConstructors = resolveCurrentConstructorStandings(constructorStandings)

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
    val carbonTop = lerp(previewSecondary, Color(0xFF121218), 0.58f)
    val carbonBottom = Color(0xFF0B0B10)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = P1TTheme.surfaceStandingCard,
        border = BorderStroke(1.dp, P1TTheme.borderStandingCard),
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
                    color = P1TTheme.pillBg,
                    border = BorderStroke(1.dp, P1TTheme.borderStandingCard)
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

            // Widget Content Box (crisp dark carbon framing)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(carbonTop, carbonBottom)))
                    .border(1.dp, previewAccent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                when (widgetType) {
                    WidgetStyleType.NEXT_GP_COUNTDOWN -> {
                        val countdownValue = when {
                            nextWeekend.daysToGo > 1 -> "${nextWeekend.daysToGo}"
                            nextWeekend.daysToGo == 1 -> "1"
                            nextWeekend.daysToGo == 0 -> "LIVE"
                            else -> "FIN"
                        }
                        val countdownUnit = when {
                            nextWeekend.daysToGo > 1 -> "DAYS TO GO"
                            nextWeekend.daysToGo == 1 -> "DAY TO GO"
                            nextWeekend.daysToGo == 0 -> "RACE WEEKEND"
                            else -> "COMPLETED"
                        }
                        val statusBadgeLabel = when {
                            nextWeekend.daysToGo == 0 -> "TRACK CLEAR"
                            nextWeekend.isSprintWeekend -> "SPRINT WEEKEND"
                            else -> "NEXT ROUND"
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Dark carbon broadcast strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(previewAccentReadable)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${nextWeekend.countryFlagEmoji} ${nextWeekend.grandPrixName.uppercase()}",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "R${nextWeekend.round.toString().padStart(2, '0')}",
                                    color = P1TColors.TextMuted,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Hero countdown + status pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = countdownValue,
                                        color = previewAccentReadable,
                                        fontSize = 26.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 26.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = countdownUnit,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                                BroadcastStatusPill(
                                    statusText = statusBadgeLabel,
                                    darkCanvas = true
                                )
                            }

                            // Tabular circuit metrics bar
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF16161E),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = "CIRCUIT",
                                            color = P1TColors.TextMuted,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.7.sp
                                        )
                                        Text(
                                            text = nextWeekend.circuitName.ifBlank { nextWeekend.location },
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "LENGTH / LAPS",
                                            color = P1TColors.TextMuted,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.7.sp
                                        )
                                        Text(
                                            text = "${nextWeekend.circuitLengthKm}KM · ${nextWeekend.totalLaps}L",
                                            color = P1TColors.TextSecondary,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
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
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                                    .height(18.dp)
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
                                                    text = res.team.displayName,
                                                    color = P1TColors.TextSecondary,
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (stintSummary != null) {
                                                    Text(
                                                        text = stintSummary,
                                                        color = P1TColors.TextMuted,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = res.finishTimeOrGap,
                                                color = if (res.position == 1) P1TColors.SectorGreen else Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WidgetStyleType.LIVE_TIMING_BANNER -> {
                        val sessionTag = liveSession.shortSessionCode.ifBlank {
                            if (liveSession.isLive) "LIVE" else "SESSION"
                        }
                        val headerTag = if (liveSession.isLive) "LIVE · $sessionTag" else "LAST · $sessionTag"
                        val topDrivers = liveSession.drivers.take(3)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Dark carbon broadcast strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (liveSession.isLive) P1TColors.AccentRed else P1TColors.TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = headerTag,
                                    color = if (liveSession.isLive) Color(0xFFFF8A80) else Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveSession.grandPrixName.uppercase(),
                                    color = P1TColors.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Vivid RaceControlStatusBanner-style pill + session meta
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BroadcastStatusPill(
                                    statusText = liveSession.trackStatusText.ifBlank { "TRACK CLEAR" },
                                    trailingTime = liveSession.timeRemaining.takeIf { it.isNotBlank() },
                                    darkCanvas = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${liveSession.carsOnTrack} CARS",
                                    color = P1TColors.TextMuted,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (topDrivers.isNotEmpty()) {
                                // Tabular timing columns header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "POS · DRIVER",
                                        color = P1TColors.TextMuted,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "BEST",
                                        color = P1TColors.TextMuted,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GAP",
                                        color = P1TColors.TextMuted,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(54.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    topDrivers.forEach { drv ->
                                        val drvTeamColor = Color(drv.team.colorHex)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF16161E),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "P${drv.position}",
                                                    color = if (drv.position == 1) P1TColors.GoldTrophy else Color.White,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.width(24.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(16.dp)
                                                        .clip(RoundedCornerShape(1.5.dp))
                                                        .background(drvTeamColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                DriverPortraitAvatar(
                                                    driverCodeOrName = drv.fullName.ifBlank { drv.code },
                                                    driverCode = drv.code,
                                                    teamColor = drvTeamColor,
                                                    size = 22.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = drv.code,
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = drv.bestLapTime,
                                                    color = if (drv.isOverallBest || drv.position == 1) {
                                                        P1TColors.SectorPurple
                                                    } else {
                                                        Color.White
                                                    },
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.width(64.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (drv.position == 1) "LEADER" else drv.gapToLeader,
                                                    color = if (drv.position == 1) P1TColors.SectorGreen else P1TColors.TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.width(54.dp)
                                                )
                                            }
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
