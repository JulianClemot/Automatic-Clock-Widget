---
name: add-compose-screen
description: >
  Create a new Jetpack Compose screen following this project's Clean Architecture and MVVM pattern.
  Use when adding a new screen, feature UI, or navigation destination. Covers Route (Navigation 3
  type-safe), EntryPoint + Content composables, ViewModel with StateFlow/UiEvent, Koin DI
  registration, and AppNavigator wiring. Triggers on "add a screen", "create a new screen",
  "new feature screen", "new composable screen", "add navigation destination".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: low
tags: [compose, ui, scaffold, mvvm, navigation]
---

# Add Compose Screen

Screens live in `app/src/main/java/com/julian/automaticclockwidget/ui/[feature]/`:
- `[Feature]Screen.kt` — Route, EntryPoint, Content composables
- `[Feature]ViewModel.kt` — ViewModel + UiState + UiEvent

## Step 1: Route (type-safe Navigation 3)

```kotlin
// In [Feature]Screen.kt
@Serializable
data object [Feature]Route : NavKey
```

## Step 2: ViewModel + UiState + UiEvent

```kotlin
// [Feature]ViewModel.kt
class [Feature]ViewModel(
    private val someUseCase: SomeUseCase,
    private val appContext: Context,
) : ViewModel() {

    private val initialState = [Feature]UiState()
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<[Feature]UiState> = _uiState
        .onStart { loadInitialData() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState,
        )

    fun onEvent(event: [Feature]UiEvent) {
        when (event) {
            is [Feature]UiEvent.SomeAction -> handleSomeAction(event)
            is [Feature]UiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun handleSomeAction(event: [Feature]UiEvent.SomeAction) {
        viewModelScope.launch {
            someUseCase.execute(event.param).fold(
                onSuccess = { result -> _uiState.update { it.copy(data = result, errorMessage = null) } },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = mapErrorToMessage(error)) } },
            )
        }
    }

    private fun loadInitialData() { /* triggered by onStart */ }

    private fun mapErrorToMessage(error: Throwable): String = when (error) {
        is AppError -> error.message ?: "Unexpected error"
        else -> error.message ?: "Unexpected error"
    }
}

data class [Feature]UiState(
    val data: SomeData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed interface [Feature]UiEvent {
    data class SomeAction(val param: String) : [Feature]UiEvent
    data object DismissError : [Feature]UiEvent
}
```

## Step 3: Composables

```kotlin
// [Feature]Screen.kt
@Composable
fun [Feature]EntryPoint(viewModel: [Feature]ViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    [Feature]Content(state) { event -> viewModel.onEvent(event) }
}

@Composable
fun [Feature]Content(
    state: [Feature]UiState,
    onEvent: ([Feature]UiEvent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent([Feature]UiEvent.DismissError)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
        ) {
            if (state.isLoading) CircularProgressIndicator()
            else { /* content */ }
        }
    }
}
```

## Step 4: Register in Koin (`AppModule.kt`)

```kotlin
viewModel<[Feature]ViewModel> { [Feature]ViewModel(get(), androidApplication()) }
```

## Step 5: Add to Navigation (`AppNavigator.kt`)

```kotlin
composable<[Feature]Route> {
    [Feature]EntryPoint(viewModel = koinViewModel())
}
```

## Key Rules

- **EntryPoint** — only connects ViewModel to Content via `collectAsStateWithLifecycle()`
- **Content** — pure UI, no ViewModel; receives `state` + `onEvent` lambda
- **UiState** — immutable `data class`; mutate with `_uiState.update { it.copy(...) }`
- **UiEvent** — `sealed interface`; one subtype per user action
- **Errors** — always Snackbar via `LaunchedEffect` + `DismissError` event
- Private backing: `_uiState: MutableStateFlow`; public: `uiState: StateFlow`

## Checklist

- [ ] `[Feature]Screen.kt` — Route, EntryPoint, Content
- [ ] `[Feature]ViewModel.kt` — StateFlow, UiState, UiEvent, `mapErrorToMessage`
- [ ] `viewModel<>` in `AppModule.kt`
- [ ] Route in `AppNavigator.kt`
- [ ] Unit tests for ViewModel
- [ ] `./gradlew :app:testDebugUnitTest`
