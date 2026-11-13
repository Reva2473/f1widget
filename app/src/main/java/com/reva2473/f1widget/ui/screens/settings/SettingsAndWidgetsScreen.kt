package com.reva2473.f1widget.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
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
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.domain.model.WidgetStyleType
import com.reva2473.f1widget.ui.components.DriverPortraitAvatar
import com.reva2473.f1widget.ui.components.F1CarSideProfileCanvas
import com.reva2473.f1widget.ui.components.F1CarTopDownCanvas
import com.reva2473.f1widget.ui.components.TeamCarShowcaseImage
import com.reva2473.f1widget.ui.components.TeamLogoBadge
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors

/**
 * Screen 4: SettingsAndWidgetsScreen
 * Featuring:
 * 1. In-app Widgets Preview Gallery displaying all 7 Home/Lock screen widget styles
 *    personalized with user's selected Favorite Driver & Favorite Team.
 * 2. Team Colour Accent Selector (all 11 F1 constructors — tapping immediately updates F1TeamId theme accent).
 * 3. Theme Mode (Dark, Light, System), Pit Radio Alerts, Offline Caching, and 1950–2026 Archive Browser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndWidgetsScreen(
    userPreferences: UserPreferencesState,
    drivers: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    driverStandings: List<DriverStanding> = drivers,
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    onUpdatePreferences: (UserPreferencesState) -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier
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
        containerColor = P1TColors.BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Widgets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = P1TColors.BackgroundPrimary
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
                    color = P1TColors.TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Gallery Category Tabs
            item {
                val primaryAccent = LocalF1Accent.current.primaryAccent
                val onAccentColor = if (primaryAccent.luminance() > 0.45f) Color(0xFF0F0F10) else Color.White

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    galleryTabs.forEachIndexed { idx, tabTitle ->
                        val isSelected = idx == selectedGalleryTab
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) primaryAccent else P1TColors.SurfaceCard,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
                            modifier = Modifier.clickable { selectedGalleryTab = idx }
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) onAccentColor else P1TColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Active Gallery Content
            item {
                val primaryAccent = LocalF1Accent.current.primaryAccent
                val onAccentColor = if (primaryAccent.luminance() > 0.45f) Color(0xFF0F0F10) else Color.White
                val context = LocalContext.current

                when (selectedGalleryTab) {
                    0 -> WccSquareCardsShowcase(constructorStandings = constructorStandings)
                    1 -> LiveSessionBannerWidgetShowcase()
                    2 -> ConstructorsBarChartWidgetShowcase(constructorStandings = constructorStandings)
                    3 -> TeamFormAndTeammatesWidgetShowcase(constructorStandings = constructorStandings)
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sub-selector of the 7 standard customizable widget styles
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                widgetTypes.forEachIndexed { idx, type ->
                                    val isSelected = idx == selectedWidgetIndex
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFF2C2C3C) else Color(0xFF161620),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, primaryAccent) else null,
                                        modifier = Modifier.clickable { selectedWidgetIndex = idx }
                                    ) {
                                        Text(
                                            text = type.title,
                                            color = if (isSelected) Color.White else P1TColors.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            val currentType = widgetTypes[selectedWidgetIndex]
                            WidgetLivePreviewCard(
                                widgetType = currentType,
                                preferences = userPreferences,
                                driverStandings = driverStandings,
                                constructorStandings = constructorStandings
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
                                    .height(44.dp)
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
                    color = P1TColors.TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = P1TColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Select Team Accent",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dynamically tints buttons, header glows, charts and widget badges.",
                            color = P1TColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // 11 Team Color Badges Grid/Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(F1TeamId.entries.toTypedArray()) { team ->
                                val isSelected = team == userPreferences.favoriteTeam
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
                                            .background(Color(team.colorHex))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (team == F1TeamId.HAAS || team == F1TeamId.AUDI_SAUBER) Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = team.shortCode,
                                        color = if (isSelected) Color.White else P1TColors.TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Favourite Driver Personalization
            item {
                Text(
                    text = "FAVOURITE DRIVER",
                    color = P1TColors.TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                val favDriver = drivers.firstOrNull { it.code == userPreferences.favoriteDriverCode }
                    ?: drivers.firstOrNull()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = P1TColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${drv.team.displayName} · #${drv.position} in Championship",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change driver",
                            tint = P1TColors.TextMuted
                        )
                    }
                }
            }

            // Section 4: Display & Appearance
            item {
                Text(
                    text = "APPEARANCE & NOTIFICATIONS",
                    color = P1TColors.TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = P1TColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            val primaryAccent = LocalF1Accent.current.primaryAccent
                            val onAccentColor = if (primaryAccent.luminance() > 0.45f) Color(0xFF0F0F10) else Color.White

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                                    val isSelected = userPreferences.themeMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) primaryAccent else P1TColors.SurfaceElevated,
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences(userPreferences.copy(themeMode = mode))
                                        }
                                    ) {
                                        Text(
                                            text = mode.lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) onAccentColor else P1TColors.TextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Pit Radio Audio Alerts switch
                        SettingsToggleRow(
                            title = "Pit Radio Alerts",
                            subtitle = "Notify when interesting team radio messages are flagged",
                            checked = userPreferences.pitRadioAlertsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(pitRadioAlertsEnabled = it))
                            }
                        )

                        HorizontalDivider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Session Reminders switch
                        SettingsToggleRow(
                            title = "Session Reminders",
                            subtitle = "Notify 15 minutes before Practice, Quali & Grand Prix",
                            checked = userPreferences.sessionRemindersEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(sessionRemindersEnabled = it))
                            }
                        )

                        HorizontalDivider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

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
                    color = P1TColors.SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFF85858B),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "F1 Historical Archive (1950–2026)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "77 seasons of champion drivers, constructors & race results",
                                    color = P1TColors.TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open archive",
                            tint = P1TColors.AccentRed
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showDriverPicker) {
        Dialog(onDismissRequest = { showDriverPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = P1TColors.SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
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
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDriverPicker = false }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(drivers) { drv ->
                            val isSelected = drv.code == userPreferences.favoriteDriverCode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF282834) else P1TColors.SurfaceElevated,
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
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = drv.fullName,
                                        color = P1TColors.TextSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = drv.team.shortCode,
                                        color = P1TColors.TextMuted,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = P1TColors.TextSecondary,
                fontSize = 11.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LocalF1Accent.current.primaryAccent,
                uncheckedThumbColor = Color(0xFF888899),
                uncheckedTrackColor = Color(0xFF252530)
            )
        )
    }
}

/**
 * 2×2 Team WCC Square Cards Grid (6-Card Showcase from Screenshots 1 & 2)
 */
