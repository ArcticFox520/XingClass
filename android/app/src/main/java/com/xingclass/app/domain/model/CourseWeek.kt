package com.xingclass.app.domain.model

/**
 * One fetched week of the schedule.
 *
 * The Chaoxing endpoint returns the *requested* week's lessons when `week` is supplied, but the
 * `currentWeek` field inside the response always describes the real current week. Keep both
 * values: [week] is what the user is looking at, [realCurrentWeek] powers "回到本周".
 */
data class CourseWeek(
    val courses: List<Course>,
    val week: Int,
    val realCurrentWeek: Int,
    val maxWeek: Int,
    /** Raw period clock strings from the same response, used by the grid/drawer. */
    val lessonTimes: List<String> = emptyList(),
)
