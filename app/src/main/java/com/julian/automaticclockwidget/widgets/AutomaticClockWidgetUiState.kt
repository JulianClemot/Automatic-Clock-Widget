package com.julian.automaticclockwidget.widgets

import com.julian.automaticclockwidget.clocks.StoredClock
import kotlinx.serialization.Serializable

@Serializable
sealed interface AutomaticClockWidgetUiState {

    @Serializable
    data class Loaded(
        val clocks: List<StoredClock>,
    ) : AutomaticClockWidgetUiState

    @Serializable
    data object Empty : AutomaticClockWidgetUiState
}