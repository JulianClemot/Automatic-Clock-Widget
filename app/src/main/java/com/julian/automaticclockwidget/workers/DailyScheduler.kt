package com.julian.automaticclockwidget.workers

import androidx.work.BackoffPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Utilities to build a daily PeriodicWorkRequest that runs every day at midnight.
 */
object DailyScheduler {

    /**
     * Compute the delay in milliseconds from now until the next midnight.
     */
    fun initialDelayToNextMidnight(): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val nextMidnight = now.plusDays(1).with(LocalTime.NOON)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0)
    }

    /**
     * Create a daily PeriodicWorkRequest that runs every day at midnight.
     */
    fun createDailyCalendarRefreshWorkRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<CalendarRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(CalendarRefreshWorker.unmeteredNetworkConstraints())
            .setInitialDelay(initialDelayToNextMidnight(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                12, // 12 hours
                TimeUnit.HOURS
            )
            .build()
    }
}