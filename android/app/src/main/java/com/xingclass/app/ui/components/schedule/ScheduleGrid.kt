package com.xingclass.app.ui.components.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.xingclass.app.R
import com.xingclass.app.core.AppError
import com.xingclass.app.domain.model.Course
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicLoadingDots
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.neumorphic
import com.xingclass.app.ui.schedule.ScheduleLayoutCalculator
import com.xingclass.app.ui.schedule.ScheduleStatus
import com.xingclass.app.ui.schedule.ScheduleUiState
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.asString
import com.xingclass.app.ui.util.toUiText
import kotlin.math.roundToInt

object ScheduleMetrics {
    val PeriodColumnWidth = 38.dp
    val Gap = 6.dp
    val GridPadding = 6.dp
}

/**
 * The recessed grid base. The whole week (7 × 10) is always laid out; there is no scroll
 * container anywhere in this composable, by design.
 */
@Composable
fun ScheduleGrid(
    state: ScheduleUiState,
    onRetry: () -> Unit,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    val shape = RoundedCornerShape(XingRadii.large)
    val placements = remember(state.courses) {
        ScheduleLayoutCalculator.place(state.courses)
    }

    Box(
        modifier = modifier.neumorphic(
            shape = shape,
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
                .clip(shape)
                .padding(ScheduleMetrics.GridPadding),
        ) {
            GridGuides(
                todayIndex = state.todayIndex,
                modifier = Modifier.matchParentSize(),
            )

            when {
                state.courses.isEmpty() &&
                    (state.status == ScheduleStatus.LOADING || state.status == ScheduleStatus.REFRESHING) ->
                    GridLoading()

                state.courses.isEmpty() && state.error != null ->
                    GridError(error = state.error ?: AppError.Unknown, onRetry = onRetry)

                state.courses.isEmpty() -> GridEmpty()

                else -> ScheduleGridBody(
                    placements = placements,
                    onCourseClick = onCourseClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun GridGuides(
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val columnWidth = size.width / ScheduleLayoutCalculator.DAYS
        val rowHeight = size.height / ScheduleLayoutCalculator.PERIODS

        if (todayIndex in 0 until ScheduleLayoutCalculator.DAYS) {
            drawRoundRect(
                color = colors.accent.copy(alpha = if (colors.isDark) 0.06f else 0.045f),
                topLeft = Offset(todayIndex * columnWidth, 0f),
                size = Size(columnWidth, size.height),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
        }

        val lineWidth = 0.7.dp.toPx().coerceAtLeast(1f)
        for (row in 1 until ScheduleLayoutCalculator.PERIODS) {
            val y = row * rowHeight
            drawLine(
                color = colors.darkShadow.copy(alpha = 0.30f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineWidth,
            )
            drawLine(
                color = colors.lightShadow.copy(alpha = 0.50f),
                start = Offset(0f, y + lineWidth),
                end = Offset(size.width, y + lineWidth),
                strokeWidth = lineWidth,
            )
        }
        for (column in 1 until ScheduleLayoutCalculator.DAYS) {
            val x = column * columnWidth
            drawLine(
                color = colors.darkShadow.copy(alpha = 0.16f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = lineWidth,
            )
        }
    }
}

@Composable
private fun ScheduleGridBody(
    placements: List<com.xingclass.app.ui.schedule.CoursePlacement>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    Layout(
        modifier = modifier,
        content = {
            placements.forEach { placement ->
                key(placement.course.id) {
                    CourseCard(
                        course = placement.course,
                        palette = colors.paletteFor(placement.course.color),
                        showLocation = placement.rowSpan >= 2,
                        compact = placement.columnCount > 1,
                        onClick = { onCourseClick(placement.course) },
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        if (width <= 0 || height <= 0 || measurables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }

        val dayWidth = width / ScheduleLayoutCalculator.DAYS.toFloat()
        val rowHeight = height / ScheduleLayoutCalculator.PERIODS.toFloat()
        val gap = 1.5.dp.roundToPx().coerceAtLeast(1)

        val placed = measurables.mapIndexed { index, measurable ->
            val placement = placements[index]
            val columnWidth = dayWidth / placement.columnCount
            val left = placement.dayIndex * dayWidth + placement.columnIndex * columnWidth
            val top = placement.rowIndex * rowHeight
            val cardWidth = (columnWidth - gap * 2).roundToInt().coerceAtLeast(1)
            val cardHeight = (placement.rowSpan * rowHeight - gap * 2).roundToInt().coerceAtLeast(1)
            val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))
            placeable to Offset(left + gap, top + gap)
        }

        layout(width, height) {
            placed.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x.roundToInt(), offset.y.roundToInt())
            }
        }
    }
}

@Composable
private fun GridLoading() {
    val colors = LocalNeumorphicColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeumorphicLoadingDots()
        Spacer(Modifier.height(XingSpacing.md))
        Text(
            text = stringResource(R.string.schedule_loading),
            style = XingClassType.caption,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun GridEmpty() {
    val colors = LocalNeumorphicColors.current
    GridMessage(
        title = stringResource(R.string.schedule_empty_title),
        body = stringResource(R.string.schedule_empty_hint),
        titleColor = colors.textPrimary,
    )
}

@Composable
private fun GridError(
    error: AppError,
    onRetry: () -> Unit,
) {
    val colors = LocalNeumorphicColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(XingSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.schedule_error_title),
            style = XingClassType.bodyStrong,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(XingSpacing.xs))
        Text(
            text = error.toUiText().asString(),
            style = XingClassType.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(XingSpacing.md))
        NeumorphicButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(
                text = stringResource(R.string.schedule_retry),
                style = XingClassType.label,
            )
        }
    }
}

@Composable
private fun GridMessage(
    title: String,
    body: String,
    titleColor: androidx.compose.ui.graphics.Color,
) {
    val colors = LocalNeumorphicColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(XingSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = XingClassType.bodyStrong,
            color = titleColor,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(XingSpacing.xs))
        Text(
            text = body,
            style = XingClassType.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
