package com.julian.automaticclockwidget.calendars

interface CalendarsRepository {
    suspend fun getCalendar(uri: String): Result<Calendar>
}