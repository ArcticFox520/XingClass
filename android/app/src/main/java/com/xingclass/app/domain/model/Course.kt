package com.xingclass.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Unified internal course model. The UI never sees Chaoxing JSON.
 *
 * @param day 0 = Monday … 6 = Sunday
 * @param start 1-based period number
 * @param span number of consecutive periods the lesson occupies
 */
@Serializable
data class Course(
    val id: String,
    val day: Int,
    val start: Int,
    val span: Int,
    val title: String,
    val location: String? = null,
    val teacher: String? = null,
    val color: CourseColor = CourseColor.LAVENDER,
) {
    val endPeriod: Int get() = start + span - 1
}
