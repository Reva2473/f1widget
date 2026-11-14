# ApexGrid Codebase Audit — Issues Report

> **Workspace:** `c:\Users\Ranja\Desktop\projects\apexgrid`  
> **Verification Checks (Latest Run):**
> - `./gradlew testDebugUnitTest --rerun-tasks`: **PASS** (28 tests in `F1RepositoryTest`, 0 failures, 0 errors, 0 skipped)
> - `./gradlew assembleDebug --rerun-tasks`: **PASS** (`compileDebugKotlin` & `compileDebugUnitTestKotlin`: **0 errors, 0 deprecation/compiler warnings**)
> - `./gradlew lintDebug --rerun-tasks`: **PASS** (0 errors, 63 warnings — `GradleDependency`: 36, `UnusedAttribute`: 24, `AndroidGradlePluginVersion`: 3; 0 `ModifierParameter`, 0 `UnusedResources`, 0 `ObsoleteSdkInt`, 0 `MonochromeLauncherIcon`)

---

## Comprehensive Audit Issues (Identified & Resolved 2026-11-13)

### Critical

#### A1. [FIXED] Duplicate `LazyColumn` Keys & Mis-mapped Historical Constructors
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/domain/model/F1Models.kt` (`F1TeamId.fromName`)
  - `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt` (`getConstructorStandings`)
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/standings/StandingsScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/live/LiveTimingScreen.kt`
- **Resolution:** Added `F1TeamId.OTHER`, expanded `fromName` aliases (including `toro_rosso`, `alfa_romeo`, `force_india`, `racing_point`, `lotus`, `tyrrell`, `brawn`), grouped constructor drivers by exact constructor ID/name instead of collapsed `F1TeamId`, and keyed `LazyColumn` items in `StandingsScreen` and `LiveTimingScreen` on unique constructor/driver/overtake identifiers + index.

#### A2. [FIXED] "Last Grand Prix Winner" Hero Slide Shows Upcoming Race + Hardcoded Winner
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/MainActivity.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt` (`HomeScreen`, `HeroSlideLastRaceWinner`)
- **Resolution:** Passed `state.lastCompletedRace` from `MainActivity` to `HomeScreen` and bound `HeroSlideLastRaceWinner` strictly to `lastCompletedRace` / explicit `lastRaceResults` with a clean empty/pending fallback rather than upcoming weekend data or hardcoded Verstappen stats.

#### A3. [FIXED] Showcase & Preview Widgets Hardcode Teams, Colors, and Spliced Data
- **File:** `app/src/main/java/com/reva2473/f1widget/ui/screens/settings/SettingsAndWidgetsScreen.kt`
- **Resolution:** Bound `TeamFormAndTeammatesWidgetShowcase`, `ConstructorsBarChartWidgetShowcase`, `WccSquareCardsShowcase`, `LiveSessionBannerWidgetShowcase`, and `WidgetLivePreviewCard` (`LAST_RACE_PODIUM`, `FAVORITE_TEAM`, etc.) to `userPreferences.favoriteTeam`, live standings, `liveSession`, `nextWeekend`, and `lastCompletedRace` with proper team codes (`VRB`) and aligned teammate results.

---

### High

#### A4. [FIXED] 2026 Offline Seed Data Poisons Disk Cache for 6 Hours (`DEFAULT_TTL_MS`)
- **Files:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`, `app/src/main/java/com/reva2473/f1widget/data/local/F1Database.kt`
- **Resolution:** Stopped writing `SeedDataProvider` fallbacks for `2026` driver/constructor standings into `F1Database`, ensuring subsequent online refreshes query `jolpicaApi` rather than serving stale seed fixtures from disk cache.

