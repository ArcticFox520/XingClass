package com.xingclass.app.domain.model

/** Snapshot of the locally cached schedule. */
data class CourseCacheSnapshot(
    val courses: List<Course>,
    val savedAtMillis: Long,
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
