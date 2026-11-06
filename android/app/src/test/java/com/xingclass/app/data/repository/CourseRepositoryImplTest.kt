package com.xingclass.app.data.repository

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.model.CourseWeek
import com.xingclass.app.testutil.FakeAuthRepository
import com.xingclass.app.testutil.FakeCourseCache
import com.xingclass.app.testutil.FakeCourseRemote
import com.xingclass.app.testutil.FixedAppClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CourseRepositoryImplTest {

    private val clock = FixedAppClock(LocalDateTime.of(2026, 11, 6, 10, 30))
    private val auth = FakeAuthRepository()

    private fun course(id: String) = Course(
        id = id,
        day = 0,
        start = 1,
        span = 2,
        title = "课程$id",
    )

    private fun week(
        courses: List<Course>,
        week: Int = 10,
        realCurrentWeek: Int = 10,
        maxWeek: Int = 22,
    ) = CourseWeek(
        courses = courses,
        week = week,
        realCurrentWeek = realCurrentWeek,
        maxWeek = maxWeek,
        lessonTimes = listOf("08:30-09:15", "09:20-10:05"),
    )

    private fun repository(
        remote: FakeCourseRemote,
        cache: FakeCourseCache,
    ) = CourseRepositoryImpl(
        remote = remote,
        cache = cache,
        authRepository = auth,
        clock = clock,
        logger = NoopLogger(),
    )

    @Test
    fun `remote success writes cache with week metadata`() = runTest {
        val remote = FakeCourseRemote(AppResult.Success(week(listOf(course("a")))))
        val cache = FakeCourseCache()
        val repository = repository(remote, cache)

        val result = repository.refreshCourses(null)

        assertTrue(result is AppResult.Success)
        val snapshot = (result as AppResult.Success).value
        assertEquals(1, snapshot.courses.size)
        assertEquals(10, snapshot.week)
        assertEquals(10, snapshot.realCurrentWeek)
        assertEquals(22, snapshot.maxWeek)
        assertEquals(1, cache.writes)
        assertEquals(clock.nowMillis(), cache.snapshot?.savedAtMillis)
        assertEquals(10, cache.snapshot?.week)
        assertEquals(22, cache.snapshot?.maxWeek)
        assertEquals(listOf("08:30-09:15", "09:20-10:05"), cache.snapshot?.lessonTimes)
        assertNull(remote.lastWeek)
    }

    @Test
    fun `target week is forwarded and kept in the snapshot`() = runTest {
        val remote = FakeCourseRemote(
            AppResult.Success(week(listOf(course("w9")), week = 9, realCurrentWeek = 10)),
        )
        val cache = FakeCourseCache()
        val repository = repository(remote, cache)

        val result = repository.refreshCourses(9)

        assertTrue(result is AppResult.Success)
        val snapshot = (result as AppResult.Success).value
        assertEquals(9, remote.lastWeek)
        assertEquals(9, snapshot.week)
        assertEquals(10, snapshot.realCurrentWeek)
        assertEquals(9, cache.snapshot?.week)
    }

    @Test
    fun `remote failure keeps the previous cache untouched`() = runTest {
        val cached = CourseCacheSnapshot(
            courses = listOf(course("cached")),
            savedAtMillis = 111L,
            week = 9,
            realCurrentWeek = 10,
            maxWeek = 22,
        )
        val cache = FakeCourseCache(cached)
        val remote = FakeCourseRemote(AppResult.Failure(AppError.Network))
        val repository = repository(remote, cache)

        val result = repository.refreshCourses(9)

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Network, (result as AppResult.Failure).error)
        assertEquals(0, cache.writes)
        assertEquals(cached, cache.snapshot)
    }

    @Test
    fun `session expiry invalidates the local session and does not write cache`() = runTest {
        val cache = FakeCourseCache()
        val remote = FakeCourseRemote(AppResult.Failure(AppError.SessionExpired))
        val repository = repository(remote, cache)

        val result = repository.refreshCourses(null)

        assertEquals(AppError.SessionExpired, (result as AppResult.Failure).error)
        assertEquals(1, auth.invalidateCalls)
        assertEquals(0, cache.writes)
    }

    @Test
    fun `cache hit returns the stored snapshot with its week`() = runTest {
        val cached = CourseCacheSnapshot(
            courses = listOf(course("cached")),
            savedAtMillis = 42L,
            week = 9,
            realCurrentWeek = 10,
            maxWeek = 22,
        )
        val repository = repository(
            FakeCourseRemote(AppResult.Success(week(emptyList()))),
            FakeCourseCache(cached),
        )

        val snapshot = repository.getCachedCourses()

        assertEquals(42L, snapshot?.savedAtMillis)
        assertEquals(1, snapshot?.courses?.size)
        assertEquals(9, snapshot?.week)
        assertEquals(10, snapshot?.realCurrentWeek)
        assertEquals(22, snapshot?.maxWeek)
    }

    @Test
    fun `cache miss returns null`() = runTest {
        val repository = repository(
            FakeCourseRemote(AppResult.Success(week(emptyList()))),
            FakeCourseCache(),
        )

        assertNull(repository.getCachedCourses())
    }

    @Test
    fun `clearCourses clears the cache store`() = runTest {
        val cache = FakeCourseCache(CourseCacheSnapshot(listOf(course("x")), 1L))
        val repository = repository(FakeCourseRemote(AppResult.Success(week(emptyList()))), cache)

        repository.clearCourses()

        assertEquals(1, cache.clears)
        assertNull(cache.snapshot)
    }

    @Test
    fun `an empty but successful remote response is cached as empty`() = runTest {
        val cache = FakeCourseCache(CourseCacheSnapshot(listOf(course("old")), 1L))
        val repository = repository(FakeCourseRemote(AppResult.Success(week(emptyList()))), cache)

        val result = repository.refreshCourses(null)

        assertTrue((result as AppResult.Success).value.courses.isEmpty())
        assertEquals(1, cache.writes)
        assertTrue(cache.snapshot!!.courses.isEmpty())
        assertEquals(10, cache.snapshot?.week)
    }
}