#### A5. [FIXED] OpenF1 Live Enrichment Creates Hybrid State & Misconverts Weather Units
- **Files:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`, `app/src/main/java/com/reva2473/f1widget/data/remote/OpenF1ApiService.kt`
- **Resolution:** Converted `OpenF1WeatherDto.windSpeed` from `m/s` to `km/h` (`* 3.6`), mapped `rainfall > 0.0` to `rainfallPercent`, populated `windDirection`, generated stable `RaceControlMessage.id` values, and made `OpenF1StintDto.compound` nullable (`null` default instead of `"MEDIUM"`).

#### A6. [FIXED] `getLatestCompletedRace` Wrong-Season Fallback & `daysToGo == 0` Ambiguity
- **Files:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`, `app/src/main/java/com/reva2473/f1widget/domain/model/F1Models.kt`
- **Resolution:** Guarded `getLatestCompletedRace(season)` by actual race completion timestamps rather than clamped `daysToGo == 0`, avoided cross-season 2024 fallback for 2026, preserved fractional/half-points, and fetched `QualifyingResults` when mapping race results.

#### A7. [FIXED] Missing `SprintQualifying` DTO Field & Uncoordinated `context == null` Singletons
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/data/remote/JolpicaApiService.kt` (`RaceDto`)
  - `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
  - `app/src/main/java/com/reva2473/f1widget/data/local/F1Database.kt`
- **Resolution:** Added `@SerialName("SprintQualifying")` and `@SerialName("SprintShootout")` to `RaceDto` and mapped them in `F1Repository`; added canonical-path-keyed `UserPreferencesStore.forFallbackFile` and `F1Database.getInstance(cacheDir)` (`instancesByPath`) so coexisting fallback/test instances share coordinated DataStore and DB singletons.

#### A8. [FIXED] Navigation, Backstack & State Persistence Bugs
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/standings/StandingsScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/MainActivity.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/live/LiveTimingScreen.kt`
- **Resolution:** Routed `StandingsScreen` `"Home"` clicks to `onTabSelected("Home")`, routed WDC/WCC hero slides in `HomeHeroPager` to `onStandingsTap`, unified `FloatingPillBottomBar` outside `AnimatedContent`, cleared `backStack` when navigating to `AppScreen.HOME`, wired `SavedStateHandle` into `MainViewModel`, processed deep-link intents via `onNewIntent`/recreation-safe `handleIntent`, and switched local tab/filter states to `rememberSaveable`.

#### A9. [FIXED] 60–120 FPS Recompositions & Allocations in `Canvas` `onDraw`
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/components/CircuitCanvas.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/components/SharedComponents.kt` (`P1TRadialBackground`)
- **Resolution:** Hoisted circuit geometry, `CachedCircuitPath`, `PathMeasure`, `Paint`, and radial gradient `Brush` allocations outside `Canvas` `onDraw` using `remember(...)` and deferred animated pulse reads into the draw phase.

#### A10. [FIXED] Year Change Stale State, Fabricated Career Stats & Broken Compare Formatting
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/standings/StandingsScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/MainActivity.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/drivers/DriverProfilesAndCompareScreen.kt`
- **Resolution:** Added loading/error state (`standingsError`) and year-synchronized loading in `MainViewModel.setYear`, removed fabricated DNF/fastest-lap formulas in `MainActivity`, fixed `DriverProfilesAndCompareScreen` leader/gap formatting, rate formatting, `0 vs 0` bar handling, empty search states, and light-mode contrast in `SplitHeroBanner`.

#### A11. [FIXED] Non-Functional Notifications & Session Reminders
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/notifications/SessionReminderManager.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/settings/SettingsAndWidgetsScreen.kt`
  - `app/src/main/AndroidManifest.xml`
- **Resolution:** Implemented `SessionReminderScheduler` and `SessionReminderReceiver` using `NotificationChannel` (`apexgrid_session_reminders`), `AlarmManager.setAndAllowWhileIdle` 15 minutes before sessions, `DataStore`-persisted `reminder_session_keys`, runtime `POST_NOTIFICATIONS` permission checks, and `RECEIVE_BOOT_COMPLETED` resynchronization.

