package com.julian.automaticclockwidget.clocks

import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.calendars.Calendar
import com.julian.automaticclockwidget.calendars.DownloadCalendarUseCase
import com.julian.automaticclockwidget.calendars.Event
import com.julian.automaticclockwidget.calendars.Events
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.fixtures.FakeAirportsRepository
import com.julian.automaticclockwidget.fixtures.FakeCalendarsRepository
import com.julian.automaticclockwidget.fixtures.FakeClocksPreferencesRepository
import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshTimezonesUseCaseTest {

    @Test
    fun preserves_order_of_successes_even_when_some_fail() = runBlocking {
        val urlRepo = com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository().apply {
            addUrl("https://ics"); selectUrl("https://ics")
        }
        val clocksRepo = com.julian.automaticclockwidget.fixtures.FakeClocksPreferencesRepository()

        // Events in order: AAA, BBB, CCC
        val events = com.julian.automaticclockwidget.calendars.Events(
            listOf(
                com.julian.automaticclockwidget.calendars.Event("Foo - 111ON - AAA", kotlinx.datetime.LocalDateTime(2099,1,10,12,0,0), kotlinx.datetime.LocalDateTime(2099,1,10,13,0,0)),
                com.julian.automaticclockwidget.calendars.Event("Bar - 222ON - BBB", kotlinx.datetime.LocalDateTime(2099,1,11,12,0,0), kotlinx.datetime.LocalDateTime(2099,1,11,13,0,0)),
                com.julian.automaticclockwidget.calendars.Event("Baz - 333ON - CCC", kotlinx.datetime.LocalDateTime(2099,1,12,12,0,0), kotlinx.datetime.LocalDateTime(2099,1,12,13,0,0)),
            )
        )
        val calRepo = com.julian.automaticclockwidget.fixtures.FakeCalendarsRepository().apply { result = Result.success(com.julian.automaticclockwidget.calendars.Calendar("cid", events)) }
        val airRepo = com.julian.automaticclockwidget.fixtures.FakeAirportsRepository().apply {
            responses["AAA"] = Result.success(com.julian.automaticclockwidget.airports.Airport("AAA", "A City", kotlinx.datetime.TimeZone.of("UTC")))
            responses["BBB"] = Result.failure(Exception("not available"))
            responses["CCC"] = Result.success(com.julian.automaticclockwidget.airports.Airport("CCC", "C City", kotlinx.datetime.TimeZone.of("UTC")))
        }
        val downloadUC = com.julian.automaticclockwidget.calendars.DownloadCalendarUseCase(calRepo)
        val airportUC = com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase(airRepo)
        val getUpcomingUC = com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase(downloadUC, airportUC)

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        val res = sut.refreshNow()
        org.junit.Assert.assertTrue(res.isSuccess)

        val saved = clocksRepo.getClocks().getOrThrow()
        // BBB failed, so we should have AAA then CCC in that exact order
        org.junit.Assert.assertEquals(listOf("AAA", "CCC"), saved.map { it.iataCode })
    }

    @Test
    fun refresh_success_saves_mapped_clocks() = runBlocking {
        val urlRepo = FakeUrlPreferencesRepository().apply {
            addUrl("https://ics"); selectUrl("https://ics")
        }
        val clocksRepo = FakeClocksPreferencesRepository()

        val events = Events(
            listOf(
                Event("Foo - 123ON - JFK", LocalDateTime(2099,1,10,12,0,0), LocalDateTime(2099,1,10,13,0,0)),
                Event("Bar - 456ON - LHR", LocalDateTime(2099,1,11,12,0,0), LocalDateTime(2099,1,11,13,0,0)),
            )
        )
        val calRepo = FakeCalendarsRepository().apply { result = Result.success(Calendar("cid", events)) }
        val airRepo = FakeAirportsRepository().apply {
            responses["JFK"] = Result.success(Airport("JFK", "John F Kennedy", TimeZone.of("America/New_York")))
            responses["LHR"] = Result.success(Airport("LHR", "Heathrow", TimeZone.of("Europe/London")))
        }
        val downloadUC = DownloadCalendarUseCase(calRepo)
        val airportUC = GetAirportTimezoneUseCase(airRepo)
        val getUpcomingUC = GetUpcomingClocksUseCase(downloadUC, airportUC)

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        val res = sut.refreshNow()
        assertTrue(res.isSuccess)

        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(2, saved.size)
        assertEquals("JFK", saved[0].iataCode)
        assertEquals("America/New_York", saved[0].timezoneId)
        assertEquals("LHR", saved[1].iataCode)
        assertEquals("Europe/London", saved[1].timezoneId)
    }

    @Test
    fun no_selected_url_is_noop_success_and_does_not_modify_repo() = runBlocking {
        val urlRepo = FakeUrlPreferencesRepository() // no urls selected
        val clocksRepo = FakeClocksPreferencesRepository().apply {
            saveClocks(listOf(StoredClock("ABC", "X", "UTC")))
        }
        // Build getUpcoming with fakes (won't be called)
        val calRepo = FakeCalendarsRepository()
        val airRepo = FakeAirportsRepository()
        val getUpcomingUC = GetUpcomingClocksUseCase(DownloadCalendarUseCase(calRepo), GetAirportTimezoneUseCase(airRepo))

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        val res = sut.refreshNow()
        assertTrue(res.isSuccess)
        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(listOf(StoredClock("ABC", "X", "UTC")), saved)
    }

    @Test
    fun failure_from_getUpcoming_propagates_and_repo_unchanged() = runBlocking {
        val urlRepo = FakeUrlPreferencesRepository().apply {
            addUrl("https://ics"); selectUrl("https://ics")
        }
        val clocksRepo = FakeClocksPreferencesRepository().apply {
            saveClocks(listOf(StoredClock("DEF", "Y", "UTC")))
        }
        val calRepo = FakeCalendarsRepository().apply { result = Result.failure(Exception("boom")) }
        val airRepo = FakeAirportsRepository()
        val getUpcomingUC = GetUpcomingClocksUseCase(DownloadCalendarUseCase(calRepo), GetAirportTimezoneUseCase(airRepo))

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        val res = sut.refreshNow()
        assertTrue(res.isFailure)
        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(listOf(StoredClock("DEF", "Y", "UTC")), saved)
    }
}
