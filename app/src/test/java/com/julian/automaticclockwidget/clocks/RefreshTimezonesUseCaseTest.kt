@file:OptIn(ExperimentalTime::class)

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
import kotlin.time.ExperimentalTime

class RefreshTimezonesUseCaseTest {

    @Test
    fun `given mixed airport resolutions then saved clocks preserve order of successes only`() = runBlocking {
        // Given
        val urlRepo = FakeUrlPreferencesRepository().apply {
            addUrl("https://ics"); selectUrl("https://ics")
        }
        val clocksRepo = FakeClocksPreferencesRepository()

        // Events in order: AAA, BBB, CCC
        val events = Events(
            listOf(
                Event("Foo - 111ON - AAA",
                    LocalDateTime(2099,1,10,12,0,0),
                    LocalDateTime(2099,1,10,13,0,0)
                ),
                Event("Bar - 222ON - BBB",
                    LocalDateTime(2099,1,11,12,0,0),
                    LocalDateTime(2099,1,11,13,0,0)
                ),
                Event("Baz - 333ON - CCC",
                    LocalDateTime(2099,1,12,12,0,0),
                    LocalDateTime(2099,1,12,13,0,0)
                ),
            )
        )
        val calRepo = FakeCalendarsRepository().apply { result = Result.success(
            Calendar("cid", events)
        ) }
        val airRepo = FakeAirportsRepository().apply {
            responses["AAA"] = Result.success(Airport("AAA", "A City", TimeZone.of("UTC")))
            responses["BBB"] = Result.failure(Exception("not available"))
            responses["CCC"] = Result.success(Airport("CCC", "C City", TimeZone.of("UTC")))
        }
        val downloadUC = DownloadCalendarUseCase(calRepo)
        val airportUC = GetAirportTimezoneUseCase(airRepo)
        val getUpcomingUC = GetUpcomingClocksUseCase(downloadUC, airportUC)

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        
        // When
        val res = sut.refreshNow()

        // Then
        assertTrue(res.isSuccess)

        val saved = clocksRepo.getClocks().getOrThrow()
        // BBB failed, so we should have AAA then CCC in that exact order
        assertEquals(listOf("AAA", "CCC"), saved.map { it.iataCode })
    }

    @Test
    fun `given successful upcoming clocks then repository saves mapped clocks`() = runBlocking {
        // Given
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
        
        // When
        val res = sut.refreshNow()

        // Then
        assertTrue(res.isSuccess)

        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(2, saved.size)
        assertEquals("JFK", saved[0].iataCode)
        assertEquals("America/New_York", saved[0].timezoneId)
        assertEquals("LHR", saved[1].iataCode)
        assertEquals("Europe/London", saved[1].timezoneId)
    }

    @Test
    fun `given no selected url then refresh is a no-op success and repository unchanged`() = runBlocking {
        // Given
        val urlRepo = FakeUrlPreferencesRepository() // no urls selected
        val clocksRepo = FakeClocksPreferencesRepository().apply {
            saveClocks(listOf(StoredClock("ABC", "X", "UTC")))
        }
        // Build getUpcoming with fakes (won't be called)
        val calRepo = FakeCalendarsRepository()
        val airRepo = FakeAirportsRepository()
        val getUpcomingUC = GetUpcomingClocksUseCase(DownloadCalendarUseCase(calRepo), GetAirportTimezoneUseCase(airRepo))

        val sut = RefreshTimezonesUseCase(urlRepo, clocksRepo, getUpcomingUC)
        
        // When
        val res = sut.refreshNow()

        // Then
        assertTrue(res.isSuccess)
        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(listOf(StoredClock("ABC", "X", "UTC")), saved)
    }

    @Test
    fun `given failure from getUpcoming then refresh forwards failure and repository unchanged`() = runBlocking {
        // Given
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
        
        // When
        val res = sut.refreshNow()

        // Then
        assertTrue(res.isFailure)
        val saved = clocksRepo.getClocks().getOrThrow()
        assertEquals(listOf(StoredClock("DEF", "Y", "UTC")), saved)
    }
}