#### A12. [FIXED] Glance Widget Bitmap IPC Risk, Missing Periodic Refresh, & Sizing/Preview Issues
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/widget/F1GlanceWidgets.kt`
  - `app/src/main/java/com/reva2473/f1widget/worker/WidgetRefreshWorker.kt`
  - `app/src/main/res/xml/widget_*.xml`
  - `app/src/main/res/layout/preview_widget_*.xml`
  - `app/src/main/res/drawable/preview_widget_*.xml`
  - `app/src/main/java/com/reva2473/f1widget/domain/model/F1Models.kt` (`WidgetStyleType`)
- **Resolution:** Downsampled & `LruCache`-backed widget car/driver bitmaps to prevent `TransactionTooLargeException`, added `WidgetRefreshWorker` + `WidgetRefreshScheduler` (30-minute `WorkManager` periodic refresh), wired `WidgetPalette` (`DARK`/`LIGHT`/`SYSTEM` + `useTeamColorAccent`) and `LocalSize.current` responsive sizing across all 8 widgets, added `LIVE_TIMING_BANNER` to `WidgetStyleType` and Settings' pin flow, and added dedicated `previewImage` + `previewLayout` resources and compliant `minWidth`/`minHeight` (`110dp`+) across all 8 `widget_*.xml` files.

#### A13. [FIXED] Release Build Minification/Signing & Edge-to-Edge Theme Config
- **Files:** `app/build.gradle.kts`, `app/src/main/MainActivity.kt`, `app/src/main/res/values/themes.xml`
- **Resolution:** Enabled `isMinifyEnabled = true` and `isShrinkResources = true` for `release` builds with optional `keystore.properties` / `ANDROID_KEYSTORE_*` signing config, switched `MainActivity` to `enableEdgeToEdge(...)` with window insets handling, and removed deprecated status/nav bar color items from `themes.xml`.

---

### Medium

#### A14. [FIXED] Broken Widget Deep-Link Sub-Routing & Missing Timeout
- **Files:** `app/src/main/java/com/reva2473/f1widget/widget/F1GlanceWidgets.kt`, `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`, `app/src/main/java/com/reva2473/f1widget/ui/screens/standings/StandingsScreen.kt`
- **Resolution:** Routed `LastRacePodiumWidget` (`"LAST_RACE_RESULTS"`) to `RaceWeekendScreen` (`RACE_RESULTS` tab with `lastCompletedRace`), routed `"CONSTRUCTOR_STANDINGS"` to `StandingsTab.CONSTRUCTORS`, and wrapped widget repository fetches in `withTimeoutOrNull(WIDGET_NETWORK_TIMEOUT_MS)`.

#### A15. [FIXED] Race Weekend Fake Fallbacks & Wrong Circuit Outlines
- **Files:** `app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt`, `app/src/main/java/com/reva2473/f1widget/ui/components/CircuitCanvas.kt`
- **Resolution:** Replaced fake Brazil Sprint fallback rows with empty states, populated `gpOptions` dynamically from season schedule, added `BahrainCircuitGeometry` and championship circuit mappings in `CircuitCanvas.kt`, and guarded blank `qualifyingPosition` (`"—"`) and `fastestLapPosition == "P1"` checks in `RaceWeekendScreen`.

#### A16. [FIXED] Team Radio Pause/Mute Bugs & Live Ticker Behavior
- **Files:** `app/src/main/java/com/reva2473/f1widget/ui/screens/live/LiveTimingScreen.kt`, `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`, `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt`
- **Resolution:** Preserved radio playback progress across pause/resume, bound mute state to playback feedback, stopped moving pitted cars (`!isInPit`) and empty-grid ticks in `MainViewModel`, serialized preference updates, and memoized fallback lists in `HomeScreen`.

#### A17. [FIXED] Row Layout Overflow, Clipping & Gesture/Hit-Test Issues
- **Files:** `LiveTimingScreen.kt`, `StandingsScreen.kt`, `HomeScreen.kt`, `SettingsAndWidgetsScreen.kt`, `CircuitCanvas.kt`
- **Resolution:** Added `Modifier.weight(1f)` to flexible row text columns, added `fillMaxHeight()` alignment in `HomeBentoCard`, removed restrictive fixed clipping sizes in `SettingsAndWidgetsScreen`, and switched `LiveTrackMapCanvas` tap hit-testing to `.pointerInput(Unit)` with `rememberUpdatedState` and snapshot coordinate targets.

#### A18. [FIXED] Jolpica Pagination, Timezone/Date Parsing & Standings Formatting
- **Files:** `app/src/main/java/com/reva2473/f1widget/data/remote/JolpicaApiService.kt`, `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
- **Resolution:** Added `offset`-based pagination across Jolpica schedule/standings endpoints, parsed non-`Z` ISO offsets via `OffsetTime.parse` / `OffsetDateTime.parse`, re-localized cached weekend countdowns on read, added `forceRefresh` support to `getSeasonSchedule`, and cleaned up points/gap formatting.

