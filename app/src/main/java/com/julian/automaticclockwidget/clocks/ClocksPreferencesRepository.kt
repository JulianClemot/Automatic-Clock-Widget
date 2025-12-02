package com.julian.automaticclockwidget.clocks

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.core.content.edit

interface ClocksPreferencesRepository {
    fun getClocks(): Result<List<StoredClock>>
    fun saveClocks(clocks: List<StoredClock>): Result<Unit>
    fun clearClocks(): Result<Unit>
}

class ClocksPreferencesRepositoryImpl(
    private val context: Context
) : ClocksPreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getClocks(): Result<List<StoredClock>> = runCatching {
        val jsonStr = prefs.getString(KEY_CLOCKS_JSON, null) ?: return@runCatching emptyList()
        Json.decodeFromString<List<StoredClock>>(jsonStr)
    }

    override fun saveClocks(clocks: List<StoredClock>): Result<Unit> = runCatching {
        val json = Json.encodeToString(clocks)
        prefs.edit { putString(KEY_CLOCKS_JSON, json) }
    }

    override fun clearClocks(): Result<Unit> = runCatching {
        prefs.edit { remove(KEY_CLOCKS_JSON) }
    }

    private companion object {
        private const val PREFS_NAME = "automatic_clock_prefs"
        private const val KEY_CLOCKS_JSON = "stored_clocks_json"
    }
}
