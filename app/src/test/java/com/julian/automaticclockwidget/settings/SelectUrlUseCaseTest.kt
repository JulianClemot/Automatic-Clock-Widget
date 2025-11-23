package com.julian.automaticclockwidget.settings

import com.julian.automaticclockwidget.fixtures.FakeUrlPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectUrlUseCaseTest {

    @Test
    fun selectUrl_changes_selection_if_exists_else_ignored() {
        val repo = FakeUrlPreferencesRepository()
        val add = AddUrlUseCase(repo)
        val select = SelectUrlUseCase(repo)
        val get = GetUrlStateUseCase(repo)

        add.addUrl("https://a")
        add.addUrl("https://b") // selected
        select.selectUrl("https://a")

        var state = get.getUrlState().getOrThrow()
        assertEquals("https://a", state.selected)

        // selecting non-existing should fail
        val failure = select.selectUrl("https://c").exceptionOrNull()
        requireNotNull(failure)
        state = get.getUrlState().getOrThrow()
        assertEquals("https://a", state.selected)
    }
}
