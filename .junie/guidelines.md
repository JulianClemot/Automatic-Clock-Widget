### AutomaticClockWidget — Developer Guidelines

#### Project Overview

- Android app (minSdk 31, target/compileSdk 36) using Kotlin, Jetpack Compose, and Glance for an App
  Widget.
- DI: Koin BOM with compose integration.
- Time/calendars: `kotlinx-datetime` and `biweekly` (iCalendar parsing).
- Networking: OkHttp (BOM + logging-interceptor); Kotlinx Serialization JSON.
- Version catalog: Dependencies centralized in `gradle/libs.versions.toml`.
- Entry points:
    - App: `com.julian.automaticclockwidget.MainActivity`
    - Widget: `com.julian.automaticclockwidget.widgets.AutomaticClockWidget` via
      `AutomaticClockWidgetReceiver` and `res/xml/automatic_clock_widget_info.xml`

#### Build & Configuration

- Toolchain
    - Android Gradle Plugin: `8.13.0` (`plugins.android-application` via version catalog)
    - Kotlin: `2.2.21` (Compose compiler plugin enabled)
    - JVM target: 11 (Gradle config enforces Java 11 for source/target)
- SDKs
    - compileSdk/targetSdk: 36
    - minSdk: 31 (required by Glance + modern APIs)
- How to build from CLI
    - Assemble debug APK: `./gradlew :app:assembleDebug`
    - Full build (all variants): `./gradlew build`
    - Clean: `./gradlew clean`
- IDE setup
    - Use Android Studio Koala+ (or newer) with JDK 17 installed; Gradle itself targets JVM 11 for
      compilation. Let the IDE use the Gradle JDK to avoid mismatch.
    - Ensure Android SDK 36 Platform + Build Tools are installed.

#### Testing

- Unit tests (JVM): JUnit4, no Android deps.
    - Run all unit tests: `./gradlew :app:testDebugUnitTest`
    - Run a specific test class:
      `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.ExampleUnitTest'`
    - Run a specific test method:
      `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.ExampleUnitTest.addition_isCorrect'`
- Instrumented tests (Android): androidx test runner.
    - Requires emulator/device (API 31+). Run: `./gradlew :app:connectedDebugAndroidTest`
    - Runner: `androidx.test.runner.AndroidJUnitRunner` (declared in `defaultConfig`).
- Compose UI tests
    - Dependencies for Compose UI testing are present under `androidTest` via the Compose BOM. Add
      tests in `app/src/androidTest/...` and use `createAndroidComposeRule`.
- HTTP and time-dependent code
    - For networking, we currently do not include `mockwebserver`. If needed, add it via the version
      catalog (recommend `com.squareup.okhttp3:mockwebserver` aligned with the OkHttp BOM) and use
      it in JVM tests where possible.
    - For time, `kotlinx-datetime` simplifies pure JVM tests; abstract system clock if you need
      deterministic results.

##### Verified example (executed before writing these guidelines)

- We validated that JVM tests run successfully in this project env. Example command and result:
    - Command:
      `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.GuidelinesSanityTest.sanity_addition'`
    - Result: Passed (1/1). The temporary test file used for this verification has been removed, as
      it was only for demonstration.

##### Adding a new unit test

1. Create a file under `app/src/test/java/com/julian/automaticclockwidget/` (or a suitable package)
   with `@Test` methods using JUnit4.
2. Keep tests free of Android SDK types; use pure Kotlin/JVM utilities.
3. Use fake implementations instead of mocking, unless you have to. These fake implementations must
   be reusable across tests.
4. Run: `./gradlew :app:testDebugUnitTest` or target it with `--tests`.

##### Adding a new instrumented/Compose test

1. Create tests under `app/src/androidTest/...`.
2. Use `androidx.test.ext.junit.runners.AndroidJUnit4` and, for Compose, `createAndroidComposeRule`.
3. Launch an emulator/device (API 31+), then run: `./gradlew :app:connectedDebugAndroidTest`.

#### Troubleshooting

