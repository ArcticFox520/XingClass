package com.xingclass.app.data.remote

import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppResult
import com.xingclass.app.core.LogEvents
import com.xingclass.app.core.Logger
import com.xingclass.app.data.mapper.CourseMapper
import com.xingclass.app.domain.model.CourseWeek

/** Remote-only access to the schedule. The repository adds caching and session handling. */
interface CourseRemoteDataSource {
    /**
     * @param week 1-based teaching week, or null for the server's current week.
     */
    suspend fun fetchCourses(week: Int?): AppResult<CourseWeek>
}

class ChaoxingCourseRemoteDataSource(
    private val api: ChaoxingApi,
    private val mapper: CourseMapper,
    private val clock: AppClock,
    private val logger: Logger,
) : CourseRemoteDataSource {

    override suspend fun fetchCourses(week: Int?): AppResult<CourseWeek> {
        logger.event(LogEvents.COURSE_FETCH_STARTED)
        return when (val result = api.getMyLessons(clock.nowMillis(), week)) {
            is AppResult.Success -> {
                val schedule = result.value
                val realCurrentWeek = (schedule.realCurrentWeek ?: schedule.currentWeek ?: week ?: 1)
                    .coerceAtLeast(1)
                val selectedWeek = (week ?: schedule.currentWeek ?: realCurrentWeek)
                    .coerceAtLeast(1)
                val maxWeek = (schedule.maxWeek ?: maxOf(selectedWeek, realCurrentWeek))
                    .coerceAtLeast(1)
                AppResult.Success(
                    CourseWeek(
                        courses = mapper.toCourses(schedule.lessons),
                        week = selectedWeek.coerceIn(1, maxWeek),
                        realCurrentWeek = realCurrentWeek.coerceIn(1, maxWeek),
                        maxWeek = maxWeek,
                    ),
                )
            }

            is AppResult.Failure -> {
                logger.failure(LogEvents.COURSE_FETCH_FAILED, result.cause)
                AppResult.Failure(result.error, result.cause)
            }
        }
    }
}
