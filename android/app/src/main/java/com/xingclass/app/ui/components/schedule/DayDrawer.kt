package com.xingclass.app.ui.components.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingclass.app.R
import com.xingclass.app.domain.model.Course
import com.xingclass.app.ui.components.neumorphic.NeumorphicBadge
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicIconButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.neumorphic
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingAnimation
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.dateLabel
import com.xingclass.app.ui.util.parseLocation
import com.xingclass.app.ui.util.weekdayFull
import java.time.LocalDate

/**
 * Single-day drawer.
 *
 * Layer order required by the UI guide:
 *   global background (scrim) -> inset list container -> raised course cards.
 * The drawer shell itself is the global background colour and has no extra shadow.
 */
@Composable
fun DayDrawer(
    visible: Boolean,
    dayIndex: Int,
    date: LocalDate,
    courses: List<Course>,
    onDismiss: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    val scrimInteraction = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)),
        exit = fadeOut(tween(XingAnimation.NORMAL_MS, easing = EaseInOut)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
                ) { height -> height } + fadeIn(tween(XingAnimation.FAST_MS)),
                exit = slideOutVertically(
                    animationSpec = tween(XingAnimation.NORMAL_MS, easing = EaseInOut),
                ) { height -> height } + fadeOut(tween(XingAnimation.FAST_MS)),
            ) {
                DayDrawerPanel(
                    dayIndex = dayIndex,
                    date = date,
                    courses = courses,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun DayDrawerPanel(
    dayIndex: Int,
    date: LocalDate,
    courses: List<Course>,
    onDismiss: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    val shape = RoundedCornerShape(
        topStart = XingRadii.xlarge,
        topEnd = XingRadii.xlarge,
    )
    val insetShape = RoundedCornerShape(XingRadii.large)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.66f)
            .clip(shape)
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            .padding(horizontal = XingSpacing.lg)
            .padding(top = XingSpacing.md, bottom = XingSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weekdayFull(dayIndex),
                    style = XingClassType.screenTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateLabel(date),
                    style = XingClassType.caption,
                    color = colors.textSecondary,
                )
            }
            NeumorphicBadge(
                text = stringResource(R.string.day_drawer_section_count, courses.size),
                contentColor = colors.textSecondary,
            )
            Spacer(Modifier.width(XingSpacing.sm))
            NeumorphicIconButton(
                onClick = onDismiss,
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.day_drawer_close),
                size = 36.dp,
                iconSize = 16.dp,
            )
        }

        Spacer(Modifier.height(XingSpacing.md))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .neumorphic(
                    shape = insetShape,
                    style = NeumorphicStyle.INSET,
                    backgroundColor = colors.surface,
                    lightShadow = colors.lightShadow,
                    darkShadow = colors.darkShadow,
                    elevation = NeumorphicElevation.LARGE,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(insetShape)
                    .padding(10.dp),
            ) {
                if (courses.isEmpty()) {
                    EmptyDayHint(
                        title = stringResource(R.string.day_drawer_empty_title),
                        hint = stringResource(R.string.day_drawer_empty_hint),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(XingSpacing.sm),
                    ) {
                        courses.forEach { course ->
                            DrawerCourseRow(course = course)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerCourseRow(course: Course) {
    val colors = LocalNeumorphicColors.current
    val palette = colors.paletteFor(course.color)
    val shape = RoundedCornerShape(XingRadii.small)

    val rangeText = if (course.span <= 1) {
        course.start.toString()
    } else {
        stringResource(R.string.course_period_short_range, course.start, course.endPeriod)
    }
    val unknownLocation = stringResource(R.string.course_location_unknown)
    val location = remember(course.location) { parseLocation(course.location) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(
                shape = shape,
                style = NeumorphicStyle.RAISED,
                backgroundColor = palette.background,
                lightShadow = colors.lightShadow,
                darkShadow = colors.darkShadow,
                elevation = NeumorphicElevation.SMALL,
            )
            .padding(horizontal = XingSpacing.md, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(46.dp)) {
            Text(
                text = rangeText,
                style = XingClassType.label.copy(fontWeight = FontWeight.SemiBold),
                color = palette.content,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.course_period_unit),
                style = XingClassType.micro,
                color = palette.content,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(XingSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = XingClassType.drawerTitle,
                color = palette.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            when {
                location.building != null && location.room != null -> {
                    Text(
                        text = location.building.orEmpty(),
                        style = XingClassType.caption,
                        color = palette.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = location.room.orEmpty(),
                        style = XingClassType.caption,
                        color = palette.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                location.hasLocation -> {
                    Text(
                        text = location.raw ?: location.building ?: location.room.orEmpty(),
                        style = XingClassType.caption,
                        color = palette.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                else -> {
                    Text(
                        text = unknownLocation,
                        style = XingClassType.caption,
                        color = palette.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!course.teacher.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = course.teacher,
                    style = XingClassType.caption,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
