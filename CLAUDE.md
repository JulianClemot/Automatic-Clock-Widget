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

- `properties/airports.properties` — must contain `API_KEY` and `BASE_URL`
- `properties/tracking.properties` — must contain `SENTRY_DSN` and `SENTRY_ORGANISATION`

## Tech Stack

- Kotlin 2.3.0, AGP 8.13.2, JVM 11, Min SDK 31, Target/Compile SDK 36
- Jetpack Compose + Glance (widgets), Navigation 3 (type-safe, `@Serializable` routes)
- Koin (DI), WorkManager, OkHttp, biweekly (iCal parsing), kotlinx-datetime
- JUnit 4, no ktlint/detekt (Android Lint only)

## Architecture

Clean Architecture with feature-based packages under `com.julian.automaticclockwidget/`:

```
core/               # AppError sealed hierarchy
airports/           # Airport timezone lookup (REST source)
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

Data flow: User adds iCal URL → `CalendarRefreshWorker` downloads & parses it → extracts airport IATA codes from events → fetches timezones via REST → stores `StoredClock` list → `AutomaticClockWidget` reads and renders clocks.

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
- `SourceNounRepository` for implementations — e.g., `RestAirportRepository`, `ICalendarRepository`
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

## Key Patterns

### Use Cases
Single public method, return `Result<T>`, `suspend` for async:
```kotlin
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
    }
}
```

### Repository
Interface in feature package; implementation in `source/` subpackage with `.toModelName()` mapping:
```kotlin
// airports/AirportsRepository.kt
interface AirportsRepository {
    suspend fun findAirport(iataCode: String): Result<Airport>
}

// airports/rest/RestAirportRepository.kt
class RestAirportRepository(private val client: OkHttpClient) : AirportsRepository {
    override suspend fun findAirport(iataCode: String) = withContext(Dispatchers.IO) { ... }
}
```

### Error Handling
Sealed error hierarchy rooted at `AppError`. Use `runCatching { }` + `recoverCatching { }` in repositories:
```kotlin
override suspend fun getCalendar(uri: String) = runCatching {
    val body = downloadCalendar(uri)
    parseCalendar(body)
}.recoverCatching { t ->
    when (t) {
        is CalendarError -> throw t
        is IOException -> throw CalendarError.Network(cause = t)
        else -> throw UnknownError(cause = t)
    }
}
```
ViewModels map errors to strings:
```kotlin
private fun mapErrorToMessage(error: Throwable): String = when (error) {
    is SettingsError.InvalidInput -> "Invalid URL"
    is CalendarError.Network -> "Network unavailable"
    is AppError -> error.message ?: "Unexpected error"
    else -> error.message ?: "Unexpected error"
}
```

### ViewModel State
```kotlin
private val initialState = HomeUiState(...)
private val _uiState = MutableStateFlow(initialState)
val uiState: StateFlow<HomeUiState> = _uiState
    .onStart { refreshUrls() }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialState
    )
```

### Compose Screen Structure
```kotlin
// Route (type-safe navigation)
@Serializable
data object HomeRoute : NavKey

// Entry point (connects ViewModel to UI)
@Composable
fun HomeEntryPoint(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state) { event -> viewModel.onEvent(event) }
}

// Content (pure UI, no ViewModel dependency)
@Composable
fun HomeContent(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit) { ... }
```

Events:
```kotlin
sealed interface HomeUiEvent {
    data class AddUrl(val url: String) : HomeUiEvent
    data object ManualRefresh : HomeUiEvent
}
```

### DI (Koin)
```kotlin
val appModule = module {
    viewModel<HomeViewModel> { HomeViewModel(get(), get(), androidApplication()) }
    single { GetAirportTimezoneUseCase(get()) }
    single<AirportsRepository> { RestAirportRepository(get()) }
    worker { CalendarRefreshWorker(get(), get(), get()) }
}
```

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
