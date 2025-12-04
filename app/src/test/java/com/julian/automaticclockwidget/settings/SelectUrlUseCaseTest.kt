package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectUrlUseCaseTest {

    @Test
    fun `given existing url when selecting then selection is updated`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val select = SelectUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        // When
        add.addUrl("https://a")
        add.addUrl("https://b") // selected
        select.selectUrl("https://a")

        // Then
        val state = get.getUrlState().getOrThrow()
        assertEquals("https://a", state.selected)
    }

    @Test
    fun `given non existing url when selecting then failure is returned and selection unchanged`() {
        // Given
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val select = SelectUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        add.addUrl("https://a")
        add.addUrl("https://b") // selected

        // When
        val failure = select.selectUrl("https://c").exceptionOrNull()

        // Then
        requireNotNull(failure)
        val state = get.getUrlState().getOrThrow()
        assertEquals("https://b", state.selected)
    }
}