- Build fails with SDK mismatch: Ensure Android 36 platform and corresponding Build Tools are
  installed. Sync Gradle and re-import.
- Compose/Glance preview issues: Clear Gradle caches, invalidate IDE caches. Some Glance previews
  require running on device; rely on `providePreview` when possible.
- Runtime widget issues: Check that the widget provider XML and receiver `exported`/`intent-filters`
  match what Glance expects; verify `APPWIDGET_UPDATE` delivery in logs.

## Architecture Overview

This project follows **Clean Architecture** principles with clear separation of concerns.

## SOLID Principles Implementation

### Single Responsibility Principle (SRP)
- **Use Cases**: Each use case handles one specific business operation
  ```kotlin
  class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {

    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
    }
  }
  ```

- **Repository Interfaces**: Each repository interface focuses on one data concern
  ```kotlin
  interface AirportsRepository {
    suspend fun findAirport(iataCode : String) : Result<Airport>
  }
  ```

### Open/Closed Principle (OCP)
- **Repository Pattern**: Implementations can be extended without modifying interfaces
- **Dependency Injection**: New implementations can be injected without changing existing code
- **Use Case Pattern**: New use cases can be added without modifying existing ones

### Liskov Substitution Principle (LSP)
- **Repository Implementations**: All implementations must honor the contract defined by interfaces
- **Test Doubles**: Stubs and mocks must behave consistently with real implementations

### Interface Segregation Principle (ISP)
- **Focused Interfaces**: Repository interfaces are split by concern (GetAllCrisisRepository, SaveCrisisRepository)
- **Avoid Fat Interfaces**: Each interface contains only methods relevant to its specific responsibility

### Dependency Inversion Principle (DIP)
- **Abstractions**: High-level modules depend on abstractions (repository interfaces)
- **Dependency Injection**: Concrete implementations are injected via Koin
- **Inversion of Control**: Dependencies flow inward toward the domain layer

## Code Organization Standards

### Package Structure
```
app/
├── src/
│   ├── main/
│   │   ├── kotlin/java/com/julian/automaticclockwidget
│   │   │   ├── features/
│   │   │   │   ├── authentication/
│   │   │   │   │   ├── domain/
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── usecase/
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   └── presentation/
│   │   │   │   │       ├── ui/
│   │   │   │   │       ├── viewmodel/
│   │   │   │   │       └── mapper/
│   │   │   │   ├── profile/
│   │   │   │   └── [feature-name]/
│   │   │   ├── core/
│   │   │   │   ├── di/
│   │   │   │   ├── network/
│   │   │   │   ├── database/
│   │   │   │   └── util/
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── test/
│   └── androidTest/
└── build.gradle.kts
```

### Naming Conventions
- **Use Cases**: `VerbNounUseCase` (e.g., `GetAllCrisisUseCase`, `SaveCrisisUseCase`)
- **Repositories**: `NounRepository` (e.g., `CrisisRepository`)
- **ViewModels**: `ScreenNameViewModel` (e.g., `RegisterEndoCrisisViewModel`)
- **Domain Models**: Simple nouns (e.g., `Crisis`, `CrisisLevel`)
- **Data Models**: `LocalNoun` or `RemoteNoun` (e.g., `LocalCrisis`)

## Domain Layer Guidelines

### Domain Models
- **Pure Kotlin**: No framework dependencies
- **Immutable**: Use `data class` with `val` properties
- **Value Classes**: Use for type safety and performance
  ```kotlin
  @JvmInline
  value class Id(val value: Int) {
      init {
          require(value >0) { "Id should be superior to 0" }
      }
  }
  ```

### Use Cases
- **Single Public Method**: Each use case should have one main public method
- **Dependency Injection**: Accept dependencies through constructor
- **Error Handling**: Use `Result<T>` for error handling
- **Suspend Functions**: Use for asynchronous operations
  ```kotlin
  class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
    }
  }
  ```

