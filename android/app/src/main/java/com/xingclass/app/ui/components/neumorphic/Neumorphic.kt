package com.xingclass.app.ui.components.neumorphic

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified neumorphic shadow system. No component is allowed to hand-roll shadow numbers; they
 * all pick one of the three elevations below (see AGENT_GUIDE §11/§15).
 *
 * @param offset distance of the light/dark blurred edge
 * @param blur shadow blur radius
 * @param depth thickness of the inset ring that produces the recessed shading
 */
data class NeumorphicShadow(
    val offset: Dp,
    val blur: Dp,
    val depth: Dp,
)

enum class NeumorphicElevation(val shadow: NeumorphicShadow) {
    // Deliberately pronounced: the raised/inset contrast is the core of XingClass' look.
    SMALL(NeumorphicShadow(offset = 4.dp, blur = 9.dp, depth = 4.dp)),
    MEDIUM(NeumorphicShadow(offset = 7.dp, blur = 14.dp, depth = 7.dp)),
    LARGE(NeumorphicShadow(offset = 10.dp, blur = 22.dp, depth = 10.dp)),
}

enum class NeumorphicStyle {
    /** No shadow at all; used inside already-shadowed content. */
    FLAT,

    /** Extruded: light halo top-left, dark halo bottom-right. */
    RAISED,

    /** Recessed: dark inner band top-left, light inner band bottom-right. */
    INSET,
}

/**
 * Draws the neumorphic surface.
 *
 * [pressProgress] animates RAISED -> INSET without any translation, exactly as required by the
 * style guide. When [style] is RAISED the raised halos fade out and the inset shading fades in.
 * [shadowScale] is used for the "input focus opens the channel" rule: the inset depth shrinks.
 */
fun Modifier.neumorphic(
    shape: Shape,
    style: NeumorphicStyle,
    backgroundColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    elevation: NeumorphicElevation = NeumorphicElevation.MEDIUM,
    pressProgress: Float = 0f,
    shadowScale: Float = 1f,
): Modifier = drawBehind {
    drawNeumorphicSurface(
        shape = shape,
        style = style,
        backgroundColor = backgroundColor,
        lightShadow = lightShadow,
        darkShadow = darkShadow,
        shadow = elevation.shadow,
        pressProgress = pressProgress.coerceIn(0f, 1f),
        shadowScale = shadowScale.coerceIn(0.2f, 1.5f),
    )
}

private fun DrawScope.drawNeumorphicSurface(
    shape: Shape,
    style: NeumorphicStyle,
    backgroundColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    shadow: NeumorphicShadow,
    pressProgress: Float,
    shadowScale: Float,
) {
    if (size.width <= 0f || size.height <= 0f) return

    val outline = shape.createOutline(size, layoutDirection, this)
    val fillPaint = Paint().apply {
        color = backgroundColor
        isAntiAlias = true
    }

    val offsetPx = shadow.offset.toPx() * shadowScale
    val blurPx = shadow.blur.toPx() * shadowScale
    val depthPx = shadow.depth.toPx() * shadowScale

    when (style) {
        NeumorphicStyle.FLAT -> drawIntoCanvas { it.drawPath(outline.toPath(), fillPaint) }

        NeumorphicStyle.INSET -> drawInset(
            shape = shape,
            outline = outline,
            backgroundColor = backgroundColor,
            lightShadow = lightShadow,
            darkShadow = darkShadow,
            offsetPx = offsetPx,
            blurPx = blurPx,
            depthPx = depthPx,
            alpha = 1f,
        )

        NeumorphicStyle.RAISED -> {
            val raisedAlpha = 1f - pressProgress
            if (raisedAlpha > 0.02f) {
                drawRaised(
                    outline = outline,
                    paint = fillPaint,
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                    offsetPx = offsetPx,
                    blurPx = blurPx,
                    alpha = raisedAlpha,
                )
            } else {
                drawIntoCanvas { it.drawPath(outline.toPath(), fillPaint) }
            }
            if (pressProgress > 0.02f) {
                drawInset(
                    shape = shape,
                    outline = outline,
                    backgroundColor = backgroundColor,
                    lightShadow = lightShadow,
                    darkShadow = darkShadow,
                    offsetPx = offsetPx,
                    blurPx = blurPx,
                    depthPx = depthPx,
                    alpha = pressProgress,
                )
            }
        }
    }
}

private fun DrawScope.drawRaised(
    outline: Outline,
    paint: Paint,
    lightShadow: Color,
    darkShadow: Color,
    offsetPx: Float,
    blurPx: Float,
    alpha: Float,
) {
    val path = outline.toPath()
    drawIntoCanvas { canvas ->
        val native = paint.asFrameworkPaint()
        native.isAntiAlias = true

        if (blurPx > 0.5f) {
            val light = lightShadow.copy(alpha = lightShadow.alpha * alpha)
            native.setShadowLayer(blurPx, -offsetPx, -offsetPx, light.toArgb())
        }
        canvas.drawPath(path, paint)

        if (blurPx > 0.5f) {
            val dark = darkShadow.copy(alpha = darkShadow.alpha * alpha)
            native.setShadowLayer(blurPx, offsetPx, offsetPx, dark.toArgb())
        }
        canvas.drawPath(path, paint)
    }
}

/**
 * Inset shadows are produced by stroking a border ring with a shadow layer and clipping it to the
 * surface: the dark offset ring lands on the top-left inner rim, the light one on the bottom-right
 * rim. This keeps the physical light direction intact and needs no blur mask filter.
 */
private fun DrawScope.drawInset(
    shape: Shape,
    outline: Outline,
    backgroundColor: Color,
    lightShadow: Color,
    darkShadow: Color,
    offsetPx: Float,
    blurPx: Float,
    depthPx: Float,
    alpha: Float,
) {
    if (depthPx <= 0f) return
    val innerWidth = size.width - depthPx * 2f
    val innerHeight = size.height - depthPx * 2f
    if (innerWidth <= 0f || innerHeight <= 0f) return

    val outerPath = outline.toPath()
    val innerOutline = shape.createOutline(Size(innerWidth, innerHeight), layoutDirection, this)
    val innerPath = innerOutline.toPath().apply { translate(Offset(depthPx, depthPx)) }
    val ring = Path().apply {
        addPath(outerPath)
        op(this, innerPath, PathOperation.Difference)
    }

    val paint = Paint().apply {
        color = backgroundColor
        isAntiAlias = true
    }
    val native = paint.asFrameworkPaint()
    native.isAntiAlias = true

    clipPath(outerPath) {
        drawIntoCanvas { canvas ->
            if (blurPx > 0.5f) {
                val light = lightShadow.copy(alpha = lightShadow.alpha * alpha)
                native.setShadowLayer(blurPx, -offsetPx, -offsetPx, light.toArgb())
            }
            canvas.drawPath(ring, paint)

            if (blurPx > 0.5f) {
                val dark = darkShadow.copy(alpha = darkShadow.alpha * alpha)
                native.setShadowLayer(blurPx, offsetPx, offsetPx, dark.toArgb())
            }
            canvas.drawPath(ring, paint)
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
