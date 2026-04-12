package com.julian.automaticclockwidget.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.ObservabilityRepository
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
    private val coroutineContext: CoroutineContext,
    private val observability: ObservabilityRepository,
) : WidgetUpdateUseCase, KoinComponent {
    override suspend fun updateAll(context: Context) = withContext(coroutineContext) {
        observability.log(message = "Widget update starting", category = "widget")
        clocksPreferencesRepository.getClocks()
            .onFailure { t ->
                observability.sendErrorEvent(
                    throwable = t,
                    context = t.toErrorContext() + ("stage" to "widget.getClocks"),
                    tags = mapOf("feature" to "widget"),
                )
            }
            .onSuccess { clocks ->
                val manager = GlanceAppWidgetManager(context)
                val widget = AutomaticClockWidget()
                val glanceIds = manager.getGlanceIds(AutomaticClockWidget::class.java)
                observability.log(
                    message = "Widget updating with clocks",
                    category = "widget",
                    data = mapOf("count" to clocks.size, "widgetIds" to glanceIds.size),
                )
                glanceIds.forEach { id ->
                    try {
                        updateAppWidgetState(
                            context,
                            definition = AutomaticClockGlanceStateDefinition,
                            glanceId = id,
                            updateState = { AutomaticClockWidgetUiState.Loaded(clocks) }
                        )
                        widget.update(context, id)
                    } catch (t: Throwable) {
                        observability.sendErrorEvent(
                            throwable = t,
                            context = t.toErrorContext() + ("stage" to "widget.update") + ("glanceId" to id.toString()),
                            tags = mapOf("feature" to "widget"),
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    manager.setWidgetPreviews(
                        AutomaticClockWidgetReceiver::class
                    )
                }
            }
        observability.log(message = "Widget update finished", category = "widget")
        return@withContext
    }
}
