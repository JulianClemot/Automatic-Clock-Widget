package com.julian.automaticclockwidget.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext

/**
 * Abstraction to trigger a refresh of all AutomaticClockWidget instances.
 */
interface WidgetUpdateUseCase {
    suspend fun updateAll(context: Context)
}

class GlanceWidgetUpdateUseCase(
    private val clocksPreferencesRepository: ClocksPreferencesRepository,
    private val coroutineContext: CoroutineContext
) : WidgetUpdateUseCase, KoinComponent {
    override suspend fun updateAll(context: Context) = withContext(coroutineContext) {
        clocksPreferencesRepository.getClocks().onSuccess { clocks ->
            val manager = GlanceAppWidgetManager(context)
            val widget = AutomaticClockWidget()
            manager.getGlanceIds(AutomaticClockWidget::class.java).forEach { id ->
                updateAppWidgetState(
                    context,
                    definition = AutomaticClockGlanceStateDefinition,
                    glanceId = id,
                    updateState = { AutomaticClockWidgetUiState.Loaded(clocks) }
                )
                widget.update(context, id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
               manager.setWidgetPreviews(
                    AutomaticClockWidgetReceiver::class
                )
            }
        }
        return@withContext
    }
}
