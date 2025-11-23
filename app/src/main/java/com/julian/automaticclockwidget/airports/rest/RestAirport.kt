package com.julian.automaticclockwidget.airports.rest

import com.julian.automaticclockwidget.airports.Airport
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RestAirport(
    val code: String,
    val icao: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Int?,
    val url: String?,
    @SerialName("time_zone")
    val timezone: String,
    @SerialName("city_code")
    val cityCode: String?,
    val country: String?,
    val city: String?,
    val state: String?,
    val county: String?,
    val type: String?
) {
    fun toAirport() = Airport(
        iataCode = code,
        city = timezone.split("/").lastOrNull() ?: city.orEmpty(),
        timezone = TimeZone.of(timezone),
    )
}