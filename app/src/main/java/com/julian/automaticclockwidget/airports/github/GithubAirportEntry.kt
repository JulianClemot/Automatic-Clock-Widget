package com.julian.automaticclockwidget.airports.github

import kotlinx.serialization.Serializable

@Serializable
data class GithubAirportEntry(
    val icao: String = "",
    val iata: String = "",
    val name: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val elevation: Int = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tz: String = "",
)
