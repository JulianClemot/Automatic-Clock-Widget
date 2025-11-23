package com.julian.automaticclockwidget.calendars

import kotlinx.datetime.LocalDateTime

data class Events(val items: List<Event>) {

    fun getEventFromRange(startDate: LocalDateTime): List<Event> {
        return items.filter { it.startDate >= startDate }
            .filter { it.description.matches(relevantEventsRegex) }
    }

    companion object {
        private val relevantEventsRegex = Regex("^.+\\s*-\\s*\\d+ON\\s*-\\s*.{3,5}$")
    }
}