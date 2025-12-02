package com.julian.automaticclockwidget.clocks

import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.StoredClock
import kotlin.time.ExperimentalTime
import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalTime::class)
class RefreshTimezonesUseCase(
    private val urlRepo: UrlPreferencesRepository,
    private val clocksRepo: ClocksPreferencesRepository,
    private val getUpcomingClocksUseCase: GetUpcomingClocksUseCase,
) {
    suspend fun refreshNow(): Result<Unit> {
        // Read selected URL
        val selectedUrl = urlRepo.getSelectedUrl().getOrElse { return Result.failure(it) }
        if (selectedUrl.isNullOrBlank()) {
            // No URL selected: treat as success no-op
            return Result.success(Unit)
        }

        // Compute startDate = now - 3 days (local tz)
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val startDate = now.toLocalDateTime(tz)

        // Fetch upcoming airports
        val airportsResult = getUpcomingClocksUseCase.getUpcomingClocks(selectedUrl, startDate)
        val airports = airportsResult.getOrElse { return Result.failure(it) }

        val clocks = airports.map { it.toStoredClock() }
        return clocksRepo.saveClocks(clocks)
    }
}

private fun Airport.toStoredClock(): StoredClock =
    StoredClock(
        iataCode = iataCode,
        name = city.replace("_", " "),
        timezoneId = timezone.id
    )
