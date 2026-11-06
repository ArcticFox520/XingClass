package com.xingclass.app.data.repository

import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.NoopLogger
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseCacheSnapshot
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
    fun `remote success writes cache and returns snapshot`() = runTest {
        val remote = FakeCourseRemote(AppResult.Success(listOf(course("a"))))
        val cache = FakeCourseCache()
        val repository = repository(remote, cache)

        val result = repository.refreshCourses()

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).value.courses.size)
        assertEquals(1, cache.writes)
        assertEquals(clock.nowMillis(), cache.snapshot?.savedAtMillis)
        assertEquals(1, cache.snapshot?.courses?.size)
    }

    @Test
    fun `remote failure keeps the previous cache untouched`() = runTest {
        val cached = CourseCacheSnapshot(listOf(course("cached")), savedAtMillis = 111L)
        val cache = FakeCourseCache(cached)
        val remote = FakeCourseRemote(AppResult.Failure(AppError.Network))
        val repository = repository(remote, cache)

        val result = repository.refreshCourses()

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

        val result = repository.refreshCourses()

        assertEquals(AppError.SessionExpired, (result as AppResult.Failure).error)
        assertEquals(1, auth.invalidateCalls)
        assertEquals(0, cache.writes)
    }

    @Test
    fun `cache hit returns the stored snapshot`() = runTest {
        val cached = CourseCacheSnapshot(listOf(course("cached")), savedAtMillis = 42L)
        val repository = repository(FakeCourseRemote(AppResult.Success(emptyList())), FakeCourseCache(cached))

        val snapshot = repository.getCachedCourses()

        assertEquals(42L, snapshot?.savedAtMillis)
        assertEquals(1, snapshot?.courses?.size)
    }

    @Test
    fun `cache miss returns null`() = runTest {
        val repository = repository(FakeCourseRemote(AppResult.Success(emptyList())), FakeCourseCache())

        assertNull(repository.getCachedCourses())
    }

    @Test
    fun `clearCourses clears the cache store`() = runTest {
        val cache = FakeCourseCache(CourseCacheSnapshot(listOf(course("x")), 1L))
        val repository = repository(FakeCourseRemote(AppResult.Success(emptyList())), cache)

        repository.clearCourses()

        assertEquals(1, cache.clears)
        assertNull(cache.snapshot)
    }

    @Test
    fun `an empty but successful remote response is cached as empty`() = runTest {
        val cache = FakeCourseCache(CourseCacheSnapshot(listOf(course("old")), 1L))
        val repository = repository(FakeCourseRemote(AppResult.Success(emptyList())), cache)

        val result = repository.refreshCourses()

        assertTrue((result as AppResult.Success).value.courses.isEmpty())
        assertEquals(1, cache.writes)
        assertTrue(cache.snapshot!!.courses.isEmpty())
    }
}
