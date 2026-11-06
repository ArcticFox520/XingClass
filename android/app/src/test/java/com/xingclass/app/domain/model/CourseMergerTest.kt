package com.xingclass.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CourseMergerTest {

    private val times = PeriodTimes.parse(
        listOf(
            "08:30-09:15",
            "09:20-10:05",
            "10:25-11:10",
            "11:15-12:00",
            "14:00-14:45",
            "14:50-15:35",
        ),
    )

    private fun course(
        id: String,
        start: Int,
        span: Int = 2,
        day: Int = 0,
        title: String = "高等数学",
        location: String? = "示例楼C201",
        teacher: String? = "王老师",
    ) = Course(
        id = id,
        day = day,
        start = start,
        span = span,
        title = title,
        location = location,
        teacher = teacher,
    )

    @Test
    fun `merges identical adjacent periods`() {
        val merged = CourseMerger.merge(
            listOf(course("a", 1), course("b", 3)),
            times,
        )

        assertEquals(1, merged.size)
        assertEquals(1, merged.single().start)
        assertEquals(4, merged.single().span)
        assertEquals("a+b", merged.single().id)
    }

    @Test
    fun `merges overlapping identical entries`() {
        val merged = CourseMerger.merge(
            listOf(course("a", 1, span = 2), course("b", 2, span = 2)),
            times,
        )

        assertEquals(1, merged.size)
        assertEquals(1, merged.single().start)
        assertEquals(3, merged.single().span)
    }

    @Test
    fun `does not merge across lunch break`() {
        // Period 4 ends 12:00; period 5 starts 14:00 (120 minute gap).
        val merged = CourseMerger.merge(
            listOf(course("a", 3), course("b", 5)),
            times,
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `does not merge non adjacent periods`() {
        val merged = CourseMerger.merge(
            listOf(course("a", 1), course("b", 5)),
            times,
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `does not merge different content`() {
        val merged = CourseMerger.merge(
            listOf(
                course("a", 1),
                course("b", 3, title = "大学英语"),
                course("c", 5, teacher = "李老师"),
            ),
            times,
        )

        assertEquals(3, merged.size)
    }

    @Test
    fun `does not merge different days`() {
        val merged = CourseMerger.merge(
            listOf(course("a", 1, day = 0), course("b", 3, day = 1)),
            times,
        )

        assertEquals(2, merged.size)
        assertNotEquals(merged[0].day, merged[1].day)
    }

    @Test
    fun `merges three consecutive entries`() {
        val merged = CourseMerger.merge(
            listOf(
                course("a", 1, span = 1),
                course("b", 2, span = 1),
                course("c", 3, span = 1),
            ),
            times,
        )

        assertEquals(1, merged.size)
        assertEquals(1, merged.single().start)
        assertEquals(3, merged.single().span)
    }

    @Test
    fun `single course is returned unchanged`() {
        val only = course("a", 1)
        assertEquals(listOf(only), CourseMerger.merge(listOf(only), times))
    }
}
