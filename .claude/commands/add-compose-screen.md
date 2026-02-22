
# Adding a Compose Screen

This project uses Jetpack Compose with MVVM architecture and type-safe navigation.

## File Structure

All UI components go in `app/src/main/java/com/julian/automaticclockwidget/ui/[feature]/`:

1. **[Feature]Screen.kt** - Route, EntryPoint, and Content composables
2. **[Feature]ViewModel.kt** - ViewModel with StateFlow and event handling

## Step 1: Create Route (Type-Safe Navigation)

```kotlin
package com.julian.automaticclockwidget.ui.[feature]

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object [Feature]Route : NavKey
```

## Step 2: Define UI State and Events

In **[Feature]ViewModel.kt**:

```kotlin
package com.julian.automaticclockwidget.ui.[feature]

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class [Feature]ViewModel(
  private val someUseCase: SomeUseCase,
  private val appContext: Context,
) : ViewModel() {

  private val initialState = [Feature]UiState(
        // ... initial values
    )

  private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<[Feature]UiState> = _uiState
        .onStart { loadInitialData() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState
        )

    fun onEvent(event: [Feature]UiEvent) {
        when (event) {
            is [Feature]UiEvent.SomeAction -> handleSomeAction(event)
            // ... handle other events
      }
  }

    private fun handleSomeAction(event: [Feature]UiEvent.SomeAction) {
        viewModelScope.launch {
            someUseCase.execute(event.param).fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        data = result,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = mapErrorToMessage(error)
                    )
                }
            )
      }
  }

    private fun loadInitialData() {
        // Load initial data if needed
  }

    private fun mapErrorToMessage(error: Throwable): String = when (error) {
        is [Feature]Error.SomeError -> "User-friendly message"
        is AppError -> error.message ?: "Unexpected error"
        else -> error.message ?: "Unexpected error"
  }
}

data class [Feature]UiState(
    val data: SomeData? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
)

sealed interface [Feature]UiEvent {
    data class SomeAction(val param: String) : [Feature]UiEvent
    data object Refresh : [Feature]UiEvent
    data object DismissError : [Feature]UiEvent
}
```

## Step 3: Create Composables

In **[Feature]Screen.kt**:

```kotlin
package com.julian.automaticclockwidget.ui.[feature]

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun [Feature]EntryPoint(viewModel: [Feature]ViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    [Feature]Content(state) { event -> viewModel.onEvent(event) }
}

@Composable
fun [Feature]Content(
    state: [Feature]UiState,
    onEvent: ([Feature]UiEvent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onEvent([Feature]UiEvent.DismissError)
      }
  }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // UI components here
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                // Display content
            }
      }
  }
}
```

## Step 4: Register ViewModel in Koin

In **AppModule.kt**:

```kotlin
val appModule = module {
    // ... other dependencies
    
    viewModel<[Feature]ViewModel> {
        [Feature]ViewModel(get(), androidApplication())
  }
}
```

## Step 5: Add to Navigation

In **AppNavigator.kt**:

```kotlin
import com.julian.automaticclockwidget.ui.[feature].[Feature]EntryPoint
import com.julian.automaticclockwidget.ui.[feature].[Feature]Route
import org.koin.androidx.compose.koinViewModel

// Inside NavHost:
composable<[Feature]Route> {
    [Feature]EntryPoint(viewModel = koinViewModel())
}
```

## Best Practices

### State Management
- Use `StateFlow` for reactive state
- Emit initial state with `.onStart { }`
- Use `SharingStarted.WhileSubscribed(5_000)` for lifecycle awareness
- Keep state immutable (data class with `copy()`)

### Event Handling
- Use sealed interface for type-safe events
- Handle all events in `onEvent()` when expression
- Launch coroutines in `viewModelScope` for async operations

### Error Handling
- Map domain errors to user-friendly messages
- Display errors via Snackbar with `LaunchedEffect`
- Clear error after showing (DismissError event)

### Composable Patterns
- **EntryPoint**: Connects ViewModel to UI, uses `koinViewModel()`
- **Content**: Pure UI function, receives state and event handler
- Use `remember` for non-state values (like `SnackbarHostState`)
- Use `collectAsStateWithLifecycle()` for StateFlow

### Testing
- Test ViewModels by observing state changes
- Test event handlers produce correct state
- Use fake repositories for dependencies

## Checklist

- [ ] Create `[Feature]Screen.kt` with Route, EntryPoint, Content
- [ ] Create `[Feature]ViewModel.kt` with StateFlow and events
- [ ] Define UI state as immutable data class
- [ ] Define events as sealed interface
- [ ] Implement error mapping
- [ ] Register ViewModel in Koin module
- [ ] Add route to navigation graph
- [ ] Test ViewModel logic with unit tests
- [ ] Run app to verify UI works: `./gradlew installDebug`
