package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetUrlStateUseCaseTest {

    @Test
    fun `given urls added and one selected then state reflects entries and selected`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val select = SelectUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("", "https://a")
        add.addUrl("", "https://b")
        select.selectUrl("https://a")

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf(CalendarEntry("", "https://a"), CalendarEntry("", "https://b")), state.entries)
        assertEquals("https://a", state.selected)
    }

    @Test
    fun `given no selected or mismatch then state falls back to last url else null`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("", "https://a")
        add.addUrl("", "https://b") // selected becomes b
        // Simulate mismatch by deleting selected and not reselecting
        repo.deleteEntry("https://b")
        val state = get.getUrlState().getOrThrow()
        // Should fallback to last remaining ("https://a")
        assertEquals(listOf(CalendarEntry("", "https://a")), state.entries)
        assertEquals("https://a", state.selected)

        // If no entries, selected is null
        repo.deleteEntry("https://a")
        val emptyState = get.getUrlState().getOrThrow()
        assertEquals(emptyList<CalendarEntry>(), emptyState.entries)
        assertNull(emptyState.selected)
    }
}
