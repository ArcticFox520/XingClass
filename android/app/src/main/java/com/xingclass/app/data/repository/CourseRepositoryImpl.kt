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
            logger.event(LogEvents.COURSE_CACHE_HIT)
        }
        return snapshot
    }

    override suspend fun refreshCourses(): AppResult<CourseCacheSnapshot> {
        return when (val result = remote.fetchCourses()) {
            is AppResult.Success -> {
                val snapshot = CourseCacheSnapshot(
                    courses = result.value,
                    savedAtMillis = clock.nowMillis(),
                )
                runCatching { cache.write(snapshot) }
                    .onSuccess { logger.event(LogEvents.COURSE_CACHE_WRITTEN) }
                    .onFailure { logger.failure(LogEvents.COURSE_FETCH_FAILED, it) }
                logger.event(LogEvents.COURSE_FETCH_SUCCESS)
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
        runCatching { cache.clear() }
            .onFailure { logger.failure(LogEvents.COURSE_CACHE_CLEARED, it) }
        logger.event(LogEvents.COURSE_CACHE_CLEARED)
    }
}
