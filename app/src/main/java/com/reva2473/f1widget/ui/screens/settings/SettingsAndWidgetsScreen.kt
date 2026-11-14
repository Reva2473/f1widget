package com.reva2473.f1widget.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var selectedGalleryTab by remember { mutableIntStateOf(0) }
    var selectedWidgetIndex by remember { mutableIntStateOf(0) }
    var showDriverPicker by remember { mutableStateOf(false) }

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

                when (selectedGalleryTab) {
                    0 -> WccSquareCardsShowcase(constructorStandings = constructorStandings)
                    1 -> LiveSessionBannerWidgetShowcase(
                        liveSession = liveSession,
                        userPreferences = userPreferences
                    )
                    2 -> ConstructorsBarChartWidgetShowcase(
                        constructorStandings = constructorStandings,
                        driverStandings = driverStandings,
                        schedule = schedule
                    )
                    3 -> TeamFormAndTeammatesWidgetShowcase(
                        userPreferences = userPreferences,
                        constructorStandings = constructorStandings,
                        driverStandings = driverStandings,
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
                                driverStandings = driverStandings,
                                constructorStandings = constructorStandings,
                                liveSession = liveSession,
                                nextWeekend = nextWeekend,
                                lastCompletedRace = lastCompletedRace,
                                schedule = schedule
                            )

                            // Pin Widget to Home Screen action button
                            Button(
                                onClick = {
                                    val appWidgetManager = AppWidgetManager.getInstance(context)
                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        val receiverClass = when (currentType) {
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
                                        val success = appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                        if (!success) {
                                            Toast.makeText(context, "Could not request widget pin", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Pinning widgets is not supported by your launcher", Toast.LENGTH_SHORT).show()
                                    }
                                },
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
                val favDriver = driverStandings.firstOrNull { it.code == userPreferences.favoriteDriverCode }
                    ?: driverStandings.firstOrNull()
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
                                driverCodeOrName = drv.code,
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
                        // Theme mode toggle: Dark / Light / System
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
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )

                            val primaryAccent = LocalF1Accent.current.primaryAccent
                            val onAccentColor = P1TTheme.onColor(primaryAccent)

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
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 12.dp))

                        // Pit Radio Audio Alerts switch
                        SettingsToggleRow(
                            title = "Pit Radio Alerts",
                            subtitle = "Notify when interesting team radio messages are flagged",
                            checked = userPreferences.pitRadioAlertsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(pitRadioAlertsEnabled = it))
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
                        items(driverStandings, key = { it.driverId.ifBlank { it.code } }) { drv ->
                            val isSelected = drv.code == userPreferences.favoriteDriverCode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) P1TTheme.pillSelectedBg else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.dp, Color(drv.team.colorHex))
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                        driverCodeOrName = drv.code,
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
 * 2×2 Team WCC Square Cards Grid (6-Card Showcase)
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
    val leaderNameUpper = leaderStanding?.team?.displayName?.uppercase() ?: "P1"

    val merc = sorted.firstOrNull { it.team == F1TeamId.MERCEDES } ?: sorted[0]
    val ferrari = sorted.firstOrNull { it.team == F1TeamId.FERRARI } ?: sorted.getOrNull(1) ?: sorted[0]
    val redBull = sorted.firstOrNull { it.team == F1TeamId.RED_BULL } ?: sorted.getOrNull(3) ?: sorted[0]
    val mclaren = sorted.firstOrNull { it.team == F1TeamId.MCLAREN } ?: sorted.getOrNull(2) ?: sorted[0]
    val rb = sorted.firstOrNull { it.team == F1TeamId.RACING_BULLS }
    val alpine = sorted.firstOrNull { it.team == F1TeamId.ALPINE }

    fun gapLabel(teamPts: Int, isLeader: Boolean): String {
        return if (isLeader && secondStanding != null) {
            val lead = (teamPts - secondStanding.points.toInt()).coerceAtLeast(0)
            "+$lead ON ${secondStanding.team.displayName.uppercase()}"
        } else {
            val behind = (leaderPts - teamPts).coerceAtLeast(0)
            "-$behind TO $leaderNameUpper"
        }
    }

    val mercPts = merc.points.toInt()
    val mercWins = merc.wins
    val mercPos = merc.position
    val mercGapLabel = gapLabel(mercPts, merc.team == leaderStanding?.team)

    val ferrariPts = ferrari.points.toInt()
    val ferrariPos = ferrari.position
    val ferrariGapLabel = gapLabel(ferrariPts, ferrari.team == leaderStanding?.team)

    val rbPts = redBull.points.toInt()
    val rbWins = redBull.wins
    val rbPos = redBull.position
    val rbGapLabel = gapLabel(rbPts, redBull.team == leaderStanding?.team)

    val mclPts = mclaren.points.toInt()
    val mclWins = mclaren.wins
    val mclPos = mclaren.position
    val mclGapLabel = gapLabel(mclPts, mclaren.team == leaderStanding?.team)

    val rbTeamPts = rb?.points?.toInt() ?: 0
    val rbTeamWins = rb?.wins ?: 0
    val rbTeamPos = rb?.position ?: 9
    val rbTeamGapLabel = gapLabel(rbTeamPts, rb != null && rb.team == leaderStanding?.team)

    val alpinePts = alpine?.points?.toInt() ?: 0
    val alpineWins = alpine?.wins ?: 0
    val alpinePos = alpine?.position ?: 6
    val alpineGapLabel = gapLabel(alpinePts, alpine != null && alpine.team == leaderStanding?.team)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "2×2 CONSTRUCTORS (WCC) SQUARES (6-CARD SHOWCASE)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Row 1: Card 1 (Mercedes P1) & Card 2 (Ferrari P2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1 (Mercedes P1)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF041212),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2BE).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0A2220), Color(0xFF030D0C))
                            )
                        )
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Mercedes star emblem & huge 1 + WCC
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            TeamLogoBadge(team = F1TeamId.MERCEDES, sizeDp = 22)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$mercPos",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 32.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "WCC",
                                    color = Color(0xFF00D2BE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Car side profile
                        F1CarSideProfileCanvas(
                            team = F1TeamId.MERCEDES,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        )

                        // Skewed white banner
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
                                        .background(Color(0xFF00D2BE))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$mercPts PTS  $mercWins WINS",
                                    color = Color.Black,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Footer
                        Text(
                            text = mercGapLabel,
                            color = Color(0xFF00D2BE),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card 2 (Ferrari P2)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF160205),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8002D).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF5A0813), Color(0xFF1B0205))
                            )
                        )
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Ferrari shield + FERRARI & bold P2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                TeamLogoBadge(team = F1TeamId.FERRARI, sizeDp = 20)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "FERRARI",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "P$ferrariPos",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "$ferrariPts PTS",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Car top-down
                        F1CarTopDownCanvas(
                            team = F1TeamId.FERRARI,
                            vertical = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )

                        // Footer
                        Text(
                            text = ferrariGapLabel,
                            color = Color(0xFFE8002D),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Row 2: Card 3 (Red Bull P3) & Card 4 (McLaren P4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3 (Red Bull P3)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF050B16),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(F1TeamId.RED_BULL.colorHex).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0E1F3E), Color(0xFF040812))
                            )
                        )
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
                                text = "RED BULL",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "P$rbPos",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$rbPts PTS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$rbWins WINS",
                                color = P1TColors.TextSecondary,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = rbGapLabel,
                                color = Color(F1TeamId.RED_BULL.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Right side: vertical Red Bull car
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            F1CarTopDownCanvas(
                                team = F1TeamId.RED_BULL,
                                vertical = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }
                }
            }

            // Card 4 (McLaren P4)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14141A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8000).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top half: Papaya Orange
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFFF8000))
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
                                        text = "$mclPos",
                                        color = Color.Black,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "WCC",
                                        color = Color.Black.copy(alpha = 0.75f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TeamLogoBadge(team = F1TeamId.MCLAREN, sizeDp = 20)
                            }
                            Text(
                                text = "$mclPts PTS",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Bottom half: White
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
                                team = F1TeamId.MCLAREN,
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
                                        text = "MCLAREN",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$mclWins WINS",
                                        color = Color(0xFF555560),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = mclGapLabel,
                                    color = Color(0xFFB34E00),
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

        // Row 3: Card 5 (VRB P5) & Card 6 (Alpine P6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 5 (VRB P5)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16171E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6692FF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
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
                                text = F1TeamId.RACING_BULLS.shortCode,
                                color = Color(0xFF6692FF),
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
                            // Pit-box bracket on left
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(96.dp)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                F1CarTopDownCanvas(
                                    team = F1TeamId.RACING_BULLS,
                                    vertical = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Info on right
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$rbTeamPos",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "$rbTeamPts POINTS",
                                    color = Color(0xFF6692FF),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "$rbTeamWins WINS",
                                    color = P1TColors.TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Text(
                            text = rbTeamGapLabel,
                            color = P1TColors.TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Card 6 (Alpine P6)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14050D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF87BC).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 188.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF320E22), Color(0xFF12030B))
                            )
                        )
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
                                text = "ALPINE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            TeamLogoBadge(team = F1TeamId.ALPINE, sizeDp = 20)
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "P$alpinePos",
                                color = Color(0xFFFF87BC),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$alpinePts PTS",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        F1CarSideProfileCanvas(
                            team = F1TeamId.ALPINE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        )

                        Text(
                            text = "$alpineWins WINS  ·  $alpineGapLabel",
                            color = P1TColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
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
    val trackStatusUpper = liveSession.trackStatusText.uppercase()
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
                            color = Color(0xFFE1251B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = liveSession.shortSessionCode.ifBlank { "LIVE" },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
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

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TIME LEFT ${liveSession.timeRemaining.uppercase()}",
                        color = P1TColors.TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status pill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                                    color = if (isHighlighted) accentColor else Color.White,
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

                // Bottom red progress line
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
                            .fillMaxWidth(0.65f)
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
    val completedRounds = schedule.count { it.results.isNotEmpty() || it.daysToGo < 0 }
        .takeIf { it > 0 }
        ?: (schedule.firstOrNull { it.daysToGo >= 0 }?.round?.minus(1)?.coerceAtLeast(1))
        ?: totalRounds

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
                                color = Color(team.colorHex),
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

    val completedRounds = schedule.count { it.results.isNotEmpty() || it.daysToGo < 0 }
        .takeIf { it > 0 }
        ?: (schedule.firstOrNull { it.daysToGo >= 0 }?.round?.minus(1)?.coerceAtLeast(1))
        ?: schedule.size.coerceAtLeast(1)
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

                    // Table with horizontal scroll for small screens
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Table headers
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "DRIVER", color = Color(0xFF888899), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 108.dp))
                            races.forEach { r ->
                                Text(text = r, color = Color(0xFF888899), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 30.dp), textAlign = TextAlign.Center)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4E4EC), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Driver 1 row
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = d1Label, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 108.dp, max = 140.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            d1Results.forEach { res ->
                                val isPodium = res == "3" || res == "2" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.widthIn(min = 30.dp)
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
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = d2Label, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 108.dp, max = 140.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            d2Results.forEach { res ->
                                val isPodium = res == "2" || res == "3" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.widthIn(min = 30.dp)
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(d1Points.toFloat().coerceAtLeast(1f))
                                        .fillMaxHeight()
                                        .background(darkAccent)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(d2Points.toFloat().coerceAtLeast(1f))
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
    val activeDrivers = if (driverStandings.isNotEmpty()) driverStandings else SeedDataProvider.getDriverStandings2026()
    val activeConstructors = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()

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
                                color = previewAccent,
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
                                        color = if (session.isRace) previewAccent else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (session.isRace) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.localTime,
                                        color = if (session.isRace) previewAccent else P1TColors.TextSecondary,
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
                                        color = Color(teamItem.team.colorHex),
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
                                            driverCodeOrName = it.code,
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
                                    color = Color(driverTeam.colorHex),
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
                                        color = Color(displayTeam.colorHex),
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
                                    color = P1TColors.AccentRed,
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
                        val trackUpper = liveSession.trackStatusText.uppercase()
                        val badgeColor = when {
                            trackUpper.contains("RED") -> P1TColors.AccentRed
                            trackUpper.contains("YELLOW") || trackUpper.contains("SC") || trackUpper.contains("SAFETY") -> P1TColors.SectorYellow
                            else -> P1TColors.SectorGreen
                        }
                        val leader = liveSession.drivers.firstOrNull()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE · ${liveSession.shortSessionCode.ifBlank { "LIVE" }}",
                                    color = P1TColors.AccentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E1E26)
                                ) {
                                    Text(
                                        text = "${liveSession.timeRemaining} · $trackUpper",
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
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
