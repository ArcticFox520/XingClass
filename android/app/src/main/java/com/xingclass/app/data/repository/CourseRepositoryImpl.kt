package com.xingclass.app.data.repository

import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.local.CourseCacheStore
import com.xingclass.app.data.remote.CourseRemoteDataSource
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.repository.AuthRepository
import com.xingclass.app.domain.repository.CourseRepository
import kotlinx.coroutines.CancellationException

/**
 * Cache-first schedule repository.
 *
 * - Startup: cached snapshot first, then a remote refresh.
 * - Refresh failure: the cache is left untouched, so offline users keep their last schedule.
 * - Session expiry: the invalid session is cleared exactly once and surfaced to the UI.
 */
class CourseRepositoryImpl(
    private val remote: CourseRemoteDataSource,
    private val cache: CourseCacheStore,
    private val authRepository: AuthRepository,
    private val clock: AppClock,
    private val logger: Logger,
) : CourseRepository {

    override suspend fun getCachedCourses(): CourseCacheSnapshot? {
        val snapshot = cache.read()
        if (snapshot == null) {
            logger.event(LogEvents.COURSE_CACHE_MISS)
        } else {
            logger.event(LogEvents.COURSE_CACHE_HIT, "week=${snapshot.week} count=${snapshot.courses.size}")
        }
        return snapshot
    }

    override suspend fun refreshCourses(targetWeek: Int?): AppResult<CourseCacheSnapshot> {
        return when (val result = remote.fetchCourses(targetWeek)) {
            is AppResult.Success -> {
                val data = result.value
                val snapshot = CourseCacheSnapshot(
                    courses = data.courses,
                    savedAtMillis = clock.nowMillis(),
                    week = data.week,
                    realCurrentWeek = data.realCurrentWeek,
                    maxWeek = data.maxWeek,
                    lessonTimes = data.lessonTimes,
                )
                try {
                    cache.write(snapshot)
                    logger.event(LogEvents.COURSE_CACHE_WRITTEN)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.failure(LogEvents.COURSE_FETCH_FAILED, e)
                }
                logger.event(
                    LogEvents.COURSE_FETCH_SUCCESS,
                    "week=${snapshot.week} count=${snapshot.courses.size}",
                )
                AppResult.Success(snapshot)
            }

            is AppResult.Failure -> {
                if (result.error == AppError.SessionExpired) {
                    authRepository.invalidateSession()
                }
                AppResult.Failure(result.error, result.cause)
            }
        }
    }

    override suspend fun clearCourses() {
        try {
            cache.clear()
            logger.event(LogEvents.COURSE_CACHE_CLEARED)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.failure(LogEvents.COURSE_CACHE_CLEARED, e)
        }
    }
}
