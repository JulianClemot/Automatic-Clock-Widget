package com.julian.automaticclockwidget.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

interface UrlPreferencesRepository {
    fun getUrls(): Result<List<String>>
    fun addUrl(url: String): Result<Unit>
    fun deleteUrl(url: String): Result<Unit>
    fun getSelectedUrl(): Result<String?>
    fun selectUrl(url: String): Result<Unit>
}

class UrlPreferencesRepositoryImpl(
    private val context: Context
) : UrlPreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getUrls(): Result<List<String>> = runCatching {
        val jsonStr = prefs.getString(KEY_URLS_JSON, null) ?: return@runCatching emptyList()
        val element = Json.parseToJsonElement(jsonStr)
        element.jsonArray.map { it.jsonPrimitive.content }.distinct()
    }

    override fun addUrl(url: String): Result<Unit> = runCatching {
        val sanitized = url.trim()
        if (sanitized.isEmpty()) throw com.julian.automaticclockwidget.core.SettingsError.InvalidInput("URL cannot be blank")
        val current = getUrls().getOrElse { throw it }.toMutableList()
        // Remove any duplicate, then append to make it the last (and thus default selection below)
        current.removeAll { it.equals(sanitized, ignoreCase = true) }
        current.add(sanitized)
        saveUrls(current)
        // By default, the last entered URL becomes the selected one
        selectUrl(sanitized).getOrElse { throw it }
    }

    override fun deleteUrl(url: String): Result<Unit> = runCatching {
        val current = getUrls().getOrElse { throw it }.toMutableList()
        val removed = current.removeAll { it.equals(url, ignoreCase = true) }
        if (!removed) throw com.julian.automaticclockwidget.core.SettingsError.NotFound("URL not found: $url")
        saveUrls(current)
        val selected = getSelectedUrl().getOrElse { throw it }
        if (selected != null && selected.equals(url, ignoreCase = true)) {
            // If we deleted the selected URL, select the last one if any, otherwise clear selection
            val newSelection = current.lastOrNull()
            if (newSelection == null) {
                prefs.edit().remove(KEY_SELECTED_URL).apply()
            } else {
                selectUrl(newSelection).getOrElse { throw it }
            }
        }
    }

    override fun getSelectedUrl(): Result<String?> = runCatching {
        val selected = prefs.getString(KEY_SELECTED_URL, null)
        val urls = getUrls().getOrElse { throw it }
        if (selected != null && urls.any { it.equals(selected, ignoreCase = true) }) {
            selected
        } else {
            // If stored selection is invalid, fall back to last URL if any
            urls.lastOrNull()
        }
    }

    override fun selectUrl(url: String): Result<Unit> = runCatching {
        val exists = getUrls().getOrElse { throw it }.any { it.equals(url, ignoreCase = true) }
        if (!exists) throw com.julian.automaticclockwidget.core.SettingsError.NotFound("URL not found: $url")
        prefs.edit().putString(KEY_SELECTED_URL, url).apply()
    }

    private fun saveUrls(urls: List<String>) {
        val json = Json.encodeToString(urls)
        prefs.edit().putString(KEY_URLS_JSON, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "automatic_clock_prefs"
        private const val KEY_URLS_JSON = "ics_urls_json"
        private const val KEY_SELECTED_URL = "selected_ics_url"
    }
}
