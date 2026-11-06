package com.xingclass.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Applies the resolved appearance. The effective dark/light decision lives in the root
 * ViewModel so "auto" is recomputed once per minute, not on every recomposition.
 */
@Composable
fun XingClassTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors

    // Material is only used for primitives (Text/Icon semantics). All visible surfaces are drawn
    // by the neumorphic components, never by Material's elevated surfaces.
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.textSecondary,
            error = colors.danger,
            onError = colors.background,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.textSecondary,
            error = colors.danger,
            onError = colors.background,
        )
    }

    CompositionLocalProvider(LocalNeumorphicColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
