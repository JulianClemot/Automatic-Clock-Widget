package com.julian.automaticclockwidget.calendars

class DownloadCalendarUseCase(private val calendarsRepository: CalendarsRepository) {

    suspend fun downloadCalendar(uri: String) = calendarsRepository.getCalendar(uri)
}