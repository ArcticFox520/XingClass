package com.xingclass.app.data.repository

import android.content.Context
import com.xingclass.app.domain.model.ChaoxingKey
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-sensitive user preferences. Plain SharedPreferences is fine here: the appearance mode is
 * not a credential, and the Chaoxing AES key is a public constant of the web client. Both must
 * survive app restarts.
 */
class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val themeState = MutableStateFlow(
        ThemeMode.fromStorage(preferences.getString(KEY_THEME_MODE, null)),
    )

    private val cryptoKeyState = MutableStateFlow(readCryptoKey())

    override val themeMode: StateFlow<ThemeMode> = themeState.asStateFlow()

    override val cryptoKey: StateFlow<String> = cryptoKeyState.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        themeState.value = mode
    }

    override fun setCryptoKey(key: String): Boolean {
        val normalized = ChaoxingKey.normalize(key)
        if (!ChaoxingKey.isValid(normalized)) return false
        preferences.edit().putString(KEY_CRYPTO, normalized).apply()
        cryptoKeyState.value = normalized
        return true
    }

    private fun readCryptoKey(): String {
        val stored = preferences.getString(KEY_CRYPTO, null)
            ?.let(ChaoxingKey::normalize)
        return stored?.takeIf(ChaoxingKey::isValid) ?: ChaoxingKey.DEFAULT
    }

    private companion object {
        const val FILE_NAME = "xingclass_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CRYPTO = "chaoxing_aes_key"
    }
}