### Repository Interfaces
- **Focused Responsibility**: Each interface should handle one data concern
- **Result Type**: Return `Result<T>` for operations that can fail
- **Suspend Functions**: Use for asynchronous operations
- **No Implementation Details**: Interfaces should not leak implementation details
  ```kotlin
  class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
    }
  }
  ```
  
## Data Layer Guidelines

### Repository Implementations

- **Error Handling**: Use `runCatching` to convert exceptions to `Result`
- **Extensions**: Use Extensions in data models to convert between data models and domain models
- **Single Responsibility**: Each repository should handle one data source
- Interface definition is XXXRepository (example : `AirportsRepository`)
- Implementation should have the source as prefix (example : `LocalAirportsRepository` for a
  repository that retrieves data from the local storage, `RestAirportsRepository` for a
  repository that retrieves data from a remote server)
- An implementation should be injected with the appropriate dispatcher

### Data Models
- **Framework Specific**: Can contain framework annotations (Room, Serialization)
- data model should have the source as prefix (example : `LocalAirport` for a
    repository that retrieves data from the local storage, `RestAirport` for a
    repository that retrieves data from a remote server)
- **Mapping Functions**: Provide conversion methods to/from domain models
  ```kotlin
  @Serializable
  data class RestAirport(
      val code: String,
      val icao: String,
      val name: String?,
      val latitude: Double?,
      val longitude: Double?,
      val elevation: Int?,
      val url: String?,
      @SerialName("time_zone")
      val timezone: String,
      @SerialName("city_code")
      val cityCode: String?,
      val country: String?,
      val city: String?,
      val state: String?,
      val county: String?,
      val type: String?
      ) {
      fun toAirport() = Airport(
      iataCode = code,
      city = timezone.split("/").lastOrNull() ?: city.orEmpty(),
      timezone = TimeZone.of(timezone),
      )
  }
  ```

## Presentation Layer Guidelines

### ViewModels
- **Extend ViewModel**: Use Android Architecture Components ViewModel
- **Dependency Injection**: Accept use cases through constructor
- **State Management**: Use StateFlow/LiveData for UI state
- **Coroutine Scope**: Use `viewModelScope` for coroutines
- **Error Handling**: Handle and expose errors to UI appropriately
  ```kotlin
  class MainViewModel(private val getAirportsUseCase: GetAirportsUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(
        MainUiState.InitialState
    )

    val uiState = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RegisterEndoCrisisUiState.InitialState
    )

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.OnEvent -> TODO()
        }
    }
  }
  ```

### UI Screens
- **Composable Functions**: Use Jetpack Compose for UI
- **State Hoisting**: Lift state up to ViewModels
- **Separation of Concerns**: Keep UI logic separate from business logic
- **Reusable Components**: Extract common UI patterns into reusable components

### Navigation
- **Type-Safe Navigation**: Use sealed classes for routes
- **Deep Linking**: Support deep linking where appropriate
- **Back Stack Management**: Handle back navigation properly

## Dependency Injection Guidelines

### Module Organization
- **Feature Modules**: Organize DI modules by feature
- **Layer Separation**: Separate modules for different layers when needed
- **Platform-Specific**: Use expect/actual for platform-specific dependencies

