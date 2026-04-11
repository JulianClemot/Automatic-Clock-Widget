package com.julian.automaticclockwidget.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.automaticclockwidget.core.AppError
import com.julian.automaticclockwidget.core.AirportError
import com.julian.automaticclockwidget.core.CalendarError
import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.clocks.ClearClocksUseCase
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.CalendarEntry
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val addUrlUseCase: AddUrlUseCase,
    private val deleteUrlUseCase: DeleteUrlUseCase,
    private val selectUrlUseCase: SelectUrlUseCase,
    private val getUrlStateUseCase: GetUrlStateUseCase,
    private val clearClocksUseCase: ClearClocksUseCase,
    private val refreshTimezonesUseCase: RefreshTimezonesUseCase,
    private val widgetUpdateUseCase: WidgetUpdateUseCase,
    private val appContext: Context,
) : ViewModel() {

    private val initialState = HomeUiState(
        entries = emptyList(),
        selected = null,
        errorMessage = null,
        successMessage = null,
        refreshState = RefreshState.Idle,
        deletionState = DeletionState.Idle,
        perMinuteTickEnabled = false,
        requestExactAlarmPermission = false,
    )

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { refreshEntries() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState,
        )

    private fun refreshEntries() {
        getUrlStateUseCase.getUrlState().fold(
            onSuccess = { snapshot ->
                _uiState.update { it.copy(entries = snapshot.entries, selected = snapshot.selected, errorMessage = null) }
            },
            onFailure = { err ->
                _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) }
            },
        )
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.AddCalendar -> {
                addUrlUseCase.addUrl(event.name, event.url).fold(
                    onSuccess = { refreshEntries() },
                    onFailure = { err -> _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) } },
                )
            }
            is HomeUiEvent.RequestDeleteUrl -> {
                _uiState.update { it.copy(deletionState = DeletionState.ConfirmationPending(event.url)) }
            }
            HomeUiEvent.DismissDeleteConfirmation -> {
                _uiState.update { it.copy(deletionState = DeletionState.Idle) }
            }
            is HomeUiEvent.DeleteUrl -> {
                _uiState.update { it.copy(deletionState = DeletionState.Idle) }
                deleteUrlUseCase.deleteUrl(event.url).fold(
                    onSuccess = { refreshEntries() },
                    onFailure = { err -> _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) } },
                )
            }
            is HomeUiEvent.SelectUrl -> {
                selectUrlUseCase.selectUrl(event.url).fold(
                    onSuccess = {
                        refreshEntries()
                        clearClocksUseCase.clearClocks().fold(
                            onSuccess = {
                                viewModelScope.launch {
                                    refreshTimezonesUseCase.refreshNow().onFailure { err ->
                                        _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) }
                                    }
                                }
                            },
                            onFailure = { err ->
                                _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) }
                            },
                        )
                    },
                    onFailure = { err -> _uiState.update { it.copy(errorMessage = mapErrorToMessage(err)) } },
                )
            }
            HomeUiEvent.ManualRefresh -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(refreshState = RefreshState.Refreshing) }
                    refreshTimezonesUseCase.refreshNow().fold(
                        onSuccess = {
                            try {
                                widgetUpdateUseCase.updateAll(appContext)
                                _uiState.update { it.copy(
                                    refreshState = RefreshState.Idle,
                                    successMessage = "Clocks updated and widget refreshed",
                                    errorMessage = null,
                                ) }
                            } catch (t: Throwable) {
                                _uiState.update { it.copy(
                                    refreshState = RefreshState.Idle,
                                    errorMessage = mapErrorToMessage(t),
                                    successMessage = null,
                                ) }
                            }
                        },
                        onFailure = { err ->
                            _uiState.update { it.copy(
                                refreshState = RefreshState.Idle,
                                errorMessage = mapErrorToMessage(err),
                                successMessage = null,
                            ) }
                        },
                    )
                }
            }
            HomeUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            HomeUiEvent.DismissSuccess -> _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun mapErrorToMessage(error: Throwable): String = when (error) {
        is SettingsError.InvalidInput -> "Invalid URL"
        is SettingsError.NotFound -> "URL not found"
        is SettingsError.StorageFailure -> "A storage error occurred"
        is CalendarError.Network -> "Network unavailable"
        is CalendarError.Parse -> "Invalid calendar format"
        is AirportError.NotFound -> "Airport not found"
        is AirportError.Network -> "Network unavailable"
        is AppError -> error.message ?: "Unexpected error"
        else -> error.message ?: "Unexpected error"
    }
}

sealed interface RefreshState {
    data object Idle : RefreshState
    data object Refreshing : RefreshState
}

sealed interface DeletionState {
    data object Idle : DeletionState
    data class ConfirmationPending(val url: String) : DeletionState
}

data class HomeUiState(
    val entries: List<CalendarEntry>,
    val selected: String?,
    val errorMessage: String?,
    val successMessage: String?,
    val refreshState: RefreshState,
    val deletionState: DeletionState,
    val perMinuteTickEnabled: Boolean,
    val requestExactAlarmPermission: Boolean,
)

sealed interface HomeUiEvent {
    data class AddCalendar(val name: String, val url: String) : HomeUiEvent
    data class RequestDeleteUrl(val url: String) : HomeUiEvent
    data object DismissDeleteConfirmation : HomeUiEvent
    data class DeleteUrl(val url: String) : HomeUiEvent
    data class SelectUrl(val url: String) : HomeUiEvent
    /** User-initiated manual refresh: fetch, save clocks, and immediately update widgets. */
    data object ManualRefresh : HomeUiEvent
    data object DismissError : HomeUiEvent
    data object DismissSuccess : HomeUiEvent
}
