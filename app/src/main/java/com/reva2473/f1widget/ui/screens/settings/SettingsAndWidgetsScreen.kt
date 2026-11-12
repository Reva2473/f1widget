package com.reva2473.f1widget.ui.screens.settings

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.domain.model.WidgetStyleType
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
    drivers: List<DriverStanding>,
    onUpdatePreferences: (UserPreferencesState) -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWidgetIndex by remember { mutableStateOf(0) }
    var showDriverPicker by remember { mutableStateOf(false) }

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

            // Horizontal selector of widget types
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    widgetTypes.forEachIndexed { idx, type ->
                        val isSelected = idx == selectedWidgetIndex
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) LocalF1Accent.current.primaryAccent else P1TColors.SurfaceCard,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, P1TColors.BorderSubtle),
                            modifier = Modifier.clickable { selectedWidgetIndex = idx }
                        ) {
                            Text(
                                text = type.title,
                                color = if (isSelected) Color.White else P1TColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Widget Preview Container
            item {
                val currentType = widgetTypes[selectedWidgetIndex]
                WidgetLivePreviewCard(
                    widgetType = currentType,
                    preferences = userPreferences,
                    drivers = drivers
                )
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(drv.team.secondaryHex))
                                    .border(1.5.dp, Color(drv.team.colorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = drv.code,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                                    val isSelected = userPreferences.themeMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) LocalF1Accent.current.primaryAccent else P1TColors.SurfaceElevated,
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences(userPreferences.copy(themeMode = mode))
                                        }
                                    ) {
                                        Text(
                                            text = mode.lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) Color.White else P1TColors.TextSecondary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Pit Radio Audio Alerts switch
                        SettingsToggleRow(
                            title = "Pit Radio Alerts",
                            subtitle = "Notify when interesting team radio messages are flagged",
                            checked = userPreferences.pitRadioAlertsEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(pitRadioAlertsEnabled = it))
                            }
                        )

                        Divider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        // Session Reminders switch
                        SettingsToggleRow(
                            title = "Session Reminders",
                            subtitle = "Notify 15 minutes before Practice, Quali & Grand Prix",
                            checked = userPreferences.sessionRemindersEnabled,
                            onCheckedChange = {
                                onUpdatePreferences(userPreferences.copy(sessionRemindersEnabled = it))
                            }
                        )

                        Divider(color = P1TColors.BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

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
                            Text(
                                text = "🏆",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 10.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(drv.team.colorHex))
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
 * Renders an exact in-app preview of any of the 7 widget styles
 */
@Composable
private fun WidgetLivePreviewCard(
    widgetType: WidgetStyleType,
    preferences: UserPreferencesState,
    drivers: List<DriverStanding>
) {
    val favDriver = drivers.firstOrNull { it.code == preferences.favoriteDriverCode }
        ?: drivers.firstOrNull()

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
                                    text = "🇲🇾 MALAYSIAN GP",
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            drivers.take(4).forEach { d ->
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
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(d.team.colorHex))
                                    )
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                F1TeamId.FERRARI to 482,
                                F1TeamId.MCLAREN to 438,
                                F1TeamId.MERCEDES to 395,
                                F1TeamId.RED_BULL to 360
                            ).forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = P1TColors.TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(18.dp)
                                    )
                                    Text(
                                        text = pair.first.displayName,
                                        color = Color(pair.first.colorHex),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${pair.second} pts",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    WidgetStyleType.FAVORITE_DRIVER -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = favDriver?.fullName ?: "Driver",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "P${favDriver?.position ?: 1}",
                                    color = Color(preferences.favoriteTeam.colorHex),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${favDriver?.points?.toInt() ?: 0} pts · ${favDriver?.wins ?: 0} wins",
                                color = P1TColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Recent: ${favDriver?.last3Summary ?: "P1, P1, P1"}",
                                color = P1TColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    WidgetStyleType.FAVORITE_TEAM -> {
                        Column {
                            Text(
                                text = preferences.favoriteTeam.displayName,
                                color = Color(preferences.favoriteTeam.colorHex),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Constructor Championship P1 · 482 pts",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Season wins: 8 · Poles: 11",
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "🥇 P1  L. Hamilton", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "2:06:54.430", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "🥈 P2  C. Leclerc", color = Color.White, fontSize = 12.sp)
                                Text(text = "+2.411", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "🥉 P3  M. Verstappen", color = Color.White, fontSize = 12.sp)
                                Text(text = "+5.820", color = P1TColors.TextSecondary, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
