package com.julian.automaticclockwidget

import androidx.lifecycle.ViewModel
import com.julian.automaticclockwidget.core.AppError
import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.clocks.ClearClocksUseCase
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val addUrlUseCase: AddUrlUseCase,
    private val deleteUrlUseCase: DeleteUrlUseCase,
    private val selectUrlUseCase: SelectUrlUseCase,
    private val getUrlStateUseCase: GetUrlStateUseCase,
    private val clearClocksUseCase: ClearClocksUseCase,
    private val refreshTimezonesUseCase: RefreshTimezonesUseCase,
    private val widgetUpdateUseCase: com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase,
) : ViewModel() { 

    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(MainUiState(emptyList(), null, null, null, perMinuteTickEnabled = false, requestExactAlarmPermission = false))
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        refreshUrls()
    }

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

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.AddUrl -> {
                addUrlUseCase.addUrl(event.url).fold(
                    onSuccess = { refreshUrls() },
                    onFailure = { err -> _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err)) }
                )
            }
            is MainUiEvent.DeleteUrl -> {
                deleteUrlUseCase.deleteUrl(event.url).fold(
                    onSuccess = { refreshUrls() },
                    onFailure = { err -> _uiState.value = _uiState.value.copy(errorMessage = mapErrorToMessage(err)) }
                )
            }
            is MainUiEvent.SelectUrl -> {
                selectUrlUseCase.selectUrl(event.url).fold(
                    onSuccess = {
                        // Update UI selection state immediately
                        refreshUrls()
                        // Clear stored clocks first
                        clearClocksUseCase.clearClocks().fold(
                            onSuccess = {
                                // Then refresh timezones and store them
                                vmScope.launch {
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
            MainUiEvent.LoadUrls -> refreshUrls()
            is MainUiEvent.ManualRefresh -> {
                vmScope.launch {
                    val res = refreshTimezonesUseCase.refreshNow()
                    res.fold(
                        onSuccess = {
                            // Immediately refresh widgets so UI reflects new clocks
                            try {
                                widgetUpdateUseCase.updateAll()
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
            MainUiEvent.DismissError -> _uiState.value = _uiState.value.copy(errorMessage = null)
            MainUiEvent.DismissSuccess -> _uiState.value = _uiState.value.copy(successMessage = null)

        }
    }

    private fun mapErrorToMessage(error: Throwable): String = when (error) {
        is SettingsError.InvalidInput -> "Invalid URL"
        is SettingsError.NotFound -> "URL not found"
        is SettingsError.StorageFailure -> "A storage error occurred"
        is AppError -> error.message ?: "Unexpected error"
        else -> error.message ?: "Unexpected error"
    }
}

data class MainUiState(
    val urls: List<String>,
    val selected: String?,
    val errorMessage: String?,
    val successMessage: String?,
    val perMinuteTickEnabled: Boolean,
    val requestExactAlarmPermission: Boolean,
)

sealed interface MainUiEvent {
    data class AddUrl(val url: String) : MainUiEvent
    data class DeleteUrl(val url: String) : MainUiEvent
    data class SelectUrl(val url: String) : MainUiEvent
    data object LoadUrls : MainUiEvent
    /** User-initiated manual refresh: fetch, save clocks, and immediately update widgets. */
    data object ManualRefresh : MainUiEvent
    data object DismissError : MainUiEvent
    data object DismissSuccess : MainUiEvent
}