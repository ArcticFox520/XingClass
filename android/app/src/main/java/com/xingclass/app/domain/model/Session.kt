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

/** Result of checking the locally persisted Chaoxing session. */
enum class SessionStatus {
    /** Server confirmed the session cookie is still valid. */
    VALID,

    /** Server explicitly rejected the session. */
    INVALID,

    /** Server answered but the response could not be interpreted (e.g. "参数为空"). */
    UNKNOWN,

    /** No local session cookie at all. */
    ABSENT,
}
