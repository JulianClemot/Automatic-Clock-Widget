@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.julian.automaticclockwidget.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase

class CalendarRefreshWorker(
    val appContext: Context,
    params: WorkerParameters,
    private val clocksRepository: ClocksPreferencesRepository,
    private val refreshUseCase: RefreshTimezonesUseCase,
    private val appUpdateUseCase: WidgetUpdateUseCase,
    private val observability: ObservabilityRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val tx = observability.startTransaction("calendar.refresh", "workflow")
        tx.setData("trigger", inputData.getString("trigger") ?: "worker")

        observability.log(
            message = "CalendarRefreshWorker started",
            category = "worker",
            data = mapOf("workName" to UNIQUE_ONE_TIME_WORK_NAME),
        )
        Log.i(TAG, "CalendarRefreshWorker: start refresh")

        return try {
            refreshUseCase.refreshNow()
                .map {
                    Log.i(TAG, "CalendarRefreshWorker: refresh success; timezones refreshed")
                }.mapCatching { clocksRepository.getClocks().getOrThrow() }.fold(
                    onSuccess = { clocks ->
                        Log.i(TAG, "CalendarRefreshWorker: refresh success; clocks saved")
                        appUpdateUseCase.updateAll(appContext)
                        Log.i(TAG, "CalendarRefreshWorker: widget updated successfully")
                        observability.log(
                            message = "CalendarRefreshWorker success",
                            category = "worker",
                            data = mapOf("clocksCount" to clocks.size),
                        )
                        tx.setStatus("ok")
                        Result.success()
                    },
                    onFailure = { err ->
                        Log.e(TAG, "CalendarRefreshWorker: refresh failed=${err.message}", err)
                        observability.sendErrorEvent(
                            throwable = err,
                            context = err.toErrorContext() + ("stage" to "refresh"),
                            tags = mapOf("workflow" to "calendar.refresh"),
                        )
                        tx.setStatus("internal_error")
                        Result.retry()
                    }
                )
        } catch (t: Throwable) {
            Log.e(TAG, "CalendarRefreshWorker: unexpected error=${t.message}", t)
            observability.sendErrorEvent(
                throwable = t,
                context = t.toErrorContext() + ("stage" to "worker_uncaught"),
                tags = mapOf("workflow" to "calendar.refresh"),
            )
            tx.setStatus("internal_error")
            Result.retry()
        } finally {
            tx.finish()
        }
    }

    companion object {
        private const val TAG = "CalendarRefreshWorker"
        const val UNIQUE_ONE_TIME_WORK_NAME = "CalendarRefreshWorker_OneTime"

        fun setNetworkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
