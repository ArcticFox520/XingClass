package com.xingclass.app.data.repository

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.DiagnosticsLog
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.testutil.FakeCourseCache
import com.xingclass.app.testutil.FixedAppClock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime

class DiagnosticsRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val clock = FixedAppClock(LocalDateTime.of(2026, 11, 6, 10, 30))

    private fun createRepository(cache: FakeCourseCache): Pair<DiagnosticsRepositoryImpl, DiagnosticsLog> {
        val log = DiagnosticsLog(
            file = File(temporaryFolder.root, "diagnostics.log"),
            now = clock::nowMillis,
        )
        val repository = DiagnosticsRepositoryImpl(
            cache = cache,
            diagnostics = log,
            logger = NoopLogger(),
            clock = clock,
            versionName = "1.0.0",
        )
        return repository to log
    }

    private fun snapshot() = CourseCacheSnapshot(
        courses = listOf(
            Course(
                id = "a",
                day = 0,
                start = 1,
                span = 2,
                title = "示例课程",
                location = "示例楼C201",
                teacher = "王老师",
            ),
        ),
        savedAtMillis = 123L,
        week = 9,
        realCurrentWeek = 10,
        maxWeek = 22,
        lessonTimes = listOf("08:30-09:15", "09:20-10:05"),
    )

    @Test
    fun `exports the diagnostics log with metadata`() = runTest {
        val (repository, log) = createRepository(FakeCourseCache())
        log.info("COURSE_FETCH_SUCCESS")

        val result = repository.exportLogs()

        val document = (result as AppResult.Success).value
        assertTrue(document.fileName.startsWith("xingclass-log-"))
        assertTrue(document.fileName.endsWith(".txt"))
        assertEquals("text/plain", document.mimeType)
        assertTrue(document.content.contains("XingClass 运行日志"))
        assertTrue(document.content.contains("version: 1.0.0"))
        assertTrue(document.content.contains("COURSE_FETCH_SUCCESS"))
    }

    @Test
    fun `exports cached courses as json without credentials`() = runTest {
        val (repository, _) = createRepository(FakeCourseCache(snapshot()))

        val result = repository.exportCourses()

        val document = (result as AppResult.Success).value
        assertTrue(document.fileName.startsWith("xingclass-courses-"))
        assertTrue(document.fileName.endsWith(".json"))
        assertEquals("application/json", document.mimeType)

        val json = Json.parseToJsonElement(document.content).jsonObject
        assertEquals(9, json["week"]!!.jsonPrimitive.int)
        assertEquals(10, json["realCurrentWeek"]!!.jsonPrimitive.int)
        assertEquals(22, json["maxWeek"]!!.jsonPrimitive.int)
        assertEquals(1, json["courses"]!!.jsonArray.size)
        assertEquals("示例课程", json["courses"]!!.jsonArray[0].jsonObject["title"]!!.jsonPrimitive.content)
        assertFalse(document.content.contains("password", ignoreCase = true))
        assertFalse(document.content.contains("cookie", ignoreCase = true))
    }

    @Test
    fun `course export fails cleanly when nothing is cached`() = runTest {
        val (repository, _) = createRepository(FakeCourseCache())

        val result = repository.exportCourses()

        assertEquals(AppError.NoData, (result as AppResult.Failure).error)
    }
}
