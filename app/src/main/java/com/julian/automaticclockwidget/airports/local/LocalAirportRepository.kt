package com.julian.automaticclockwidget.airports.local

import android.content.SharedPreferences
import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.airports.AirportsRepository
import com.julian.automaticclockwidget.airports.github.AirportSource
import com.julian.automaticclockwidget.core.AppError
import com.julian.automaticclockwidget.core.AirportError
import com.julian.automaticclockwidget.core.UnknownError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LocalAirportRepository(
    private val dao: AirportDao,
    private val source: AirportSource,
    private val prefs: SharedPreferences,
) : AirportsRepository {

    override suspend fun findAirport(iataCode: String): Result<Airport> = withContext(Dispatchers.IO) {
        runCatching {
            syncIfNeeded()
            dao.findByIata(iataCode)?.toAirport()
                ?: throw AirportError.NotFound("Airport $iataCode not found in local database")
        }.recoverCatching { t ->
            when (t) {
                is AppError -> throw t
                is IOException -> throw AirportError.Network("Network error while syncing airports", t)
                else -> throw UnknownError(cause = t)
            }
        }
    }

    private suspend fun syncIfNeeded() {
        val lastDownload = prefs.getLong(KEY_LAST_DOWNLOAD, -1L)
        if (lastDownload == -1L) {
            downloadAndStore()
        } else {
            val lastCommit = source.getLastCommitInstant()
            if (lastCommit.toEpochMilliseconds() > lastDownload) {
                downloadAndStore()
            }
        }
    }

    private suspend fun downloadAndStore() {
        val entities = source.downloadAirports()
        dao.replaceAll(entities)
        prefs.edit()
            .putLong(KEY_LAST_DOWNLOAD, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val KEY_LAST_DOWNLOAD = "airports_last_download_epoch"
    }
}
