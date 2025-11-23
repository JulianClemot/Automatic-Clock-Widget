@file:OptIn(ExperimentalTime::class)

package com.julian.automaticclockwidget.calendars.iCalendar

import biweekly.ICalendar
import com.julian.automaticclockwidget.calendars.Calendar
import com.julian.automaticclockwidget.calendars.Event
import com.julian.automaticclockwidget.calendars.Events
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


fun ICalendar.toCalendar(timezone: TimeZone): Calendar {
    val events = Events(events.map {
        Event(
            it.summary.value,
            it.dateStart.value.toInstant().toKotlinInstant()
                .toLocalDateTime(timezone),
            it.dateEnd.value.toInstant().toKotlinInstant()
                .toLocalDateTime(timezone)
        )
    })
    return Calendar(uid?.value ?: "1", events)
}