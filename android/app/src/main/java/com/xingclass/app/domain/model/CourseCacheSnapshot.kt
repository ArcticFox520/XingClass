package com.xingclass.app.domain.model

/** Snapshot of the locally cached schedule, including the week it belongs to. */
data class CourseCacheSnapshot(
    val courses: List<Course>,
    val savedAtMillis: Long,
    val week: Int = 1,
    val realCurrentWeek: Int = 1,
    val maxWeek: Int = 1,
    val lessonTimes: List<String> = emptyList(),
)
