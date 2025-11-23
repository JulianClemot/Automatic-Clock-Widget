package com.julian.automaticclockwidget.airports.rest

import com.julian.automaticclockwidget.BuildConfig
import com.julian.automaticclockwidget.airports.AirportsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

class RestAirportRepository(private val client: okhttp3.OkHttpClient) : AirportsRepository {

    override suspend fun findAirport(iataCode: String) = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(FIND_AIRPORT_REQUEST.replace("{:iata}", iataCode))

        buildHeaders().forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val request = requestBuilder.build()
        runCatching {
            val response = client.newCall(request).execute()
            val restResult = Json.decodeFromString<RestAirport>(response.body.string())
            restResult.toAirport()
        }

    }

    companion object {
        private const val FIND_AIRPORT_ENDPOINT =
            "/{:iata}/"
        private const val FIND_AIRPORT_REQUEST =
            "${BuildConfig.AIRPORTS_BASE_URL} + $FIND_AIRPORT_ENDPOINT"

        private fun buildHeaders() = listOf(
            "x-rapidapi-host" to "iata-airports.p.rapidapi.com",
            "x-rapidapi-key" to BuildConfig.AIRPORTS_API_KEY
        )
    }
}