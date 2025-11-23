package com.julian.automaticclockwidget

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.julian.automaticclockwidget.ui.theme.appModule
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker
import com.julian.automaticclockwidget.workers.DailyScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AutomaticClockWidgetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AutomaticClockWidgetApplication)
            modules(appModule)
        }

        // Enqueue unique weekly calendar refresh work
        val request = DailyScheduler.createDailyCalendarRefreshWorkRequest()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CalendarRefreshWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}