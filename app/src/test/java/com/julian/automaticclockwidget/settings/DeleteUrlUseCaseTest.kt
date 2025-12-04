package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteUrlUseCaseTest {

    @Test
    fun `given multiple urls when deleting selected then it is removed and previous becomes selected`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val delete = DeleteUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("https://a")
        add.addUrl("https://b")
        add.addUrl("https://c") // selected
        delete.deleteUrl("https://c")

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals(listOf("https://a", "https://b"), state.urls)
        assertEquals("https://b", state.selected)
    }

    @Test
    fun `given only one url when deleting then list becomes empty and selected is null`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val delete = DeleteUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("https://a")
        delete.deleteUrl("https://a")

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals(emptyList<String>(), state.urls)
        assertNull(state.selected)
    }

    @Test
    fun `given unknown url when deleting then returns NotFound error`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val delete = DeleteUrlUseCase(repo)
        
        // When
        val result = delete.deleteUrl("https://does-not-exist")
        
        // Then
        val error = result.exceptionOrNull()
        assertTrue(error is SettingsError.NotFound)
    }
}