#### A19. [FIXED] Seed Data Contradictions & Factual Errors
- **File:** `app/src/main/java/com/reva2473/f1widget/data/seed/SeedDataProvider.kt`
- **Resolution:** Corrected duplicate/missing race positions, aligned `recentTeamForm` and `seasonWins` counts, sorted 2026 schedule dates chronologically, fixed Bahrain Q1 gaps/sector sums/P11–P22 rows/overtake data, and corrected 1950 British GP Alfa Romeo team mapping.

#### A20. [FIXED] Asset & Preview Parity Gaps
- **Files:** `app/src/main/java/com/reva2473/f1widget/ui/components/F1AssetMapper.kt`, `preview/index.html`, `preview/assets/cars/cadillac.png`
- **Resolution:** Added `F1TeamId.OTHER` (`getTeamCarResOrNull` returns `null`, `getTeamCarRes` safely falls back), added `CADILLAC`/`BOT`/`LIN`/`PER` parity in `preview/index.html` and `preview/assets/cars/cadillac.png`, and improved `DriverPortraitAvatar` fallback initials/codes for unmapped/historical drivers.

#### A21. [FIXED] CI, ProGuard, Backup & Unused Test BOM
- **Files:** `.github/workflows/android-ci.yml`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/build.gradle.kts`
- **Resolution:** Updated CI to use `./gradlew` with `permissions: contents: read`, `lintDebug`, and `assembleDebug assembleRelease`; tightened `proguard-rules.pro` (removed `-dontwarn` and broad package keeps); excluded `f1_offline_cache` from Android backup/extraction rules; and removed unused `androidTestImplementation(platform(libs.androidx.compose.bom))`.

---

### Low & Tech Debt

#### A22. [FIXED] Contrast, Touch Targets & Dropdown Performance
- **Files:** `Theme.kt`, `SharedComponents.kt`, `LiveTimingScreen.kt`, `StandingsScreen.kt`
- **Resolution:** Lightened `P1TColors.TextMuted` (`0xFF8E8E9B`) and strengthened `adaptiveTeamText`, enlarged interactive icon/tab touch targets to `48.dp` compliance, and converted `StandingsScreen` year selector to lazy `LazyColumn` / `LazyVerticalGrid`.

#### A23. [FIXED] Unused Code, Minor Formatting & Build Cleanup
- **Files:** `MainViewModel.kt`, `HomeScreen.kt`, `StandingsScreen.kt`, `app/build.gradle.kts`, `app/src/main/res/mipmap-anydpi/`, `app/src/main/res/mipmap-anydpi-v33/`
- **Resolution:** Cleaned up unused ViewModel methods, fixed `HomeBentoCard` fastest-lap/tie formatting and `StandingsScreen` half-point formatting, removed redundant `useSupportLibrary` and `kotlinx-coroutines-core` dependency entries, and placed `<monochrome>` adaptive icon definitions in `mipmap-anydpi-v33/`.

#### A24. [FIXED] Unit Test Gaps & Documentation Drift
- **Files:** `app/src/test/java/com/reva2473/f1widget/F1RepositoryTest.kt`, `.agents/HANDOFF.md`, `README.md`
- **Resolution:** Expanded `F1RepositoryTest` to 28 passing unit tests (covering DTO parsing, `F1Database` atomic writes/double-checked locking, `UserPreferencesStore` single-transaction updates, OpenF1 timestamp/offline-cache behavior, and constructor driver separation) and synchronized `README.md` and `.agents/HANDOFF.md` with the 8-widget Glance suite, JSON file cache + `DataStore` + `WorkManager` + `AlarmManager` architecture, `F1AssetMapper.kt`, manifest permissions, and `assembleRelease` CI verification.

---

## Open / Remaining Issues (Resolved 2026-11-13 — Follow-Up Check)

### High / Medium

#### N1. [FIXED] GP & circuit routing edge cases in `RaceWeekendScreen`, `MainViewModel`, and `HomeScreen`
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt`
- **Resolution:** Added location/circuit compatibility checks (`isLocationCompatible`) across `RaceWeekendScreen.effectiveWeekend` and `MainViewModel.findBestWeekendMatch`/`resolveGrandPrixFallback`, ordered exact `schedule`/`weekend` matches before loose matches, routed historical `"Brazilian"` / `"São Paulo"` queries through historical schedules instead of 2026, updated `HomeScreen.LiveTrackSummaryCard` to fall back from `circuitId` to `circuitType`, and passed resolved `circuitId` directly into `CircuitOutlineCanvas` in `HeroSlideNextRace`.

