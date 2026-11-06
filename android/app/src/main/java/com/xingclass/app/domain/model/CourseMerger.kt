package com.xingclass.app.domain.model

/**
 * Merges completely identical, time-contiguous courses into a single grid block.
 *
 * "Identical" means same day, title, location and teacher. "Time-contiguous" means either
 * overlapping periods or the immediately following period with no more than
 * [MAX_CONTIGUOUS_GAP_MINUTES] of clock time between them. This avoids merging a morning course
 * with the same course after lunch.
 */
object CourseMerger {
    const val MAX_CONTIGUOUS_GAP_MINUTES = 30

    fun merge(
        courses: List<Course>,
        periodTimes: List<PeriodTime?> = emptyList(),
    ): List<Course> {
        if (courses.size < 2) return courses

        val sorted = courses.sortedWith(
            compareBy({ it.day }, { it.start }, { it.span }, { it.id }),
        )
        val merged = mutableListOf<Course>()

        for (course in sorted) {
            val last = merged.lastOrNull()
            if (last != null && sameContent(last, course) && isContiguous(last, course, periodTimes)) {
                val newEnd = maxOf(last.endPeriod, course.endPeriod)
                merged[merged.lastIndex] = last.copy(
                    span = newEnd - last.start + 1,
                    id = "${last.id}+${course.id}",
                )
            } else {
                merged += course
            }
        }
        return merged
    }

    private fun sameContent(a: Course, b: Course): Boolean =
        a.day == b.day &&
            a.title == b.title &&
            a.location == b.location &&
            a.teacher == b.teacher

    private fun isContiguous(
        last: Course,
        next: Course,
        times: List<PeriodTime?>,
    ): Boolean {
        if (next.day != last.day) return false
        if (next.start <= last.endPeriod) return true // overlapping identical entries
        if (next.start != last.endPeriod + 1) return false

        val lastEnd = times.getOrNull(last.endPeriod - 1)?.end
        val nextStart = times.getOrNull(next.start - 1)?.start
        if (lastEnd == null || nextStart == null) return true

        val gap = PeriodTimes.gapMinutes(lastEnd, nextStart) ?: return true
        return gap in 0..MAX_CONTIGUOUS_GAP_MINUTES
    }
}
