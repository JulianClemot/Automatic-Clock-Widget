package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetUrlStateUseCaseTest {

    @Test
    fun `given urls added and one selected then state reflects urls and selected`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val select = SelectUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("https://a")
        add.addUrl("https://b")
        select.selectUrl("https://a")

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf("https://a", "https://b"), state.urls)
        assertEquals("https://a", state.selected)
    }

    @Test
    fun `given no selected or mismatch then state falls back to last url else null`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("https://a")
        add.addUrl("https://b") // selected becomes b
        // Simulate mismatch by deleting selected and not reselecting
        repo.deleteUrl("https://b")
        val state = get.getUrlState().getOrThrow()
        // Should fallback to last remaining ("https://a")
        assertEquals(listOf("https://a"), state.urls)
        assertEquals("https://a", state.selected)

        // If no urls, selected is null
        repo.deleteUrl("https://a")
        val emptyState = get.getUrlState().getOrThrow()
        assertEquals(emptyList<String>(), emptyState.urls)
        assertNull(emptyState.selected)
    }
}
