package com.reva2473.f1widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.DriverCareerComparison
import com.reva2473.f1widget.domain.model.DriverStanding
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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        addOnNewIntentListener { newIntent ->
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
            val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                                    onYearSelected = { viewModel.setYear(it) },
                                    onBackClick = { viewModel.navigateBack() },
                                    onTabSelected = { tab -> handleBottomTabSelection(tab, viewModel) }
                                )
                            }

                            AppScreen.RACE_WEEKEND -> {
                                RaceWeekendScreen(
                                    weekend = state.raceWeekend,
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
                                        // Find matching career comparison driver or derive deterministically from standing
                                        val match = state.careerComparisons.firstOrNull {
                                            it.code.equals(driver.code, ignoreCase = true) || it.driverId.equals(driver.driverId, ignoreCase = true)
                                        } ?: SeedDataProvider.getDriverComparisons().firstOrNull {
                                            it.code.equals(driver.code, ignoreCase = true) || it.driverId.equals(driver.driverId, ignoreCase = true)
                                        } ?: run {
                                            val safeRaces = driver.racesCount.coerceAtLeast(1)
                                            val derivedPoles = driver.poles
                                            val derivedFastestLaps = (driver.wins / 2).coerceAtLeast(0)
                                            val derivedDnfs = (safeRaces - driver.recentRaces.size).coerceAtLeast(0)
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
                                                polePositions = derivedPoles,
                                                fastestLaps = derivedFastestLaps,
                                                careerPoints = driver.points,
                                                raceStarts = safeRaces,
                                                dnfs = derivedDnfs,
                                                winRatePercent = (driver.wins * 100.0 / safeRaces).coerceAtLeast(0.0),
                                                podiumRatePercent = (driver.podiums * 100.0 / safeRaces).coerceAtLeast(0.0),
                                                poleRatePercent = (derivedPoles * 100.0 / safeRaces).coerceAtLeast(0.0)
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
                                    drivers = state.driverStandings,
                                    driverStandings = state.driverStandings,
                                    constructorStandings = state.constructorStandings,
                                    onUpdatePreferences = { viewModel.updatePreferences(it) },
                                    onNavigateToArchive = { viewModel.navigateTo(AppScreen.DRIVER_PROFILES) }
                                )
                            }
                        }
                    }

                    // Floating Pill Bottom Bar on screens where global navigation applies
                    if (state.currentScreen == AppScreen.SETTINGS_WIDGETS) {
                        FloatingPillBottomBar(
                            currentTab = "Settings",
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
            "Season" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
            "Standings" -> viewModel.navigateTo(AppScreen.STANDINGS)
            "Settings" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
            else -> viewModel.navigateTo(AppScreen.HOME)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent?.getStringExtra("EXTRA_TARGET_SCREEN")?.uppercase()?.trim() ?: return
        when (target) {
            "RACE_WEEKEND", "WEEKEND", "SEASON" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
            "STANDINGS", "DRIVER_STANDINGS", "CONSTRUCTOR_STANDINGS" -> viewModel.navigateTo(AppScreen.STANDINGS)
            "LIVE_TIMING", "LIVE" -> viewModel.navigateTo(AppScreen.LIVE_TIMING)
            "SETTINGS_WIDGETS", "SETTINGS", "WIDGETS" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
            "HOME" -> viewModel.navigateTo(AppScreen.HOME)
            "DRIVER_PROFILES", "DRIVERS", "PROFILES" -> viewModel.navigateTo(AppScreen.DRIVER_PROFILES)
            "COMPARE_DRIVERS", "COMPARE" -> viewModel.navigateTo(AppScreen.COMPARE_DRIVERS)
        }
    }
}
