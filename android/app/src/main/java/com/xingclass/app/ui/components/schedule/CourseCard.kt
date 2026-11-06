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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
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
import com.xingclass.app.ui.util.parseLocation

/**
 * A single lesson inside the grid.
 *
 * The card is raised out of the recessed grid base. Pressing it transitions the shadow from
 * raised to inset (no translation) and opens the day drawer.
 *
 * Space usage is measured, not guessed from the span:
 *  - the title gets 1..5 lines depending on available height;
 *  - if there is room, the location is split into a building line and a room line;
 *  - if only one line fits, the full location is shown instead of hiding it.
 *
 * The same measured budget also includes font scaling (sp -> dp), so a large system font cannot
 * push the location out of the card. The card clips its content as a final safety net.
 */
@Composable
fun CourseCard(
    course: Course,
    palette: CoursePalette,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
        label = "coursePress",
    )

    val location = remember(course.location) { parseLocation(course.location) }
    val titleStyle = if (compact) XingClassType.gridTitleCompact else XingClassType.gridTitle
    val titleLineHeight = with(density) { titleStyle.lineHeight.toDp() }
    val captionLineHeight = with(density) { XingClassType.gridCaption.lineHeight.toDp() }
    val locationGap = 1.dp
    val shape = RoundedCornerShape(XingRadii.small)

    BoxWithConstraints(
        modifier = modifier
            .neumorphic(
                shape = shape,
                style = NeumorphicStyle.RAISED,
                backgroundColor = palette.background,
                lightShadow = colors.lightShadow,
                darkShadow = colors.darkShadow,
                elevation = NeumorphicElevation.SMALL,
                pressProgress = pressProgress,
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        val available = maxHeight
        val locationLineCount = when {
            location.building != null && location.room != null -> 2
            location.hasLocation -> 1
            else -> 0
        }
        val textLayout = CourseCardLayout.compute(
            availableHeight = available,
            titleLineHeight = titleLineHeight,
            captionLineHeight = captionLineHeight,
            lineGap = locationGap,
            locationLineCount = locationLineCount,
        )
        val titleLines = textLayout.titleLines
        val shownLocationLines = textLayout.locationLines

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = course.title,
                style = titleStyle,
                color = palette.content,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis,
            )

            if (shownLocationLines == 2) {
                Spacer(Modifier.height(locationGap))
                Text(
                    text = location.building.orEmpty(),
                    style = XingClassType.gridCaption,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = location.room.orEmpty(),
                    style = XingClassType.gridCaption,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (shownLocationLines == 1) {
                Spacer(Modifier.height(locationGap))
                val oneLine = if (location.hasSplit) location.raw else location.room ?: location.building
                Text(
                    text = oneLine.orEmpty(),
                    style = XingClassType.gridCaption,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
