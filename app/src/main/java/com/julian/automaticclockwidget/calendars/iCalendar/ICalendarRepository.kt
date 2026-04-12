@file:OptIn(ExperimentalTime::class)

package com.julian.automaticclockwidget.calendars.iCalendar

import biweekly.Biweekly
import com.julian.automaticclockwidget.calendars.Calendar
import com.julian.automaticclockwidget.calendars.CalendarsRepository
import com.julian.automaticclockwidget.core.CalendarError
import com.julian.automaticclockwidget.core.UnknownError
import com.julian.automaticclockwidget.core.sanitizeUrl
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.BreadcrumbLevel
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.time.ExperimentalTime

class ICalendarRepository(
    private val client: OkHttpClient,
    private val observability: ObservabilityRepository,
) : CalendarsRepository {

    override suspend fun getCalendar(uri: String) = runCatching {
        val body = downloadCalendar(uri)
        parseCalendar(body)
    }.recoverCatching { t ->
        when (t) {
            is CalendarError -> throw t
            is IOException -> throw CalendarError.Network(
                message = "Network error while downloading calendar",
                cause = t
            )
            is IllegalArgumentException, is IllegalStateException ->
                throw CalendarError.Parse(
                    message = "Invalid iCalendar content",
                    cause = t
                )
            else -> throw UnknownError(cause = t)
        }
    }.onFailure { t ->
        observability.sendErrorEvent(
            throwable = t,
            context = t.toErrorContext() + ("stage" to "downloadCalendar") + ("host" to sanitizeUrl(uri)),
            tags = mapOf("feature" to "calendars"),
        )
    }

    private suspend fun downloadCalendar(uri: String) = withContext(Dispatchers.IO) {
        val host = sanitizeUrl(uri)
        observability.log(
            message = "Downloading calendar",
            category = "network",
            data = mapOf("host" to host),
        )

        val request = Request.Builder()
            .url(uri.replace("webcal://", "https://"))
            .addHeader("Accept", "text/calendar")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                observability.log(
                    message = "Calendar HTTP failure",
                    category = "network",
                    level = BreadcrumbLevel.WARNING,
                    data = mapOf("code" to response.code, "host" to host),
                )
                throw CalendarError.HttpFailure(
                    code = response.code,
                    message = response.message
                )
            }
            val body = response.body.string()
            observability.log(
                message = "Calendar downloaded",
                category = "network",
                data = mapOf("bytes" to body.length, "host" to host),
            )
            body
        }
    }

    private fun parseCalendar(calendarContent: String): Calendar {
        observability.log(
            message = "Parsing calendar",
            category = "parse",
            data = mapOf("bytes" to calendarContent.length),
        )
        val timeZone = TimeZone.currentSystemDefault()
        val icalendar = Biweekly.parse(calendarContent).first()
        val calendar = icalendar.toCalendar(timeZone)
        observability.log(
            message = "Calendar parsed",
            category = "parse",
            data = mapOf("eventsCount" to calendar.events.items.size),
        )
        return calendar
    }
}
