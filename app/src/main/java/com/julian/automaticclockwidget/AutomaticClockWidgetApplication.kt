package com.julian.automaticclockwidget

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class AutomaticClockWidgetApplication : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AutomaticClockWidgetApplication)
            workManagerFactory()
            modules(appModule)
        }
    }
}