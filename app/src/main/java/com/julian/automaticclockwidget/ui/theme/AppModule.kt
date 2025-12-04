package com.julian.automaticclockwidget.ui.theme

import com.julian.automaticclockwidget.MainViewModel
import com.julian.automaticclockwidget.airports.AirportsRepository
import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.airports.rest.RestAirportRepository
import com.julian.automaticclockwidget.calendars.CalendarsRepository
import com.julian.automaticclockwidget.calendars.DownloadCalendarUseCase
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.calendars.iCalendar.ICalendarRepository
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepositoryImpl
import com.julian.automaticclockwidget.settings.AddUrlUseCase
import com.julian.automaticclockwidget.settings.DeleteUrlUseCase
import com.julian.automaticclockwidget.settings.GetUrlStateUseCase
import com.julian.automaticclockwidget.settings.SelectUrlUseCase
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import com.julian.automaticclockwidget.settings.UrlPreferencesRepositoryImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ViewModel injects URL use cases only (no direct repo, no GetUpcomingClocksUseCase)
    viewModel<MainViewModel> { MainViewModel(
        get<AddUrlUseCase>(),
        get<DeleteUrlUseCase>(),
        get<SelectUrlUseCase>(),
        get<GetUrlStateUseCase>(),
        get<com.julian.automaticclockwidget.clocks.ClearClocksUseCase>(),
        get<com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase>(),
        get<com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase>(),
    ) }

    // Use cases
    single { GetAirportTimezoneUseCase(get()) }
    single { DownloadCalendarUseCase(get()) }
    single { GetUpcomingClocksUseCase(get(), get()) }
    single { com.julian.automaticclockwidget.clocks.ClearClocksUseCase(get()) }
    single { com.julian.automaticclockwidget.clocks.RefreshTimezonesUseCase(get(), get(), get()) }
    single<com.julian.automaticclockwidget.widgets.WidgetUpdateUseCase> { com.julian.automaticclockwidget.widgets.GlanceWidgetUpdateUseCase(get()) }

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
    single<com.julian.automaticclockwidget.settings.SettingsPreferencesRepository> { com.julian.automaticclockwidget.settings.SettingsPreferencesRepositoryImpl(get()) }

    // Networking
    single<OkHttpClient> {
        OkHttpClient.Builder().also {
            val aLogger = HttpLoggingInterceptor()
            aLogger.level = (HttpLoggingInterceptor.Level.BODY)
            it.addInterceptor(aLogger)
        }.build()
    }
}