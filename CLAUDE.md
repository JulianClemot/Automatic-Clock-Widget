# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About

Android widget app that displays clocks for different timezones, driven by airport codes extracted from iCalendar feeds.

## Build & Test Commands

### Build
```bash
./gradlew clean                    # Clean build artifacts
./gradlew :app:assembleDebug       # Build debug APK
./gradlew :app:assembleRelease     # Build release APK
./gradlew build                    # Full build (all variants)
./gradlew check                    # Run tests + lint
./gradlew installDebug             # Install to connected device
```

### Test
```bash
./gradlew :app:testDebugUnitTest   # Unit tests (fast, JVM)

# Single test class
./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.settings.AddUrlUseCaseTest'

# Single test method (use exact method name in backticks)
./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.settings.AddUrlUseCaseTest.given urls added with spaces and duplicates then list is deduped trimmed and last is selected'

./gradlew :app:connectedDebugAndroidTest  # Instrumented tests (requires device/emulator)
./gradlew connectedCheck
```

### Lint
```bash
./gradlew lintDebug                # Lint debug variant
./gradlew lintFix                  # Apply safe fixes
```

## Required Properties Files

The build **will fail** without these files in `properties/`:

- `properties/tracking.properties` — must contain `SENTRY_DSN` and `SENTRY_ORGANISATION`

> `properties/airports.properties` is no longer required — airport data is fetched from the mwgg/Airports GitHub repo and cached locally in Room.

## Tech Stack

- Kotlin 2.3.20, AGP 9.1.1, KSP 2.3.10, JVM 11, Min SDK 31, Target/Compile SDK 36
- Jetpack Compose + Glance (widgets), Navigation 3 (type-safe, `@Serializable` routes)
- Koin (DI), WorkManager, OkHttp, Room 2.7.1 (local airport DB), biweekly (iCal parsing), kotlinx-datetime
- JUnit 4, no ktlint/detekt (Android Lint only)

## Architecture

Clean Architecture with feature-based packages under `com.julian.automaticclockwidget/`:

```
core/               # AppError sealed hierarchy
airports/           # Airport lookup: domain model, local/ (Room DB), github/ (mwgg source)
calendars/          # iCalendar download & parsing (iCalendar source)
clocks/             # Clock storage & display formatting
settings/           # URL management preferences
airplanemode/       # Airplane mode detection service
ui/
  home/             # HomeViewModel + HomeScreen (Compose)
  AppNavigator.kt   # Navigation 3 setup
widgets/            # Glance widget (AutomaticClockWidget)
workers/            # CalendarRefreshWorker (WorkManager)
AppModule.kt        # Koin module
```

Data flow: User adds iCal URL → `CalendarRefreshWorker` downloads & parses it → extracts airport IATA codes from events → looks up timezone in local Room DB (seeded from mwgg/Airports JSON on first use, refreshed when GitHub has a newer commit) → stores `StoredClock` list → `AutomaticClockWidget` reads and renders clocks.

## Code Style

### Imports
- No wildcard imports — always explicit
- Order: `android.*` → `androidx.*` → third-party → `com.julian.*` → `java.*` → `kotlin.*`

### Formatting
- 4-space indent, K&R braces, trailing commas in multi-line lists
- Line length: ~100-120 chars (soft limit)

### Types
- Infer types for local variables; explicit types for function return types and public properties
- Nullability always explicit with `?`; prefer non-null types, use `?.` and `?:`

### Naming Conventions

**Classes:**
- `VerbNounUseCase` — e.g., `GetAirportTimezoneUseCase`, `RefreshTimezonesUseCase`
- `NounRepository` for interfaces — e.g., `AirportsRepository`
- `SourceNounRepository` for implementations — e.g., `LocalAirportRepository`, `ICalendarRepository`
- `ScreenNameViewModel` — e.g., `HomeViewModel`
- Simple nouns for data classes — e.g., `Airport`, `Event`, `Calendar`
- Sealed classes for state/errors — e.g., `AppError`, `HomeUiEvent`

**Functions:** camelCase; composables PascalCase (`HomeContent`, `HomeEntryPoint`); event handlers `onEvent(event: HomeUiEvent)`

**Variables:** Private mutable state prefixed with `_`:
```kotlin
private val _uiState = MutableStateFlow(initialState)
val uiState: StateFlow<HomeUiState> = _uiState
```

**Constants:** `SCREAMING_SNAKE_CASE` in companion objects.

## Testing

- Mirror source structure in `app/src/test/`; test files named `[Class]Test.kt`
- Fakes over mocks; fakes in `fixtures/` package
- Backtick names with Given-When-Then: `` `given X when Y then Z` ``

Fake pattern:
```kotlin
/**
 * In-memory fake for UrlPreferencesRepository used in unit tests.
 * Behavior mirrors production rules: ...
 */
class FakeUrlPreferencesRepository : UrlPreferencesRepository {
    private val urls = mutableListOf<String>()
    // ... implementation matching production behavior
}
```

## Documentation

- KDoc for public APIs and complex classes
- Inline comments for non-obvious logic
- File-level annotations for experimental APIs:
  ```kotlin
  @file:OptIn(ExperimentalTime::class)
  ```

## Key Reminders

- Run tests before committing: `./gradlew :app:testDebugUnitTest`
- Use `Result<T>` for all fallible operations
- Map data models to domain models at repository boundaries
- Prefer immutability (`val` over `var`, immutable collections)
