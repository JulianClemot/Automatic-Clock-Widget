package com.julian.automaticclockwidget.workers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.*

class WeeklySchedulerTest {

    private fun compute(now: ZonedDateTime): Long =
        WeeklyScheduler.computeInitialDelayToNextSundayAtEight(now)

    @Test
    fun sunday_before_0800_targets_today_0800() {
        val now = ZonedDateTime.of(2025, 1, 5, 7, 59, 0, 0, ZoneOffset.UTC) // Sunday
        val delayMs = compute(now)
        assertEquals(Duration.ofMinutes(1).toMillis(), delayMs)
    }

    @Test
    fun sunday_at_0800_targets_next_week() {
        val now = ZonedDateTime.of(2025, 1, 5, 8, 0, 0, 0, ZoneOffset.UTC) // Sunday
        val delayMs = compute(now)
        assertEquals(Duration.ofDays(7).toMillis(), delayMs)
    }

    @Test
    fun sunday_after_0800_targets_next_week_6d23h() {
        val now = ZonedDateTime.of(2025, 1, 5, 9, 0, 0, 0, ZoneOffset.UTC) // Sunday 09:00
        val delayMs = compute(now)
        val expected = Duration.ofDays(6).plusHours(23).toMillis()
        assertEquals(expected, delayMs)
    }

    @Test
    fun saturday_1000_targets_sunday_0800_in_22h() {
        val now = ZonedDateTime.of(2025, 1, 4, 10, 0, 0, 0, ZoneOffset.UTC) // Saturday 10:00
        val delayMs = compute(now)
        assertEquals(Duration.ofHours(22).toMillis(), delayMs)
    }

    @Test
    fun monday_1200_targets_next_sunday_0800_in_5d20h() {
        val now = ZonedDateTime.of(2025, 1, 6, 12, 0, 0, 0, ZoneOffset.UTC) // Monday 12:00
        val delayMs = compute(now)
        val expected = Duration.ofDays(5).plusHours(20).toMillis()
        assertEquals(expected, delayMs)
    }
}
