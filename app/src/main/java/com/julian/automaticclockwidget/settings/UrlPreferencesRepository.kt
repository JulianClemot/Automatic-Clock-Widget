package com.julian.automaticclockwidget.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.julian.automaticclockwidget.core.SettingsError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface UrlPreferencesRepository {
    fun getEntries(): Result<List<CalendarEntry>>
    fun addEntry(name: String, url: String): Result<Unit>
    fun deleteEntry(url: String): Result<Unit>
    fun getSelectedUrl(): Result<String?>
    fun selectUrl(url: String): Result<Unit>
}

class UrlPreferencesRepositoryImpl(
    private val context: Context
) : UrlPreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getEntries(): Result<List<CalendarEntry>> = runCatching {
        val jsonStr = prefs.getString(KEY_URLS_JSON, null) ?: return@runCatching emptyList()
        try {
            Json.decodeFromString<List<CalendarEntry>>(jsonStr)
        } catch (_: Exception) {
            // Legacy format: plain URL strings — migrate transparently with empty name
            Json.decodeFromString<List<String>>(jsonStr).map { CalendarEntry(name = "", url = it) }
        }
    }

    override fun addEntry(name: String, url: String): Result<Unit> = runCatching {
        val sanitizedUrl = url.trim()
        val sanitizedName = name.trim()
        if (sanitizedUrl.isEmpty()) throw SettingsError.InvalidInput("URL cannot be blank")
        val current = getEntries().getOrElse { throw it }.toMutableList()
        // Remove any duplicate by URL (case-insensitive), then append so it becomes the last / selected
        current.removeAll { it.url.equals(sanitizedUrl, ignoreCase = true) }
        current.add(CalendarEntry(name = sanitizedName, url = sanitizedUrl))
        saveEntries(current)
        selectUrl(sanitizedUrl).getOrElse { throw it }
    }

    override fun deleteEntry(url: String): Result<Unit> = runCatching {
        val current = getEntries().getOrElse { throw it }.toMutableList()
        val removed = current.removeAll { it.url.equals(url, ignoreCase = true) }
        if (!removed) throw SettingsError.NotFound("URL not found: $url")
        saveEntries(current)
        val selected = getSelectedUrl().getOrElse { throw it }
        if (selected != null && selected.equals(url, ignoreCase = true)) {
            val newSelection = current.lastOrNull()?.url
            if (newSelection == null) {
                prefs.edit { remove(KEY_SELECTED_URL) }
            } else {
                selectUrl(newSelection).getOrElse { throw it }
            }
        }
    }

    override fun getSelectedUrl(): Result<String?> = runCatching {
        val selected = prefs.getString(KEY_SELECTED_URL, null)
        val entries = getEntries().getOrElse { throw it }
        if (selected != null && entries.any { it.url.equals(selected, ignoreCase = true) }) {
            selected
        } else {
            // Stored selection is stale or absent — fall back to last entry
            entries.lastOrNull()?.url
        }
    }

    override fun selectUrl(url: String): Result<Unit> = runCatching {
        val exists = getEntries().getOrElse { throw it }.any { it.url.equals(url, ignoreCase = true) }
        if (!exists) throw SettingsError.NotFound("URL not found: $url")
        prefs.edit { putString(KEY_SELECTED_URL, url) }
    }

    private fun saveEntries(entries: List<CalendarEntry>) {
        prefs.edit { putString(KEY_URLS_JSON, Json.encodeToString(entries)) }
    }

    companion object {
        private const val PREFS_NAME = "automatic_clock_prefs"
        private const val KEY_URLS_JSON = "ics_urls_json"
        private const val KEY_SELECTED_URL = "selected_ics_url"
    }
}
