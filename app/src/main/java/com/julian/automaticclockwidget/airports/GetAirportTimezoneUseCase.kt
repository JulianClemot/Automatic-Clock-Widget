package com.julian.automaticclockwidget.airports

import com.julian.automaticclockwidget.observability.ObservabilityRepository

class GetAirportTimezoneUseCase(
    private val airportsRepository: AirportsRepository,
    private val observability: ObservabilityRepository,
) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        observability.log(
            message = "Looking up airport",
            category = "workflow",
            data = mapOf("iataCode" to iataCode),
        )
        return airportsRepository.findAirport(iataCode)
            .recoverCatching { throwable ->
                // Re-wrap into a domain-specific error while preserving the cause
                throw com.julian.automaticclockwidget.core.AirportError.NotFound(
                    message = "Airport $iataCode not found",
                    cause = throwable
                )
            }
    }
}
