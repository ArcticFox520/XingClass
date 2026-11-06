package com.xingclass.app.data.remote

import com.xingclass.app.data.remote.dto.ChaoxingLessonParser
import com.xingclass.app.data.remote.dto.LessonPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaoxingLessonParserTest {

    private fun lessonsOf(raw: String) = ChaoxingLessonParser.parse(raw) as LessonPayload.Lessons

    @Test
    fun `parses the documented data array`() {
        val raw = """
            {
              "status": 200,
              "data": [
                {"id": "c1", "name": "高等数学", "teacher": "张老师", "time": "周一第1-2节", "location": "A101", "status": "1"},
                {"id": "c2", "name": "大学英语", "teacher": "李老师", "time": "周三第3节", "location": "B202"}
              ]
            }
        """.trimIndent()

        val lessons = lessonsOf(raw).lessons
        assertEquals(2, lessons.size)
        assertEquals("高等数学", lessons[0].title)
        assertEquals("张老师", lessons[0].teacher)
        assertEquals("A101", lessons[0].location)
        assertEquals("周一第1-2节", lessons[0].timeText)
        assertEquals("c1", lessons[0].id)
        // Day/section live in the time text; the mapper resolves them.
        assertNull(lessons[0].dayIndex)
        assertNull(lessons[0].start)
    }

    @Test
    fun `parses numeric week day and sections`() {
        val raw = """
            {"code": 1, "data": [
              {"courseId": "42", "courseName": "线性代数", "weekDay": 1, "startSection": 3, "endSection": 4,
               "classroom": "C303", "teacherName": "王老师"}
            ]}
        """.trimIndent()

        val lesson = lessonsOf(raw).lessons.single()
        assertEquals("线性代数", lesson.title)
        assertEquals(0, lesson.dayIndex) // weekDay 1 (Monday) -> index 0
        assertEquals(3, lesson.start)
        assertEquals(4, lesson.end)
        assertEquals("C303", lesson.location)
        assertEquals("王老师", lesson.teacher)
    }

    @Test
    fun `parses a json string nested inside data`() {
        val raw = """{"status":200,"data":"[{\"name\":\"体育\",\"weekDay\":7,\"start\":7,\"span\":2}]"}"""
        val lesson = lessonsOf(raw).lessons.single()
        assertEquals("体育", lesson.title)
        assertEquals(6, lesson.dayIndex) // 7 -> Sunday
        assertEquals(7, lesson.start)
        assertEquals(2, lesson.span)
    }

    @Test
    fun `parses data wrapping a list object`() {
        val raw = """
            {"status":200,"data":{"total":1,"list":[
              {"name":"操作系统","day":4,"start":5,"span":2,"teacher":["赵老师","钱老师"],
               "locationInfo":{"name":"实验楼"}}
            ]}}
        """.trimIndent()

        val lesson = lessonsOf(raw).lessons.single()
        assertEquals("操作系统", lesson.title)
        assertEquals(4, lesson.dayIndex)
        assertEquals(5, lesson.start)
        assertEquals("赵老师、钱老师", lesson.teacher)
        assertEquals("实验楼", lesson.location)
    }

    @Test
    fun `parses sections array`() {
        val raw = """[{"name":"数据结构","day":2,"sections":[3,4],"place":"D404"}]"""
        val lesson = lessonsOf(raw).lessons.single()
        assertEquals(3, lesson.start)
        assertEquals(4, lesson.end)
        assertEquals(2, lesson.span)
        assertEquals("D404", lesson.location)
    }

    @Test
    fun `empty lesson array is a valid empty payload`() {
        val payload = ChaoxingLessonParser.parse("""{"status":200,"data":[]}""")
        assertTrue(payload is LessonPayload.Lessons)
        assertEquals(0, (payload as LessonPayload.Lessons).lessons.size)
    }

    @Test
    fun `invalid json is reported separately from a missing container`() {
        assertTrue(ChaoxingLessonParser.parse("not-json") is LessonPayload.InvalidPayload)
        assertTrue(ChaoxingLessonParser.parse("""{"status":200}""") is LessonPayload.NoLessonContainer)
    }

    @Test
    fun `skips entries without a title`() {
        val raw = """[{"id":"1","weekDay":1,"start":1},{"name":"有效课程","weekDay":1,"start":2}]"""
        val lessons = lessonsOf(raw).lessons
        assertEquals(1, lessons.size)
        assertEquals("有效课程", lessons.single().title)
    }

    @Test
    fun `ignores one based out of range week day`() {
        val raw = """[{"name":"异常课程","weekDay":9,"start":1}]"""
        val lesson = lessonsOf(raw).lessons.single()
        assertNull(lesson.dayIndex)
    }
}
