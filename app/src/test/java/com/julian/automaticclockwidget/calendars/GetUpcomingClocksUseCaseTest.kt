package com.julian.automaticclockwidget.calendars

import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.fixtures.FakeAirportsRepository
import com.julian.automaticclockwidget.fixtures.FakeCalendarsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetUpcomingClocksUseCaseTest {

    @Test
    fun success_filters_events_and_aggregates_successful_airports_only() = runBlocking {
        // Arrange calendar with mixed events
        val start = LocalDateTime(2025, 1, 10, 12, 0, 0)
        val events = Events(
            listOf(
                // Before startDate -> filtered out
                Event("Before - 123ON - JFK", LocalDateTime(2025, 1, 9, 10, 0, 0), LocalDateTime(2025, 1, 9, 11, 0, 0)),
                // After but invalid description (no 123ON section) -> filtered out
                Event("Invalid description", LocalDateTime(2025, 1, 10, 13, 0, 0), LocalDateTime(2025, 1, 10, 14, 0, 0)),
                // Valid -> IATA JFK
                Event("Foo - 123ON - JFK", LocalDateTime(2025, 1, 10, 14, 0, 0), LocalDateTime(2025, 1, 10, 15, 0, 0)),
                // Valid -> IATA LHR
                Event("Bar - 456ON - LHR", LocalDateTime(2025, 1, 11, 8, 0, 0), LocalDateTime(2025, 1, 11, 9, 0, 0)),
            )
        )

        val fakeCalRepo = FakeCalendarsRepository().apply {
            result = Result.success(Calendar("cid", events))
        }
        val downloadUC = DownloadCalendarUseCase(fakeCalRepo)

        val fakeAirRepo = FakeAirportsRepository().apply {
            responses["JFK"] = Result.success(Airport("JFK", "John F Kennedy", TimeZone.of("America/New_York")))
            responses["LHR"] = Result.failure(Exception("not available"))
        }
        val airportUC = GetAirportTimezoneUseCase(fakeAirRepo)

        val uc = GetUpcomingClocksUseCase(downloadUC, airportUC)

        // Act
        val result = uc.getUpcomingClocks("https://ics", start)

        // Assert
        assertTrue(result.isSuccess)
        val airports = result.getOrNull()!!
        // Only JFK should be included (LHR failed) -> list size 1
        assertEquals(1, airports.size)
        assertEquals("JFK", airports.first().iataCode)
    }

    @Test
    fun failure_is_propagated_from_calendar_download() = runBlocking {
        val fakeCalRepo = FakeCalendarsRepository().apply {
            result = Result.failure(Exception("download failed"))
        }
        val downloadUC = DownloadCalendarUseCase(fakeCalRepo)

        val fakeAirRepo = FakeAirportsRepository() // should not be consulted
        val airportUC = GetAirportTimezoneUseCase(fakeAirRepo)

        val uc = GetUpcomingClocksUseCase(downloadUC, airportUC)

        val result = uc.getUpcomingClocks("https://ics", LocalDateTime(2025, 1, 10, 0, 0, 0))
        assertTrue(result.isFailure)
    }
}
