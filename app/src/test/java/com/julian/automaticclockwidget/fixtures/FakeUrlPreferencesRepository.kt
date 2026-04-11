package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.core.SettingsError
import com.julian.automaticclockwidget.settings.CalendarEntry
import com.julian.automaticclockwidget.settings.UrlPreferencesRepository

/**
 * In-memory fake for UrlPreferencesRepository used in unit tests.
 * Behavior mirrors production rules:
 * - addEntry: trims URL, de-duplicates (case-insensitive on URL), appends, and selects the added URL
 * - deleteEntry: removes by URL (case-insensitive). If deleted URL was selected, select last remaining or clear
 * - getSelectedUrl: returns selection if valid, otherwise last URL or null
 * - selectUrl: selects only if URL exists (case-insensitive)
 */
class FakeUrlPreferencesRepository : UrlPreferencesRepository {
    private val entries = mutableListOf<CalendarEntry>()
    private var selected: String? = null

    override fun getEntries(): Result<List<CalendarEntry>> = Result.success(entries.toList())

    override fun addEntry(name: String, url: String): Result<Unit> {
        val sanitizedUrl = url.trim()
        if (sanitizedUrl.isEmpty()) return Result.failure(SettingsError.InvalidInput("URL cannot be blank"))
        entries.removeAll { it.url.equals(sanitizedUrl, ignoreCase = true) }
        entries.add(CalendarEntry(name = name.trim(), url = sanitizedUrl))
        selectUrl(sanitizedUrl)
        return Result.success(Unit)
    }

    override fun deleteEntry(url: String): Result<Unit> {
        val removed = entries.removeAll { it.url.equals(url, ignoreCase = true) }
        if (!removed) return Result.failure(SettingsError.NotFound("URL not found: $url"))
        if (selected != null && selected.equals(url, ignoreCase = true)) {
            selected = entries.lastOrNull()?.url
        }
        return Result.success(Unit)
    }

    override fun getSelectedUrl(): Result<String?> = Result.success(selected ?: entries.lastOrNull()?.url)

    override fun selectUrl(url: String): Result<Unit> {
        if (entries.any { it.url.equals(url, ignoreCase = true) }) {
            selected = url
            return Result.success(Unit)
        }
        return Result.failure(SettingsError.NotFound("URL not found: $url"))
    }
}
