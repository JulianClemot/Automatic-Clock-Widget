package com.julian.automaticclockwidget.workers

import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker.Companion.setNetworkConstraints

/**
 * Utilities to build a daily PeriodicWorkRequest that runs every day at midnight.
 */
object AirplaneModeScheduler {
    /**
     * Create a daily PeriodicWorkRequest that runs every day at midnight.
     */
    fun createOneTimeRequestForRefreshTimezones(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<CalendarRefreshWorker>()
            .setConstraints(setNetworkConstraints())
            .build()
}