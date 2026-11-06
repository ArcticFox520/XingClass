package com.xingclass.app.ui.schedule

import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleLayoutCalculatorTest {

    private fun course(
        id: String,
        day: Int,
        start: Int,
        span: Int,
        title: String = id,
    ) = Course(
        id = id,
        day = day,
        start = start,
        span = span,
        title = title,
        color = CourseColor.LAVENDER,
    )

    @Test
    fun `maps day start and span to a grid position`() {
        val placement = ScheduleLayoutCalculator.place(
            listOf(course("a", day = 2, start = 3, span = 2)),
        ).single()

        assertEquals(2, placement.dayIndex)
        assertEquals(2, placement.rowIndex) // start 3 -> third row (0 based 2)
        assertEquals(2, placement.rowSpan)
        assertEquals(0, placement.columnIndex)
        assertEquals(1, placement.columnCount)
    }

    @Test
    fun `places every weekday in its own column`() {
        val placements = ScheduleLayoutCalculator.place(
            (0..6).map { day -> course("c$day", day = day, start = 1, span = 1) },
        )
        assertEquals(7, placements.size)
        assertEquals((0..6).toList(), placements.map { it.dayIndex })
    }

    @Test
    fun `clamps a span that overflows the grid`() {
        val placement = ScheduleLayoutCalculator.place(
            listOf(course("a", day = 1, start = 10, span = 4)),
        ).single()
        assertEquals(9, placement.rowIndex)
        assertEquals(1, placement.rowSpan)
    }

    @Test
    fun `ignores out of range days`() {
        val placements = ScheduleLayoutCalculator.place(
            listOf(course("a", day = -1, start = 1, span = 1), course("b", day = 7, start = 1, span = 1)),
        )
        assertTrue(placements.isEmpty())
    }

    @Test
    fun `splits overlapping lessons into columns`() {
        val placements = ScheduleLayoutCalculator.place(
            listOf(
                course("a", day = 0, start = 1, span = 2),
                course("b", day = 0, start = 2, span = 2),
            ),
        )
        assertEquals(2, placements.size)
        assertTrue(placements.all { it.columnCount == 2 })
        assertEquals(setOf(0, 1), placements.map { it.columnIndex }.toSet())
    }

    @Test
    fun `back to back lessons share one column`() {
        val placements = ScheduleLayoutCalculator.place(
            listOf(
                course("a", day = 0, start = 1, span = 2),
                course("b", day = 0, start = 3, span = 2),
            ),
        )
        assertEquals(2, placements.size)
        assertTrue(placements.all { it.columnCount == 1 })
    }

    @Test
    fun `three overlapping lessons share three columns`() {
        val placements = ScheduleLayoutCalculator.place(
            listOf(
                course("a", day = 3, start = 1, span = 3),
                course("b", day = 3, start = 2, span = 2),
                course("c", day = 3, start = 3, span = 2),
            ),
        )
        assertEquals(3, placements.size)
        assertTrue(placements.all { it.columnCount == 3 })
        assertEquals(setOf(0, 1, 2), placements.map { it.columnIndex }.toSet())
    }

    @Test
    fun `empty input produces no placements`() {
        assertTrue(ScheduleLayoutCalculator.place(emptyList()).isEmpty())
    }

    @Test
    fun `every placement stays inside the grid`() {
        val courses = (0..6).flatMap { day ->
            (1..10).map { period -> course("$day-$period", day = day, start = period, span = 1) }
        }
        ScheduleLayoutCalculator.place(courses).forEach { placement ->
            assertTrue(placement.dayIndex in 0..6)
            assertTrue(placement.rowIndex in 0..9)
            assertTrue(placement.rowSpan >= 1)
            assertTrue(placement.rowIndex + placement.rowSpan <= 10)
            assertTrue(placement.columnIndex < placement.columnCount)
        }
    }
}
