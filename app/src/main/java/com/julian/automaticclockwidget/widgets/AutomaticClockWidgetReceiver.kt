package com.julian.automaticclockwidget.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.julian.automaticclockwidget.workers.AirplaneModeScheduler
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker.Companion.UNIQUE_ONE_TIME_WORK_NAME
import org.koin.core.component.KoinComponent

class AutomaticClockWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {

    override val glanceAppWidget: GlanceAppWidget = AutomaticClockWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val request = AirplaneModeScheduler.createOneTimeRequestForRefreshTimezones()
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName = UNIQUE_ONE_TIME_WORK_NAME,
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                request = request
            )

        }
    }
}