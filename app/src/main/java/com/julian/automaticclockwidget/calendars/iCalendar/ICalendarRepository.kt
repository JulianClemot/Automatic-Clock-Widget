@file:OptIn(ExperimentalTime::class)

package com.julian.automaticclockwidget.calendars.iCalendar

import biweekly.Biweekly
import com.julian.automaticclockwidget.calendars.Calendar
import com.julian.automaticclockwidget.calendars.CalendarsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.ExperimentalTime

class ICalendarRepository(private val client: OkHttpClient) : CalendarsRepository {

    override suspend fun getCalendar(uri: String) = runCatching {
        parseCalendar(downloadCalendar(uri))
    }

    private suspend fun downloadCalendar(uri: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(uri.replace("webcal://", "https://"))
            .addHeader("Accept", "text/calendar")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Throwable(response.message)
            }
            response.body.string()
        }
    }

    private fun parseCalendar(calendarContent: String): Calendar {
        val timeZone = TimeZone.currentSystemDefault()
        val icalendar = Biweekly.parse(calendarContent).first()
        return icalendar.toCalendar(timeZone)
    }
}