package com.xingclass.app.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger

/**
 * [SecureStorage] backed by Android Keystore + EncryptedSharedPreferences.
 *
 * Master key: AES-256-GCM in the Android Keystore (hardware-backed where available).
 * Keys: AES-256-SIV. Values: AES-256-GCM.
 *
 * Failure handling is fail-closed: if the keystore entry is unusable the encrypted file is
 * deleted and recreated. If that still fails, an in-memory store is used for this process only.
 * The app must never fall back to plaintext storage.
 */
class KeystoreSecureStorage(
    context: Context,
    private val logger: Logger,
) : SecureStorage {

    private val appContext = context.applicationContext

    private val delegate: SecureStorage by lazy {
        try {
            EncryptedPrefsStorage(createPrefs())
        } catch (first: Throwable) {
            logger.failure(LogEvents.SECURE_STORAGE_FAILED, first)
            runCatching { appContext.deleteSharedPreferences(FILE_NAME) }
            try {
                EncryptedPrefsStorage(createPrefs())
            } catch (second: Throwable) {
                logger.failure(LogEvents.SECURE_STORAGE_FAILED, second)
                InMemorySecureStorage()
            }
        }
    }

    override fun getString(key: String): String? =
        runCatching { delegate.getString(key) }
            .onFailure { logger.failure(LogEvents.SECURE_STORAGE_FAILED, it) }
            .getOrNull()

    override fun putString(key: String, value: String) {
        runCatching { delegate.putString(key, value) }
            .onFailure { logger.failure(LogEvents.SECURE_STORAGE_FAILED, it) }
    }

    override fun remove(key: String) {
        runCatching { delegate.remove(key) }
            .onFailure { logger.failure(LogEvents.SECURE_STORAGE_FAILED, it) }
    }

    override fun clear() {
        runCatching { delegate.clear() }
            .onFailure { logger.failure(LogEvents.SECURE_STORAGE_FAILED, it) }
    }

    private fun createPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * commit() is deliberate: a session cookie that was written immediately before a process
     * death must not be lost. Writes are tiny and happen only on login / logout.
     */
    @SuppressLint("ApplySharedPref")
    private class EncryptedPrefsStorage(
        private val prefs: SharedPreferences,
    ) : SecureStorage {
        override fun getString(key: String): String? = prefs.getString(key, null)

        override fun putString(key: String, value: String) {
            prefs.edit().putString(key, value).commit()
        }

        override fun remove(key: String) {
            prefs.edit().remove(key).commit()
        }

        override fun clear() {
            prefs.edit().clear().commit()
        }
    }

    private companion object {
        const val FILE_NAME = "xingclass_secure_prefs"
    }
}
