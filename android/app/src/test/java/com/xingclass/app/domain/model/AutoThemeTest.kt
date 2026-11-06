package com.xingclass.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AutoThemeTest {

    private fun at(hour: Int, minute: Int) =
        LocalDateTime.of(2026, 11, 6, hour, minute)

    @Test
    fun `auto switches exactly at 08 and 22`() {
        assertTrue(AutoTheme.isDark(ThemeMode.AUTO, at(7, 59)))
        assertFalse(AutoTheme.isDark(ThemeMode.AUTO, at(8, 0)))
        assertFalse(AutoTheme.isDark(ThemeMode.AUTO, at(21, 59)))
        assertTrue(AutoTheme.isDark(ThemeMode.AUTO, at(22, 0)))
        assertTrue(AutoTheme.isDark(ThemeMode.AUTO, at(0, 0)))
    }

    @Test
    fun `explicit modes ignore the clock`() {
        assertFalse(AutoTheme.isDark(ThemeMode.LIGHT, at(23, 0)))
        assertTrue(AutoTheme.isDark(ThemeMode.DARK, at(12, 0)))
    }

    @Test
    fun `theme mode storage falls back to auto`() {
        assertTrue(ThemeMode.fromStorage(null) == ThemeMode.AUTO)
        assertTrue(ThemeMode.fromStorage("garbage") == ThemeMode.AUTO)
        assertTrue(ThemeMode.fromStorage("DARK") == ThemeMode.DARK)
    }
}