### Koin Setup
```kotlin
val appModule = module {

    // ViewModel injects URL use cases only (no direct repo, no GetUpcomingClocksUseCase)
    viewModel<MainViewModel> { MainViewModel(
        get<AddUrlUseCase>(),
        get<DeleteUrlUseCase>(),
        get<SelectUrlUseCase>(),
        get<GetUrlStateUseCase>(),
        get<com.julian.automaticclockwidget.clocks.ClearClocksUseCase>(),
        get<com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase>(),
        get<com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase>(),
    ) }

    // Use cases
    single { GetAirportTimezoneUseCase(get()) }
    single { DownloadCalendarUseCase(get()) }
    single { GetUpcomingClocksUseCase(get(), get()) }
    single { com.julian.automaticclockwidget.clocks.ClearClocksUseCase(get()) }
    single { com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase(get(), get(), get()) }
    single<com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase> { com.julian.automaticclockwidget.widgets.GlanceWidgetUpdateUseCase(get()) }

    // URL management use cases
    single { AddUrlUseCase(get()) }
    single { DeleteUrlUseCase(get()) }
    single { SelectUrlUseCase(get()) }
    single { GetUrlStateUseCase(get()) }

    // Repositories
    single<AirportsRepository> { RestAirportRepository(get()) }
    single<CalendarsRepository> { ICalendarRepository(get()) }
    single<ClocksPreferencesRepository> { ClocksPreferencesRepositoryImpl(get()) }

    single<UrlPreferencesRepository> { UrlPreferencesRepositoryImpl(get()) }
    single<com.julian.automaticclockwidget.settings.SettingsPreferencesRepository> { com.julian.automaticclockwidget.settings.SettingsPreferencesRepositoryImpl(get()) }

    // Networking
    single<OkHttpClient> {
        OkHttpClient.Builder().also {
            val aLogger = HttpLoggingInterceptor()
            aLogger.level = (HttpLoggingInterceptor.Level.BODY)
            it.addInterceptor(aLogger)
        }.build()
    }
}
```

## Testing Strategy - Test Pyramid

### Unit Tests (Base of Pyramid - Most Tests)
**Target**: Domain layer (Use Cases, Domain Models)
**Characteristics**:
- Fast execution
- No external dependencies
- Test business logic in isolation
- Use test doubles (fakes, stubs, mocks)
- Use meaningful test methods using sentences and back quotes

**Example**:
```kotlin
// File: app/src/test/java/com/julian/automaticclockwidget/airports/GetAirportTimezoneUseCaseTest.kt
@Test
fun `given repository returns airport when requesting timezone then use case forwards success`() = runBlocking {
    // Given
    val repo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(
            Airport("JFK", "John F Kennedy", TimeZone.of("America/New_York"))
        )
    }
    val useCase = GetAirportTimezoneUseCase(repo)

    // When
    val result = useCase.getAirportTimezone("JFK")

    // Then
    assertTrue(result.isSuccess)
    assertEquals("JFK", result.getOrThrow().iataCode)
}

@Test
fun `given repository fails when requesting timezone then use case forwards failure`() = runBlocking {
    // Given
    val repo = FakeAirportsRepository().apply {
        responses["LHR"] = Result.failure(Exception("Not found"))
    }
    val useCase = GetAirportTimezoneUseCase(repo)

    // When
    val result = useCase.getAirportTimezone("LHR")

    // Then
    assertTrue(result.isFailure)
}
```

**Guidelines**:
- Test all use cases with various scenarios (success, failure, edge cases)
- Test domain model validation logic
- Use Given-When-Then structure
- Mock/stub external dependencies
- Test error handling paths
- Aim for 100% coverage of domain layer

### Integration Tests (Middle of Pyramid - Moderate Amount)
**Target**: Data layer integration, Repository implementations
**Characteristics**:
- Test interaction between components
- May use real databases (in-memory)
- Test data mapping and persistence
- Slower than unit tests but faster than UI tests

**Example**:
```kotlin
// Example Android integration test with MockWebServer for ICalendarRepository
// File suggestion: app/src/androidTest/java/.../calendars/iCalendar/ICalendarRepositoryTest.kt
@RunWith(AndroidJUnit4::class)
class ICalendarRepositoryTest {
    private val server = MockWebServer()

    @Before fun setUp() { server.start() }
    @After fun tearDown() { server.shutdown() }

    @Test
    fun `downloads ics and maps events to domain calendar`() = runBlocking {
        // Given
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:1
            DTSTART:20250101T090000Z
            DTEND:20250101T100000Z
            SUMMARY:Test Event
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(ics).setHeader("Content-Type", "text/calendar"))

        val baseUrl = server.url("/test.ics").toString()
        val okHttp = OkHttpClient()
        val repo = ICalendarRepository(okHttp)

        // When
        val result = repo.getCalendar(baseUrl)

        // Then
        assertTrue(result.isSuccess)
        val calendar = result.getOrThrow()
        assertTrue(calendar.events.events.isNotEmpty())
        assertEquals("Test Event", calendar.events.events.first().title)
    }
}
```

