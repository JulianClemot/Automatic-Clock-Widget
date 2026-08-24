package com.julian.automaticclockwidget

import android.content.Context
import androidx.room.Room
import com.julian.automaticclockwidget.airports.AirportsRepository
import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.airports.github.AirportSource
import com.julian.automaticclockwidget.airports.github.GithubAirportSource
import com.julian.automaticclockwidget.airports.local.AirportDatabase
import com.julian.automaticclockwidget.airports.local.LocalAirportRepository
import com.julian.automaticclockwidget.calendars.CalendarsRepository
import com.julian.automaticclockwidget.calendars.DownloadCalendarUseCase
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.calendars.iCalendar.ICalendarRepository
import com.julian.automaticclockwidget.clocks.ClearClocksUseCase
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepositoryImpl
import com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.observability.sentry.SentryObservabilityRepository
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.settings.SettingsPreferencesRepository
import com.julian.automaticclockwidget.settings.SettingsPreferencesRepositoryImpl
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import com.julian.automaticclockwidget.settings.UrlPreferencesRepositoryImpl
import com.julian.automaticclockwidget.ui.home.HomeViewModel
import com.julian.automaticclockwidget.widgets.GlanceWidgetUpdateUseCase
import com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase
import com.julian.automaticclockwidget.workers.CalendarRefreshWorker
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Observability
    single<ObservabilityRepository> { SentryObservabilityRepository() }

    // ViewModel
    viewModel<HomeViewModel> {
        HomeViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), androidApplication()
        )
    }

    // Use cases
    single { GetAirportTimezoneUseCase(get(), get()) }
    single { DownloadCalendarUseCase(get()) }
    single { GetUpcomingClocksUseCase(get(), get(), get()) }
    single { ClearClocksUseCase(get()) }
    single { RefreshTimezonesUseCase(get(), get(), get(), get()) }
    single<WidgetUpdateUseCase> {
        GlanceWidgetUpdateUseCase(
            get(),
            Dispatchers.Default,
            get(),
        )
    }

    // URL management use cases
    single { AddUrlUseCase(get()) }
    single { DeleteUrlUseCase(get()) }
    single { SelectUrlUseCase(get()) }
    single { GetUrlStateUseCase(get()) }

    // Repositories
    single {
        Room.databaseBuilder(androidApplication(), AirportDatabase::class.java, "airports.db").build()
    }
    single { get<AirportDatabase>().airportDao() }
    single<AirportSource> { GithubAirportSource(get()) }
    single<AirportsRepository> {
        LocalAirportRepository(
            dao = get(),
            source = get(),
            prefs = androidApplication().getSharedPreferences("automatic_clock_prefs", Context.MODE_PRIVATE),
        )
    }
    single<CalendarsRepository> { ICalendarRepository(get(), get()) }
    single<ClocksPreferencesRepository> { ClocksPreferencesRepositoryImpl(androidContext(), get()) }
    single<UrlPreferencesRepository> { UrlPreferencesRepositoryImpl(androidContext(), get()) }
    single<SettingsPreferencesRepository> { SettingsPreferencesRepositoryImpl(androidContext(), get()) }

    worker { CalendarRefreshWorker(get(), get(), get(), get(), get(), get()) }

    // Networking
    single<OkHttpClient> {
        OkHttpClient.Builder().also {
            val aLogger = HttpLoggingInterceptor()
            aLogger.level = (HttpLoggingInterceptor.Level.BODY)
            it.addInterceptor(aLogger)
        }.build()
    }
}
