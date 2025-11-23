package com.julian.automaticclockwidget.airports

import com.julian.automaticclockwidget.fixtures.FakeAirportsRepository
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAirportTimezoneUseCaseTest {

    @Test
    fun forwards_success_from_repository() = runBlocking {
        val repo = FakeAirportsRepository().apply {
            responses["JFK"] = Result.success(Airport("JFK", "John F Kennedy", TimeZone.of("America/New_York")))
        }
        val uc = GetAirportTimezoneUseCase(repo)

        val result = uc.getAirportTimezone("JFK")
        assertTrue(result.isSuccess)
        assertEquals("JFK", result.getOrNull()!!.iataCode)
    }

    @Test
    fun forwards_failure_from_repository() = runBlocking {
        val repo = FakeAirportsRepository().apply {
            responses["LHR"] = Result.failure(Exception("Not found"))
        }
        val uc = GetAirportTimezoneUseCase(repo)

        val result = uc.getAirportTimezone("LHR")
        assertTrue(result.isFailure)
    }
}
