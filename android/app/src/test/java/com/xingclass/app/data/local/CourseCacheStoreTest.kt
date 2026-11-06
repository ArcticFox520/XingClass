package com.xingclass.app.data.local

import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.model.CourseColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CourseCacheStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun course(
        id: String = "1",
        day: Int = 0,
        start: Int = 1,
        span: Int = 2,
    ) = Course(
        id = id,
        day = day,
        start = start,
        span = span,
        title = "高等数学",
        location = "A101",
        teacher = "张老师",
        color = CourseColor.LAVENDER,
    )

    @Test
    fun `writes and reads a snapshot`() = runTest {
        val file = File(temporaryFolder.root, "cache.json")
        val store = FileCourseCacheStore(file, UnconfinedTestDispatcher(testScheduler))
        val snapshot = CourseCacheSnapshot(
            courses = listOf(course()),
            savedAtMillis = 123456L,
            week = 9,
            realCurrentWeek = 10,
            maxWeek = 22,
        )

        store.write(snapshot)

        assertTrue(file.exists())
        val restored = store.read()
        assertEquals(123456L, restored?.savedAtMillis)
        assertEquals(9, restored?.week)
        assertEquals(10, restored?.realCurrentWeek)
        assertEquals(22, restored?.maxWeek)
        assertEquals(1, restored?.courses?.size)
        assertEquals("高等数学", restored?.courses?.single()?.title)
        assertEquals("A101", restored?.courses?.single()?.location)
        assertEquals(CourseColor.LAVENDER, restored?.courses?.single()?.color)
    }

    @Test
    fun `missing cache returns null`() = runTest {
        val store = FileCourseCacheStore(
            File(temporaryFolder.root, "missing.json"),
            UnconfinedTestDispatcher(testScheduler),
        )
        assertNull(store.read())
    }

    @Test
    fun `corrupt cache is discarded`() = runTest {
        val file = File(temporaryFolder.root, "corrupt.json")
        file.writeText("this is not json")
        val store = FileCourseCacheStore(file, UnconfinedTestDispatcher(testScheduler))

        assertNull(store.read())
        assertFalse(file.exists())
    }

    @Test
    fun `invalid courses are filtered on read`() = runTest {
        val file = File(temporaryFolder.root, "invalid.json")
        val store = FileCourseCacheStore(file, UnconfinedTestDispatcher(testScheduler))
        store.write(
            CourseCacheSnapshot(
                courses = listOf(course(id = "ok"), course(id = "bad", day = 9)),
                savedAtMillis = 1L,
            ),
        )

        val restored = store.read()
        assertEquals(1, restored?.courses?.size)
        assertEquals("ok", restored?.courses?.single()?.id)
    }

    @Test
    fun `clear removes the cache file`() = runTest {
        val file = File(temporaryFolder.root, "clear.json")
        val store = FileCourseCacheStore(file, UnconfinedTestDispatcher(testScheduler))
        store.write(CourseCacheSnapshot(listOf(course()), 1L))
        assertTrue(file.exists())

        store.clear()

        assertFalse(file.exists())
        assertNull(store.read())
    }
}
