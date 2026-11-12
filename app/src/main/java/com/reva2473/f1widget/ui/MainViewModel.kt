package com.reva2473.f1widget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.*
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

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedYear: Int = 2026,
    val userPreferences: UserPreferencesState = UserPreferencesState(),
    val raceWeekend: RaceWeekend = SeedDataProvider.getNextRaceWeekend(),
    val liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState(),
    val driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    val constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    val careerComparisons: List<DriverCareerComparison> = SeedDataProvider.getDriverComparisons(),
    val compareDriver1: DriverCareerComparison = SeedDataProvider.getDriverComparisons()[0], // VER
    val compareDriver2: DriverCareerComparison = SeedDataProvider.getDriverComparisons()[1], // NOR
    val selectedDriverDetail: DriverStanding? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = F1Repository(application.applicationContext)
    private val preferencesStore = repository.preferencesStore

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
            val nextWeekend = repository.getNextRaceWeekend()
            val drivers = repository.getDriverStandings(2026)
            val constructors = repository.getConstructorStandings(2026)
            val live = repository.getLiveSessionState()
            _uiState.update {
                it.copy(
                    raceWeekend = nextWeekend,
                    driverStandings = drivers,
                    constructorStandings = constructors,
                    liveSession = live
                )
            }
        }

        // Live coroutine ticker that smoothly animates car positions along circuit path!
        viewModelScope.launch {
            while (isActive) {
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
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        viewModelScope.launch {
            val standings = repository.getDriverStandings(year)
            val constructors = repository.getConstructorStandings(year)
            _uiState.update {
                it.copy(
                    driverStandings = standings,
                    constructorStandings = constructors
                )
            }
        }
    }

    fun selectDriverForDetail(driver: DriverStanding) {
        _uiState.update {
            it.copy(
                selectedDriverDetail = driver,
                currentScreen = AppScreen.DRIVER_PROFILES
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

    fun updatePreferences(newPrefs: UserPreferencesState) {
        viewModelScope.launch {
            preferencesStore.updateFavoriteTeam(newPrefs.favoriteTeam)
            preferencesStore.updateFavoriteDriver(newPrefs.favoriteDriverCode)
            preferencesStore.updateThemeMode(newPrefs.themeMode)
            preferencesStore.updateUseTeamColor(newPrefs.useTeamColorAccent)
            preferencesStore.updatePitRadioAlerts(newPrefs.pitRadioAlertsEnabled)
            preferencesStore.updateSessionReminders(newPrefs.sessionRemindersEnabled)
            preferencesStore.updateOfflineCache(newPrefs.offlineCacheEnabled)
        }
        _uiState.update { it.copy(userPreferences = newPrefs) }
    }
}