Notes:
- This project includes OkHttp BOM; add `mockwebserver` test dependency aligned with the BOM in `gradle/libs.versions.toml` if not present.
- If you prefer a pure JVM integration-style test, target just the mapping by calling `Biweekly.parse(...).first().toCalendar(TimeZone.currentSystemDefault())` with a sample `.ics` string to validate mapping without network.

**Guidelines**:
- Test repository implementations with real databases
- Test data mapping between layers
- Test database migrations
- Test error scenarios (database failures, network issues)
- Use test databases (in-memory or test containers)

### UI Tests (Top of Pyramid - Fewest Tests)
**Target**: End-to-end user flows
**Characteristics**:
- Test complete user journeys
- Slowest to execute
- Most brittle
- Test critical user paths only

**Example**:
```kotlin
// Minimal UI test asserting MainActivity renders expected controls
// File suggestion: app/src/androidTest/java/com/julian/automaticclockwidget/MainActivityTest.kt
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_refresh_button_and_manage_urls_text() {
        // Launch MainActivity
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent { AutomaticClockWidgetTheme { Text("Manage ICS URLs") } }
        }
        // Check presence of expected UI strings
        composeRule.onNodeWithText("Manage ICS URLs").assertExists()
        // When integrated with full MainActivity content, you can assert:
        // composeRule.onNodeWithText("Refresh now and update widget").assertExists()
    }
}
```

Notes:
- Glance widgets are RemoteViews-backed; prefer Activity/Compose UI tests for the configuration screen. For Glance, validate update side-effects via repository state or a dedicated abstraction tested with JVM tests.

**Guidelines**:
- Focus on critical user journeys
- Test happy paths and major error scenarios
- Keep tests stable and maintainable
- Use page object pattern for complex UIs
- Run in CI/CD pipeline but not on every commit

### Test Organization
```
src/
├── commonTest/          # Unit tests
│   └── domain/         # Domain layer tests
├── androidTest/        # Integration tests (Android)
├── iosTest/           # Integration tests (iOS)
└── uiTest/            # UI/E2E tests
```

### Testing Best Practices

#### General Testing Principles
- **GWT Pattern**: Given, When, Then
- **Test Naming**: Use descriptive names that explain the scenario. Use back quotes and a full sentence explaining the business rule behind the test
- **One Assertion Per Test**: Focus each test on one specific behavior
- **Test Independence**: Tests should not depend on each other
- **Fast Feedback**: Unit tests should run quickly

#### Mocking and Test Doubles
- **Prefer fakes**: Use simple fakes for straightforward scenarios
- **Mock Frameworks**: Consider MockK for complex mocking needs
- **Verify Interactions**: Test that dependencies are called correctly
- **Don't Mock Value Objects**: Mock behavior, not data

#### Error Testing
- **Test Error Paths**: Ensure error scenarios are properly handled
- **Exception Testing**: Test that appropriate exceptions are thrown
- **Result Type Testing**: Test both success and failure cases for Result<T>

#### Coroutine Testing
- **Use runTest**: For testing suspend functions
- **Test Dispatchers**: Use TestDispatchers for deterministic testing
- **Advance Time**: Use advanceUntilIdle() for time-dependent tests

### Mobile-Specific Considerations
- **Memory Management**: Be mindful of memory usage on mobile devices
- **Battery Optimization**: Minimize background processing
- **Network Efficiency**: Implement proper caching and offline support
- **User Experience**: Follow platform-specific UI guidelines

### Performance Optimization
- **Lazy Loading**: Implement lazy loading for large datasets
- **Image Optimization**: Use appropriate image formats and sizes
- **Database Optimization**: Use proper indexing and query optimization
- **Coroutine Management**: Properly manage coroutine lifecycles

## Error Handling Standards

