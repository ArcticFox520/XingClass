package com.xingclass.app.data.mapper

import com.xingclass.app.data.remote.dto.ChaoxingLessonDto
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseColor
import com.xingclass.app.domain.model.GridSpec

/**
 * Chaoxing DTO -> internal [Course] model.
 *
 * Rules:
 *  - title is mandatory; everything else has a documented fallback.
 *  - day must resolve to a valid weekday and start to a valid period, otherwise the entry is
 *    dropped (never silently moved to a different slot).
 *  - span is clamped so a lesson can never overflow the 10-period grid.
 *  - colour is derived from the course title, so the same course keeps its colour across
 *    refreshes and devices.
 */
class CourseMapper {

    fun toCourses(dtos: List<ChaoxingLessonDto>): List<Course> {
        val seen = HashSet<String>()
        val result = ArrayList<Course>(dtos.size)
        for (dto in dtos) {
            val course = toCourse(dto) ?: continue
            if (seen.add(course.id)) result += course
        }
        return result.sortedWith(compareBy({ it.day }, { it.start }, { it.title }))
    }

    fun toCourse(dto: ChaoxingLessonDto): Course? {
        val title = dto.title?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val parsed = CourseTimeParser.parse(dto.timeText)
        val day = dto.dayIndex ?: parsed.day ?: return null
        val start = dto.start ?: parsed.start ?: return null
        if (day !in 0 until GridSpec.DAYS || start !in 1..GridSpec.PERIODS) return null

        val span = when {
            dto.span != null && dto.span > 0 -> dto.span
            dto.end != null && dto.end >= start -> dto.end - start + 1
            parsed.span != null -> parsed.span
            else -> 1
        }.coerceIn(1, GridSpec.PERIODS - start + 1)

        val sourceId = dto.id?.trim()?.takeIf { it.isNotEmpty() }
        val id = occurrenceId(sourceId, title, day, start, span)

        return Course(
            id = id,
            day = day,
            start = start,
            span = span,
            title = title,
            location = dto.location?.trim()?.takeIf { it.isNotEmpty() },
            teacher = dto.teacher?.trim()?.takeIf { it.isNotEmpty() },
            color = colorFor(title),
        )
    }

    fun colorFor(title: String): CourseColor =
        CourseColor.entries[Math.floorMod(titleHash(title), CourseColor.entries.size)]

    /**
     * A source id can identify a course rather than a single occurrence (Chaoxing's courseId
     * repeats for every lesson of that course in a week). The grid slot and a title hash are
     * therefore part of the internal id: different occurrences stay distinct, while an exact
     * duplicate entry still collapses during de-duplication.
     */
    private fun occurrenceId(
        sourceId: String?,
        title: String,
        day: Int,
        start: Int,
        span: Int,
    ): String {
        val hash = titleHash(title)
        return if (sourceId != null) {
            "$sourceId@$day-$start-$span-$hash"
        } else {
            "cx-$day-$start-$span-$hash"
        }
    }

    private fun titleHash(title: String): Int {
        var hash = 0
        for (character in title.trim()) {
            hash = 31 * hash + character.code
        }
        return hash
    }

    companion object {
        const val MAX_PERIODS = GridSpec.PERIODS
    }
}
