package com.xingclass.app.domain.model

import kotlinx.serialization.Serializable

/** Visual bucket for a course. Actual colours live in the UI design tokens. */
@Serializable
enum class CourseColor {
    LAVENDER,
    MINT,
    PEACH,
    SKY,
    SAND,
    ROSE,
    TEAL,
    OLIVE,
}
