package com.reva2473.f1widget.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Navigation destination routes in the app
 */
enum class AppScreen {
    HOME,
    STANDINGS,
    RACE_WEEKEND,
    DRIVER_PROFILES,
    COMPARE_DRIVERS,
    LIVE_TIMING,
    SETTINGS_WIDGETS
}

private val defaultFallbackDriver = DriverCareerComparison(
    driverId = "verstappen",
    code = "VER",
    firstName = "Max",
    lastName = "Verstappen",
    nationality = "Dutch",
    team = F1TeamId.RED_BULL,
    championships = 4,
    raceWins = 71,
    podiums = 134,
    polePositions = 48,
    fastestLaps = 37,
    careerPoints = 3607.5,
    raceStarts = 248,
    dnfs = 37,
    winRatePercent = 28.6,
    podiumRatePercent = 54.0,
    poleRatePercent = 19.4
)

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val canNavigateBack: Boolean = false,
    val selectedYear: Int = 2026,
    val userPreferences: UserPreferencesState = UserPreferencesState(),
    val raceWeekend: RaceWeekend = SeedDataProvider.getNextRaceWeekend(),
    val fullSchedule2026: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026),
    val liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState(),
    val driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    val constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    val careerComparisons: List<DriverCareerComparison> = SeedDataProvider.getDriverComparisons(),
    val compareDriver1: DriverCareerComparison = SeedDataProvider.getDriverComparisons().firstOrNull() ?: defaultFallbackDriver,
    val compareDriver2: DriverCareerComparison = SeedDataProvider.getDriverComparisons().getOrNull(1)
        ?: SeedDataProvider.getDriverComparisons().firstOrNull()
        ?: defaultFallbackDriver,
    val selectedDriverDetail: DriverStanding? = null
) {
    val selectedWeekend: RaceWeekend get() = raceWeekend
    val currentWeekend: RaceWeekend get() = raceWeekend
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = F1Repository(application.applicationContext)
    private val preferencesStore = repository.preferencesStore

    private val backStack = ArrayDeque<AppScreen>()
    private var isAppInForeground: Boolean = true
    private var yearSelectionJob: Job? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect user preferences from DataStore
        viewModelScope.launch {
            repository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(userPreferences = prefs) }
            }
        }

        // Load initial repository data
        viewModelScope.launch {
            try {
                val nextWeekend = repository.getNextRaceWeekend()
                val drivers = repository.getDriverStandings(2026)
                val constructors = repository.getConstructorStandings(2026)
                val live = repository.getLiveSessionState()
                val schedule2026 = repository.getSeasonSchedule(2026)
                _uiState.update { current ->
                    if (current.selectedYear == 2026) {
                        current.copy(
                            raceWeekend = nextWeekend,
                            driverStandings = drivers,
                            constructorStandings = constructors,
                            liveSession = live,
                            fullSchedule2026 = if (schedule2026.isNotEmpty()) schedule2026 else current.fullSchedule2026
                        )
                    } else {
                        current.copy(
                            raceWeekend = nextWeekend,
                            liveSession = live,
                            fullSchedule2026 = if (schedule2026.isNotEmpty()) schedule2026 else current.fullSchedule2026
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Fall back to seed defaults already populated in MainUiState
            }
        }

        // Live coroutine ticker that smoothly animates car positions along circuit path!
        // Gated so it ONLY updates _uiState when isAppInForeground && (HOME or LIVE_TIMING)
        viewModelScope.launch {
            while (isActive) {
                val currentScreen = _uiState.value.currentScreen
                if (isAppInForeground && (currentScreen == AppScreen.HOME || currentScreen == AppScreen.LIVE_TIMING)) {
                    delay(80) // ~12 fps track progress ticker
                    _uiState.update { state ->
                        val updatedDrivers = state.liveSession.drivers.map { driver ->
                            val speed = 0.0032f + (22 - driver.position).coerceAtLeast(1) * 0.00008f
                            var nextProgress = driver.trackProgress + speed
                            if (nextProgress > 1.0f) nextProgress -= 1.0f
                            driver.copy(trackProgress = nextProgress)
                        }
                        state.copy(
                            liveSession = state.liveSession.copy(drivers = updatedDrivers)
                        )
                    }
                } else {
                    delay(500)
                }
            }
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
    }

    fun setLiveTickerActive(active: Boolean) {
        isAppInForeground = active
    }

    fun navigateTo(screen: AppScreen) {
        val current = _uiState.value.currentScreen
        if (current != screen) {
            if (backStack.lastOrNull() != current) {
                backStack.addLast(current)
            }
            _uiState.update {
                it.copy(
                    currentScreen = screen,
                    canNavigateBack = backStack.isNotEmpty() || screen != AppScreen.HOME
                )
            }
        }
    }

    fun selectBottomTab(tab: String) {
        val target = when (tab) {
            "Home" -> AppScreen.HOME
            "Season" -> AppScreen.RACE_WEEKEND
            "Standings" -> AppScreen.STANDINGS
            "Settings" -> AppScreen.SETTINGS_WIDGETS
            else -> AppScreen.HOME
        }
        navigateTo(target)
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeLast()
            _uiState.update {
                it.copy(
                    currentScreen = prev,
                    canNavigateBack = backStack.isNotEmpty() || prev != AppScreen.HOME
                )
            }
            return true
        } else if (_uiState.value.currentScreen != AppScreen.HOME) {
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.HOME,
                    canNavigateBack = false
                )
            }
            return true
        }
        return false
    }

    fun selectGrandPrix(gpName: String) {
        val query = gpName.trim()
        val from2026 = _uiState.value.fullSchedule2026.firstOrNull {
            it.grandPrixName.contains(query, ignoreCase = true) ||
            query.contains(it.grandPrixName, ignoreCase = true) ||
            it.circuitName.contains(query, ignoreCase = true) ||
            query.contains(it.circuitName, ignoreCase = true)
        }

        val weekend = from2026 ?: when {
            query.contains("Brazilian", ignoreCase = true) -> SeedDataProvider.getBrazilianGpWeekend2026()
            query.contains("São Paulo", ignoreCase = true) || query.contains("Sao Paulo", ignoreCase = true) -> SeedDataProvider.getSaoPaulo2024Results()
            query.contains("Abu Dhabi", ignoreCase = true) -> SeedDataProvider.getAbuDhabi2023Results()
            query.contains("British", ignoreCase = true) || query.contains("Silverstone", ignoreCase = true) -> {
                SeedDataProvider.getSeasonSchedule(1950).firstOrNull { it.circuitType == "SILVERSTONE" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            query.contains("Monaco", ignoreCase = true) -> {
                SeedDataProvider.getSeasonSchedule(1950).firstOrNull { it.circuitType == "MONACO" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            query.contains("Monza", ignoreCase = true) || query.contains("Italian", ignoreCase = true) -> {
                SeedDataProvider.getSeasonSchedule(1950).firstOrNull { it.circuitType == "MONZA" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            else -> {
                SeedDataProvider.getSeasonSchedule(2026).firstOrNull {
                    it.grandPrixName.contains(query, ignoreCase = true) ||
                    query.contains(it.grandPrixName, ignoreCase = true) ||
                    it.circuitName.contains(query, ignoreCase = true)
                } ?: SeedDataProvider.getNextRaceWeekend()
            }
        }
        _uiState.update { it.copy(raceWeekend = weekend) }
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        yearSelectionJob?.cancel()
        yearSelectionJob = viewModelScope.launch {
            try {
                val standings = repository.getDriverStandings(year)
                val constructors = repository.getConstructorStandings(year)
                if (_uiState.value.selectedYear == year) {
                    _uiState.update {
                        if (it.selectedYear == year) {
                            it.copy(
                                driverStandings = standings,
                                constructorStandings = constructors
                            )
                        } else {
                            it
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Keep existing standings on error
            }
        }
    }

    fun selectDriverForDetail(driver: DriverStanding) {
        val current = _uiState.value.currentScreen
        if (current != AppScreen.DRIVER_PROFILES && backStack.lastOrNull() != current) {
            backStack.addLast(current)
        }
        _uiState.update {
            it.copy(
                selectedDriverDetail = driver,
                currentScreen = AppScreen.DRIVER_PROFILES,
                canNavigateBack = true
            )
        }
    }

    fun setCompareDriver1(driver: DriverCareerComparison) {
        _uiState.update { it.copy(compareDriver1 = driver) }
    }

    fun setCompareDriver2(driver: DriverCareerComparison) {
        _uiState.update { it.copy(compareDriver2 = driver) }
    }

    fun swapCompareDrivers() {
        _uiState.update {
            it.copy(
                compareDriver1 = it.compareDriver2,
                compareDriver2 = it.compareDriver1
            )
        }
    }

    fun updatePreferences(newPrefs: UserPreferencesState, context: Context? = null) {
        _uiState.update { it.copy(userPreferences = newPrefs) }

        viewModelScope.launch {
            try {
                preferencesStore.updateFavoriteTeam(newPrefs.favoriteTeam)
                preferencesStore.updateFavoriteDriver(newPrefs.favoriteDriverCode)
                preferencesStore.updateThemeMode(newPrefs.themeMode)
                preferencesStore.updateUseTeamColor(newPrefs.useTeamColorAccent)
                preferencesStore.updatePitRadioAlerts(newPrefs.pitRadioAlertsEnabled)
                preferencesStore.updateSessionReminders(newPrefs.sessionRemindersEnabled)
                preferencesStore.updateLapRecordAlerts(newPrefs.lapRecordAlertsEnabled)
                preferencesStore.updateOfflineCache(newPrefs.offlineCacheEnabled)

                // Trigger widget updates ONLY after all preferences are successfully committed
                val appContext = context ?: getApplication<Application>()
                com.reva2473.f1widget.widget.NextRaceCountdownWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.WeekendScheduleWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.DriverStandingsWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.ConstructorStandingsWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.FavoriteDriverWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.FavoriteTeamWidget().updateAll(appContext)
                com.reva2473.f1widget.widget.LastRacePodiumWidget().updateAll(appContext)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Ignore widget update errors if widgets aren't currently bound on home screen
            }
        }
    }
}
