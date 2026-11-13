# ApexGrid Codebase Audit — Issues Report (All Resolved)

> **Workspace:** `c:\Users\Ranja\Desktop\projects\apexgrid`  
> **Verification Checks (After All Fixes):**
> - `./gradlew testDebugUnitTest --rerun-tasks`: **PASS** (20 tests in `F1RepositoryTest`, 0 failures, 0 skipped)
> - `./gradlew lintDebug`: **PASS** (0 errors)
> - `./gradlew assembleDebug`: **PASS** (`compileDebugKotlin`: **0 errors, 0 deprecation warnings**)

---

## Critical (All Fixed)

### 1. [FIXED] Test-coupled stack-trace branching in production code
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/data/seed/SeedDataProvider.kt` (`getLiveSessionState`)
  - `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt` (`getLiveSessionState`)
  - `app/src/test/java/com/reva2473/f1widget/F1RepositoryTest.kt`
- **Resolution:** Removed `Thread.currentThread().stackTrace` inspection from `SeedDataProvider.getLiveSessionState()`, added an explicit `sessionKey: String = "SAO_PAULO"` parameter to both `SeedDataProvider` and `F1Repository`, and updated `F1RepositoryTest` to pass explicit session keys.

### 2. [FIXED] 2026 standings bypass live API & historical fallback returns 2026 data
- **File:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
  - `getDriverStandings`, `getConstructorStandings`
- **Resolution:**
  - Removed the `if (season == 2026)` pre-API short-circuit so live 2026 standings query `jolpicaApi` first and only fall back to `SeedDataProvider.getDriverStandings2026()` / `getConstructorStandings2026()` when offline/unseeded.
  - Added `season < 1958` check in `getConstructorStandings` returning `emptyList()`, and routed historical fallbacks (`season != 2026`) through `getHistoricalDriverStandings(season)` / `getHistoricalConstructorStandings(season)` (returning `emptyList()` when unseeded rather than 2026 standings).

### 3. [FIXED] `CancellationException` swallowed in suspend functions
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`
- **Resolution:** Added explicit `catch (e: CancellationException) { throw e }` blocks before general `Exception` / `Throwable` catch blocks across all suspend functions and coroutines, verified by `testCancellationExceptionNotSwallowedInSuspendFunctions`.

### 4. [FIXED] Hardcoded mock data overrides passed/selected state across UI screens
- **`HomeScreen.kt`** (`app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt`):
  - `HeroSlideNextRace`, `HeroSlideLastRaceWinner`, `HomeBentoCard`, and `LiveTrackSummaryCard` now bind dynamically to `weekend`, `driverStandings`, `liveSession`, and `lastRaceResults`, including dynamic circuit outline selection (`CircuitOutlineCanvas`).
- **`RaceWeekendScreen.kt`** (`app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt`):
  - `WeekendCircuitHeaderCard`, `RaceInfoSection(weekend, results)`, and `CircuitInfoSection(weekend)` now bind dynamically to the active `RaceWeekend` and compute mathematically consistent `Race Distance` (`laps * lengthKm`).
- **`LiveTimingScreen.kt`** (`app/src/main/java/com/reva2473/f1widget/ui/screens/live/LiveTimingScreen.kt`):
  - `TrackMapCard` dynamically binds `sessionState.circuitId`, sector lengths, and `sessionState.trackStatus`; `TimingTowerDriverCard` reads `driver.isOutLap` directly; overtakes empty state formats `sessionState.sessionName.lowercase()`.
- **`SettingsAndWidgetsScreen.kt`** (`app/src/main/java/com/reva2473/f1widget/ui/screens/settings/SettingsAndWidgetsScreen.kt`):
  - Showcase cards and `WidgetLivePreviewCard` (`FAVORITE_TEAM`, `FAVORITE_DRIVER`, `WDC_STANDINGS`, `WCC_STANDINGS`) now accept and bind `driverStandings` and `constructorStandings` consistently with `StandingsScreen` and user preferences.

