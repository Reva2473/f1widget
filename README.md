# ApexGrid (`F1Widget`) — Formula 1 Historic Data, Live Timing & Android Widgets

A native Android application and Jetpack Glance home-screen widget suite built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Jetpack Glance**.

## Key Features

### 1. Home ("Race Week at a Glance")
- **5-Slide Hero Pager**:
  - **Next Grand Prix Card**: Round countdown (`ROUND 16 · IN 1 DAY`), circuit title & location, local session schedule (`FP1`, `FP2`, `FP3`, `Quali`, `Race`), glowing double-stroke circuit outline (`SepangOutlineCanvas`), and 4-column circuit stat strip (`DAY TO GO`, `KM`, `LAPS`, `CORNERS`).
  - **Welcome & Archive Overview Card**: Quick entry to historical records since 1950.
  - **Championship Leaders & Last Race Podium Slides**.
- **Explore F1 Stats CTA**: Instant access to all 77 seasons (1950–2026) and 880+ driver profiles.
- **Live Session Track Map Card**: Live `Q1` countdown, `Track Clear` status pill, top-3 timing summary (`VER`, `HAD`, `ANT`), and interactive 2D circuit map with real-time team-colored driver dots.
- **Weekend / Title / Race Bento Strip**: 3-column summary card (`LEC fastest in FP2` | `+66 Antonelli leads` | `08:00 Sun`).

### 2. Driver & Constructor Standings (1950–2026)
- **Driver Standings**: Team-colored left accent stripe, driver portrait badge (`P1`, `P2`, `P3`), wins & podiums counters, and expandable inner cards for `Avg Per Race`, `Last 3 Races (Recent Form)` with individual race results (`Azerbaijan GP`, `Spanish GP`, `Italian GP`), and `Pole Positions`.
- **Constructor Standings**: Team-colored cards (`Mercedes`, `Ferrari`, `McLaren`, `Red Bull`, etc.) with `Team Drivers` breakdown and expandable `Season Race Wins` list.

### 3. Race Weekend Hub, Results & Tyre Stints
- **3-Sector Colored Circuit Map (`MultiSectorCircuitMap`)**: Renders `Sector 1` (Red), `Sector 2` (Cyan), and `Sector 3` (Yellow), numbered corner markers (`01`–`16`), checkered start/finish flag, and `SPEED TRAP` callout badge.
- **Weekend Schedule**: Session rows with countdown badges (`🚩 Race in 36 days`) and notification toggles.
- **Race Results & Tyre Stint Strategy**: Team-tinted result cards that expand on tap to reveal visual horizontal tyre stint timelines (`16L (M)` → `21.6s Lap 16` → `27L (H)` → `21.2s Lap 43` → `15L (H)`), `Lap Times`, and `Driver Stats` (`Qualifying Position`, `Qualifying Time`, `Fastest Lap`).

### 4. Driver Profiles & Head-to-Head Comparison
- **Driver Profiles**: Searchable directory of drivers across 1950–2026 with year chips, wins, gaps, team color bars, and trophy badges.
- **Compare Drivers**: Split two-tone team-color `VS` header (`VER Max VERSTAPPEN` vs `NOR Lando NORRIS`), dual center-aligned comparison bars for `Championships`, `Race Wins`, `Podiums`, `Pole Positions`, `Fastest Laps`, `Career Points`, `Race Starts`, `DNFs`, and `Success Rates` (`Win Rate`, `Podium Rate`, `Pole Rate`).

### 5. Live Timing & Telemetry
- **Interactive Track Map (`LiveTrackMapCanvas`)**: Real-time car positions around the circuit. Tap any driver on the map or timing tower (e.g. `HAM`) to highlight them with a target ring and expand their live telemetry card (`SECTOR 1`, `SECTOR 2`, `SECTOR 3`, `LAST LAP`, `BEST LAP`, `TYRE`).
- **Team Radio Player**: Playable team radio clips (`Fernando Alonso · Aston Martin`) with waveform progress bar.
- **Sub-Tabs**: `Track`, `Race control`, `Radio`, `Overtakes`, and `Weather`.

### 6. 8 Native Android Home Screen Widgets (Jetpack Glance)
1. **Next Grand Prix & Circuit Widget** (`NextRaceCountdownWidget`, 4×2) — deep-links to `NEXT_RACE` (Race Weekend Schedule)
2. **Weekend Session Schedule Widget** (`WeekendScheduleWidget`, 4×3) — deep-links to `WEEKEND_SCHEDULE` (Race Weekend Schedule)
3. **Driver Standings (WDC) Widget** (`DriverStandingsWidget`, 4×3) — deep-links to `DRIVER_STANDINGS` (Standings — Drivers)
4. **Constructor Standings (WCC) Widget** (`ConstructorStandingsWidget`, 4×3) — deep-links to `CONSTRUCTOR_STANDINGS` (Standings — Constructors)
5. **Favourite Driver Card Widget** (`FavoriteDriverWidget`, 2×2) — deep-links to `DRIVER_STANDINGS` (Standings — Drivers)
6. **Favourite Team Garage Widget** (`FavoriteTeamWidget`, 2×2) — deep-links to `FAVORITE_TEAM` (Settings & Widgets)
7. **Last Race Podium & Stints Widget** (`LastRacePodiumWidget`, 4×2) — deep-links to `LAST_RACE_RESULTS` (Race Weekend — Race Results)
8. **Live Timing Banner Widget** (`LiveTimingBannerWidget`, 4×2) — deep-links to `LIVE_TIMING` (Live Timing & Telemetry)

