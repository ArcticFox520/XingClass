package com.xingclass.app.domain.model

/**
 * Clock times for one teaching period, e.g. 08:30-09:15.
 *
 * The real `getMyLessons` payload provides these via
 * `data.curriculum.lessonTimeConfigArray`; they are parsed on demand and cached with the
 * schedule. Missing/unparsable entries stay null so period indexes never shift.
 */
data class PeriodTime(
    val start: String,
    val end: String,
) {
    val range: String get() = "$start-$end"
}

object PeriodTimes {
    /** The grid shows 10 periods; the API may return an 11th entry. */
    const val MAX_PERIODS = GridSpec.PERIODS

    private val separator = Regex("\\s*[-~—–]\\s*")
    private val clock = Regex("^\\d{1,2}:\\d{2}$")

    fun parse(lines: List<String>): List<PeriodTime?> =
        lines.take(MAX_PERIODS).map { line ->
            val parts = line.trim().split(separator, limit = 2)
            if (parts.size != 2) {
                null
            } else {
                val start = parts[0].trim()
                val end = parts[1].trim()
                if (clock.matches(start) && clock.matches(end)) PeriodTime(start, end) else null
            }
        }

    /** Full time range of a (possibly multi-period) course. */
    fun forCourse(course: Course, times: List<PeriodTime?>): PeriodTime? {
        val start = times.getOrNull(course.start - 1) ?: return null
        val end = times.getOrNull(course.endPeriod - 1) ?: return null
        return PeriodTime(start.start, end.end)
    }

    fun minutesOf(time: String): Int? {
        val parts = time.trim().split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /** Minutes between the end of one period and the start of the next; null if overnight/bad. */
    fun gapMinutes(end: String, start: String): Int? {
        val endMinutes = minutesOf(end) ?: return null
        val startMinutes = minutesOf(start) ?: return null
        return if (startMinutes >= endMinutes) startMinutes - endMinutes else null
    }
}