### 5. [FIXED] Track map gesture detector swallows Home card clicks
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/components/CircuitCanvas.kt` (`LiveTrackMapCanvas`)
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt` (`LiveTrackSummaryCard`)
- **Resolution:** Made `onDriverTap: ((String) -> Unit)? = null` nullable on `LiveTrackMapCanvas` and only attached `.pointerInput(onDriverTap)` when `onDriverTap != null`, allowing taps on the right half of `LiveTrackSummaryCard` on `HomeScreen` to trigger `onOpenLiveTiming()`.

---

## High (All Fixed)

### 6. [FIXED] Concurrency race conditions in `MainViewModel` and `F1Database`
- **`setYear(year)` race (`MainViewModel.kt`):** Added `yearSelectionJob: Job?`, cancelled prior year-load coroutines on rapid year changes, and guarded state updates so older responses never overwrite newer `selectedYear` selections.
- **`updatePreferences` race (`MainViewModel.kt` & `F1Database.kt`):** Added `UserPreferencesStore.updateAllPreferences(prefs)` to commit all 8 preference keys in a single atomic DataStore transaction, and awaited completion before calling `updateAll(context)` on Glance widgets.
- **Double-checked locking (`F1Database.kt`):** Re-checked `INSTANCE` inside `synchronized(this)` in `F1Database.getInstance` and moved `cacheDir.mkdirs()` into `ensureCacheDir()` on `Dispatchers.IO`.

### 7. [FIXED] Glance widgets ignore preferences/repository, overflow `minHeight`, and misuse cutouts
- **File:** `app/src/main/java/com/reva2473/f1widget/widget/F1GlanceWidgets.kt` & `app/src/main/res/xml/widget_*.xml`
- **Resolution:**
  - Connected all Glance widgets to `UserPreferencesStore` and `F1Repository` with seed fallback (`FavoriteDriverWidget` uses `prefs.favoriteDriverCode`; `FavoriteTeamWidget` uses `prefs.favoriteTeam`).
  - Used `getDriverHeadshotRes` inside circular avatars and replaced `R.drawable.driver_ver` fallback with a clean driver-code badge when unmapped.
  - Updated `NextRaceCountdownWidget` to explicitly select the Race session (`it.isRace || it.fullName.equals("Race", true)`).
  - Set `override val sizeMode: SizeMode = SizeMode.Exact`, tuned paddings/heights and XML `minHeight`/`targetCellHeight`, and passed unique `EXTRA_TARGET_SCREEN` deep-link intents.

### 8. [FIXED] Missing 2026 driver & Cadillac assets in `F1AssetMapper.kt`
- **Files:** `app/src/main/java/com/reva2473/f1widget/ui/components/F1AssetMapper.kt` & `app/src/main/res/drawable-nodpi/`
- **Resolution:** Added `driver_bot.png`, `driver_full_bot.png`, `driver_lin.png`, `driver_full_lin.png`, `driver_per.png`, `driver_full_per.png`, and `car_cadillac.png`, mapped `BOT`, `LIN`, `PER`, and `F1TeamId.CADILLAC` in `F1AssetMapper.kt`, and removed the 5 unused duplicate car PNGs.

### 9. [FIXED] UTC dates/times never converted to local timezone & stale cached countdowns
- **File:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
- **Resolution:** Parsed session dates/times at `ZoneOffset.UTC`, converted to `ZoneId.systemDefault()`, sorted sessions chronologically, dynamically recomputed `daysToGo` and `countdownBadge` on cache reads, formatted full weekend date ranges, and added real circuit specs lookup for all 24 championship circuits.

### 10. [FIXED] Dead OpenF1 integration & non-atomic cache fallback
- **Files:** `F1Repository.kt` & `F1Database.kt`
- **Resolution:** Wired `openF1Api` into `getLiveSessionState()` to enrich weather and race control data when online, shared a single configured `OkHttpClient` with 15s timeouts, and ensured `atomicWrite` cleans up `.tmp` files in `try/catch` while using `Files.move(..., ATOMIC_MOVE)` with safe fallback.

