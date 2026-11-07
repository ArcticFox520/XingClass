package com.xingclass.app.data.mapper

import com.xingclass.app.domain.model.GridSpec

/**
 * Parses Chaoxing's human readable time text, e.g. "周一第1-2节", "星期三 5-6节".
 *
 * Deliberately conservative: section numbers are only recognised when the text clearly talks
 * about periods ("节"). Clock ranges such as "08:00-09:40" are ignored instead of being
 * misinterpreted as periods.
 */
object CourseTimeParser {

    data class Parsed(
        val day: Int? = null,
        val start: Int? = null,
        val span: Int? = null,
    )

    private val dayPattern = Regex("(?:星期|周|礼拜)\\s*([一二三四五六日天1-7])")
    private val rangePattern = Regex("(\\d{1,2})\\s*[-~—至到]\\s*(\\d{1,2})\\s*节")
    private val singlePattern = Regex("第\\s*(\\d{1,2})\\s*节")

    fun parse(text: String?): Parsed {
        if (text.isNullOrBlank()) return Parsed()

        val day = dayPattern.find(text)?.groupValues?.getOrNull(1)?.let(::parseDay)

        var start: Int? = null
        var span: Int? = null
        val range = rangePattern.find(text)
        if (range != null) {
            val first = range.groupValues[1].toIntOrNull()
            val second = range.groupValues[2].toIntOrNull()
            if (first != null && second != null) {
                val low = minOf(first, second)
                val high = maxOf(first, second)
                start = low
                span = (high - low + 1).coerceAtLeast(1)
            }
        } else {
            val single = singlePattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (single != null) {
                start = single
                span = 1
            }
        }

        return Parsed(
            day = day,
            start = start?.takeIf { it in 1..GridSpec.PERIODS },
            span = span?.takeIf { it in 1..GridSpec.PERIODS },
        )
    }

    private fun parseDay(token: String): Int? = when (token) {
        "一" -> 0
        "二" -> 1
        "三" -> 2
        "四" -> 3
        "五" -> 4
        "六" -> 5
        "日", "天" -> 6
        else -> token.toIntOrNull()?.minus(1)?.takeIf { it in 0 until GridSpec.DAYS }
    }
}
