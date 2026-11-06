package com.xingclass.app.domain.model

import java.time.LocalDateTime

/** Three-state appearance setting. */
enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

/**
 * Resolves the effective appearance.
 *
 * Auto: 08:00–22:00 is light, everything else is dark. Only the hour is relevant, so the
 * caller can recompute once per minute instead of every second.
 */
object AutoTheme {
    const val DAY_START_HOUR = 8
    const val NIGHT_START_HOUR = 22

    fun isDark(mode: ThemeMode, now: LocalDateTime): Boolean = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> now.hour < DAY_START_HOUR || now.hour >= NIGHT_START_HOUR
    }
}