### 7. Personalization, Notifications & Offline-First Data Architecture
- **Team Colour Accent**: Dynamically tunes the app's primary accent and top radial header glow to match your chosen constructor (`Ferrari`, `Mercedes`, `McLaren`, `Red Bull`, `Aston Martin`, `Alpine`, `Williams`, `Racing Bulls`, `Haas`, `Kick Sauber`, `Cadillac`).
- **Data Sources & Offline Storage**:
  - **Jolpica-F1 API** (`https://api.jolpi.ca/ergast/f1/`) for 1950–2026 historical seasons, schedules, standings, results, qualifying, and pit stops.
  - **OpenF1 API** (`https://api.openf1.org/v1/`) for live timing, car positions, tyre stints, team radio, race control, overtakes, and weather.
  - **JSON File Cache (`F1Database` / `F1Dao`)**: Disk-backed JSON files stored under `filesDir/f1_offline_cache` (`standings_*.json`, `schedule_*.json`, `race_result_*.json`) with atomic writes (`*.tmp` + `Files.move`), mutex synchronization, and a 6-hour TTL (`DEFAULT_TTL_MS`).
  - **Jetpack `DataStore` (`UserPreferencesStore`)**: Persists user preferences (`user_preferences.preferences_pb`) and explicit `reminder_session_keys` in atomic transactions.
  - **Pre-Seeded Replay Engine (`SeedDataProvider`)**: Powers offline mode and live timing simulation without poisoning the live API cache.
- **Background Refresh & Session Reminders**:
  - **WorkManager (`WidgetRefreshWorker` / `WidgetRefreshScheduler`)**: Runs 30-minute periodic and immediate refreshes across all 8 Glance widgets, warming live session data during active race windows.
  - **AlarmManager & Notifications (`SessionReminderScheduler` / `SessionReminderReceiver`)**: Schedules `setAndAllowWhileIdle` alarms 15 minutes before sessions on channel `apexgrid_session_reminders`, checks `POST_NOTIFICATIONS` on Android 13+, and resyncs alarms on `BOOT_COMPLETED`.

## Build, Permissions & CI
- **Manifest Permissions (`AndroidManifest.xml`)**: `INTERNET`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`.
- **Release & CI Verification (`.github/workflows/android-ci.yml`)**: Runs `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug assembleRelease` (`release` enables `isMinifyEnabled = true` and `isShrinkResources = true` with R8/ProGuard rules in `app/proguard-rules.pro`).

## Project Structure
```
app/src/main/
├── AndroidManifest.xml
├── java/com/reva2473/f1widget/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── local/F1Database.kt
│   │   ├── remote/JolpicaApiService.kt
│   │   ├── remote/OpenF1ApiService.kt
│   │   ├── repository/F1Repository.kt
│   │   └── seed/SeedDataProvider.kt
│   ├── domain/model/F1Models.kt
│   ├── notifications/SessionReminderManager.kt
│   ├── ui/
│   │   ├── MainViewModel.kt
│   │   ├── components/
│   │   │   ├── CircuitCanvas.kt
│   │   │   ├── F1AssetMapper.kt
│   │   │   └── SharedComponents.kt
│   │   ├── screens/
│   │   │   ├── drivers/DriverProfilesAndCompareScreen.kt
│   │   │   ├── home/HomeScreen.kt
│   │   │   ├── live/LiveTimingScreen.kt
│   │   │   ├── settings/SettingsAndWidgetsScreen.kt
│   │   │   ├── standings/StandingsScreen.kt
│   │   │   └── weekend/RaceWeekendScreen.kt
│   │   └── theme/Theme.kt
│   ├── widget/F1GlanceWidgets.kt
│   └── worker/WidgetRefreshWorker.kt
└── res/
    ├── drawable/preview_widget_*.xml
    ├── layout/{widget_loading.xml, preview_widget_*.xml}
    ├── mipmap-anydpi/{ic_launcher.xml, ic_launcher_round.xml}
    ├── values/{colors.xml, strings.xml, themes.xml}
    └── xml/{backup_rules.xml, data_extraction_rules.xml, widget_*.xml}
```