#### N2. [FIXED] Host-timezone flakiness in `F1RepositoryTest`
- **File:** `app/src/test/java/com/reva2473/f1widget/F1RepositoryTest.kt` (`testF1RepositoryWithFakeJolpicaApiServiceMidSeasonSwitchAndUtcTime`)
- **Resolution:** Replaced hardcoded `"FRI"` / `"SUN"` assertions with `ZoneId.systemDefault()` conversion computed dynamically from `"2026-11-06T15:30:00Z"` and `"2026-11-08T17:00:00Z"`.

#### N3. [FIXED] Sequential network fetch & mixed live/seed data in `F1Repository.getConstructorStandings`
- **File:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
- **Resolution:** Launched `jolpicaApi.getConstructorStandings` and `mapDriverStandings(jolpicaApi.getDriverStandings(...))` concurrently via `coroutineScope` + `async` without falling back to `SeedDataProvider` on partial driver-standings failure (keeping `drivers = emptyList()` instead of caching hybrid live/seed payloads). Verified in `F1RepositoryTest.testGetConstructorStandingsDoesNotMixSeedDriversWhenDriverStandingsFails`.

#### N4. [FIXED] `SettingsAndWidgetsScreen` showcase & live preview gaps
- **File:** `app/src/main/java/com/reva2473/f1widget/ui/screens/settings/SettingsAndWidgetsScreen.kt`
- **Resolution:** Added `driverStandings` -> `SeedDataProvider` fallback resolution in `WidgetLivePreviewCard` (`WidgetStyleType.FAVORITE_TEAM`) when `teamStanding?.drivers` is empty, and added the `mclGapLabel` badge via `gapLabel(...)` to Card 4 (McLaren) in `WccSquareCardsShowcase`.

### Medium & Low

