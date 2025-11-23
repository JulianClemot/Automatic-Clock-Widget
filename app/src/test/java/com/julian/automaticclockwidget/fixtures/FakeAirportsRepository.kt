package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.airports.AirportsRepository

/** In-memory fake AirportsRepository for tests. */
class FakeAirportsRepository : AirportsRepository {
    val responses: MutableMap<String, Result<Airport>> = mutableMapOf()
    override suspend fun findAirport(iataCode: String): Result<Airport> =
        responses[iataCode] ?: Result.failure(IllegalArgumentException("Unknown $iataCode"))
}