### Meaningful Error Design Principles
- **Errors Should Be Meaningful**: All errors must provide clear, actionable information about what went wrong and why
- **Domain-Specific Errors**: Create custom error types that reflect business domain concepts
- **Layer-Appropriate Errors**: Each layer should define errors relevant to its responsibilities
- **Error Context**: Include relevant context information (IDs, timestamps, operation details) in error messages

### Custom Error Hierarchy

#### Domain Layer Errors
Create custom errors that inherit from `Throwable` to represent business rule violations and domain-specific errors:

```kotlin
// Base domain error
sealed class DomainError(message: String, cause: Throwable? = null) : Exception(message, cause)

// Business rule violations
class AirportInexistantError(level: Int) : DomainError(
    "The airport you are looking for doesn't  exist"
)

class TimezoneFormatInvalid(crisisId: String) : DomainError(
    "The timezone is not properley formatted"
)
```

#### Data Layer Errors
Create errors that represent data access and persistence errors:

```kotlin
// Base data error
sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause)

// Database-specific errors
class DatabaseConnectionError(cause: Throwable) : DataError(
    "Failed to connect to database", cause
)

class DatabaseCorruptionError(tableName: String, cause: Throwable) : DataError(
    "Database table '$tableName' is corrupted", cause
)

class DataMappingError(sourceType: String, targetType: String, cause: Throwable) : DataError(
    "Failed to map data from $sourceType to $targetType", cause
)

// Network-specific errors (for future remote data sources)
class NetworkUnavailableError : DataError(
    "Network connection is not available"
)

class ApiResponseError(statusCode: Int, message: String) : DataError(
    "API request failed with status $statusCode: $message"
)
```

### Result Type Usage
- **Domain Layer**: Use `Result<T>` for operations that can fail
- **Consistent Error Handling**: Handle errors consistently across layers
- **Error Propagation**: Let errors bubble up to appropriate handling layer
- **Custom Exception Wrapping**: Wrap custom exceptions in Result.failure() for consistent error handling

### Exception Handling Implementation

#### Domain Layer Error Handling
```kotlin
// Example: wrap repository failure into a domain-specific error
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
            .recoverCatching { throwable ->
                // Re-wrap into domain error while preserving cause
                throw com.julian.automaticclockwidget.core.AirportError.NotFound(
                    "Airport $iataCode not found",
                    throwable
                )
            }
    }
}
```

#### Data Layer Error Handling
```kotlin
// Example: map networking and parsing errors in ICalendarRepository to CalendarError
class ICalendarRepository(private val client: OkHttpClient) : CalendarsRepository {
    override suspend fun getCalendar(uri: String): Result<Calendar> = runCatching {
        val body = downloadCalendar(uri)
        parseCalendar(body)
    }.recoverCatching { t ->
        when (t) {
            is com.julian.automaticclockwidget.core.CalendarError -> throw t
            is IOException -> throw com.julian.automaticclockwidget.core.CalendarError.Network(
                message = "Network error while downloading calendar",
                cause = t
            )
            is IllegalArgumentException, is IllegalStateException ->
                throw com.julian.automaticclockwidget.core.CalendarError.Parse(
                    message = "Invalid iCalendar content",
                    cause = t
                )
            else -> throw com.julian.automaticclockwidget.core.UnknownError(cause = t)
        }
    }

    private suspend fun downloadCalendar(uri: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(uri.replace("webcal://", "https://"))
            .addHeader("Accept", "text/calendar")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw com.julian.automaticclockwidget.core.CalendarError.HttpFailure(
                    code = response.code,
                    message = response.message
                )
            }
            response.body.string()
        }
    }
}
```

