package com.julian.automaticclockwidget.airports

import kotlinx.datetime.TimeZone

data class Airport(
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val timezone: TimeZone,
)
