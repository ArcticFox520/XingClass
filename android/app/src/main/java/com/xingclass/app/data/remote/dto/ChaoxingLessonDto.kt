package com.xingclass.app.data.remote.dto

/**
 * Normalized view of one Chaoxing lesson entry.
 *
 * The real lesson payload of `getMyLessons` is not fully documented (see docs/API.md, section
 * "UNVERIFIED"). The parser therefore accepts several field aliases and this DTO keeps both
 * numeric section data and the raw time text so the mapper can fall back to text parsing.
 */
data class ChaoxingLessonDto(
    val id: String? = null,
    val title: String? = null,
    val teacher: String? = null,
    val location: String? = null,
    /** 0 = Monday … 6 = Sunday, already normalized from one-based fields when necessary. */
    val dayIndex: Int? = null,
    val start: Int? = null,
    val end: Int? = null,
    val span: Int? = null,
    val timeText: String? = null,
)

sealed interface LessonPayload {
    data class Lessons(val lessons: List<ChaoxingLessonDto>) : LessonPayload

    /** JSON was valid but contained no recognizable lesson list. */
    data object NoLessonContainer : LessonPayload

    data object InvalidPayload : LessonPayload
}
