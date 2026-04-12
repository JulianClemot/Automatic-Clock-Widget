package com.julian.automaticclockwidget.clocks

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.julian.automaticclockwidget.core.toErrorContext
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ClocksPreferencesRepository {
    fun getClocks(): Result<List<StoredClock>>
    fun saveClocks(clocks: List<StoredClock>): Result<Unit>
    fun clearClocks(): Result<Unit>
}

class ClocksPreferencesRepositoryImpl(
    private val context: Context,
    private val observability: ObservabilityRepository,
) : ClocksPreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getClocks(): Result<List<StoredClock>> = runCatching {
        val jsonStr = prefs.getString(KEY_CLOCKS_JSON, null) ?: return@runCatching emptyList()
        Json.decodeFromString<List<StoredClock>>(jsonStr)
    }.onFailure { t ->
        observability.sendErrorEvent(
            throwable = t,
            context = t.toErrorContext() + ("stage" to "getClocks"),
        )
    }

    override fun saveClocks(clocks: List<StoredClock>): Result<Unit> = runCatching {
        val json = Json.encodeToString(clocks)
        prefs.edit { putString(KEY_CLOCKS_JSON, json) }
    }.onSuccess {
        observability.log(
            message = "Clocks persisted",
            category = "storage",
            data = mapOf(
                "count" to clocks.size,
                "iataCodes" to clocks.map { it.iataCode },
            ),
        )
    }.onFailure { t ->
        observability.sendErrorEvent(
            throwable = t,
            context = t.toErrorContext() + ("stage" to "saveClocks") + ("count" to clocks.size),
        )
    }

    override fun clearClocks(): Result<Unit> = runCatching {
        prefs.edit { remove(KEY_CLOCKS_JSON) }
    }.onSuccess {
        observability.log(message = "Clocks cleared", category = "storage")
    }.onFailure { t ->
        observability.sendErrorEvent(
            throwable = t,
            context = t.toErrorContext() + ("stage" to "clearClocks"),
        )
    }

    private companion object {
        private const val PREFS_NAME = "automatic_clock_prefs"
        private const val KEY_CLOCKS_JSON = "stored_clocks_json"
    }
}
