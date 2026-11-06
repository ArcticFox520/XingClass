package com.xingclass.app.data.remote

import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.mapper.CourseMapper
import com.xingclass.app.domain.model.Course

/** Remote-only access to the schedule. The repository adds caching and session handling. */
interface CourseRemoteDataSource {
    suspend fun fetchCourses(): AppResult<List<Course>>
}

class ChaoxingCourseRemoteDataSource(
    private val api: ChaoxingApi,
    private val mapper: CourseMapper,
    private val clock: AppClock,
    private val logger: Logger,
) : CourseRemoteDataSource {

    override suspend fun fetchCourses(): AppResult<List<Course>> {
        logger.event(LogEvents.COURSE_FETCH_STARTED)
        return when (val result = api.getMyLessons(clock.nowMillis())) {
            is AppResult.Success -> AppResult.Success(mapper.toCourses(result.value))
            is AppResult.Failure -> {
                logger.failure(LogEvents.COURSE_FETCH_FAILED, result.cause)
                AppResult.Failure(result.error, result.cause)
            }
        }
    }
}
