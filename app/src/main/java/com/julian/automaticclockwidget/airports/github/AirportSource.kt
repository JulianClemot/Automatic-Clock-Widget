package com.julian.automaticclockwidget.airports.github

import com.julian.automaticclockwidget.airports.local.AirportEntity
import kotlin.time.Instant

interface AirportSource {
    suspend fun downloadAirports(): List<AirportEntity>
    suspend fun getLastCommitInstant(): Instant
}
