@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.julian.automaticclockwidget.clocks

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockDisplayFormatterTest {

    @Test
    fun format_produces_expected_time_and_day_for_timezones() {
        // 2025-01-05T12:34:00Z
        val instant = Instant.parse("2025-01-05T12:34:00Z")

        val ny = ClockDisplayFormatter.format("New York", "America/New_York", instant)
        // Jan 5, 2025 is Sunday. 12:34Z -> 07:34 in New York (EST, UTC-5)
        assertEquals("07:34", ny.time)
        assertEquals("Dimanche", ny.day)

        val london = ClockDisplayFormatter.format("London", "Europe/London", instant)
        // 12:34Z -> 12:34 in London (GMT, UTC+0 in Jan)
        assertEquals("12:34", london.time)
        assertEquals("Dimanche", london.day)
    }

    @Test
    fun format_invalid_timezone_returns_fallback() {
        val instant = Instant.parse("2025-01-05T00:00:00Z")
        val x = ClockDisplayFormatter.format("X", "Not/AZone", instant)
        assertEquals("--:--", x.time)
        assertEquals("N/A", x.day)
    }
}
