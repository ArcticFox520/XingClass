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
    /** Raw `weeks` field (e.g. "10"); used to verify week switching in the live test. */
    val weeks: String? = null,
)

/**
 * A complete lesson page. Week fields come from `data.curriculum`; when [week] is requested via
 * the `week` query parameter the server still reports the *real* current week in
 * [currentWeek]/[realCurrentWeek], which is exactly what the UI needs for "回到本周".
 */
data class ChaoxingScheduleDto(
    val lessons: List<ChaoxingLessonDto>,
    val currentWeek: Int? = null,
    val realCurrentWeek: Int? = null,
    val maxWeek: Int? = null,
    /** Raw `curriculum.lessonTimeConfigArray` (e.g. "08:30-09:15"); parsed by PeriodTimes. */
    val lessonTimes: List<String> = emptyList(),
)

sealed interface LessonPayload {
    data class Lessons(val schedule: ChaoxingScheduleDto) : LessonPayload {
        val lessons: List<ChaoxingLessonDto> get() = schedule.lessons
    }

    /** JSON was valid but contained no recognizable lesson list. */
    data object NoLessonContainer : LessonPayload

    data object InvalidPayload : LessonPayload
}
