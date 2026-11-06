package com.xingclass.app.ui.schedule

import com.xingclass.app.domain.model.Course

/**
 * Pure layout math for the one-screen schedule.
 *
 * The grid is always 7 days × 10 periods. A course is placed with:
 *  - rowIndex = start - 1
 *  - rowSpan  = span
 *  - columnIndex/columnCount for overlapping lessons (data can contain clashes)
 *
 * This class has no Compose dependency so the mapping is unit-testable.
 */
data class CoursePlacement(
    val course: Course,
    val dayIndex: Int,
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int = 0,
    val columnCount: Int = 1,
)

object ScheduleLayoutCalculator {
    const val DAYS = 7
    const val PERIODS = 10

    fun place(courses: List<Course>): List<CoursePlacement> {
        val valid = courses.filter {
            it.day in 0 until DAYS && it.start in 1..PERIODS
        }
        return valid
            .groupBy { it.day }
            .toSortedMap()
            .flatMap { (day, dayCourses) -> placeDay(day, dayCourses) }
            .sortedWith(compareBy({ it.dayIndex }, { it.rowIndex }, { it.columnIndex }))
    }

    private data class Slot(val course: Course, val start: Int, val span: Int)

    private fun placeDay(day: Int, courses: List<Course>): List<CoursePlacement> {
        val slots = courses.map {
            Slot(
                course = it,
                start = it.start,
                span = it.span.coerceIn(1, PERIODS - it.start + 1),
            )
        }.sortedWith(compareBy({ it.start }, { -it.span }, { it.course.title }))

        val placed = mutableListOf<CoursePlacement>()
        var cluster = mutableListOf<Slot>()
        var clusterEnd = 0

        fun flushCluster() {
            if (cluster.isEmpty()) return
            placed += assignColumns(day, cluster)
            cluster = mutableListOf()
            clusterEnd = 0
        }

        for (slot in slots) {
            if (cluster.isNotEmpty() && slot.start > clusterEnd) {
                flushCluster()
            }
            cluster += slot
            clusterEnd = maxOf(clusterEnd, slot.start + slot.span - 1)
        }
        flushCluster()
        return placed
    }

    /** Greedy interval graph colouring: returns one sub-column per mutually overlapping lesson. */
    private fun assignColumns(day: Int, cluster: List<Slot>): List<CoursePlacement> {
        val columns = mutableListOf<MutableList<Slot>>()
        for (slot in cluster) {
            var columnIndex = columns.indexOfFirst { column ->
                column.none { existing -> overlaps(existing, slot) }
            }
            if (columnIndex == -1) {
                columns.add(mutableListOf())
                columnIndex = columns.lastIndex
            }
            columns[columnIndex].add(slot)
        }
        val columnCount = columns.size
        return columns.flatMapIndexed { index, column ->
            column.map { slot ->
                CoursePlacement(
                    course = slot.course,
                    dayIndex = day,
                    rowIndex = slot.start - 1,
                    rowSpan = slot.span,
                    columnIndex = index,
                    columnCount = columnCount,
                )
            }
        }
    }

    private fun overlaps(a: Slot, b: Slot): Boolean =
        a.start < b.start + b.span && b.start < a.start + a.span
}
