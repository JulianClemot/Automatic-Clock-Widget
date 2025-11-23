package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.settings.UrlPreferencesRepository

/**
 * In-memory fake for UrlPreferencesRepository used in unit tests.
 * Behavior mirrors production rules:
 * - addUrl: trims, de-duplicates (case-insensitive), appends, and selects the added URL
 * - deleteUrl: removes (case-insensitive). If the deleted URL was selected, select last remaining or clear selection
 * - getSelectedUrl: returns selection if valid, otherwise last URL or null
 * - selectUrl: selects only if URL exists (case-insensitive)
 */
class FakeUrlPreferencesRepository : UrlPreferencesRepository {
    private val urls = mutableListOf<String>()
    private var selected: String? = null

    override fun getUrls(): Result<List<String>> = Result.success(urls.toList())

    override fun addUrl(url: String): Result<Unit> {
        val sanitized = url.trim()
        if (sanitized.isEmpty()) return Result.failure(com.julian.automaticclockwidget.core.SettingsError.InvalidInput("URL cannot be blank"))
        urls.removeAll { it.equals(sanitized, ignoreCase = true) }
        urls.add(sanitized)
        selectUrl(sanitized)
        return Result.success(Unit)
    }

    override fun deleteUrl(url: String): Result<Unit> {
        val removed = urls.removeAll { it.equals(url, ignoreCase = true) }
        if (!removed) return Result.failure(com.julian.automaticclockwidget.core.SettingsError.NotFound("URL not found: $url"))
        if (selected != null && selected.equals(url, ignoreCase = true)) {
            selected = urls.lastOrNull()
        }
        return Result.success(Unit)
    }

    override fun getSelectedUrl(): Result<String?> = Result.success(selected ?: urls.lastOrNull())

    override fun selectUrl(url: String): Result<Unit> {
        if (urls.any { it.equals(url, ignoreCase = true) }) {
            selected = url
            return Result.success(Unit)
        }
        return Result.failure(com.julian.automaticclockwidget.core.SettingsError.NotFound("URL not found: $url"))
    }
}
