package com.xingclass.app.ui.components.schedule

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingclass.app.domain.model.Course
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.neumorphic
import com.xingclass.app.ui.theme.CoursePalette
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingAnimation
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii

/**
 * A single lesson inside the grid.
 *
 * The card is raised out of the recessed grid base. Pressing it transitions the shadow from
 * raised to inset (no translation) and opens the day drawer.
 *
 * Text is always clamped and ellipsized; the number of title lines is derived from the measured
 * card height so a long course name can never push the layout out of the grid.
 */
@Composable
fun CourseCard(
    course: Course,
    palette: CoursePalette,
    showLocation: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "coursePress",
    )

    BoxWithConstraints(
        modifier = modifier
            .neumorphic(
                shape = RoundedCornerShape(XingRadii.small),
                style = NeumorphicStyle.RAISED,
                backgroundColor = palette.background,
                lightShadow = colors.lightShadow,
                darkShadow = colors.darkShadow,
                elevation = NeumorphicElevation.SMALL,
                pressProgress = pressProgress,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        val titleLine = 12.dp
        val captionLine = 10.dp
        val canShowLocation = showLocation && maxHeight >= 42.dp
        val reserved = if (canShowLocation) captionLine + 2.dp else 0.dp
        val titleLines = ((maxHeight - reserved) / titleLine).toInt().coerceIn(1, 4)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = course.title,
                style = if (compact) XingClassType.gridTitleCompact else XingClassType.gridTitle,
                color = palette.content,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (canShowLocation && !course.location.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = course.location,
                    style = XingClassType.gridCaption,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
