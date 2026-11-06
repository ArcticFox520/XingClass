package com.xingclass.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xingclass.app.R
import com.xingclass.app.domain.model.ThemeMode

@Composable
fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.AUTO -> R.string.settings_theme_auto
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)