#### N5. [FIXED] Remaining Light-mode contrast & Home Red Flag color issues
- **Files:** `DriverProfilesAndCompareScreen.kt`, `StandingsScreen.kt`, `HomeScreen.kt`, `Theme.kt`
- **Resolution:** Strengthened `P1TTheme.adaptiveTeamText` (`lum <= 0.12f`, target `0.11`) and added `P1TTheme.redFlagBorder` and `P1TTheme.poleGold` in `Theme.kt`; toned down `SplitHeroBanner` bright gradient stops over `SurfaceCard` in `DriverProfilesAndCompareScreen.kt` and switched `"View all years"` icon/text to `P1TTheme.accentRed`; replaced `P1TColors.AccentRed` and `Color(0xFFB58900)` in `StandingsScreen.kt` with `P1TTheme.accentRed` and `P1TTheme.poleGold`; and updated `HomeScreen.LiveTrackSummaryCard` to color Red Flags with `P1TTheme.accentRed` / `P1TTheme.redFlagBg` / `P1TTheme.redFlagBorder`.

#### N6. [FIXED] Unkeyed `StandingsScreen` expansion state & `CircuitCanvas` Haas `Paint` allocation
- **Resolution:** Passed `selectedYear` into `DriverStandingCard` and `ConstructorStandingCard` and keyed their `rememberSaveable` and `LazyColumn` items on `selectedYear` in `StandingsScreen.kt`; hoisted `haasPaint` via `remember` outside `Canvas` `onDraw` in `TeamLogoBadge` in `CircuitCanvas.kt`.

#### N7. [FIXED] Seed Round 16 date collision
- **File:** `app/src/main/java/com/reva2473/f1widget/data/seed/SeedDataProvider.kt`
- **Resolution:** Shifted Round 15 Dutch GP to `"FRI 21 AUG - SUN 23 AUG"` and Round 16 (`getNextRaceWeekend()`) to `"FRI 28 AUG - SUN 30 AUG"` with consistent session dates before Round 17 (`"FRI 04 SEP - SUN 06 SEP"`) and Round 18 (`"FRI 18 SEP - SUN 20 SEP"`).

---

## Open / Remaining Issues (Resolved 2026-11-13)

### High

#### O1. [FIXED] `RaceWeekend.circuitId` default (`"SEPANG"`) overrides `circuitType` (`"INTERLAGOS"`, `"YAS_MARINA"`, etc.)
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/domain/model/F1Models.kt`
  - `app/src/main/java/com/reva2473/f1widget/data/seed/SeedDataProvider.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/home/HomeScreen.kt`
- **Resolution:** Changed `RaceWeekend.circuitId` default to `""` in `F1Models.kt` and set explicit `circuitId`/`circuitType` values across `SeedDataProvider`, so `circuitId.ifBlank { circuitType }` resolves the actual circuit. Verified in `F1RepositoryTest.testRaceWeekendDefaultCircuitIdDoesNotShadowCircuitType`.

#### O2. [FIXED] GP selector substring matching hijacks historical (2023/2024) & Bahrain 2026 selections
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt` (`selectGrandPrix`, `findBestWeekendMatch`, `resolveGrandPrixFallback`)
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/weekend/RaceWeekendScreen.kt` (`effectiveWeekend`, `WeekendScheduleSection`, `RaceResultExpandedCard`, `EmptyWeekendTabCard`)
- **Resolution:** Extracted explicit 4-digit year tokens and matched season-aware exact labels/names/circuits before substring fallbacks across both `MainViewModel.selectGrandPrix` and `RaceWeekendScreen.effectiveWeekend`; removed the 2024 São Paulo `sampleRaceResults` fallback in favor of an empty-state card (`EmptyWeekendTabCard`); and keyed `currentSelectedGp`, `reminderSet`, `isExpanded`, and result list items on race/season identifiers.

#### O3. [FIXED] `SettingsAndWidgetsScreen` showcase math, column alignment, & parameter split
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/ui/screens/settings/SettingsAndWidgetsScreen.kt`
  - `app/src/main/java/com/reva2473/f1widget/MainActivity.kt`
