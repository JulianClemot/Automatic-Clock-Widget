package com.julian.automaticclockwidget.calendars

import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDateTime

class GetUpcomingClocksUseCase(
    private val downloadCalendarUseCase: DownloadCalendarUseCase,
    private val getAirportTimezoneUseCase: GetAirportTimezoneUseCase,
    private val observability: ObservabilityRepository,
) {

    suspend fun getUpcomingClocks(uri: String, startDate: LocalDateTime) =
        downloadCalendarUseCase.downloadCalendar(uri).map {
            it.events.getEventFromRange(startDate)
        }.mapCatching { events ->
            observability.log(
                message = "Filtered events to upcoming",
                category = "workflow",
                data = mapOf("eventsCount" to events.size),
            )
            val iataCodesByDate = events.map { event ->
                event.startDate to event.description.split("-").last().trim()
            }
            observability.log(
                message = "Extracted IATA codes",
                category = "workflow",
                data = mapOf(
                    "iataCodes" to iataCodesByDate.map { it.second },
                    "count" to iataCodesByDate.size,
                ),
            )
            coroutineScope {
                iataCodesByDate.map { iataCodeByDate ->
                    async {
                        iataCodeByDate.first to getAirportTimezoneUseCase.getAirportTimezone(
                            iataCodeByDate.second
                        )
                    }
                }.awaitAll()
            }
        }.mapCatching { results ->
            val sorted = results.sortedBy { it.first }
            val successes = sorted.count { it.second.isSuccess }
            val failures = sorted.filter { it.second.isFailure }
            observability.log(
                message = "Airport lookups completed",
                category = "workflow",
                data = mapOf("successCount" to successes, "failureCount" to failures.size),
            )
            failures.forEach { (_, result) ->
                result.exceptionOrNull()?.let { t ->
                    observability.sendErrorEvent(
                        throwable = t,
                        context = t.toErrorContext() + ("stage" to "airportLookup"),
                        tags = mapOf("feature" to "airports"),
                    )
                }
            }
            sorted.mapNotNull { it.second.getOrNull() }
        }
}