#### Presentation Layer Error Handling
```kotlin
// Example: MainViewModel mapping Result failure to user-facing message
class MainViewModel(
    private val refreshTimezones: com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase,
    private val widgetUpdate: com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase,
    // ... other use cases
) : ViewModel() {
    private val _ui = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _ui

    fun onEvent(event: MainUiEvent) {
        if (event is MainUiEvent.ManualRefresh) {
            viewModelScope.launch {
                refreshTimezones.refresh().
                    onSuccess {
                        _ui.value = _ui.value.copy(successMessage = "Widget updated")
                        widgetUpdate.updateAll()
                    }.
                    onFailure { throwable ->
                        val msg = when (throwable) {
                            is com.julian.automaticclockwidget.core.CalendarError.Network -> "Network unavailable"
                            is com.julian.automaticclockwidget.core.CalendarError.Parse -> "Invalid calendar format"
                            is com.julian.automaticclockwidget.core.AirportError.NotFound -> "Airport not found"
                            else -> "Unexpected error"
                        }
                        _ui.value = _ui.value.copy(errorMessage = msg)
                    }
            }
        }
    }
}
```

### Error Handling Best Practices

#### Error Design Guidelines
- **Specific Error Types**: Create specific error types for different error scenarios
- **Meaningful Names**: Use descriptive names that clearly indicate the error condition
- **Contextual Information**: Include relevant context (IDs, values, operation details) in error messages
- **Cause Chain**: Preserve the original exception as the cause when wrapping errors
- **Immutable Error Data**: Include relevant data as properties in custom errors for programmatic access

#### Error Message Guidelines
- **User-Friendly Messages**: Provide clear, non-technical messages for end users
- **Developer-Friendly Details**: Include technical details in error messages for debugging
- **Actionable Information**: Tell users what they can do to resolve the error
- **Consistent Tone**: Maintain a consistent, helpful tone in all error messages
- **Localization Ready**: Structure messages to support future localization

#### Error Logging and Monitoring
- **Log All Exceptions**: Ensure all exceptions are properly logged with appropriate levels
- **Structured Logging**: Use structured logging to make error analysis easier
- **Error Tracking**: Implement error tracking for production monitoring
- **Performance Impact**: Consider the performance impact of error handling code

#### Testing Error Scenarios
- **Test All Error Paths**: Ensure comprehensive test coverage for all error scenarios
- **Custom Error Testing**: Test that custom errors are thrown in appropriate situations
- **Error Message Testing**: Verify that error messages are meaningful and helpful
- **Error Recovery Testing**: Test error recovery mechanisms and fallback behaviors

## Performance Guidelines

### Coroutines
- **Appropriate Dispatchers**: Use correct dispatcher for the operation type
    - `Dispatchers.IO` for I/O operations
    - `Dispatchers.Default` for CPU-intensive work
    - `Dispatchers.Main` for UI updates
- **Context Switching**: Switch context at repository layer, not in use cases
- **Cancellation**: Ensure operations are cancellable

### Database Operations
- **Batch Operations**: Use batch operations for multiple inserts/updates
- **Indexing**: Add appropriate database indexes for query performance
- **Pagination**: Implement pagination for large datasets

### Memory Management
- **Avoid Memory Leaks**: Properly manage ViewModels and coroutines
- **Efficient Data Structures**: Use appropriate collections for the use case
- **Image Loading**: Use efficient image loading libraries (Coil, etc.)

## Code Quality Standards

### Code Style
- **Kotlin Coding Conventions**: Follow official Kotlin coding conventions
- **Consistent Formatting**: Use automated formatting tools
- **Meaningful Names**: Use descriptive names for classes, methods, and variables

### Documentation
- **KDoc Comments**: Document public APIs with KDoc
- **README Updates**: Keep README.md updated with setup instructions
- **Architecture Documentation**: Document architectural decisions

### Code Review Guidelines
- **Architecture Compliance**: Ensure code follows Clean Architecture principles
- **SOLID Principles**: Verify SOLID principles are followed
- **Test Coverage**: Ensure adequate test coverage for new code
- **Performance Impact**: Consider performance implications of changes

## Continuous Integration

### Build Pipeline
- **Automated Testing**: Run all tests in CI pipeline
- **Code Quality Checks**: Include linting and static analysis
- **Build Verification**: Ensure code compiles for all platforms

### Quality Gates
- **Test Coverage**: Maintain minimum test coverage thresholds
- **Code Quality**: Use tools like Detekt for code quality analysis
- **Security Scanning**: Include security vulnerability scanning