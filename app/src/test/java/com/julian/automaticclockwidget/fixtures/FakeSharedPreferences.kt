package com.julian.automaticclockwidget.fixtures

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {
    private val store = mutableMapOf<String, Any?>()

    override fun getLong(key: String, defValue: Long): Long = (store[key] as? Long) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (store[key] as? Boolean) ?: defValue
    override fun getString(key: String, defValue: String?): String? = (store[key] as? String) ?: defValue
    override fun getInt(key: String, defValue: Int): Int = (store[key] as? Int) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (store[key] as? Float) ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
    override fun getAll(): MutableMap<String, *> = store.toMutableMap()
    override fun contains(key: String): Boolean = store.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()

        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = this
        override fun remove(key: String) = apply { pending.remove(key) }
        override fun clear() = apply { pending.clear() }

        override fun apply() { store.putAll(pending) }
        override fun commit(): Boolean { store.putAll(pending); return true }
    }
}
