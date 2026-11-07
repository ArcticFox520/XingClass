package com.xingclass.app.ui.components.neumorphic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingAnimation
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.util.UiText
import com.xingclass.app.ui.util.asString

/**
 * Bottom message capsule shared by the schedule and settings screens.
 *
 * [visible] drives the animation while [message] keeps the last text during the exit animation.
 */
@Composable
fun NeumorphicMessageBar(
    visible: Boolean,
    message: UiText?,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val colors = LocalNeumorphicColors.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(XingAnimation.FAST_MS)) +
            slideInVertically(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)) { it / 2 },
        exit = fadeOut(tween(XingAnimation.NORMAL_MS)) +
            slideOutVertically(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)) { it / 2 },
    ) {
        NeumorphicSurface(
            style = NeumorphicStyle.INSET,
            shape = RoundedCornerShape(XingRadii.pill),
            elevation = NeumorphicElevation.SMALL,
            contentColor = colors.textSecondary,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message?.asString().orEmpty(),
                style = XingClassType.caption,
                color = colors.textSecondary,
                maxLines = maxLines,
                textAlign = TextAlign.Center,
            )
        }
    }
}
