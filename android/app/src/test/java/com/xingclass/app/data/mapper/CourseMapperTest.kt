package com.xingclass.app.data.mapper

import com.xingclass.app.data.remote.dto.ChaoxingLessonDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseMapperTest {

    private val mapper = CourseMapper()

    @Test
    fun `maps a normal lesson`() {
        val courses = mapper.toCourses(
            listOf(
                ChaoxingLessonDto(
                    id = "1",
                    title = "高等数学",
                    teacher = "张老师",
                    location = "A101",
                    dayIndex = 0,
                    start = 1,
                    span = 2,
                ),
            ),
        )

        val course = courses.single()
        assertTrue(course.id.startsWith("1@0-1-2-"))
        assertEquals("高等数学", course.title)
        assertEquals(0, course.day)
        assertEquals(1, course.start)
        assertEquals(2, course.span)
        assertEquals(2, course.endPeriod)
        assertEquals("A101", course.location)
        assertEquals("张老师", course.teacher)
    }

    @Test
    fun `missing location and teacher become null`() {
        val course = mapper.toCourse(
            ChaoxingLessonDto(title = "自习", dayIndex = 3, start = 5, span = 1, location = "  ", teacher = ""),
        )!!
        assertNull(course.location)
        assertNull(course.teacher)
    }

    @Test
    fun `cross period lesson keeps its span`() {
        val course = mapper.toCourse(
            ChaoxingLessonDto(title = "实验课", dayIndex = 1, start = 3, end = 5),
        )!!
        assertEquals(3, course.start)
        assertEquals(3, course.span)
    }

    @Test
    fun `supports all seven weekdays`() {
        val courses = mapper.toCourses(
            (0..6).map { day ->
                ChaoxingLessonDto(title = "课程$day", dayIndex = day, start = 1, span = 1)
            },
        )
        assertEquals(7, courses.size)
        assertEquals((0..6).toList(), courses.map { it.day })
    }

    @Test
    fun `empty dto list maps to empty courses`() {
        assertTrue(mapper.toCourses(emptyList()).isEmpty())
    }

    @Test
    fun `falls back to time text when day and start are missing`() {
        val course = mapper.toCourse(
            ChaoxingLessonDto(title = "毛概", timeText = "周三第5-6节"),
        )!!
        assertEquals(2, course.day)
        assertEquals(5, course.start)
        assertEquals(2, course.span)
    }

    @Test
    fun `drops lessons that cannot be placed`() {
        assertNull(mapper.toCourse(ChaoxingLessonDto(title = "无时间", dayIndex = 1)))
        assertNull(mapper.toCourse(ChaoxingLessonDto(title = "无星期", start = 1)))
        assertNull(mapper.toCourse(ChaoxingLessonDto(title = "越界", dayIndex = 9, start = 1)))
        assertNull(mapper.toCourse(ChaoxingLessonDto(title = "越界", dayIndex = 1, start = 11)))
        assertNull(mapper.toCourse(ChaoxingLessonDto(title = "   ", dayIndex = 1, start = 1)))
    }

    @Test
    fun `clamps a span that would overflow the ten period grid`() {
        val course = mapper.toCourse(
            ChaoxingLessonDto(title = "晚课", dayIndex = 4, start = 9, span = 5),
        )!!
        assertEquals(9, course.start)
        assertEquals(2, course.span)
        assertEquals(10, course.endPeriod)
    }

    @Test
    fun `generates a stable id when none is provided`() {
        val dto = ChaoxingLessonDto(title = "大学物理", dayIndex = 2, start = 3, span = 2)
        val first = mapper.toCourse(dto)!!
        val second = mapper.toCourse(dto)!!
        assertEquals(first.id, second.id)
        assertTrue(first.id.startsWith("cx-"))
    }

    @Test
    fun `deduplicates exact duplicate entries`() {
        val duplicate = ChaoxingLessonDto(id = "same", title = "重复", dayIndex = 1, start = 1)
        val courses = mapper.toCourses(listOf(duplicate, duplicate))
        assertEquals(1, courses.size)
    }

    @Test
    fun `keeps different occurrences of one course distinct`() {
        // Real Chaoxing payloads repeat a courseId for every lesson of that course in the week;
        // the lessonId is unique per occurrence. Both must survive mapping.
        val first = ChaoxingLessonDto(id = "lesson-1", title = "高等数学", dayIndex = 0, start = 1, span = 2)
        val second = ChaoxingLessonDto(id = "lesson-2", title = "高等数学", dayIndex = 2, start = 3, span = 2)
        val courses = mapper.toCourses(listOf(first, second))

        assertEquals(2, courses.size)
        assertEquals(2, courses.map { it.id }.toSet().size)
        assertEquals(2, courses.map { it.day }.toSet().size)
    }

    @Test
    fun `course level ids at different slots stay distinct`() {
        val first = ChaoxingLessonDto(id = "course-1", title = "大学英语", dayIndex = 0, start = 1, span = 2)
        val second = first.copy(dayIndex = 3, start = 5, span = 2)
        val courses = mapper.toCourses(listOf(first, second))

        assertEquals(2, courses.size)
        assertEquals(2, courses.map { it.id }.toSet().size)
    }

    @Test
    fun `course colour is stable per title`() {
        repeat(3) {
            assertEquals(mapper.colorFor("高等数学"), mapper.colorFor("高等数学"))
        }
    }

    @Test
    fun `courses are sorted by day and start`() {
        val courses = mapper.toCourses(
            listOf(
                ChaoxingLessonDto(title = "C", dayIndex = 4, start = 2, span = 1),
                ChaoxingLessonDto(title = "A", dayIndex = 1, start = 3, span = 1),
                ChaoxingLessonDto(title = "B", dayIndex = 1, start = 1, span = 1),
            ),
        )
        assertEquals(listOf("B", "A", "C"), courses.map { it.title })
    }
}
