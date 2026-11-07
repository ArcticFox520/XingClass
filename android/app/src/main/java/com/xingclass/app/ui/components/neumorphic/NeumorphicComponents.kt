package com.xingclass.app.ui.components.neumorphic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingAnimation
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingControlSize
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing

/**
 * The single surface primitive. Shadow colours, elevation and radius all come from design
 * tokens; callers only choose a role (raised / inset / flat) and a background colour.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(XingRadii.medium),
    style: NeumorphicStyle = NeumorphicStyle.RAISED,
    elevation: NeumorphicElevation = NeumorphicElevation.MEDIUM,
    backgroundColor: Color = LocalNeumorphicColors.current.surface,
    contentColor: Color = LocalNeumorphicColors.current.textPrimary,
    pressProgress: Float = 0f,
    shadowScale: Float = 1f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .neumorphic(
                    shape = shape,
                    style = style,
                    backgroundColor = backgroundColor,
                    lightShadow = colors.lightShadow,
                    darkShadow = colors.darkShadow,
                    elevation = elevation,
                    pressProgress = pressProgress,
                    shadowScale = shadowScale,
                )
                .padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    /**
     * Renders an unavailable button as a recessed slot. Used where "nothing to do" should read
     * as pressed-in rather than as a faded, still-raised button (e.g. the login-key actions).
     */
    insetWhenDisabled: Boolean = false,
    shape: Shape = RoundedCornerShape(XingRadii.medium),
    elevation: NeumorphicElevation = NeumorphicElevation.MEDIUM,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 22.dp,
        vertical = XingControlSize.actionVerticalPadding,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val disabledInset = !enabled && insetWhenDisabled
    val pressProgress by animateFloatAsState(
        targetValue = if ((pressed && enabled) || disabledInset) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "buttonPress",
    )

    NeumorphicSurface(
        modifier = modifier
            .alpha(if (enabled || insetWhenDisabled) 1f else 0.45f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        elevation = elevation,
        backgroundColor = if (accent) colors.accent else colors.surface,
        contentColor = when {
            disabledInset -> colors.textTertiary
            accent -> colors.onAccent
            else -> colors.textPrimary
        },
        pressProgress = pressProgress,
        contentPadding = contentPadding,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    iconSize: Dp = 18.dp,
    elevation: NeumorphicElevation = NeumorphicElevation.SMALL,
    contentColor: Color = LocalNeumorphicColors.current.textSecondary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "iconButtonPress",
    )

    NeumorphicSurface(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        elevation = elevation,
        contentColor = contentColor,
        pressProgress = pressProgress,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
    }
}

@Composable
fun NeumorphicTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: NeumorphicElevation = NeumorphicElevation.SMALL,
    shape: Shape = RoundedCornerShape(XingRadii.small),
    contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
    content: @Composable () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (selected || pressed) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "tabPress",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textSecondary,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "tabColor",
    )

    NeumorphicSurface(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        shape = shape,
        elevation = elevation,
        contentColor = contentColor,
        pressProgress = pressProgress,
        contentPadding = contentPadding,
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun NeumorphicBadge(
    text: String,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
    contentColor: Color = LocalNeumorphicColors.current.textSecondary,
) {
    NeumorphicSurface(
        modifier = modifier,
        shape = RoundedCornerShape(XingRadii.pill),
        style = if (inset) NeumorphicStyle.INSET else NeumorphicStyle.RAISED,
        elevation = NeumorphicElevation.SMALL,
        contentColor = contentColor,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = XingClassType.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalNeumorphicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusProgress by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "fieldFocus",
    )
    // Focus "opens the channel": the inset gets shallower instead of deeper.
    val shadowScale = 1f - 0.45f * focusProgress

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = XingClassType.label,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(XingSpacing.sm))
        }
        NeumorphicSurface(
            shape = RoundedCornerShape(XingRadii.small),
            style = NeumorphicStyle.INSET,
            elevation = NeumorphicElevation.SMALL,
            shadowScale = shadowScale,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        singleLine = true,
                        textStyle = XingClassType.body.copy(
                            color = if (enabled) colors.textPrimary else colors.textTertiary,
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        visualTransformation = if (isPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        interactionSource = interactionSource,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = XingClassType.body,
                                        color = colors.textTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(XingSpacing.sm))
                    trailing()
                }
            }
        }
    }
}

/**
 * Three breathing dots used for loading states. No Material spinner: the visual language stays
 * within the neumorphic system.
 */
@Composable
fun NeumorphicLoadingDots(
    modifier: Modifier = Modifier,
    color: Color = LocalNeumorphicColors.current.accent,
    dotSize: Dp = 6.dp,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = EaseInOut,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(dotSize)
                    .background(color = color.copy(alpha = alpha), shape = CircleShape),
            )
        }
    }
}
