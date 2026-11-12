package com.reva2473.f1widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        setContent {
            val state by viewModel.uiState.collectAsState()

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
                                    onNavigateToLiveTiming = { viewModel.navigateTo(AppScreen.LIVE_TIMING) },
                                    onNavigateToStandings = { viewModel.navigateTo(AppScreen.STANDINGS) },
                                    onNavigateToWeekend = { viewModel.navigateTo(AppScreen.RACE_WEEKEND) },
                                    onNavigateToExplore = { viewModel.navigateTo(AppScreen.DRIVER_PROFILES) },
                                    onTabSelected = { tab ->
                                        when (tab) {
                                            "Home" -> viewModel.navigateTo(AppScreen.HOME)
                                            "Season" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
                                            "Settings", "Standings" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
                                        }
                                    }
                                )
                            }

                            AppScreen.STANDINGS -> {
                                StandingsScreen(
                                    driverStandings = state.driverStandings,
                                    constructorStandings = state.constructorStandings,
                                    onBackClick = { viewModel.navigateTo(AppScreen.HOME) },
                                    onTabSelected = { tab ->
                                        when (tab) {
                                            "Home" -> viewModel.navigateTo(AppScreen.HOME)
                                            "Season" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
                                            "Settings" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
                                        }
                                    }
                                )
                            }

                            AppScreen.RACE_WEEKEND -> {
                                RaceWeekendScreen(
                                    weekend = state.raceWeekend,
                                    onBackClick = { viewModel.navigateTo(AppScreen.HOME) },
                                    onTabSelected = { tab ->
                                        when (tab) {
                                            "Home" -> viewModel.navigateTo(AppScreen.HOME)
                                            "Season" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
                                            "Settings" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
                                        }
                                    }
                                )
                            }

                            AppScreen.DRIVER_PROFILES -> {
                                DriverProfilesScreen(
                                    drivers = state.driverStandings,
                                    selectedYear = state.selectedYear,
                                    onYearSelected = { viewModel.setYear(it) },
                                    onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) },
                                    onNavigateToCompare = { viewModel.navigateTo(AppScreen.COMPARE_DRIVERS) },
                                    onDriverSelected = { driver ->
                                        // Find matching career comparison driver or create from standing
                                        val match = state.careerComparisons.firstOrNull { it.code == driver.code }
                                            ?: DriverCareerComparison(
                                                driverId = driver.driverId,
                                                code = driver.code,
                                                firstName = driver.firstName,
                                                lastName = driver.lastName,
                                                nationality = driver.nationality,
                                                team = driver.team,
                                                championships = 0,
                                                raceWins = driver.wins,
                                                podiums = driver.podiums,
                                                polePositions = driver.poles,
                                                fastestLaps = 2,
                                                careerPoints = driver.points,
                                                raceStarts = driver.racesCount * 3,
                                                dnfs = 4,
                                                winRatePercent = (driver.wins * 100.0 / (driver.racesCount * 3)).coerceAtLeast(0.0),
                                                podiumRatePercent = (driver.podiums * 100.0 / (driver.racesCount * 3)).coerceAtLeast(0.0),
                                                poleRatePercent = (driver.poles * 100.0 / (driver.racesCount * 3)).coerceAtLeast(0.0)
                                            )
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
                                    onNavigateBack = { viewModel.navigateTo(AppScreen.DRIVER_PROFILES) }
                                )
                            }

                            AppScreen.LIVE_TIMING -> {
                                LiveTimingScreen(
                                    sessionState = state.liveSession,
                                    onClose = { viewModel.navigateTo(AppScreen.HOME) }
                                )
                            }

                            AppScreen.SETTINGS_WIDGETS -> {
                                SettingsAndWidgetsScreen(
                                    userPreferences = state.userPreferences,
                                    drivers = state.driverStandings,
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
                            onSelectTab = { tab ->
                                when (tab) {
                                    "Home" -> viewModel.navigateTo(AppScreen.HOME)
                                    "Season" -> viewModel.navigateTo(AppScreen.RACE_WEEKEND)
                                    "Settings" -> viewModel.navigateTo(AppScreen.SETTINGS_WIDGETS)
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
