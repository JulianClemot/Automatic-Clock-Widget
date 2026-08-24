package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.airports.github.AirportSource
import com.julian.automaticclockwidget.airports.local.AirportEntity
import kotlin.time.Instant

class FakeGithubAirportsSource : AirportSource {
    var airportsToReturn: List<AirportEntity> = emptyList()
    var lastCommitInstant: Instant = Instant.fromEpochMilliseconds(0)
    var downloadCallCount = 0
    var commitCheckCallCount = 0

    override suspend fun downloadAirports(): List<AirportEntity> {
        downloadCallCount++
        return airportsToReturn
    }

    override suspend fun getLastCommitInstant(): Instant {
        commitCheckCallCount++
        return lastCommitInstant
    }
}
