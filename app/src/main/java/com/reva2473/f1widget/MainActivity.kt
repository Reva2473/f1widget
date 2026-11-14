package com.reva2473.f1widget

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.notifications.SessionReminderScheduler
import com.reva2473.f1widget.ui.AppScreen
import com.reva2473.f1widget.ui.MainViewModel
import com.reva2473.f1widget.ui.components.FloatingPillBottomBar
import com.reva2473.f1widget.ui.screens.drivers.CompareDriversScreen
import com.reva2473.f1widget.ui.screens.drivers.DriverProfilesScreen
import com.reva2473.f1widget.ui.screens.home.HomeScreen
import com.reva2473.f1widget.ui.screens.live.LiveTimingScreen
import com.reva2473.f1widget.ui.screens.settings.SettingsAndWidgetsScreen
import com.reva2473.f1widget.ui.screens.standings.StandingsScreen
import com.reva2473.f1widget.ui.screens.weekend.RaceWeekendScreen
import com.reva2473.f1widget.ui.theme.F1WidgetTheme
import com.reva2473.f1widget.worker.WidgetRefreshScheduler

class MainActivity : ComponentActivity() {

    private companion object {
        const val EXTRA_TARGET_SCREEN = "EXTRA_TARGET_SCREEN"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WidgetRefreshScheduler.schedulePeriodicRefresh(applicationContext)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        addOnNewIntentListener { newIntent ->
            setIntent(newIntent)
            handleIntent(newIntent)
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setAppInForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.setAppInForeground(false)
                else -> Unit
            }
        })

        setContent {
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = when (state.userPreferences.themeMode) {
                "LIGHT" -> false
                "SYSTEM" -> isSystemInDarkTheme()
                else -> true
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            // System and hardware back gesture support
            BackHandler(enabled = state.currentScreen != AppScreen.HOME) {
                viewModel.navigateBack()
            }

            F1WidgetTheme(
                teamAccent = state.userPreferences.favoriteTeam,
                useTeamAccent = state.userPreferences.useTeamColorAccent,
                themeMode = state.userPreferences.themeMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Screen Content Router with animated transitions
                    AnimatedContent(
                        targetState = state.currentScreen,
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    weekend = state.raceWeekend,
                                    liveSession = state.liveSession,
                                    driverStandings = state.driverStandings,
                                    constructorStandings = state.constructorStandings,
                                    lastCompletedRace = state.lastCompletedRace,
                                    showBottomBar = false,
                                    onNavigateToLiveTiming = { viewModel.navigateTo(AppScreen.LIVE_TIMING) },
                                    onNavigateToStandings = { viewModel.navigateTo(AppScreen.STANDINGS) },
                                    onNavigateToWeekend = { viewModel.navigateTo(AppScreen.RACE_WEEKEND) },
                                    onNavigateToExplore = { viewModel.navigateTo(AppScreen.DRIVER_PROFILES) },
                                    onNavigateToSettings = { viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS) },
                                    onTabSelected = { tab -> handleBottomTabSelection(tab, viewModel) }
                                )
                            }

                            AppScreen.STANDINGS -> {
                                StandingsScreen(
                                    driverStandings = state.driverStandings,
                                    constructorStandings = state.constructorStandings,
                                    selectedYear = state.selectedYear,
                                    initialTab = state.standingsInitialTab,
                                    isLoading = state.isStandingsLoading,
                                    errorMessage = state.standingsError,
                                    showBottomBar = false,
                                    onYearSelected = { viewModel.setYear(it) },
                                    onBackClick = { viewModel.navigateBack() },
                                    onTabSelected = { tab -> handleBottomTabSelection(tab, viewModel) }
                                )
                            }

                            AppScreen.RACE_WEEKEND -> {
                                RaceWeekendScreen(
                                    weekend = state.raceWeekend,
                                    schedule = state.fullSchedule2026,
                                    initialTab = state.raceWeekendInitialTab,
                                    reminderSessionKeys = state.reminderSessionKeys,
                                    showBottomBar = false,
                                    onToggleSessionReminder = { sessionKey, enabled ->
                                        if (enabled &&
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !SessionReminderScheduler.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.toggleSessionReminder(sessionKey, enabled)
                                    },
                                    onSelectGrandPrix = { viewModel.selectGrandPrix(it) },
                                    onBackClick = { viewModel.navigateBack() },
                                    onTabSelected = { tab -> handleBottomTabSelection(tab, viewModel) }
                                )
                            }

                            AppScreen.DRIVER_PROFILES -> {
                                DriverProfilesScreen(
                                    drivers = state.driverStandings,
                                    selectedYear = state.selectedYear,
                                    onYearSelected = { viewModel.setYear(it) },
                                    onNavigateBack = { viewModel.navigateBack() },
                                    onNavigateToCompare = { viewModel.navigateTo(AppScreen.COMPARE_DRIVERS) },
                                    onDriverSelected = { driver ->
                                        // Find matching career comparison driver or construct non-fabricated stats from standing
                                        val match = state.careerComparisons.firstOrNull {
                                            it.code.equals(driver.code, ignoreCase = true) ||
                                                it.driverId.equals(driver.driverId, ignoreCase = true)
                                        } ?: SeedDataProvider.getDriverComparisons().firstOrNull {
                                            it.code.equals(driver.code, ignoreCase = true) ||
                                                it.driverId.equals(driver.driverId, ignoreCase = true)
                                        } ?: run {
                                            val raceStarts = driver.racesCount.coerceAtLeast(0)
                                            val poles = driver.poles.coerceAtLeast(0)
                                            DriverCareerComparison(
                                                driverId = driver.driverId,
                                                code = driver.code,
                                                firstName = driver.firstName,
                                                lastName = driver.lastName,
                                                nationality = driver.nationality,
                                                team = driver.team,
                                                championships = 0,
                                                raceWins = driver.wins,
                                                podiums = driver.podiums,
                                                polePositions = poles,
                                                fastestLaps = 0,
                                                careerPoints = driver.points,
                                                raceStarts = raceStarts,
                                                dnfs = 0,
                                                winRatePercent = if (raceStarts > 0) (driver.wins * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0,
                                                podiumRatePercent = if (raceStarts > 0) (driver.podiums * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0,
                                                poleRatePercent = if (raceStarts > 0) (poles * 100.0 / raceStarts).coerceAtLeast(0.0) else 0.0
                                            )
                                        }
                                        viewModel.setCompareDriver1(match)
                                        viewModel.navigateTo(AppScreen.COMPARE_DRIVERS)
                                    }
                                )
                            }

                            AppScreen.COMPARE_DRIVERS -> {
                                CompareDriversScreen(
                                    driver1 = state.compareDriver1,
                                    driver2 = state.compareDriver2,
                                    availableDrivers = state.careerComparisons,
                                    onSelectDriver1 = { viewModel.setCompareDriver1(it) },
                                    onSelectDriver2 = { viewModel.setCompareDriver2(it) },
                                    onSwapDrivers = { viewModel.swapCompareDrivers() },
                                    onNavigateBack = { viewModel.navigateBack() }
                                )
                            }

                            AppScreen.LIVE_TIMING -> {
                                LiveTimingScreen(
                                    sessionState = state.liveSession,
                                    onClose = { viewModel.navigateBack() }
                                )
                            }

                            AppScreen.SETTINGS_WIDGETS -> {
                                SettingsAndWidgetsScreen(
                                    userPreferences = state.userPreferences,
                                    driverStandings = state.driverStandings,
                                    constructorStandings = state.constructorStandings,
                                    liveSession = state.liveSession,
                                    nextWeekend = state.raceWeekend,
                                    lastCompletedRace = state.lastCompletedRace,
                                    schedule = state.fullSchedule2026,
                                    onUpdatePreferences = { newPrefs ->
                                        val oldPrefs = state.userPreferences
                                        if (!oldPrefs.sessionRemindersEnabled &&
                                            newPrefs.sessionRemindersEnabled &&
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !SessionReminderScheduler.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.updatePreferences(newPrefs)
                                    },
                                    onNavigateToArchive = { viewModel.navigateTo(AppScreen.DRIVER_PROFILES) }
                                )
                            }
                        }
                    }

                    // Single shared Floating Pill Bottom Bar across all 4 top-level tabs
                    val currentBottomTab = when (state.currentScreen) {
                        AppScreen.HOME -> "Home"
                        AppScreen.STANDINGS -> "Standings"
                        AppScreen.RACE_WEEKEND -> "Season"
                        AppScreen.SETTINGS_WIDGETS -> "Settings"
                        else -> null
                    }
                    if (currentBottomTab != null) {
                        FloatingPillBottomBar(
                            currentTab = currentBottomTab,
                            onSelectTab = { tab -> handleBottomTabSelection(tab, viewModel) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    private fun handleBottomTabSelection(tab: String, viewModel: MainViewModel) {
        when (tab) {
            "Home" -> viewModel.navigateTo(AppScreen.HOME)
            "Season", "Schedule" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
            "Standings" -> viewModel.navigateTo(AppScreen.STANDINGS)
            "Settings", "Widgets" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
            else -> viewModel.navigateTo(AppScreen.HOME)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent?.getStringExtra(EXTRA_TARGET_SCREEN)?.trim()?.takeIf { it.isNotEmpty() } ?: return
        intent.removeExtra(EXTRA_TARGET_SCREEN)
        viewModel.handleWidgetDeepLink(target)
    }
}
