package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.calendars.Calendar
import com.julian.automaticclockwidget.calendars.CalendarsRepository

/** In-memory fake CalendarsRepository for tests. */
class FakeCalendarsRepository : CalendarsRepository {
    var lastUri: String? = null
    var result: Result<Calendar> = Result.failure(IllegalStateException("unset"))
    override suspend fun getCalendar(uri: String): Result<Calendar> {
        lastUri = uri
        return result
    }
}
