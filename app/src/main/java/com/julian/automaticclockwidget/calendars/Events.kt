package com.julian.automaticclockwidget.calendars

import kotlinx.datetime.LocalDateTime

data class Events(val items: List<Event>) {

    fun getEventFromRange(startDate: LocalDateTime): List<Event> {
        val test = listOf(
            Event(
                "HFR234234 - 4ON - ABJ",
                LocalDateTime(2025, 11, 21, 12, 0, 0),
                LocalDateTime(2025, 11, 23, 13, 0, 0)
            )
        ) + items
        return test.filter {
            it.endDate.date >= startDate.date
                    && it.description.matches(relevantEventsRegex)
        }
    }

    companion object {
        private val relevantEventsRegex = Regex("^.+\\s*-\\s*\\d+ON\\s*-\\s*.{3,5}$")
    }
}