### 11. [FIXED] Theme modes, accent contrast, and bottom navigation bugs
- **Files:** `Theme.kt`, `SharedComponents.kt`, `HomeScreen.kt`, `RaceWeekendScreen.kt`, `SettingsAndWidgetsScreen.kt`
- **Resolution:** Added the 4th `"Standings"` item to `FloatingPillBottomBar`, computed luminance-aware `onAccentColor` on team-colored pills, propagated `LocalF1Accent.current.primaryAccent`, and bound `P1TRadialBackground` / `FloatingPillBottomBar` to `MaterialTheme.colorScheme` for `"LIGHT"` / `"SYSTEM"` theme support.

### 12. [FIXED] Sorting, formatting, and dead-control bugs in UI screens
- **Files:** `RaceWeekendScreen.kt`, `StandingsScreen.kt`, `DriverProfilesAndCompareScreen.kt`, `SettingsAndWidgetsScreen.kt`, `HomeScreen.kt`, `LiveTimingScreen.kt`
- **Resolution:** Sorted `QUALIFYING` tab by qualifying rank, removed `.take(4)` on `PIT_STOPS` tab, clamped pit stop counts with `coerceAtLeast(0)`, null-checked `gapToPrev`, avoided double `-` on `gapToP1`, inverted comparison bar ratios when `lowerIsBetter = true`, and wired up `MoreVert` menus, reminder bells, community links, audio mute feedback, and `requestPinAppWidget`.

### 13. [FIXED] Activity `launchMode`, `MainUiState` crash risk, & broken ProGuard config
- **Files:** `AndroidManifest.xml`, `MainActivity.kt`, `MainViewModel.kt`, `app/build.gradle.kts`, `app/proguard-rules.pro`
- **Resolution:** Added `android:launchMode="singleTop"`, guarded `handleIntent(intent)` with `if (savedInstanceState == null)`, switched to `collectAsStateWithLifecycle()`, replaced `[0]` indexing with `.firstOrNull() ?: defaultFallbackDriver`, and updated `proguard-rules.pro` for `kotlinx.serialization` and model packages.

### 14. [FIXED] Unit test coverage gaps
- **File:** `app/src/test/java/com/reva2473/f1widget/F1RepositoryTest.kt`
- **Resolution:** Removed `sun.misc.Unsafe` reflection and expanded the test suite to 20 unit tests covering DTO deserialization with missing/Double fields, fake `JolpicaApiService` network mapping, mid-season constructor switch (`lastOrNull()`), UTC-to-local timezone conversion, expired cache fallback, `F1Database` concurrency/atomic writes, and single-transaction `UserPreferencesStore` updates.

---

## Medium & Low (All Fixed)

- **[FIXED] Data, Network & Persistence:** Added safe default values across all Jolpica/OpenF1 DTOs, increased `limit = 1000`, changed `OpenF1WeatherDto` `rainfall`/`windDirection` to `Double`, used `constructors.lastOrNull()`, preserved half-points, sorted `get2026FullSchedule()` (`1..24`) and `getBrazilianGpWeekend2026()` chronologically, fixed Norris's `last3Summary`, and matched `carsOnTrack` to `!isInPit`.
- **[FIXED] ViewModel & Activity:** Deduplicated bottom tab routing in `MainActivity.kt`, prevented duplicate consecutive `backStack` entries, expanded `selectGrandPrix` across all 24 schedule rounds, and removed unused manifest permissions.
- **[FIXED] Compose UI, Canvas & Deprecations:** Eliminated all 15 Kotlin deprecation warnings (`AutoMirrored` icons & `HorizontalDivider`), fixed `.padding().size()` modifier order, cached circuit `Path` objects in `CircuitCanvas.kt`, fixed Sepang `startFinishPos`, added `rememberSaveable` and `LazyColumn` keys, and made `LiveTimingTopBar` track status colors dynamic.
- **[FIXED] Glance Widgets, Resources, Build & Lint:** Added `widget_loading_bg.xml` to resolve `Overdraw`, added `previewLayout` to all widget XMLs, registered `LiveTimingBannerWidgetReceiver`, removed unused Room/OkHttp-logging dependencies and unused colors/drawables, and fixed `DefaultLocale`, `ModifierParameter`, `AutoboxingStateCreation`, and `ObsoleteSdkInt` lint warnings.
