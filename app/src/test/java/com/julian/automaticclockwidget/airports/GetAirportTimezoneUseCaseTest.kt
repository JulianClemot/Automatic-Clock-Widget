package com.julian.automaticclockwidget.airports

import com.julian.automaticclockwidget.fixtures.FakeAirportsRepository
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAirportTimezoneUseCaseTest {

    @Test
    fun `given repository returns airport when requesting timezone then use case forwards success`() = runBlocking {
        // Given
        val repo = FakeAirportsRepository().apply {
            responses["JFK"] = Result.success(Airport("JFK", "John F Kennedy", TimeZone.of("America/New_York")))
        }
        val uc = GetAirportTimezoneUseCase(repo)

        // When
        val result = uc.getAirportTimezone("JFK")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("JFK", result.getOrNull()!!.iataCode)
    }

    @Test
    fun `given repository fails when requesting timezone then use case forwards failure`() = runBlocking {
        // Given
        val repo = FakeAirportsRepository().apply {
            responses["LHR"] = Result.failure(Exception("Not found"))
        }
        val uc = GetAirportTimezoneUseCase(repo)

        // When
        val result = uc.getAirportTimezone("LHR")

        // Then
        assertTrue(result.isFailure)
    }
}
