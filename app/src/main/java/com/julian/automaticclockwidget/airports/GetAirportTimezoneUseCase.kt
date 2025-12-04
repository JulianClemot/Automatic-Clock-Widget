package com.julian.automaticclockwidget.airports

class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
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