@Composable
private fun WccSquareCardsShowcase(
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026()
) {
    val standings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val merc = standings.firstOrNull { it.team == F1TeamId.MERCEDES } ?: standings[0]
    val ferrari = standings.firstOrNull { it.team == F1TeamId.FERRARI } ?: standings.getOrNull(1) ?: standings[0]
    val redBull = standings.firstOrNull { it.team == F1TeamId.RED_BULL } ?: standings.getOrNull(3) ?: standings[0]
    val mclaren = standings.firstOrNull { it.team == F1TeamId.MCLAREN } ?: standings.getOrNull(2) ?: standings[0]
    val rb = standings.firstOrNull { it.team == F1TeamId.RACING_BULLS }
    val alpine = standings.firstOrNull { it.team == F1TeamId.ALPINE }

    val mercPts = merc.points.toInt()
    val mercWins = merc.wins
    val mercPos = merc.position

    val ferrariPts = ferrari.points.toInt()
    val ferrariPos = ferrari.position
    val ferrariGap = mercPts - ferrariPts

    val rbPts = redBull.points.toInt()
    val rbWins = redBull.wins
    val rbPos = redBull.position
    val rbGap = mercPts - rbPts

    val mclPts = mclaren.points.toInt()
    val mclWins = mclaren.wins
    val mclPos = mclaren.position

    val rbTeamPts = rb?.points?.toInt() ?: 22
    val rbTeamWins = rb?.wins ?: 0
    val rbTeamPos = rb?.position ?: 9
    val rbTeamGap = mercPts - rbTeamPts

    val alpinePts = alpine?.points?.toInt() ?: 64
    val alpineWins = alpine?.wins ?: 0
    val alpinePos = alpine?.position ?: 6
    val alpineGap = mercPts - alpinePts

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "2×2 CONSTRUCTORS (WCC) SQUARES (6-CARD SHOWCASE)",
            color = P1TColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Row 1: Card 1 (Mercedes P1) & Card 2 (Ferrari P2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1 (Mercedes P1)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF041212),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2BE).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(188.dp)
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
                            text = "+$ferrariGap ON FERRARI",
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
                    .height(188.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogoBadge(team = F1TeamId.FERRARI, sizeDp = 20)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "FERRARI",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
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
                            text = "-$ferrariGap TO MERCEDES",
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3 (Red Bull P3)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF050B16),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3671C6).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(188.dp)
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
                                text = "-$rbGap TO P1",
                                color = Color(0xFFFF8000),
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
                    .height(188.dp)
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
                                    .height(36.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MCLAREN",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "$mclWins WINS",
                                    color = Color(0xFF555560),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Row 3: Card 5 (RB P5) & Card 6 (Alpine P6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 5 (RB P5)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16171E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6692FF).copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1f)
                    .height(188.dp)
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
                                text = "RB",
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

                            // Info on right
                            Column(
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
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$rbTeamWins WINS",
                                    color = P1TColors.TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Text(
                            text = "-$rbTeamGap TO P1",
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
                    .height(188.dp)
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
                            text = "$alpineWins WINS  ·  -$alpineGap TO P1",
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
 * Live Session Lock-Screen / Banner Widget (LIVE · BRAZILIAN GP · ROUND 20)
 */
@Composable
private fun LiveSessionBannerWidgetShowcase() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LIVE SESSION LOCK-SCREEN / BANNER WIDGET",
            color = P1TColors.TextSecondary,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BRAZILIAN GP · ROUND 20",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "TIME LEFT 43:51",
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
                        color = Color(0xFF142E1F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2CE580).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2CE580))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "TRACK CLEAR",
                                color = Color(0xFF2CE580),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF85858B),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = "ENDS 16:30",
                            color = P1TColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5-Row Live Timing List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val timings = listOf(
                        Triple("1", "VER", "1:12.959"),
                        Triple("2", "ANT", "+0.085"),
                        Triple("3", "HAM", "+0.339"),
                        Triple("4", "NOR", "+0.461"),
                        Triple("5", "RUS", "+0.658")
                    )

                    timings.forEach { (pos, drv, time) ->
                        val isHighlighted = drv == "ANT"
                        val team = when (drv) {
                            "VER" -> F1TeamId.RED_BULL
                            "ANT" -> F1TeamId.MERCEDES
                            "HAM" -> F1TeamId.FERRARI
                            "NOR" -> F1TeamId.MCLAREN
                            else -> F1TeamId.MERCEDES
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isHighlighted) Color(0xFF162E2E) else Color(0xFF1A1A24),
                            border = if (isHighlighted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2BE)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pos,
                                    color = if (isHighlighted) Color(0xFF00D2BE) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(18.dp)
                                )
                                TeamLogoBadge(team = team, sizeDp = 16)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = drv,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = time,
                                    color = if (pos == "1") P1TColors.SectorPurple else P1TColors.TextSecondary,
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
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026()
) {
    val standings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val teamsList = standings.map { Triple(it.position, it.team, it.points.toInt()) }
    val maxPts = teamsList.maxOfOrNull { it.third }?.takeIf { it > 0 }?.toFloat() ?: 538f

    val leader = standings.firstOrNull() ?: SeedDataProvider.getConstructorStandings2026().first()
    val leaderWins = leader.wins
    val leaderPts = leader.points.toInt()
    val leaderPos = leader.position
    val leaderTeam = leader.team

    val contenders = standings.drop(1).take(3).map { standing ->
        val gap = (leaderPts - standing.points.toInt())
        Triple(standing.position.toString(), standing.team, "${standing.points.toInt()}  -$gap")
    }.ifEmpty {
        listOf(
            Triple("2", F1TeamId.FERRARI, "378  -160"),
            Triple("3", F1TeamId.MCLAREN, "306  -232"),
            Triple("4", F1TeamId.RED_BULL, "249  -289")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "CONSTRUCTORS 11-TEAM HORIZONTAL BAR-CHART (4×4)",
            color = P1TColors.TextSecondary,
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
                        text = "ROUND 19 / 24",
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
                                modifier = Modifier.width(16.dp)
                            )
                            TeamLogoBadge(team = team, sizeDp = 14)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = team.displayName,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(82.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Horizontal track line with car offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                modifier = Modifier.width(26.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "SPLIT LEADER CONSTRUCTORS WIDGET (4×2)",
            color = P1TColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Split 4x2 Widget
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14141B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
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
                            text = "CONSTRUCTORS R19",
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                F1CarSideProfileCanvas(
                                    team = t,
                                    modifier = Modifier.size(38.dp, 12.dp)
                                )
                                Text(
                                    text = info,
                                    color = Color(0xFF444455),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right dark-teal side
                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .background(Color(0xFF042421))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${leaderWins} WINS",
                            color = Color(0xFF00D2BE),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$leaderPos",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 38.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = leaderTeam.displayName.uppercase(),
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${leaderPts} PTS",
                            color = Color(0xFF00D2BE),
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
 * Ferrari "LAST 7" Team Form Matrix Widget (4×4) & McLaren Teammate Split Widget (4×2)
 */
@Composable
private fun TeamFormAndTeammatesWidgetShowcase(
    constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026()
) {
    val standings = if (constructorStandings.isNotEmpty()) constructorStandings else SeedDataProvider.getConstructorStandings2026()
    val ferrariStanding = standings.firstOrNull { it.team == F1TeamId.FERRARI }
    val mercStanding = standings.firstOrNull { it.team == F1TeamId.MERCEDES }
    val rbStanding = standings.firstOrNull { it.team == F1TeamId.RED_BULL }
    val mclarenStanding = standings.firstOrNull { it.team == F1TeamId.MCLAREN }

    val mercPts = mercStanding?.points?.toInt() ?: 538
    val ferrariPts = ferrariStanding?.points?.toInt() ?: 378
    val ferrariPos = ferrariStanding?.position ?: 2
    val rbPts = rbStanding?.points?.toInt() ?: 249
    val gapToMerc = mercPts - ferrariPts
    val gapOverRb = ferrariPts - rbPts

    val mclarenPts = mclarenStanding?.points?.toInt() ?: 306
    val mclarenPos = mclarenStanding?.position ?: 3
    val norPoints = mclarenStanding?.drivers?.firstOrNull { it.fullName.contains("Norris") }?.points?.toInt() ?: 186
    val piaPoints = mclarenStanding?.drivers?.firstOrNull { it.fullName.contains("Piastri") }?.points?.toInt() ?: 120
    val prevStanding = standings.getOrNull(mclarenPos - 2)
    val gapToPrevText = if (prevStanding != null) {
        "-${(prevStanding.points.toInt() - mclarenPts)} TO P${prevStanding.position}"
    } else {
        "P$mclarenPos"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "FERRARI 'LAST 7' TEAM FORM MATRIX WIDGET (4×4)",
            color = P1TColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Ferrari Form Matrix 4x4
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8002D).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Red Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD40020))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogoBadge(team = F1TeamId.FERRARI, sizeDp = 20)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FERRARI",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "WCC · AFTER ROUND 19",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Driver badges with real portraits
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DriverPortraitAvatar(
                                driverCodeOrName = "HAM",
                                teamColor = Color(0xFFE8002D),
                                size = 28.dp,
                                positionBadge = "44"
                            )
                            DriverPortraitAvatar(
                                driverCodeOrName = "LEC",
                                teamColor = Color(0xFFE8002D),
                                size = 28.dp,
                                positionBadge = "16"
                            )
                        }

                        // Huge P2 & Points
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "$ferrariPos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                            Text(text = "$ferrariPts PTS", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // White Body with Ferrari Car & Last 7 Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFC))
                        .padding(12.dp)
                ) {
                    F1CarSideProfileCanvas(
                        team = F1TeamId.FERRARI,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "LAST 7 RACES FORM",
                        color = Color(0xFF666677),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val races = listOf("ITA", "ESP", "AZE", "MAS", "SGP", "USA", "MEX")
                    val hamResults = listOf("6", "DNF", "6", "3", "6", "4", "2")
                    val lecResults = listOf("DNF", "4", "4", "4", "2", "2", "4")

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
                            Text(text = "DRIVER", color = Color(0xFF888899), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                            races.forEach { r ->
                                Text(text = r, color = Color(0xFF888899), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4E4EC), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // HAMILTON row
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "HAMILTON (P3 · 259)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                            hamResults.forEach { res ->
                                val isPodium = res == "3" || res == "2" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(26.dp)
                                ) {
                                    Text(text = res, color = if (res == "DNF") Color(0xFFD40020) else Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(Color(0xFFD40020)))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // LECLERC row
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "LECLERC (P5 · 245)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                            lecResults.forEach { res ->
                                val isPodium = res == "2" || res == "3" || res == "1"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(26.dp)
                                ) {
                                    Text(text = res, color = if (res == "DNF") Color(0xFFD40020) else Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                                    if (isPodium) {
                                        Box(modifier = Modifier.width(14.dp).height(2.dp).background(Color(0xFFD40020)))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4E4EC), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                        // 3-Column Footer
                        Row(
                            modifier = Modifier.widthIn(min = 300.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "+30 IN MEXICO CITY", color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "-$gapToMerc TO MERCEDES", color = Color(0xFFD40020), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$gapOverRb OVER RED BULL", color = Color(0xFF1565C0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text(
            text = "MCLAREN TEAMMATE SPLIT WIDGET (4×2)",
            color = P1TColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // McLaren 4x2 Split Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1208),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8000).copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2E1A08), Color(0xFF140D06))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Logo and car
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        TeamLogoBadge(team = F1TeamId.MCLAREN, sizeDp = 22)
                        F1CarSideProfileCanvas(
                            team = F1TeamId.MCLAREN,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                        Text(
                            text = "PAPAYA SPLIT",
                            color = Color(0xFFFF8000),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Info and Teammate Split Bar
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(text = "MCLAREN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text(text = gapToPrevText, color = Color(0xFFFF8000), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "P$mclarenPos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
                                Text(text = "$mclarenPts PTS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Teammate Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "$norPoints NOR", color = Color(0xFFFF8000), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = "$piaPoints PIA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
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
                                        .weight(norPoints.toFloat().coerceAtLeast(1f))
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF8000))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(piaPoints.toFloat().coerceAtLeast(1f))
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
 * Renders an exact in-app preview of any of the 7 widget styles
 */
@Composable
private fun WidgetLivePreviewCard(
    widgetType: WidgetStyleType,
    preferences: UserPreferencesState,
    driverStandings: List<DriverStanding> = emptyList(),
    constructorStandings: List<ConstructorStanding> = emptyList(),
    drivers: List<DriverStanding> = driverStandings
) {
    val activeDrivers = if (driverStandings.isNotEmpty()) driverStandings else drivers.ifEmpty { SeedDataProvider.getDriverStandings2026() }
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

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF14141B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Widget Header: Type and Size Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = widgetType.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = widgetType.subtitle,
                        color = P1TColors.TextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF222230)
                ) {
                    Text(
                        text = widgetType.sizeLabel,
                        color = P1TColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Widget Content Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(preferences.favoriteTeam.secondaryHex),
                                Color(0xFF101016)
                            )
                        )
                    )
                    .border(1.dp, Color(preferences.favoriteTeam.colorHex).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                when (widgetType) {
                    WidgetStyleType.NEXT_GP_COUNTDOWN -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MALAYSIAN GP",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Round 22",
                                    color = P1TColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "36 DAYS TO GO",
                                color = Color(preferences.favoriteTeam.colorHex),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Sepang International Circuit · 5.543 km",
                                color = P1TColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WidgetStyleType.WEEKEND_SESSIONS -> {
                        Column {
                            Text(
                                text = "WEEKEND SCHEDULE",
                                color = P1TColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "FP1 (Fri)", color = Color.White, fontSize = 12.sp)
                                Text(text = "05:30", color = P1TColors.TextSecondary, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Qualifying (Sat)", color = Color.White, fontSize = 12.sp)
                                Text(text = "09:00", color = P1TColors.TextSecondary, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Grand Prix (Sun)", color = Color(preferences.favoriteTeam.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "07:00", color = Color(preferences.favoriteTeam.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        modifier = Modifier.width(18.dp)
                                    )
                                    TeamLogoBadge(team = d.team, sizeDp = 16)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = d.fullName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                        modifier = Modifier.width(18.dp)
                                    )
                                    TeamLogoBadge(team = teamItem.team, sizeDp = 16)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = teamItem.team.displayName,
                                        color = Color(teamItem.team.colorHex),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = driverTeam.displayName,
                                            color = P1TColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
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
                        val pts = teamStanding?.points?.toInt() ?: 538
                        val wins = teamStanding?.wins ?: 0
                        val driversText = teamStanding?.drivers?.takeIf { it.isNotEmpty() }?.joinToString(" · ") {
                            "${it.fullName.substringAfterLast(" ")} ${it.points.toInt()}"
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamLogoBadge(team = displayTeam, sizeDp = 22)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayTeam.displayName,
                                        color = Color(displayTeam.colorHex),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
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
                        Column {
                            Text(
                                text = "LAST GRAND PRIX PODIUM",
                                color = P1TColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DriverPortraitAvatar(driverCodeOrName = "HAM", teamColor = Color(0xFFE8002D), size = 24.dp, positionBadge = "1")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "L. Hamilton", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = "2:06:54.430", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DriverPortraitAvatar(driverCodeOrName = "LEC", teamColor = Color(0xFFE8002D), size = 24.dp, positionBadge = "2")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "C. Leclerc", color = Color.White, fontSize = 12.sp)
                                }
                                Text(text = "+2.411", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DriverPortraitAvatar(driverCodeOrName = "VER", teamColor = Color(0xFF3671C6), size = 24.dp, positionBadge = "3")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "M. Verstappen", color = Color.White, fontSize = 12.sp)
                                }
                                Text(text = "+5.820", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
