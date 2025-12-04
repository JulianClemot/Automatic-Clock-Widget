package com.julian.automaticclockwidget.clocks

import com.julian.automaticclockwidget.fixtures.FakeClocksPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearClocksUseCaseTest {

    @Test
    fun `given stored clocks when clearing then repository becomes empty`() {
        // Given
        val repo = FakeClocksPreferencesRepository()
        // seed some clocks
        repo.saveClocks(
            listOf(
                StoredClock("JFK", "John F Kennedy", "America/New_York"),
                StoredClock("LHR", "Heathrow", "Europe/London")
            )
        )

        val uc = ClearClocksUseCase(repo)

        // When
        val res = uc.clearClocks()

        // Then
        assert(res.isSuccess)
        val after = repo.getClocks().getOrThrow()
        assertEquals(emptyList<StoredClock>(), after)
    }
}
