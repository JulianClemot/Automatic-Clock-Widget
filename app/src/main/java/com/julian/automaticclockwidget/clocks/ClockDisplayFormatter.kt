@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.julian.automaticclockwidget.clocks

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formats a stored clock (airport/timezone) into display-friendly values for the widget.
 */
data class ClockDisplay(
    val name: String,
    val time: String, // HH:mm
    val day: String,  // Mon/Tue/... three-letter day
)

object ClockDisplayFormatter {

    /**
     * Format the time/day for the provided timezone id at a specific Instant.
     * If the timezone id is invalid, returns a graceful fallback ("--:--", "N/A").
     */
    fun format(name: String, timezoneId: String, now: Instant): ClockDisplay {
        return try {
            val tz = TimeZone.of(timezoneId)
            val ldt = now.toLocalDateTime(tz)
            val hh = ldt.hour.toString().padStart(2, '0')
            val mm = ldt.minute.toString().padStart(2, '0')
            val time = "$hh:$mm"
            val day = toFullDay(ldt.dayOfWeek)
            ClockDisplay(name = name, time = time, day = day)
        } catch (t: Throwable) {
            ClockDisplay(name = name, time = "--:--", day = "N/A")
        }
    }

    private fun toFullDay(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "Lundi"
        DayOfWeek.TUESDAY -> "Mardi"
        DayOfWeek.WEDNESDAY -> "Mercredi"
        DayOfWeek.THURSDAY -> "Jeudi"
        DayOfWeek.FRIDAY -> "Vendredi"
        DayOfWeek.SATURDAY -> "Samedi"
        DayOfWeek.SUNDAY -> "Dimanche"
    }
}
