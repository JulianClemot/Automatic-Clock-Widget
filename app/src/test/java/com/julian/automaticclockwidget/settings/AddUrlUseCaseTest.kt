package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddUrlUseCaseTest {

    @Test
    fun `given urls added with spaces and duplicates then list is deduped trimmed and last is selected`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("  https://a  ")
        add.addUrl("https://b")
        add.addUrl("HTTPS://A") // duplicate different case -> moves to end and becomes selected

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf("https://b", "HTTPS://A"), state.urls)
        assertEquals("HTTPS://A", state.selected)
    }

    @Test
    fun `given blank url then addUrl fails with InvalidInput and state remains empty`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)

        // When
        val result = add.addUrl("   ")

        // Then
        val error = result.exceptionOrNull()
        assertTrue(error is SettingsError.InvalidInput)
        val state = GetUrlStateUseCase(repo).getUrlState().getOrThrow()
        assertEquals(emptyList<String>(), state.urls)
        assertEquals(null, state.selected)
    }
}
