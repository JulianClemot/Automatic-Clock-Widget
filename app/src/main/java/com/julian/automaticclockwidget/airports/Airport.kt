package com.julian.automaticclockwidget.airports

import kotlinx.datetime.TimeZone

data class Airport(val iataCode: String, val city: String, val timezone : TimeZone)
