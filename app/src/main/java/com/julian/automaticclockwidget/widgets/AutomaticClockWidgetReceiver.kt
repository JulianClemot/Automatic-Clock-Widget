package com.julian.automaticclockwidget.widgets

import android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_DATE_CHANGED
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.Intent.ACTION_SCREEN_ON
import android.content.Intent.ACTION_TIMEZONE_CHANGED
import android.content.Intent.ACTION_TIME_CHANGED
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomaticClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AutomaticClockWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            ACTION_NEXT_ALARM_CLOCK_CHANGED,
            ACTION_DATE_CHANGED,
            ACTION_LOCALE_CHANGED,
            ACTION_SCREEN_ON,
            ACTION_TIME_CHANGED,
            ACTION_TIMEZONE_CHANGED, -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        Log.i(TAG, "Time-related broadcast received: $action. Updating widgets…")
                        val manager = GlanceAppWidgetManager(context)
                        val ids = manager.getGlanceIds(AutomaticClockWidget::class.java)
                        val widget = AutomaticClockWidget()
                        for (id in ids) {
                            widget.update(context, id)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to update widgets on broadcast: ${t.message}", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AutoClockWidgetRcvr"
    }
}