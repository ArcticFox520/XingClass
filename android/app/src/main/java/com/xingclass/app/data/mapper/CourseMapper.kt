package com.xingclass.app.data.mapper

import com.xingclass.app.data.remote.dto.ChaoxingLessonDto
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.CourseColor

/**
 * Chaoxing DTO -> internal [Course] model.
 *
 * Rules:
 *  - title is mandatory; everything else has a documented fallback.
 *  - day must resolve to 0..6 and start to 1..10, otherwise the entry is dropped (never
 *    silently moved to a different slot).
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
        if (day !in 0..6 || start !in 1..10) return null

        val span = when {
            dto.span != null && dto.span > 0 -> dto.span
            dto.end != null && dto.end >= start -> dto.end - start + 1
            parsed.span != null -> parsed.span
            else -> 1
        }.coerceIn(1, MAX_PERIODS - start + 1)

        val id = dto.id?.trim()?.takeIf { it.isNotEmpty() }
            ?: stableId(title, day, start, span)

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

    fun colorFor(title: String): CourseColor {
        var hash = 0
        for (character in title.trim()) {
            hash = 31 * hash + character.code
        }
        return CourseColor.entries[Math.floorMod(hash, CourseColor.entries.size)]
    }

    private fun stableId(title: String, day: Int, start: Int, span: Int): String {
        var hash = 0
        for (character in title) {
            hash = 31 * hash + character.code
        }
        return "cx-$day-$start-$span-$hash"
    }

    companion object {
        const val MAX_PERIODS = 10
    }
}
