package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.clocks.ClocksPreferencesRepository
import com.julian.automaticclockwidget.clocks.StoredClock

/** In-memory fake for ClocksPreferencesRepository used in unit tests. */
class FakeClocksPreferencesRepository : ClocksPreferencesRepository {
    private val data = mutableListOf<StoredClock>()

    override fun getClocks(): Result<List<StoredClock>> = Result.success(data.toList())

    override fun saveClocks(clocks: List<StoredClock>): Result<Unit> {
        data.clear()
        data.addAll(clocks)
        return Result.success(Unit)
    }

    override fun clearClocks(): Result<Unit> {
        data.clear()
        return Result.success(Unit)
    }
}
