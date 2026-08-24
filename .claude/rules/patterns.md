---
name: key-patterns
description: Implementation templates for common patterns (use cases, repositories, error handling, ViewModels, Compose screens, DI)
paths: ["**/*.kt"]
---

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
Interface in feature package; implementation in its own named subdirectory with `.toModelName()` mapping:
```kotlin
// airports/AirportsRepository.kt
interface AirportsRepository {
    suspend fun findAirport(iataCode: String): Result<Airport>
}

// airports/local/LocalAirportRepository.kt
class LocalAirportRepository(private val dao: AirportDao, ...) : AirportsRepository {
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
    single<AirportsRepository> { LocalAirportRepository(get(), get(), get()) }
    worker { CalendarRefreshWorker(get(), get(), get()) }
}
```
