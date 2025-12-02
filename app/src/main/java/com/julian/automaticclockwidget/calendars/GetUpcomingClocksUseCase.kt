package com.julian.automaticclockwidget.calendars

import com.julian.automaticclockwidget.airports.GetAirportTimezoneUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDateTime

class GetUpcomingClocksUseCase(
    private val downloadCalendarUseCase: DownloadCalendarUseCase,
    private val getAirportTimezoneUseCase: GetAirportTimezoneUseCase,
) {

    suspend fun getUpcomingClocks(uri: String, startDate: LocalDateTime) =
        downloadCalendarUseCase.downloadCalendar(uri).map {
            it.events.getEventFromRange(startDate)
        }.mapCatching { events ->
            val iataCodesByDate = events.map { event ->
                event.startDate to event.description.split("-").last().trim()
            }
            coroutineScope {
                iataCodesByDate.map { iataCodeByDate ->
                    async {
                        iataCodeByDate.first to getAirportTimezoneUseCase.getAirportTimezone(
                            iataCodeByDate.second
                        )
                    }
                }.awaitAll()
            }
        }.mapCatching {
            it.sortedBy { result -> result.first }
                .mapNotNull { result -> result.second.getOrNull() }
        }
}