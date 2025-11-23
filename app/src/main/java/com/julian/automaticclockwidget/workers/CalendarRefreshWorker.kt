@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.julian.automaticclockwidget.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.julian.automaticclockwidget.widgets.AutomaticClockWidget
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import org.koin.core.context.GlobalContext

class CalendarRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val refreshUseCase: RefreshTimezonesUseCase = GlobalContext.get().get()
        return try {
            val result = refreshUseCase.refreshNow()
            result.fold(
                onSuccess = {
                    Log.i(TAG, "CalendarRefreshWorker: refresh success; clocks saved")
                    // Trigger widget update
                    val context = applicationContext
                    val manager = GlanceAppWidgetManager(context)
                    val ids = manager.getGlanceIds(AutomaticClockWidget::class.java)
                    val widget = AutomaticClockWidget()
                    for (id in ids) {
                        widget.update(context, id)
                    }
                    Result.success()
                },
                onFailure = { err ->
                    Log.e(TAG, "CalendarRefreshWorker: refresh failed=${err.message}", err)
                    Result.retry()
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "CalendarRefreshWorker: unexpected error=${t.message}", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CalendarRefreshWorker"
        const val UNIQUE_WORK_NAME = "CalendarRefreshWorker"

        fun unmeteredNetworkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
    }
}