- **Resolution:** Unified the duplicate `drivers` / `driverStandings` parameters into `driverStandings` across `SettingsAndWidgetsScreen`, `WidgetLivePreviewCard`, and `MainActivity`; sorted `standings` by `position` and padded/aligned `races`, `d1Results`, and `d2Results` in `TeamFormAndTeammatesWidgetShowcase` via `alignDriverResults`; and routed Red Bull, Racing Bulls, and Alpine gap labels in `WccSquareCardsShowcase` through `gapLabel(...)`.

### Medium & Low

#### O4. [FIXED] Light-mode & accent contrast thresholds
- **Files:** `Theme.kt`, `SharedComponents.kt`, `DriverProfilesAndCompareScreen.kt`, `LiveTimingScreen.kt`, `RaceWeekendScreen.kt`, `HomeScreen.kt`, `SettingsAndWidgetsScreen.kt`
- **Resolution:** Replaced the fixed `0.55f` threshold in `P1TTheme.onColor` with WCAG contrast ratio comparison, strengthened `P1TTheme.adaptiveTeamText` to darken colors with luminance `> 0.18f` in light mode (covering McLaren orange `#FF8000` and bright accents), updated `TyreCompoundBadge` for light-mode contrast across `HARD`/`MEDIUM`/`INTERMEDIATE`/`WET`, replaced `YearSelectionDialog`'s hardcoded luminance check with `P1TTheme.onColor`, and applied `P1TTheme.adaptiveTeamText` to foreground accent text/icons/borders across light surfaces.

#### O5. [FIXED] Compose API, Canvas performance, & `LiveTimingScreen` status bugs
- **Resolution:** Reordered `CircularHeaderButton` parameters to `(onClick, modifier, icon)` in `SharedComponents.kt` and added `modifier: Modifier = Modifier` to `WeekendCircuitHeaderCard` in `RaceWeekendScreen.kt`; updated `TrackMapCard` and `LiveTimingTopBar` in `LiveTimingScreen.kt` via `resolveTrackStatusColors` so Red Flags render `P1TTheme.accentRed` / `P1TTheme.redFlagBg`; and pre-allocated a remembered `sectorTextPaint` for `drawSectorLabel` plus switched `LiveTrackMapCanvas` to `.pointerInput(Unit)` with `rememberUpdatedState` in `CircuitCanvas.kt`.

