package com.julian.automaticclockwidget.airports

class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
    }
}