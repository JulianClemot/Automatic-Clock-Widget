package com.julian.automaticclockwidget.clocks

import kotlinx.serialization.Serializable

/**
 * Persisted representation of a clock derived from a calendar event.
 * - iataCode: airport IATA code (e.g., "JFK")
 * - name: human-readable airport/city name
 * - timezoneId: kotlinx-datetime TimeZone.id string (e.g., "America/New_York")
 */
@Serializable
data class StoredClock(
    val iataCode: String,
    val name: String,
    val timezoneId: String,
)
