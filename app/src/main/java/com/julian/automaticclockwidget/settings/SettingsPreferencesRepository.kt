package com.julian.automaticclockwidget.settings

import android.content.Context
import android.content.SharedPreferences

interface SettingsPreferencesRepository {
    fun isPerMinuteTickEnabled(): Result<Boolean>
    fun setPerMinuteTickEnabled(enabled: Boolean): Result<Unit>
}

class SettingsPreferencesRepositoryImpl(private val context: Context) : SettingsPreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isPerMinuteTickEnabled(): Result<Boolean> = runCatching {
        // Default ON if never set
        prefs.getBoolean(KEY_PER_MINUTE_TICK, true)
    }

    override fun setPerMinuteTickEnabled(enabled: Boolean): Result<Unit> = runCatching {
        prefs.edit().putBoolean(KEY_PER_MINUTE_TICK, enabled).apply()
    }

    private companion object {
        private const val PREFS_NAME = "automatic_clock_prefs"
        private const val KEY_PER_MINUTE_TICK = "per_minute_tick_enabled"
    }
}