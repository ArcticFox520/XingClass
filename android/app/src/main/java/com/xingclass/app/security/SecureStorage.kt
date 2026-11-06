package com.xingclass.app.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Small abstraction over Keystore-backed storage so tests and the rest of the app do not need
 * Android Keystore available.
 */
interface SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

/**
 * Non-persistent fallback. Only used when the Android Keystore is unavailable (for example a
 * corrupted keystore entry): the app keeps working for the current process, but nothing about
 * the session is written to disk in plain text.
 */
class InMemorySecureStorage : SecureStorage {
    private val values = ConcurrentHashMap<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun clear() {
        values.clear()
    }
}
