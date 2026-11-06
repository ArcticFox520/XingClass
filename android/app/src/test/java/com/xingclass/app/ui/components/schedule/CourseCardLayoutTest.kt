package com.xingclass.app.ui.components.schedule

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseCardLayoutTest {

    private fun layout(
        available: Int,
        locationLineCount: Int,
        titleLineHeight: Int = 12,
        captionLineHeight: Int = 11,
        gap: Int = 1,
    ) = CourseCardLayout.compute(
        availableHeight = available.dp,
        titleLineHeight = titleLineHeight.dp,
        captionLineHeight = captionLineHeight.dp,
        lineGap = gap.dp,
        locationLineCount = locationLineCount,
    )

    @Test
    fun `short single period card still shows one location line when it fits`() {
        val layout = layout(available = 30, locationLineCount = 2)

        assertEquals(1, layout.titleLines)
        assertEquals(1, layout.locationLines)
    }

    @Test
    fun `tall card shows building and room on separate lines`() {
        val layout = layout(available = 74, locationLineCount = 2)

        assertEquals(2, layout.locationLines)
        assertEquals(4, layout.titleLines)
    }

    @Test
    fun `no location gives the title all available lines`() {
        val layout = layout(available = 74, locationLineCount = 0)

        assertEquals(5, layout.titleLines)
        assertEquals(0, layout.locationLines)
    }

    @Test
    fun `too small for a location still shows one title line`() {
        val layout = layout(available = 20, locationLineCount = 2)

        assertEquals(1, layout.titleLines)
        assertEquals(0, layout.locationLines)
    }

    @Test
    fun `single line location fits on a small card`() {
        val layout = layout(available = 25, locationLineCount = 1)

        assertEquals(1, layout.titleLines)
        assertEquals(1, layout.locationLines)
    }

    @Test
    fun `large font line heights reduce the number of title lines`() {
        val normal = layout(available = 74, locationLineCount = 2)
        val scaled = layout(
            available = 74,
            locationLineCount = 2,
            titleLineHeight = 18,
            captionLineHeight = 16,
            gap = 1,
        )

        assertEquals(2, normal.locationLines)
        assertEquals(2, scaled.locationLines)
        assertEquals(true, scaled.titleLines < normal.titleLines)
    }
}
