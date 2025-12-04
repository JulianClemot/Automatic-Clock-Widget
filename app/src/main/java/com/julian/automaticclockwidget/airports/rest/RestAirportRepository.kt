package com.julian.automaticclockwidget.airports.rest

import com.julian.automaticclockwidget.BuildConfig
import com.julian.automaticclockwidget.airports.AirportsRepository
import com.julian.automaticclockwidget.core.AppError
import com.julian.automaticclockwidget.core.AirportError
import com.julian.automaticclockwidget.core.UnknownError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException

class RestAirportRepository(private val client: okhttp3.OkHttpClient) : AirportsRepository {

    override suspend fun findAirport(iataCode: String) = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(FIND_AIRPORT_REQUEST.replace("{:iata}", iataCode))

        buildHeaders().forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val request = requestBuilder.build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        throw AirportError.NotFound("Airport $iataCode not found")
                    } else {
                        throw AirportError.Network("HTTP ${response.code}: ${response.message}")
                    }
                }
                val body = response.body.string()
                val restResult = Json.decodeFromString<RestAirport>(body)
                restResult.toAirport()
            }
        }.recoverCatching { t ->
            when (t) {
                is AppError -> throw t
                is IOException -> throw AirportError.Network("Network error while requesting airport $iataCode", t)
                is IllegalArgumentException, is IllegalStateException -> throw UnknownError("Invalid airport response", t)
                else -> throw UnknownError(cause = t)
            }
        }
    }

    companion object {
        private const val FIND_AIRPORT_ENDPOINT =
            "/{:iata}/"
        private const val FIND_AIRPORT_REQUEST =
            "${BuildConfig.AIRPORTS_BASE_URL}$FIND_AIRPORT_ENDPOINT"

        private fun buildHeaders() = listOf(
            "x-rapidapi-host" to "iata-airports.p.rapidapi.com",
            "x-rapidapi-key" to BuildConfig.AIRPORTS_API_KEY
        )
    }
}