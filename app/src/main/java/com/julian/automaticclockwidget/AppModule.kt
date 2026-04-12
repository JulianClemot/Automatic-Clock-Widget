package com.julian.automaticclockwidget

import com.julian.automaticclockwidget.airports.AirportsRepository
import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.airports.rest.RestAirportRepository
import com.julian.automaticclockwidget.calendars.CalendarsRepository
import com.julian.automaticclockwidget.calendars.DownloadCalendarUseCase
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.calendars.iCalendar.ICalendarRepository
import com.julian.automaticclockwidget.clocks.ClearClocksUseCase
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepositoryImpl
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.settings.SettingsPreferencesRepository
import com.julian.automaticclockwidget.settings.SettingsPreferencesRepositoryImpl
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import com.julian.automaticclockwidget.settings.UrlPreferencesRepositoryImpl
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.observability.sentry.SentryObservabilityRepository
import com.julian.automaticclockwidget.ui.home.HomeViewModel
import com.julian.automaticclockwidget.widgets.GlanceWidgetUpdateUseCase
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ViewModel injects URL use cases only (no direct repo, no GetUpcomingClocksUseCase)
    viewModel<HomeViewModel> {
        HomeViewModel(
            get(), get(), get(), get(), get(), get(), get(), androidApplication()
        )
    }

    // Use cases
    single { GetAirportTimezoneUseCase(get()) }
    single { DownloadCalendarUseCase(get()) }
    single { GetUpcomingClocksUseCase(get(), get()) }
    single { ClearClocksUseCase(get()) }
    single { RefreshTimezonesUseCase(get(), get(), get()) }
    single<WidgetUpdateUseCase> {
        GlanceWidgetUpdateUseCase(
            get(),
            Dispatchers.Default
        )
    }

    // URL management use cases
    single { AddUrlUseCase(get()) }
    single { DeleteUrlUseCase(get()) }
    single { SelectUrlUseCase(get()) }
    single { GetUrlStateUseCase(get()) }

    // Repositories
    single<AirportsRepository> { RestAirportRepository(get()) }
    single<CalendarsRepository> { ICalendarRepository(get()) }
    single<ClocksPreferencesRepository> { ClocksPreferencesRepositoryImpl(get()) }

    single<UrlPreferencesRepository> { UrlPreferencesRepositoryImpl(get()) }
    single<SettingsPreferencesRepository> { SettingsPreferencesRepositoryImpl(get()) }
    single<ObservabilityRepository> { SentryObservabilityRepository() }

    worker { CalendarRefreshWorker(get(), get(), get(), get(), get()) }

    // Networking
    single<OkHttpClient> {
        OkHttpClient.Builder().also {
            val aLogger = HttpLoggingInterceptor()
            aLogger.level = (HttpLoggingInterceptor.Level.BODY)
            it.addInterceptor(aLogger)
        }.build()
    }
}