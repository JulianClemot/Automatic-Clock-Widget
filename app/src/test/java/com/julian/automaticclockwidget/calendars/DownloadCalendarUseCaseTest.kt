package com.julian.automaticclockwidget.calendars

import com.julian.automaticclockwidget.fixtures.FakeCalendarsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadCalendarUseCaseTest {

    @Test
    fun `given repository returns calendar when downloading then use case forwards success and exposes calendar id`() {
        // Given
        val repo = FakeCalendarsRepository().apply {
            result = Result.success(Calendar("id1", Events(emptyList())))
        }
        val uc = DownloadCalendarUseCase(repo)

        // When
        val res = runBlockingTry { uc.downloadCalendar("https://ics") }

        // Then
        assertTrue(res.isSuccess)
        assertEquals("id1", res.getOrNull()!!.id)
    }

    @Test
    fun `given repository is invoked when downloading then use case forwards requested uri to repository`() {
        // Given
        val repo = FakeCalendarsRepository().apply {
            result = Result.success(Calendar("id1", Events(emptyList())))
        }
        val uc = DownloadCalendarUseCase(repo)

        // When
        runBlockingTry { uc.downloadCalendar("https://ics") }

        // Then
        assertEquals("https://ics", repo.lastUri)
    }

    @Test
    fun `given repository fails when downloading then use case forwards failure`() {
        // Given
        val repo = FakeCalendarsRepository().apply {
            result = Result.failure(Exception("boom"))
        }
        val uc = DownloadCalendarUseCase(repo)

        // When
        val res = runBlockingTry { uc.downloadCalendar("https://ics") }

        // Then
        assertTrue(res.isFailure)
        assertEquals("https://ics", repo.lastUri)
    }
}

// Helper to call suspend function in plain JUnit without bringing coroutine test libs
private fun <T> runBlockingTry(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
