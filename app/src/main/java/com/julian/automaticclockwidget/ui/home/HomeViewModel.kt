package com.julian.automaticclockwidget.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julian.automaticclockwidget.core.AppError
import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.clocks.ClearClocksUseCase
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import com.julian.automaticclockwidget.core.AirportError
import com.julian.automaticclockwidget.core.CalendarError
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
        urls = emptyList(),
        selected = null,
        errorMessage = null,
        successMessage = null,
        perMinuteTickEnabled = false,
        requestExactAlarmPermission = false
    )

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { refreshUrls() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState
        )

    private fun refreshUrls() {
        getUrlStateUseCase.getUrlState().fold(
            onSuccess = { snapshot ->
                _uiState.value = _uiState.value.copy(urls = snapshot.urls, selected = snapshot.selected, errorMessage = null)
            },
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err))
            }
        )
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.AddUrl -> {
                addUrlUseCase.addUrl(event.url).fold(
                    onSuccess = { refreshUrls() },
                    onFailure = { err -> _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err)) }
                )
            }
            is HomeUiEvent.DeleteUrl -> {
                deleteUrlUseCase.deleteUrl(event.url).fold(
                    onSuccess = { refreshUrls() },
                    onFailure = { err -> _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err)) }
                )
            }
            is HomeUiEvent.SelectUrl -> {
                selectUrlUseCase.selectUrl(event.url).fold(
                    onSuccess = {
                        // Update UI selection state immediately
                        refreshUrls()
                        // Clear stored clocks first
                        clearClocksUseCase.clearClocks().fold(
                            onSuccess = {
                                // Then refresh timezones and store them
                                viewModelScope.launch {
                                    val res = refreshTimezonesUseCase.refreshNow()
                                    res.onFailure { err ->
                                        _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err))
                                    }
                                }
                            },
                            onFailure = { err ->
                                _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err))
                            }
                        )
                    },
                    onFailure = { err -> _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err)) }
                )
            }
            is HomeUiEvent.ManualRefresh -> {
                viewModelScope.launch {
                    val res = refreshTimezonesUseCase.refreshNow()
                    res.fold(
                        onSuccess = {
                            // Immediately refresh widgets so UI reflects new clocks
                            try {
                                widgetUpdateUseCase.updateAll(appContext)
                                _uiState.value = _uiState.value.copy(
                                    successMessage = "Clocks updated and widget refreshed",
                                    errorMessage = null
                                )
                            } catch (t: Throwable) {
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = mapErrorToMessage(t),
                                    successMessage = null
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = mapErrorToMessage(err),
                                successMessage = null
                            )
                        }
                    )
                }
            }
            HomeUiEvent.DismissError -> _uiState.value = _uiState.value.copy(errorMessage = null)
            HomeUiEvent.DismissSuccess -> _uiState.value = _uiState.value.copy(successMessage = null)

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

data class HomeUiState(
    val urls: List<String>,
    val selected: String?,
    val errorMessage: String?,
    val successMessage: String?,
    val perMinuteTickEnabled: Boolean,
    val requestExactAlarmPermission: Boolean,
)

sealed interface HomeUiEvent {
    data class AddUrl(val url: String) : HomeUiEvent
    data class DeleteUrl(val url: String) : HomeUiEvent
    data class SelectUrl(val url: String) : HomeUiEvent
    /** User-initiated manual refresh: fetch, save clocks, and immediately update widgets. */
    data object ManualRefresh : HomeUiEvent
    data object DismissError : HomeUiEvent
    data object DismissSuccess : HomeUiEvent
}