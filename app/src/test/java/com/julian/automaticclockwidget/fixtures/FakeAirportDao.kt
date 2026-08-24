package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.airports.local.AirportDao
import com.julian.automaticclockwidget.airports.local.AirportEntity

class FakeAirportDao : AirportDao {
    private val store = mutableMapOf<String, AirportEntity>()
    var insertCallCount = 0
    var deleteCallCount = 0

    override suspend fun findByIata(iataCode: String): AirportEntity? = store[iataCode]

    override suspend fun insertAll(airports: List<AirportEntity>) {
        insertCallCount++
        airports.forEach { store[it.iataCode] = it }
    }

    override suspend fun deleteAll() {
        deleteCallCount++
        store.clear()
    }
}
