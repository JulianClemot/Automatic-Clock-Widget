package com.julian.automaticclockwidget.airports

interface AirportsRepository {

    suspend fun findAirport(iataCode : String) : Result<Airport>
}