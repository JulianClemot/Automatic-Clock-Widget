package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddUrlUseCaseTest {

    @Test
    fun addUrl_appends_dedups_trims_and_selects_last() {
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        add.addUrl("  https://a  ")
        add.addUrl("https://b")
        add.addUrl("HTTPS://A") // duplicate different case -> moves to end and becomes selected

        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf("https://b", "HTTPS://A"), state.urls)
        assertEquals("HTTPS://A", state.selected)
    }

    @Test
    fun addUrl_blank_returns_InvalidInput_error() {
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val result = add.addUrl("   ")
        val error = result.exceptionOrNull()
        assertTrue(error is SettingsError.InvalidInput)
        val state = GetUrlStateUseCase(repo).getUrlState().getOrThrow()
        assertEquals(emptyList<String>(), state.urls)
        assertEquals(null, state.selected)
    }
}
