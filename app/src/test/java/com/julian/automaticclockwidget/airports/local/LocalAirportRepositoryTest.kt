package com.julian.automaticclockwidget.airports.local

import com.julian.automaticclockwidget.core.AirportError
import com.julian.automaticclockwidget.fixtures.FakeAirportDao
import com.julian.automaticclockwidget.fixtures.FakeGithubAirportsSource
import com.julian.automaticclockwidget.fixtures.FakeSharedPreferences
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalAirportRepositoryTest {

    private lateinit var dao: FakeAirportDao
    private lateinit var source: FakeGithubAirportsSource
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var repository: LocalAirportRepository

    private val jfkEntity = AirportEntity(
        iataCode = "JFK",
        name = "John F. Kennedy International Airport",
        city = "New York City",
        country = "US",
        timezone = "America/New_York",
    )

    @Before
    fun setUp() {
        dao = FakeAirportDao()
        source = FakeGithubAirportsSource().apply {
            airportsToReturn = listOf(jfkEntity)
            lastCommitInstant = Instant.fromEpochMilliseconds(1_000)
        }
        prefs = FakeSharedPreferences()
        repository = LocalAirportRepository(dao, source, prefs)
    }

    @Test
    fun `given no prior download when findAirport called then downloads and stores airports`() = runBlocking {
        val result = repository.findAirport("JFK")

        assertTrue(result.isSuccess)
        assertEquals("JFK", result.getOrNull()!!.iataCode)
        assertEquals(1, source.downloadCallCount)
        assertEquals(0, source.commitCheckCallCount)
        assertEquals(1, dao.insertCallCount)
    }

    @Test
    fun `given prior download and no newer commit when findAirport called then skips download`() = runBlocking {
        prefs.edit().putLong("airports_last_download_epoch", 2_000L).apply()
        dao.insertAll(listOf(jfkEntity))

        source.lastCommitInstant = Instant.fromEpochMilliseconds(1_000)
        source.downloadCallCount = 0

        val result = repository.findAirport("JFK")

        assertTrue(result.isSuccess)
        assertEquals(0, source.downloadCallCount)
        assertEquals(1, source.commitCheckCallCount)
    }

    @Test
    fun `given prior download and newer commit when findAirport called then re-downloads`() = runBlocking {
        prefs.edit().putLong("airports_last_download_epoch", 500L).apply()
        source.lastCommitInstant = Instant.fromEpochMilliseconds(1_000)

        val result = repository.findAirport("JFK")

        assertTrue(result.isSuccess)
        assertEquals(1, source.downloadCallCount)
        assertEquals(1, source.commitCheckCallCount)
    }

    @Test
    fun `given airport not in database when findAirport called then returns AirportError NotFound`() = runBlocking {
        val result = repository.findAirport("XXX")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AirportError.NotFound)
    }
}
