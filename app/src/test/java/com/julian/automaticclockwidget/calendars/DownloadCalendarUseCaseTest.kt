package com.julian.automaticclockwidget.calendars

import com.julian.automaticclockwidget.fixtures.FakeCalendarsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadCalendarUseCaseTest {

    @Test
    fun forwards_success_and_passes_uri() {
        val repo = FakeCalendarsRepository().apply {
            result = Result.success(Calendar("id1", Events(emptyList())))
        }
        val uc = DownloadCalendarUseCase(repo)

        val res = runBlockingTry { uc.downloadCalendar("https://ics") }
        assertTrue(res.isSuccess)
        assertEquals("https://ics", repo.lastUri)
        assertEquals("id1", res.getOrNull()!!.id)
    }

    @Test
    fun forwards_failure() {
        val repo = FakeCalendarsRepository().apply {
            result = Result.failure(Exception("boom"))
        }
        val uc = DownloadCalendarUseCase(repo)

        val res = runBlockingTry { uc.downloadCalendar("https://ics") }
        assertTrue(res.isFailure)
        assertEquals("https://ics", repo.lastUri)
    }
}

// Helper to call suspend function in plain JUnit without bringing coroutine test libs
private fun <T> runBlockingTry(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
