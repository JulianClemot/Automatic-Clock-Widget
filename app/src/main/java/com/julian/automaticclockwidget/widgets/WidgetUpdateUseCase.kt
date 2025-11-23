package com.julian.automaticclockwidget.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import org.koin.core.component.KoinComponent

/**
 * Abstraction to trigger a refresh of all AutomaticClockWidget instances.
 */
interface WidgetUpdateUseCase {
    suspend fun updateAll()
}

class GlanceWidgetUpdateUseCase(private val context: Context) : WidgetUpdateUseCase, KoinComponent {
    override suspend fun updateAll() {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(AutomaticClockWidget::class.java)
        val widget = AutomaticClockWidget()
        for (id in ids) {
            widget.update(context, id)
        }
    }
}
