package com.xingclass.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingclass.app.domain.model.CourseColor

/**
 * Single source of truth for the visual language.
 *
 * Neumorphism rules encoded here:
 *  - fixed light source: top-left highlight (-x/-y), bottom-right shadow (+x/+y)
 *  - no pure white / pure black backgrounds
 *  - soft, low-saturation course tints with high-contrast text
 *  - 12–28dp corners, never square
 */
@Immutable
data class CoursePalette(
    val background: Color,
    val content: Color,
)

@Immutable
data class NeumorphicColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val scrim: Color,
    val coursePalettes: List<CoursePalette>,
) {
    fun paletteFor(color: CourseColor): CoursePalette =
        coursePalettes[color.ordinal % coursePalettes.size]
}

val LightNeumorphicColors = NeumorphicColors(
    isDark = false,
    background = Color(0xFFE0E5EC),
    surface = Color(0xFFE0E5EC),
    lightShadow = Color(0xFFFFFFFF),
    darkShadow = Color(0xFFA6ADB8),
    textPrimary = Color(0xFF39414F),
    textSecondary = Color(0xFF586172),
    textTertiary = Color(0xFF5F6979),
    accent = Color(0xFF5949D2),
    onAccent = Color(0xFFF7F6FF),
    danger = Color(0xFF9E3E3A),
    scrim = Color(0x662A3040),
    coursePalettes = listOf(
        CoursePalette(Color(0xFFDBD8EC), Color(0xFF564A96)), // lavender
        CoursePalette(Color(0xFFD4E4DA), Color(0xFF356A4E)), // mint
        CoursePalette(Color(0xFFEADBD2), Color(0xFF82452E)), // peach
        CoursePalette(Color(0xFFD5E1EB), Color(0xFF315D79)), // sky
        CoursePalette(Color(0xFFE7E1D1), Color(0xFF6A582A)), // sand
        CoursePalette(Color(0xFFEAD7DE), Color(0xFF82405A)), // rose
        CoursePalette(Color(0xFFD1E3E3), Color(0xFF2C6060)), // teal
        CoursePalette(Color(0xFFE0E4CE), Color(0xFF545E2B)), // olive
    ),
)

val DarkNeumorphicColors = NeumorphicColors(
    isDark = true,
    background = Color(0xFF2B3038),
    surface = Color(0xFF2B3038),
    lightShadow = Color(0xFF434D5A),
    darkShadow = Color(0xFF15191F),
    textPrimary = Color(0xFFE3E8EF),
    textSecondary = Color(0xFFA3ADBB),
    textTertiary = Color(0xFF909AA9),
    accent = Color(0xFF9B90FF),
    onAccent = Color(0xFF12102A),
    danger = Color(0xFFE08580),
    scrim = Color(0x8C0B0D10),
    coursePalettes = listOf(
        CoursePalette(Color(0xFF3A3550), Color(0xFFC0B8EE)),
        CoursePalette(Color(0xFF2F3E37), Color(0xFF9CCFB1)),
        CoursePalette(Color(0xFF43342E), Color(0xFFE0AF98)),
        CoursePalette(Color(0xFF2C3A45), Color(0xFFA3C6E0)),
        CoursePalette(Color(0xFF3E392D), Color(0xFFD4C49B)),
        CoursePalette(Color(0xFF43313B), Color(0xFFE0ACBF)),
        CoursePalette(Color(0xFF2D3F3F), Color(0xFF9BCCCC)),
        CoursePalette(Color(0xFF3A3D2E), Color(0xFFC3CDA2)),
    ),
)

val LocalNeumorphicColors = staticCompositionLocalOf { LightNeumorphicColors }

object XingSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object XingRadii {
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val xlarge = 28.dp
    val pill = 100.dp
}

/** Reused control dimensions; keeps repeated dp values out of screens and components. */
object XingControlSize {
    val actionVerticalPadding = 13.dp
    val compactActionVerticalPadding = 12.dp
    val iconButton = 38.dp
    val iconGlyph = 17.dp
}

object XingAnimation {
    const val FAST_MS = 200
    const val NORMAL_MS = 300
}

/**
 * Text styles. Chinese system fonts are used (no bundled font): the UI should feel native to the
 * device and the APK should stay small.
 */
object XingClassType {
    val wordmark = TextStyle(
        fontSize = 26.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    )
    val title = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    )
    val screenTitle = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val sectionLabel = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
    )
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )
    val bodyStrong = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
    val label = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
    val caption = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal,
    )
    val micro = TextStyle(
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal,
    )
    val gridTitle = TextStyle(
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Medium,
    )
    val gridTitleCompact = TextStyle(
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Medium,
    )
    val gridCaption = TextStyle(
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.Normal,
    )
    val periodNumber = TextStyle(
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )
    val periodTime = TextStyle(
        fontSize = 8.sp,
        lineHeight = 9.sp,
        fontWeight = FontWeight.Normal,
    )
    val drawerTimeStart = TextStyle(
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val drawerTimeEnd = TextStyle(
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Normal,
    )
    val drawerTitle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