#### O6. [FIXED] Glance copy alignment, adaptive launcher icons, & test clock dependency
- **Resolution:** Populated `ConstructorStanding.drivers` in `F1Repository.getConstructorStandings` and added driver-pair derivation fallback in `FavoriteTeamWidget`, while aligning `widget_driver_standings_desc`, `widget_favorite_driver_desc`, and `widget_favorite_team_desc` in `strings.xml` with rendered Glance UIs; converted `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` to `<adaptive-icon>` backed by `drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, and `ic_launcher_monochrome.xml`; removed unused `import android.os.Build` in `SettingsAndWidgetsScreen.kt`; and replaced hardcoded `"2026-11-08"` in `F1RepositoryTest.kt` with dynamic `LocalDate.now(ZoneOffset.UTC)` relative dates.

---

## Remaining / Incomplete Fixes (Resolved Earlier)

### High

#### R1. [FIXED] `MainViewModel.updatePreferences` is atomic & refreshes all 8 widgets
- **File:** `app/src/main/java/com/reva2473/f1widget/ui/MainViewModel.kt`
- **Resolution:** Replaced the 8 sequential `preferencesStore.update*` calls with atomic `preferencesStore.updateAllPreferences(newPrefs)` and added `LiveTimingBannerWidget().updateAll(appContext)`.

#### R2. [FIXED] Light theme support across all 6 screens
- **Files:** `Theme.kt`, `SharedComponents.kt`, `CircuitCanvas.kt`, `HomeScreen.kt`, `StandingsScreen.kt`, `RaceWeekendScreen.kt`, `LiveTimingScreen.kt`, `DriverProfilesAndCompareScreen.kt`, `SettingsAndWidgetsScreen.kt`
- **Resolution:** Added light/dark-aware `P1TTheme` helpers in `Theme.kt` and replaced hardcoded dark surfaces, borders, and text across all 6 screens and shared components with `MaterialTheme.colorScheme.*` and `P1TTheme.*`.

#### R3. [FIXED] `CancellationException` propagation in `F1Repository` & Glance widgets
- **Files:**
  - `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`
  - `app/src/main/java/com/reva2473/f1widget/widget/F1GlanceWidgets.kt`
- **Resolution:** Replaced `runCatching` in `getLiveSessionState` with `safeSuspendCall` rethrowing `CancellationException`, and rethrew `CancellationException` in `loadUserPreferences` and all 8 Glance widget `provideGlance` blocks.

#### R4. [FIXED] OpenF1 live session call & `offlineCacheEnabled` wiring
- **File:** `app/src/main/java/com/reva2473/f1widget/data/repository/F1Repository.kt`, `app/src/test/java/com/reva2473/f1widget/F1RepositoryTest.kt`
- **Resolution:** Mapped non-numeric/blank keys to `"latest"` for OpenF1 queries, parsed ISO-8601 timestamps via `OffsetDateTime`/`Instant` -> local `HH:mm:ss`, wired `offlineCacheEnabled` across repository DB reads/writes, added `getLatestCompletedRace(season)`, and covered all behaviors in `F1RepositoryTest`.

### Medium & Low

#### R5. [FIXED] Glance widget & resource leftovers
- `app/src/main/res/xml/widget_live_timing.xml`: Updated `android:description` to `@string/widget_live_timing_desc` (eliminating `UnusedResources`).
- `app/src/main/java/com/reva2473/f1widget/widget/F1GlanceWidgets.kt` (`DriverAvatar`): Renders circular-clipped bitmaps via `Canvas` + `PorterDuff.Mode.SRC_IN` for API 26–30 compatibility.
- `LastRacePodiumWidget`: Uses `repo.getLatestCompletedRace(2026)` and dynamic `${latestRace.grandPrixName.uppercase()}`.
- `strings.xml`: Aligned `widget_next_race_desc`, `widget_constructor_standings_desc`, and `widget_live_timing_desc` with rendered widget UIs.

#### R6. [FIXED] Remaining UI, Build & Lint items
- `SettingsAndWidgetsScreen.kt`: Computed `WccSquareCardsShowcase` gaps dynamically from `leaderPts` and bound live/next/last-race showcase and preview widgets to dynamic state; added `LazyRow`/`LazyColumn` keys.
- `RaceWeekendScreen.kt`: Generalized `WeekendCircuitHeaderCard` (`circuitId` + `circuitType`), dynamic `gpOptions` from `schedule`, and data-driven race/circuit stats.
- `DriverProfilesAndCompareScreen.kt` & `LiveTimingScreen.kt`: Fixed `YearSelectionDialog` luminance-aware selected chip contrast and added `LazyColumn`/`LazyVerticalGrid` keys.
- `MainActivity.kt`: Passed live `schedule`, `liveSession`, and `raceWeekend` into `RaceWeekendScreen` and `SettingsAndWidgetsScreen`.
- `app/build.gradle.kts` & `gradle/libs.versions.toml`: Removed unused `androidx-navigation-compose`.
- `app/src/main/res/mipmap-anydpi/`: Moved adaptive launcher icons out of `mipmap-anydpi-v26/` (resolving `ObsoleteSdkInt`).
- `lintDebug`: Resolved all 8 `ModifierParameter` warnings (`CircuitCanvas.kt`, `HomeScreen.kt`, `LiveTimingScreen.kt`, `RaceWeekendScreen.kt`, `SettingsAndWidgetsScreen.kt`, `SharedComponents.kt`, `StandingsScreen.kt`).

---

## Previously Reported Items (Verified Fixed Unless Noted Above)

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
