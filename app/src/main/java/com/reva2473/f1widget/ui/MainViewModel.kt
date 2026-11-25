package com.reva2473.f1widget.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.*
import com.reva2473.f1widget.notifications.SessionReminderScheduler
import com.reva2473.f1widget.notifications.buildSessionReminderKey
import com.reva2473.f1widget.ui.screens.standings.StandingsTab
import com.reva2473.f1widget.ui.screens.weekend.WeekendFilterTab
import com.reva2473.f1widget.worker.WidgetRefreshScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    val isStandingsLoading: Boolean = false,
    val standingsError: String? = null,
    val standingsInitialTab: StandingsTab = StandingsTab.DRIVERS,
    val raceWeekendInitialTab: WeekendFilterTab = WeekendFilterTab.WEEKEND_SCHEDULE,
    val navigationToken: Int = 0,
    val selectedGrandPrix: String = "",
    val userPreferences: UserPreferencesState = UserPreferencesState(),
    val reminderSessionKeys: Set<String> = emptySet(),
    val raceWeekend: RaceWeekend = SeedDataProvider.getNextRaceWeekend(),
    val lastCompletedRace: RaceWeekend? = null,
    val fullSchedule2026: List<RaceWeekend> = SeedDataProvider.getSeasonSchedule(2026),
    val liveSession: LiveSessionState = SeedDataProvider.getLiveSessionState().copy(isLive = false),
    val driverStandings: List<DriverStanding> = SeedDataProvider.getDriverStandings2026(),
    val constructorStandings: List<ConstructorStanding> = SeedDataProvider.getConstructorStandings2026(),
    val currentSeasonDriverStandings: List<DriverStanding> = emptyList(),
    val currentSeasonConstructorStandings: List<ConstructorStanding> = emptyList(),
    val careerComparisons: List<DriverCareerComparison> = SeedDataProvider.getDriverComparisons(),
    val compareDriver1: DriverCareerComparison = SeedDataProvider.getDriverComparisons().firstOrNull() ?: defaultFallbackDriver,
    val compareDriver2: DriverCareerComparison = SeedDataProvider.getDriverComparisons().getOrNull(1)
        ?: SeedDataProvider.getDriverComparisons().firstOrNull()
        ?: defaultFallbackDriver
) {
    val selectedWeekend: RaceWeekend get() = raceWeekend
    val currentWeekend: RaceWeekend get() = raceWeekend
}

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_CURRENT_SCREEN = "current_screen"
        private const val KEY_BACK_STACK = "back_stack"
        private const val KEY_SELECTED_YEAR = "selected_year"
        private const val KEY_SELECTED_GP_NAME = "selected_gp_name"
        private const val KEY_STANDINGS_TAB = "standings_tab"
        private const val KEY_RACE_WEEKEND_TAB = "race_weekend_tab"
        private const val KEY_COMPARE_DRIVER_1 = "compare_driver_1"
        private const val KEY_COMPARE_DRIVER_2 = "compare_driver_2"

        private val TOP_LEVEL_SCREENS = setOf(
            AppScreen.HOME,
            AppScreen.RACE_WEEKEND,
            AppScreen.STANDINGS,
            AppScreen.SETTINGS_WIDGETS
        )
    }

    private val repository = F1Repository(application.applicationContext)
    private val preferencesStore = repository.preferencesStore
    private val stateJson = Json { ignoreUnknownKeys = true }
    private val preferencesMutex = Mutex()

    private val backStack = ArrayDeque<AppScreen>()
    private val isAppInForegroundFlow = MutableStateFlow(true)
    private var isAppInForeground: Boolean = true
    private var yearSelectionJob: Job? = null
    private var refreshJob: Job? = null
    private var deepLinkJob: Job? = null

    private val _uiState: MutableStateFlow<MainUiState>
    val uiState: StateFlow<MainUiState>

    private val _liveSessionFlow: MutableStateFlow<LiveSessionState>
    val liveSessionFlow: StateFlow<LiveSessionState>

    private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val userFeedback = _userFeedback.asSharedFlow()

    fun emitFeedback(message: String) {
        _userFeedback.tryEmit(message)
    }

    init {
        val defaultCompare1 = SeedDataProvider.getDriverComparisons().firstOrNull() ?: defaultFallbackDriver
        val defaultCompare2 = SeedDataProvider.getDriverComparisons().getOrNull(1) ?: defaultCompare1

        val initialScreen = savedStateHandle.get<String>(KEY_CURRENT_SCREEN)
            ?.let { name -> AppScreen.entries.firstOrNull { it.name == name } }
            ?: AppScreen.HOME

        val restoredStack = savedStateHandle.get<ArrayList<String>>(KEY_BACK_STACK)
            .orEmpty()
            .mapNotNull { name -> AppScreen.entries.firstOrNull { it.name == name } }
        backStack.addAll(restoredStack)

        val initialYear = savedStateHandle.get<Int>(KEY_SELECTED_YEAR) ?: 2026
        val initialStandingsTab = savedStateHandle.get<String>(KEY_STANDINGS_TAB)
            ?.let { name -> StandingsTab.entries.firstOrNull { it.name == name } }
            ?: StandingsTab.DRIVERS
        val initialRaceWeekendTab = savedStateHandle.get<String>(KEY_RACE_WEEKEND_TAB)
            ?.let { name -> WeekendFilterTab.entries.firstOrNull { it.name == name } }
            ?: WeekendFilterTab.WEEKEND_SCHEDULE

        val savedGpName = savedStateHandle.get<String>(KEY_SELECTED_GP_NAME)
        val initialSchedule2026 = SeedDataProvider.getSeasonSchedule(2026)
        val initialWeekend = if (!savedGpName.isNullOrBlank()) {
            resolveGrandPrix(savedGpName, initialSchedule2026)
        } else {
            SeedDataProvider.getNextRaceWeekend()
        }

        _uiState = MutableStateFlow(
            MainUiState(
                currentScreen = initialScreen,
                canNavigateBack = backStack.isNotEmpty() || initialScreen != AppScreen.HOME,
                selectedYear = initialYear,
                standingsInitialTab = initialStandingsTab,
                raceWeekendInitialTab = initialRaceWeekendTab,
                selectedGrandPrix = savedGpName.orEmpty(),
                raceWeekend = initialWeekend,
                fullSchedule2026 = initialSchedule2026,
                compareDriver1 = restoreCompareDriver(KEY_COMPARE_DRIVER_1, defaultCompare1),
                compareDriver2 = restoreCompareDriver(KEY_COMPARE_DRIVER_2, defaultCompare2)
            )
        )
        uiState = _uiState.asStateFlow()
        _liveSessionFlow = MutableStateFlow(_uiState.value.liveSession)
        liveSessionFlow = _liveSessionFlow.asStateFlow()

        // Collect user preferences from DataStore
        viewModelScope.launch {
            repository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(userPreferences = prefs) }
            }
        }

        // Collect persisted session reminder keys from DataStore
        viewModelScope.launch {
            repository.reminderSessionKeysFlow.collect { keys ->
                _uiState.update { it.copy(reminderSessionKeys = keys) }
            }
        }

        // Load initial repository data, sync reminders once on startup, and ensure periodic widget refresh is scheduled
        WidgetRefreshScheduler.schedulePeriodicRefresh(getApplication())
        viewModelScope.launch {
            try {
                SessionReminderScheduler.syncReminders(getApplication(), repository)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Ignore transient startup reminder sync errors
            }
        }
        refreshAll()

        if (initialYear != 2026) {
            setYear(initialYear)
        }

        // Live coroutine ticker that smoothly animates car positions along circuit path.
        // Suspended at 0 Hz when offline/idle (isLive == false), backgrounded, or off HOME/LIVE_TIMING.
        viewModelScope.launch {
            combine(
                isAppInForegroundFlow,
                uiState.map { it.currentScreen }.distinctUntilChanged(),
                liveSessionFlow.map { it.isLive }.distinctUntilChanged()
            ) { inForeground, screen, isLive ->
                inForeground && isLive && (screen == AppScreen.HOME || screen == AppScreen.LIVE_TIMING)
            }
                .distinctUntilChanged()
                .collectLatest { shouldRun ->
                    if (!shouldRun) return@collectLatest
                    while (isActive) {
                        val liveSnapshot = _liveSessionFlow.value
                        val currentScreen = _uiState.value.currentScreen
                        val drivers = liveSnapshot.drivers
                        val isSessionMoving = liveSnapshot.isLive &&
                            drivers.isNotEmpty() &&
                            !liveSnapshot.trackStatusText.contains("Red Flag", ignoreCase = true) &&
                            !liveSnapshot.timeRemaining.equals("0:00", ignoreCase = true) &&
                            !liveSnapshot.timeRemaining.equals("Finished", ignoreCase = true)

                        if (isAppInForeground &&
                            (currentScreen == AppScreen.HOME || currentScreen == AppScreen.LIVE_TIMING) &&
                            isSessionMoving
                        ) {
                            delay(80) // ~12.5 Hz track progress ticker
                            val screenAfterDelay = _uiState.value.currentScreen
                            if (!isAppInForeground ||
                                (screenAfterDelay != AppScreen.HOME && screenAfterDelay != AppScreen.LIVE_TIMING) ||
                                !_liveSessionFlow.value.isLive
                            ) {
                                continue
                            }
                            _liveSessionFlow.update { live ->
                                if (!live.isLive || live.drivers.isEmpty()) return@update live
                                val updatedDrivers = live.drivers.map { driver ->
                                    if (driver.isInPit) {
                                        driver
                                    } else {
                                        val speed = 0.0032f + (22 - driver.position).coerceAtLeast(1) * 0.00008f
                                        var nextProgress = driver.trackProgress + speed
                                        if (nextProgress > 1.0f) nextProgress -= 1.0f
                                        driver.copy(trackProgress = nextProgress)
                                    }
                                }
                                live.copy(drivers = updatedDrivers)
                            }
                        } else {
                            delay(500)
                        }
                    }
                }
        }
    }

    fun refreshAll() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val (nextWeekend, drivers, constructors, live, schedule2026, latestCompleted) = coroutineScope {
                    val nextDeferred = async { repository.getNextRaceWeekend() }
                    val driversDeferred = async { repository.getDriverStandings(2026) }
                    val constructorsDeferred = async { repository.getConstructorStandings(2026) }
                    val liveDeferred = async { repository.getLiveSessionState("latest") }
                    val scheduleDeferred = async { repository.getSeasonSchedule(2026) }
                    val completedDeferred = async { repository.getLatestCompletedRace(2026) }
                    InitialDataBundle(
                        nextWeekend = nextDeferred.await(),
                        drivers = driversDeferred.await(),
                        constructors = constructorsDeferred.await(),
                        live = liveDeferred.await(),
                        schedule2026 = scheduleDeferred.await(),
                        latestCompleted = completedDeferred.await()
                    )
                }

                val savedGpName = savedStateHandle.get<String>(KEY_SELECTED_GP_NAME)
                val resolvedSchedule = if (schedule2026.isNotEmpty()) schedule2026 else _uiState.value.fullSchedule2026
                val targetWeekend = when {
                    !savedGpName.isNullOrBlank() -> {
                        if (formatWeekendLabel(latestCompleted).equals(savedGpName, ignoreCase = true) ||
                            latestCompleted.grandPrixName.equals(savedGpName, ignoreCase = true)
                        ) {
                            latestCompleted
                        } else {
                            resolveGrandPrix(savedGpName, resolvedSchedule)
                        }
                    }
                    _uiState.value.raceWeekendInitialTab == WeekendFilterTab.RACE_RESULTS -> {
                        val label = formatWeekendLabel(latestCompleted)
                        savedStateHandle[KEY_SELECTED_GP_NAME] = label
                        latestCompleted
                    }
                    else -> nextWeekend
                }

                _liveSessionFlow.value = live
                _uiState.update { current ->
                    val updatedSelectedGp = savedStateHandle.get<String>(KEY_SELECTED_GP_NAME) ?: current.selectedGrandPrix
                    if (current.selectedYear == 2026) {
                        current.copy(
                            raceWeekend = targetWeekend,
                            selectedGrandPrix = updatedSelectedGp,
                            lastCompletedRace = latestCompleted,
                            driverStandings = drivers,
                            constructorStandings = constructors,
                            currentSeasonDriverStandings = drivers,
                            currentSeasonConstructorStandings = constructors,
                            liveSession = live,
                            fullSchedule2026 = resolvedSchedule
                        )
                    } else {
                        current.copy(
                            raceWeekend = targetWeekend,
                            selectedGrandPrix = updatedSelectedGp,
                            lastCompletedRace = latestCompleted,
                            currentSeasonDriverStandings = drivers,
                            currentSeasonConstructorStandings = constructors,
                            liveSession = live,
                            fullSchedule2026 = resolvedSchedule
                        )
                    }
                }
                WidgetRefreshScheduler.triggerImmediateRefresh(getApplication())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Fall back to seed defaults already populated in MainUiState
            }
        }
    }

    private data class InitialDataBundle(
        val nextWeekend: RaceWeekend,
        val drivers: List<DriverStanding>,
        val constructors: List<ConstructorStanding>,
        val live: LiveSessionState,
        val schedule2026: List<RaceWeekend>,
        val latestCompleted: RaceWeekend
    )

    private fun restoreCompareDriver(key: String, fallback: DriverCareerComparison): DriverCareerComparison {
        val raw = savedStateHandle.get<String>(key) ?: return fallback
        return try {
            stateJson.decodeFromString<DriverCareerComparison>(raw)
        } catch (_: Exception) {
            SeedDataProvider.getDriverComparisons()
                .firstOrNull { it.driverId == raw || it.code == raw }
                ?: fallback
        }
    }

    private fun saveCompareDriver(key: String, driver: DriverCareerComparison) {
        savedStateHandle[key] = try {
            stateJson.encodeToString(driver)
        } catch (_: Exception) {
            driver.driverId
        }
    }

    private fun persistNavigationState(screen: AppScreen) {
        savedStateHandle[KEY_CURRENT_SCREEN] = screen.name
        savedStateHandle[KEY_BACK_STACK] = ArrayList(backStack.map { it.name })
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        isAppInForegroundFlow.value = inForeground
    }

    fun navigateTo(screen: AppScreen) {
        navigateToInternal(screen, resetTabDefaults = screen in TOP_LEVEL_SCREENS)
    }

    private fun navigateToInternal(screen: AppScreen, resetTabDefaults: Boolean) {
        val current = _uiState.value.currentScreen
        if (screen in TOP_LEVEL_SCREENS) {
            backStack.clear()
            persistNavigationState(screen)
            if (resetTabDefaults) {
                when (screen) {
                    AppScreen.STANDINGS -> {
                        savedStateHandle[KEY_STANDINGS_TAB] = StandingsTab.DRIVERS.name
                        _uiState.update {
                            it.copy(
                                currentScreen = screen,
                                canNavigateBack = true,
                                standingsInitialTab = StandingsTab.DRIVERS,
                                navigationToken = it.navigationToken + 1
                            )
                        }
                        return
                    }
                    AppScreen.RACE_WEEKEND -> {
                        savedStateHandle[KEY_RACE_WEEKEND_TAB] = WeekendFilterTab.WEEKEND_SCHEDULE.name
                        savedStateHandle.remove<String>(KEY_SELECTED_GP_NAME)
                        val hadCustomSelection = _uiState.value.selectedGrandPrix.isNotEmpty() ||
                            _uiState.value.raceWeekendInitialTab != WeekendFilterTab.WEEKEND_SCHEDULE
                        val nextUpcoming = if (hadCustomSelection) {
                            resolveUpcomingWeekendFromSchedule(_uiState.value.fullSchedule2026)
                        } else {
                            _uiState.value.raceWeekend
                        }
                        _uiState.update {
                            it.copy(
                                currentScreen = screen,
                                canNavigateBack = true,
                                raceWeekendInitialTab = WeekendFilterTab.WEEKEND_SCHEDULE,
                                selectedGrandPrix = "",
                                raceWeekend = nextUpcoming,
                                navigationToken = it.navigationToken + 1
                            )
                        }
                        return
                    }
                    else -> Unit
                }
            }
            if (current != screen || _uiState.value.canNavigateBack != (screen != AppScreen.HOME)) {
                _uiState.update {
                    it.copy(
                        currentScreen = screen,
                        canNavigateBack = screen != AppScreen.HOME
                    )
                }
            }
            return
        }

        if (current != screen) {
            if (backStack.lastOrNull() != current) {
                backStack.addLast(current)
            }
            persistNavigationState(screen)
            _uiState.update {
                it.copy(
                    currentScreen = screen,
                    canNavigateBack = backStack.isNotEmpty() || screen != AppScreen.HOME
                )
            }
        }
    }

    private fun resolveUpcomingWeekendFromSchedule(schedule: List<RaceWeekend>): RaceWeekend {
        val now = System.currentTimeMillis()
        return schedule.firstOrNull { weekend ->
            val raceSession = weekend.sessions.firstOrNull { it.isRace } ?: weekend.sessions.lastOrNull()
            val utc = raceSession?.utcEpochMillis
            if (utc != null) utc > now else weekend.results.isEmpty()
        } ?: SeedDataProvider.getNextRaceWeekend()
    }

    fun openLastCompletedRace() {
        deepLinkJob?.cancel()
        savedStateHandle[KEY_RACE_WEEKEND_TAB] = WeekendFilterTab.RACE_RESULTS.name
        val completed = _uiState.value.lastCompletedRace
        if (completed != null) {
            val label = formatWeekendLabel(completed)
            savedStateHandle[KEY_SELECTED_GP_NAME] = label
            _uiState.update {
                it.copy(
                    raceWeekend = completed,
                    selectedGrandPrix = label,
                    raceWeekendInitialTab = WeekendFilterTab.RACE_RESULTS,
                    navigationToken = it.navigationToken + 1
                )
            }
            navigateToInternal(AppScreen.RACE_WEEKEND, resetTabDefaults = false)
        } else {
            _uiState.update {
                it.copy(
                    raceWeekendInitialTab = WeekendFilterTab.RACE_RESULTS,
                    navigationToken = it.navigationToken + 1
                )
            }
            navigateToInternal(AppScreen.RACE_WEEKEND, resetTabDefaults = false)
            deepLinkJob = viewModelScope.launch {
                try {
                    val fetched = repository.getLatestCompletedRace(2026)
                    val label = formatWeekendLabel(fetched)
                    savedStateHandle[KEY_SELECTED_GP_NAME] = label
                    _uiState.update {
                        it.copy(
                            lastCompletedRace = fetched,
                            raceWeekend = fetched,
                            selectedGrandPrix = label
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // Keep existing state if fetch fails
                }
            }
        }
    }

    fun handleWidgetDeepLink(target: String) {
        deepLinkJob?.cancel()
        val normalized = target.uppercase().trim()
        when (normalized) {
            "CONSTRUCTOR_STANDINGS" -> {
                savedStateHandle[KEY_STANDINGS_TAB] = StandingsTab.CONSTRUCTORS.name
                _uiState.update {
                    it.copy(
                        standingsInitialTab = StandingsTab.CONSTRUCTORS,
                        navigationToken = it.navigationToken + 1
                    )
                }
                navigateToInternal(AppScreen.STANDINGS, resetTabDefaults = false)
            }
            "DRIVER_STANDINGS", "STANDINGS" -> {
                savedStateHandle[KEY_STANDINGS_TAB] = StandingsTab.DRIVERS.name
                _uiState.update {
                    it.copy(
                        standingsInitialTab = StandingsTab.DRIVERS,
                        navigationToken = it.navigationToken + 1
                    )
                }
                navigateToInternal(AppScreen.STANDINGS, resetTabDefaults = false)
            }
            "LAST_RACE_RESULTS" -> {
                openLastCompletedRace()
            }
            "NEXT_RACE", "WEEKEND_SCHEDULE", "RACE_WEEKEND", "WEEKEND", "SEASON" -> {
                savedStateHandle[KEY_RACE_WEEKEND_TAB] = WeekendFilterTab.WEEKEND_SCHEDULE.name
                savedStateHandle.remove<String>(KEY_SELECTED_GP_NAME)
                _uiState.update {
                    it.copy(
                        raceWeekendInitialTab = WeekendFilterTab.WEEKEND_SCHEDULE,
                        selectedGrandPrix = "",
                        navigationToken = it.navigationToken + 1
                    )
                }
                navigateToInternal(AppScreen.RACE_WEEKEND, resetTabDefaults = false)
                deepLinkJob = viewModelScope.launch {
                    try {
                        val nextWeekend = repository.getNextRaceWeekend()
                        _uiState.update { it.copy(raceWeekend = nextWeekend) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // Keep existing state
                    }
                }
            }
            "LIVE_TIMING", "LIVE" -> {
                navigateToInternal(AppScreen.LIVE_TIMING, resetTabDefaults = false)
            }
            "FAVORITE_TEAM", "SETTINGS", "SETTINGS_WIDGETS", "WIDGETS" -> {
                navigateToInternal(AppScreen.SETTINGS_WIDGETS, resetTabDefaults = false)
            }
            "HOME" -> {
                navigateToInternal(AppScreen.HOME, resetTabDefaults = false)
            }
            "DRIVER_PROFILES", "DRIVERS", "PROFILES" -> {
                navigateToInternal(AppScreen.DRIVER_PROFILES, resetTabDefaults = false)
            }
            "COMPARE_DRIVERS", "COMPARE" -> {
                navigateToInternal(AppScreen.COMPARE_DRIVERS, resetTabDefaults = false)
            }
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeLast()
            persistNavigationState(prev)
            _uiState.update {
                it.copy(
                    currentScreen = prev,
                    canNavigateBack = backStack.isNotEmpty() || prev != AppScreen.HOME
                )
            }
            return true
        } else if (_uiState.value.currentScreen != AppScreen.HOME) {
            persistNavigationState(AppScreen.HOME)
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

    private val gpYearRegex = Regex("\\b(19\\d{2}|20\\d{2})\\b")

    private fun formatWeekendLabel(rw: RaceWeekend): String =
        if (rw.grandPrixName.contains(rw.season.toString())) rw.grandPrixName
        else "${rw.grandPrixName} ${rw.season}"

    private fun isLocationCompatible(query: String, rw: RaceWeekend): Boolean {
        val mentionsMalaysia = query.contains("Malaysia", ignoreCase = true) ||
            query.contains("Sepang", ignoreCase = true) ||
            query.contains("Kuala Lumpur", ignoreCase = true)
        val rwIsMalaysia = rw.grandPrixName.contains("Malaysia", ignoreCase = true) ||
            rw.location.contains("Malaysia", ignoreCase = true) ||
            rw.circuitName.contains("Sepang", ignoreCase = true) ||
            rw.circuitType.equals("SEPANG", ignoreCase = true) ||
            rw.circuitId.equals("SEPANG", ignoreCase = true)
        if (mentionsMalaysia && !rwIsMalaysia) return false

        val mentionsSakhir = query.contains("Sakhir", ignoreCase = true) ||
            query.contains("Bahrain International", ignoreCase = true)
        if (mentionsSakhir && rwIsMalaysia) return false

        val onlyBahrainNoMalaysia = query.contains("Bahrain", ignoreCase = true) && !mentionsMalaysia
        if (onlyBahrainNoMalaysia && rwIsMalaysia) return false

        return true
    }

    private fun findBestWeekendMatch(
        candidates: List<RaceWeekend>,
        rawQuery: String,
        cleanQuery: String,
        explicitYear: Int?
    ): RaceWeekend? {
        val seasonFiltered = if (explicitYear != null) {
            candidates.filter { it.season == explicitYear }
        } else {
            candidates
        }
        if (seasonFiltered.isEmpty()) return null

        val qualifierQuery = cleanQuery.ifBlank { rawQuery }
        val compatible = seasonFiltered.filter { isLocationCompatible(qualifierQuery, it) }
        if (compatible.isEmpty()) return null

        // Exact normalized label, grandPrixName, circuitName, or location before any substring matching
        compatible.firstOrNull { rw ->
            formatWeekendLabel(rw).equals(rawQuery, ignoreCase = true) ||
                rw.grandPrixName.equals(rawQuery, ignoreCase = true) ||
                (cleanQuery.isNotBlank() && rw.grandPrixName.equals(cleanQuery, ignoreCase = true)) ||
                rw.circuitName.equals(rawQuery, ignoreCase = true) ||
                (cleanQuery.isNotBlank() && rw.circuitName.equals(cleanQuery, ignoreCase = true)) ||
                rw.location.equals(rawQuery, ignoreCase = true) ||
                (cleanQuery.isNotBlank() && rw.location.equals(cleanQuery, ignoreCase = true))
        }?.let { return it }

        if (cleanQuery.isBlank()) return null
        return compatible.firstOrNull { rw ->
            rw.grandPrixName.contains(cleanQuery, ignoreCase = true) ||
                cleanQuery.contains(rw.grandPrixName, ignoreCase = true) ||
                rw.circuitName.contains(cleanQuery, ignoreCase = true) ||
                cleanQuery.contains(rw.circuitName, ignoreCase = true) ||
                rw.location.contains(cleanQuery, ignoreCase = true) ||
                cleanQuery.contains(rw.location, ignoreCase = true)
        }
    }

    private fun resolveGrandPrix(gpName: String, schedule2026: List<RaceWeekend>): RaceWeekend {
        val query = gpName.trim()
        val explicitYear = gpYearRegex.find(query)?.groupValues?.get(1)?.toIntOrNull()
        val cleanQuery = query.replace(gpYearRegex, "").replace(Regex("\\s+"), " ").trim()

        return if (explicitYear != null && explicitYear != 2026) {
            // Historical year explicitly requested: avoid letting 2026 schedule entries hijack it
            when {
                explicitYear == 2023 && query.contains("Abu Dhabi", ignoreCase = true) ->
                    SeedDataProvider.getAbuDhabi2023Results()
                explicitYear == 2024 && (
                    query.contains("São Paulo", ignoreCase = true) ||
                        query.contains("Sao Paulo", ignoreCase = true) ||
                        query.contains("Brazilian", ignoreCase = true)
                    ) -> SeedDataProvider.getSaoPaulo2024Results()
                else -> {
                    val historicalSchedule = SeedDataProvider.getSeasonSchedule(explicitYear)
                    findBestWeekendMatch(historicalSchedule, query, cleanQuery, explicitYear)
                        ?: resolveGrandPrixFallback(query, explicitYear)
                }
            }
        } else {
            val activeSchedule2026 = schedule2026.ifEmpty { SeedDataProvider.getSeasonSchedule(2026) }
            findBestWeekendMatch(activeSchedule2026, query, cleanQuery, explicitYear)
                ?: resolveGrandPrixFallback(query, explicitYear)
        }
    }

    fun selectGrandPrix(gpName: String) {
        val schedule2026 = _uiState.value.fullSchedule2026.ifEmpty {
            SeedDataProvider.getSeasonSchedule(2026)
        }
        val weekend = resolveGrandPrix(gpName, schedule2026)
        val resolvedLabel = gpName.trim().ifEmpty { formatWeekendLabel(weekend) }
        savedStateHandle[KEY_SELECTED_GP_NAME] = resolvedLabel
        _uiState.update { it.copy(raceWeekend = weekend, selectedGrandPrix = resolvedLabel) }
    }

    private fun resolveGrandPrixFallback(query: String, explicitYear: Int?): RaceWeekend {
        return when {
            query.contains("Abu Dhabi", ignoreCase = true) -> {
                if (explicitYear == 2026) {
                    SeedDataProvider.getSeasonSchedule(2026)
                        .firstOrNull { it.grandPrixName.contains("Abu Dhabi", ignoreCase = true) }
                        ?: SeedDataProvider.getAbuDhabi2023Results()
                } else {
                    SeedDataProvider.getAbuDhabi2023Results()
                }
            }
            query.contains("São Paulo", ignoreCase = true) || query.contains("Sao Paulo", ignoreCase = true) -> {
                if (explicitYear == 2026) {
                    SeedDataProvider.getBrazilianGpWeekend2026()
                } else if (explicitYear != null) {
                    val historical = SeedDataProvider.getSeasonSchedule(explicitYear)
                    val clean = query.replace(gpYearRegex, "").replace(Regex("\\s+"), " ").trim()
                    findBestWeekendMatch(historical, query, clean, explicitYear)
                        ?: historical.firstOrNull { it.circuitType == "INTERLAGOS" }
                        ?: SeedDataProvider.getSaoPaulo2024Results()
                } else {
                    SeedDataProvider.getSaoPaulo2024Results()
                }
            }
            query.contains("Brazilian", ignoreCase = true) -> {
                if (explicitYear == null || explicitYear == 2026) {
                    SeedDataProvider.getBrazilianGpWeekend2026()
                } else {
                    val historical = SeedDataProvider.getSeasonSchedule(explicitYear)
                    val clean = query.replace(gpYearRegex, "").replace(Regex("\\s+"), " ").trim()
                    findBestWeekendMatch(historical, query, clean, explicitYear)
                        ?: historical.firstOrNull {
                            it.circuitType == "INTERLAGOS" ||
                                it.location.contains("Brazil", ignoreCase = true) ||
                                it.grandPrixName.contains("São Paulo", ignoreCase = true) ||
                                it.grandPrixName.contains("Sao Paulo", ignoreCase = true)
                        }
                        ?: SeedDataProvider.getSaoPaulo2024Results()
                }
            }
            query.contains("Bahrain", ignoreCase = true) ||
                query.contains("Sepang", ignoreCase = true) ||
                query.contains("Malaysia", ignoreCase = true) -> {
                if ((explicitYear == null || explicitYear == 2026) &&
                    query.contains("Bahrain Grand Prix", ignoreCase = true) &&
                    !query.contains("Malaysia", ignoreCase = true) &&
                    !query.contains("Sepang", ignoreCase = true)
                ) {
                    SeedDataProvider.getSeasonSchedule(2026)
                        .firstOrNull { it.grandPrixName.equals("Bahrain Grand Prix", ignoreCase = true) }
                        ?: SeedDataProvider.getNextRaceWeekend()
                } else {
                    SeedDataProvider.getNextRaceWeekend()
                }
            }
            query.contains("British", ignoreCase = true) || query.contains("Silverstone", ignoreCase = true) -> {
                val targetYear = explicitYear ?: 1950
                SeedDataProvider.getSeasonSchedule(targetYear).firstOrNull { it.circuitType == "SILVERSTONE" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            query.contains("Monaco", ignoreCase = true) -> {
                val targetYear = explicitYear ?: 1950
                SeedDataProvider.getSeasonSchedule(targetYear).firstOrNull { it.circuitType == "MONACO" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            query.contains("Monza", ignoreCase = true) || query.contains("Italian", ignoreCase = true) -> {
                val targetYear = explicitYear ?: 1950
                SeedDataProvider.getSeasonSchedule(targetYear).firstOrNull { it.circuitType == "MONZA" }
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
            else -> {
                val targetYear = explicitYear ?: 2026
                val schedule = SeedDataProvider.getSeasonSchedule(targetYear)
                val cleanQuery = query.replace(gpYearRegex, "").replace(Regex("\\s+"), " ").trim()
                findBestWeekendMatch(schedule, query, cleanQuery, explicitYear)
                    ?: SeedDataProvider.getNextRaceWeekend()
            }
        }
    }

    fun setYear(year: Int) {
        savedStateHandle[KEY_SELECTED_YEAR] = year
        _uiState.update {
            it.copy(
                selectedYear = year,
                isStandingsLoading = true,
                standingsError = null
            )
        }
        yearSelectionJob?.cancel()
        yearSelectionJob = viewModelScope.launch {
            try {
                val (standings, constructors) = coroutineScope {
                    val driversDeferred = async { repository.getDriverStandings(year) }
                    val constructorsDeferred = async { repository.getConstructorStandings(year) }
                    driversDeferred.await() to constructorsDeferred.await()
                }
                if (_uiState.value.selectedYear == year) {
                    val error = if (standings.isEmpty() && constructors.isEmpty()) {
                        "No standings data available for $year."
                    } else {
                        null
                    }
                    _uiState.update {
                        if (it.selectedYear == year) {
                            it.copy(
                                driverStandings = standings,
                                constructorStandings = constructors,
                                isStandingsLoading = false,
                                standingsError = error
                            )
                        } else {
                            it
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                if (_uiState.value.selectedYear == year) {
                    _uiState.update {
                        if (it.selectedYear == year) {
                            it.copy(
                                driverStandings = emptyList(),
                                constructorStandings = emptyList(),
                                isStandingsLoading = false,
                                standingsError = t.localizedMessage ?: "Failed to load $year standings."
                            )
                        } else {
                            it
                        }
                    }
                }
            } finally {
                if (_uiState.value.selectedYear == year) {
                    _uiState.update {
                        if (it.selectedYear == year && it.isStandingsLoading) {
                            it.copy(isStandingsLoading = false)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun setCompareDriver1(driver: DriverCareerComparison) {
        saveCompareDriver(KEY_COMPARE_DRIVER_1, driver)
        _uiState.update { it.copy(compareDriver1 = driver) }
    }

    fun setCompareDriver2(driver: DriverCareerComparison) {
        saveCompareDriver(KEY_COMPARE_DRIVER_2, driver)
        _uiState.update { it.copy(compareDriver2 = driver) }
    }

    fun swapCompareDrivers() {
        val current = _uiState.value
        val next1 = current.compareDriver2
        val next2 = current.compareDriver1
        saveCompareDriver(KEY_COMPARE_DRIVER_1, next1)
        saveCompareDriver(KEY_COMPARE_DRIVER_2, next2)
        _uiState.update {
            it.copy(
                compareDriver1 = next1,
                compareDriver2 = next2
            )
        }
    }

    private suspend fun findSessionByReminderKey(sessionKey: String): SessionScheduleItem? {
        val state = _uiState.value
        val candidates = buildList {
            add(state.raceWeekend)
            state.lastCompletedRace?.let(::add)
            addAll(state.fullSchedule2026)
        }
        candidates.forEach { weekend ->
            weekend.sessions.firstOrNull { session ->
                buildSessionReminderKey(weekend.season, weekend.round, session) == sessionKey
            }?.let { return it }
        }
        val season = sessionKey.substringBefore('_').toIntOrNull() ?: return null
        return try {
            repository.getSeasonSchedule(season).asSequence()
                .flatMap { weekend ->
                    weekend.sessions.asSequence().map { session ->
                        buildSessionReminderKey(weekend.season, weekend.round, session) to session
                    }
                }
                .firstOrNull { (key, _) -> key == sessionKey }
                ?.second
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    fun toggleSessionReminder(
        sessionKey: String,
        enabled: Boolean = sessionKey !in _uiState.value.reminderSessionKeys
    ) {
        viewModelScope.launch {
            try {
                val session = findSessionByReminderKey(sessionKey)
                val nowMillis = System.currentTimeMillis()
                if (enabled) {
                    val sessionEpoch = session?.utcEpochMillis
                    if (session == null || sessionEpoch == null || sessionEpoch <= nowMillis) {
                        emitFeedback("Cannot set reminder for past or unscheduled session")
                        return@launch
                    }
                }

                val appContext = getApplication<Application>().applicationContext
                preferencesMutex.withLock {
                    repository.setSessionReminder(sessionKey, enabled)
                }
                SessionReminderScheduler.syncReminders(appContext, repository)
                WidgetRefreshScheduler.triggerImmediateRefresh(appContext)

                val sessionLabel = session?.fullName?.ifBlank { session.shortName }
                val message = if (enabled) {
                    if (!sessionLabel.isNullOrBlank()) "Reminder set for $sessionLabel" else "Reminder set"
                } else {
                    if (!sessionLabel.isNullOrBlank()) "Reminder removed for $sessionLabel" else "Reminder removed"
                }
                emitFeedback(message)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                emitFeedback("Failed to update session reminder")
            }
        }
    }

    fun updatePreferences(newPrefs: UserPreferencesState, context: Context? = null) {
        _uiState.update { it.copy(userPreferences = newPrefs) }

        viewModelScope.launch {
            try {
                preferencesMutex.withLock {
                    preferencesStore.updateAllPreferences(newPrefs)
                }

                val appContext = (context ?: getApplication<Application>()).applicationContext
                SessionReminderScheduler.syncReminders(appContext, repository)
                WidgetRefreshScheduler.triggerImmediateRefresh(appContext)

                val widgets = listOf(
                    com.reva2473.f1widget.widget.NextRaceCountdownWidget(),
                    com.reva2473.f1widget.widget.WeekendScheduleWidget(),
                    com.reva2473.f1widget.widget.DriverStandingsWidget(),
                    com.reva2473.f1widget.widget.ConstructorStandingsWidget(),
                    com.reva2473.f1widget.widget.FavoriteDriverWidget(),
                    com.reva2473.f1widget.widget.FavoriteTeamWidget(),
                    com.reva2473.f1widget.widget.LastRacePodiumWidget(),
                    com.reva2473.f1widget.widget.LiveTimingBannerWidget()
                )
                coroutineScope {
                    widgets.map { widget ->
                        async {
                            try {
                                widget.updateAll(appContext)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Throwable) {
                                // Ignore widget update errors if widgets aren't currently bound on home screen
                            }
                        }
                    }.awaitAll()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Ignore DataStore or widget update errors
            }
        }
    }
}
