package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteUrlUseCaseTest {

    @Test
    fun deleteUrl_removes_and_updates_selection() {
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val delete = DeleteUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        add.addUrl("https://a")
        add.addUrl("https://b")
        add.addUrl("https://c") // selected
        delete.deleteUrl("https://c")

        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf("https://a", "https://b"), state.urls)
        assertEquals("https://b", state.selected)
    }

    @Test
    fun delete_only_url_results_in_empty_and_no_selected() {
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val delete = DeleteUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        add.addUrl("https://a")
        delete.deleteUrl("https://a")

        val state = get.getUrlState().getOrThrow()
        assertEquals(emptyList<String>(), state.urls)
        assertNull(state.selected)
    }

    @Test
    fun delete_unknown_returns_NotFound_error() {
        val repo = FakeUrlPreferencesRepository()
        val delete = DeleteUrlUseCase(repo)
        val result = delete.deleteUrl("https://does-not-exist")
        val error = result.exceptionOrNull()
        assertTrue(error is SettingsError.NotFound)
    }
}
