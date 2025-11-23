package com.julian.automaticclockwidget.calendars

import kotlinx.datetime.LocalDateTime

data class Event(val description: String, val startDate: LocalDateTime, val endDate: LocalDateTime)