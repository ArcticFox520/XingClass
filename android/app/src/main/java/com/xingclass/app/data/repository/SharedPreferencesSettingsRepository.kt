package com.xingclass.app.data.repository

import android.content.Context
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-sensitive user preferences. Plain SharedPreferences is fine here: theme choice is not a
 * credential and must survive reinstall-free app restarts.
 */
class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val themeState = MutableStateFlow(
        ThemeMode.fromStorage(preferences.getString(KEY_THEME_MODE, null)),
    )

    override val themeMode: StateFlow<ThemeMode> = themeState.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        themeState.value = mode
    }

    private companion object {
        const val FILE_NAME = "xingclass_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
