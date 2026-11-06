package com.xingclass.app.ui.components.schedule

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Text budget for a grid course card.
 *
 * The card has a fixed measurement (driven by the 7×10 grid), so the title/location split is
 * computed from measured height and the *real* line heights (including font scale) instead of
 * guessing from the course span.
 */
data class CourseCardTextLayout(
    val titleLines: Int,
    val locationLines: Int,
)

object CourseCardLayout {

    const val MAX_TITLE_LINES = 5

    fun compute(
        availableHeight: Dp,
        titleLineHeight: Dp,
        captionLineHeight: Dp,
        lineGap: Dp,
        locationLineCount: Int,
        maxTitleLines: Int = MAX_TITLE_LINES,
    ): CourseCardTextLayout {
        val safeLocationLines = locationLineCount.coerceIn(0, 2)
        val shownLocationLines = when {
            safeLocationLines == 0 -> 0
            availableHeight >= titleLineHeight +
                captionLineHeight * safeLocationLines + lineGap -> safeLocationLines
            availableHeight >= titleLineHeight + captionLineHeight + lineGap -> 1
            else -> 0
        }
        val reserved = if (shownLocationLines > 0) {
            captionLineHeight * shownLocationLines + lineGap
        } else {
            0.dp
        }
        val titleLines = ((availableHeight - reserved) / titleLineHeight)
            .toInt()
            .coerceIn(1, maxTitleLines)
        return CourseCardTextLayout(
            titleLines = titleLines,
            locationLines = shownLocationLines,
        )
    }
}
