package com.xingclass.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodTimesTest {

    @Test
    fun `parses the real period time array`() {
        val times = PeriodTimes.parse(
            listOf("08:30-09:15", "09:20-10:05", "10:25-11:10", "11:15-12:00"),
        )

        assertEquals(4, times.size)
        assertEquals(PeriodTime("08:30", "09:15"), times[0])
        assertEquals(PeriodTime("09:20", "10:05"), times[1])
        assertEquals("10:25-11:10", times[2]?.range)
    }

    @Test
    fun `keeps indexes when an entry is invalid`() {
        val times = PeriodTimes.parse(listOf("08:30-09:15", "", "10:25-11:10"))

        assertEquals(3, times.size)
        assertNull(times[1])
        assertEquals(PeriodTime("10:25", "11:10"), times[2])
    }

    @Test
    fun `supports different separators`() {
        assertEquals(PeriodTime("08:30", "09:15"), PeriodTimes.parse(listOf("08:30 ~ 09:15")).single())
        assertEquals(PeriodTime("08:30", "09:15"), PeriodTimes.parse(listOf("08:30—09:15")).single())
    }

    @Test
    fun `limits to ten periods`() {
        val raw = (1..11).map { "08:00-08:45" }
        assertEquals(10, PeriodTimes.parse(raw).size)
    }

    @Test
    fun `course range uses the first start and the last end`() {
        val times = PeriodTimes.parse(listOf("08:30-09:15", "09:20-10:05", "10:25-11:10"))
        val course = Course(
            id = "1",
            day = 0,
            start = 1,
            span = 3,
            title = "示例课程",
        )

        assertEquals(PeriodTime("08:30", "11:10"), PeriodTimes.forCourse(course, times))
    }

    @Test
    fun `computes gaps between period times`() {
        assertEquals(5, PeriodTimes.gapMinutes("09:15", "09:20"))
        assertEquals(20, PeriodTimes.gapMinutes("10:05", "10:25"))
        assertEquals(120, PeriodTimes.gapMinutes("12:00", "14:00"))
        assertNull(PeriodTimes.gapMinutes("10:00", "09:00"))
        assertNull(PeriodTimes.gapMinutes("bad", "09:00"))
    }
}
