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
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase

class CalendarRefreshWorker(
    val appContext: Context,
    params: WorkerParameters,
    private val clocksRepository: ClocksPreferencesRepository,
    private val refreshUseCase: RefreshTimezonesUseCase,
    private val appUpdateUseCase: WidgetUpdateUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "CalendarRefreshWorker: start refresh")
        return try {
            refreshUseCase.refreshNow()
                .map {
                    Log.i(TAG, "CalendarRefreshWorker: refresh success; timezones refreshed")
                }.mapCatching { clocksRepository.getClocks().getOrThrow() }.fold(
                    onSuccess = { clocks ->
                        Log.i(TAG, "CalendarRefreshWorker: refresh success; clocks saved")
                        // Trigger widget update
                        appUpdateUseCase.updateAll(appContext)
                        Log.i(TAG, "CalendarRefreshWorker: widget updated successfully")
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
        private const val TAG = "AirplaneModeReceiver"
        const val UNIQUE_ONE_TIME_WORK_NAME = "CalendarRefreshWorker_OneTime"

        fun setNetworkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
