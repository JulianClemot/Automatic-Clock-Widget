package com.julian.automaticclockwidget.clocks

import com.julian.automaticclockwidget.airports.Airport
import com.julian.automaticclockwidget.calendars.GetUpcomingClocksUseCase
import com.julian.automaticclockwidget.core.sanitizeUrl
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.BreadcrumbLevel
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository
import kotlin.time.ExperimentalTime
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
    private val observability: ObservabilityRepository,
) {
    suspend fun refreshNow(): Result<Unit> {
        observability.log(message = "Loading selected URL", category = "workflow")

        // Read selected URL
        val selectedUrl = urlRepo.getSelectedUrl().getOrElse { t ->
            observability.sendErrorEvent(
                throwable = t,
                context = t.toErrorContext() + ("stage" to "getSelectedUrl"),
            )
            return Result.failure(t)
        }

        if (selectedUrl.isNullOrBlank()) {
            observability.log(
                message = "No URL selected; refresh is a no-op",
                category = "workflow",
                level = BreadcrumbLevel.WARNING,
            )
            return Result.success(Unit)
        }

        observability.log(
            message = "Selected URL resolved",
            category = "workflow",
            data = mapOf("host" to sanitizeUrl(selectedUrl)),
        )

        // Compute startDate = now (local tz)
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val startDate = now.toLocalDateTime(tz)

        observability.log(
            message = "Fetching upcoming clocks",
            category = "workflow",
            data = mapOf("startDate" to startDate.toString()),
        )

        // Fetch upcoming airports
        val airportsResult = getUpcomingClocksUseCase.getUpcomingClocks(selectedUrl, startDate)
        val airports = airportsResult.getOrElse { t ->
            observability.sendErrorEvent(
                throwable = t,
                context = t.toErrorContext() + ("stage" to "getUpcomingClocks"),
            )
            return Result.failure(t)
        }

        observability.log(
            message = "Computed clocks",
            category = "workflow",
            data = mapOf(
                "clocksCount" to airports.size,
                "iataCodes" to airports.map { it.iataCode },
            ),
        )

        val clocks = airports.map { it.toStoredClock() }
        return clocksRepo.saveClocks(clocks).onFailure { t ->
            observability.sendErrorEvent(
                throwable = t,
                context = t.toErrorContext() + ("stage" to "saveClocks"),
            )
        }.onSuccess {
            observability.log(
                message = "Clocks saved",
                category = "storage",
                data = mapOf("clocksCount" to clocks.size),
            )
        }
    }
}

private fun Airport.toStoredClock(): StoredClock =
    StoredClock(
        iataCode = iataCode,
        name = city.replace("_", " "),
        timezoneId = timezone.id
    )
