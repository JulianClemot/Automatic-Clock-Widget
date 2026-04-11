package com.julian.automaticclockwidget.settings

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEntry(val name: String, val url: String)
