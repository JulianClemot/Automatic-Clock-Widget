package com.julian.automaticclockwidget.airports.github

import com.julian.automaticclockwidget.airports.local.AirportEntity
import com.julian.automaticclockwidget.core.AirportError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private val githubJson = Json { ignoreUnknownKeys = true }

class GithubAirportSource(private val client: OkHttpClient) : AirportSource {

    override suspend fun downloadAirports(): List<AirportEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(AIRPORTS_JSON_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw AirportError.Network("HTTP ${response.code} downloading airports.json")
            val body = response.body.string()
            val entries = githubJson.decodeFromString<Map<String, GithubAirportEntry>>(body)
            entries.values
                .filter { it.iata.isNotBlank() && it.tz.isNotBlank() }
                .map { entry ->
                    AirportEntity(
                        iataCode = entry.iata,
                        name = entry.name,
                        city = entry.city,
                        country = entry.country,
                        timezone = entry.tz,
                    )
                }
        }
    }

    override suspend fun getLastCommitInstant(): Instant = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LAST_COMMIT_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw AirportError.Network("HTTP ${response.code} fetching airport commit info")
            val body = response.body.string()
            val commits = githubJson.decodeFromString<List<GithubCommitResponse>>(body)
            val dateStr = commits.firstOrNull()?.commit?.committer?.date
                ?: throw AirportError.Network("No commits found for airports.json")
            Instant.parse(dateStr)
        }
    }

    companion object {
        private const val AIRPORTS_JSON_URL =
            "https://raw.githubusercontent.com/mwgg/Airports/master/airports.json"
        private const val LAST_COMMIT_URL =
            "https://api.github.com/repos/mwgg/Airports/commits?path=airports.json&per_page=1"
    }
}

@Serializable
private data class GithubCommitResponse(val commit: Commit) {
    @Serializable
    data class Commit(val committer: Committer)

    @Serializable
    data class Committer(val date: String)
}
