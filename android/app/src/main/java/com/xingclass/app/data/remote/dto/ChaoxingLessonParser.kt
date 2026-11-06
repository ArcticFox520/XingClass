package com.xingclass.app.data.remote.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Tolerant Chaoxing lesson payload parser.
 *
 * The documentation only shows a partial example (`data: [{ id, name, teacher, time, location,
 * status }]`), so the parser walks the JSON tree looking for a lesson array and accepts common
 * field aliases. It never invents data: when the day or start period cannot be determined the
 * entry is dropped by [com.xingclass.app.data.mapper.CourseMapper].
 *
 * This is intentionally the only place in the app that knows about Chaoxing field names.
 */
object ChaoxingLessonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val preferredContainerKeys = listOf(
        // Verified live on 2026-11-06: the real payload nests lessons in data.lessonArray.
        "lessonArray", "lessons", "lessonList", "data", "list", "rows", "result", "records",
        "courseList",
    )

    private val titleKeys = arrayOf(
        "name", "title", "courseName", "lessonName", "curriculumName", "courseTitle",
    )
    private val teacherKeys = arrayOf(
        "teacher", "teacherName", "teacherNames", "teachers", "teacher_name",
    )
    private val locationKeys = arrayOf(
        "location", "place", "classroom", "room", "address", "addr", "classroomName",
    )
    private val idKeys = arrayOf(
        // lessonId identifies one occurrence; courseId identifies the course and repeats
        // across the week, so it must come last.
        "lessonId", "lesson_id", "curriculumId", "curriculum_id", "id", "courseId",
    )
    private val timeTextKeys = arrayOf(
        "time", "timeText", "sectionText", "courseTime", "lessonTime", "classTime", "timeStr",
    )
    private val oneBasedDayKeys = arrayOf(
        "weekDay", "weekday", "dayOfWeek", "week_day", "xq", "weekdayNum", "week",
    )
    private val zeroBasedDayKeys = arrayOf(
        "day", "dayIndex", "dayOfWeekIndex", "weekIndex",
    )
    private val startKeys = arrayOf(
        // "beginNumber" is the field used by the verified live lessonArray payload.
        "beginNumber", "start", "startSection", "sectionStart", "nodeStart", "beginSection",
        "startNode", "startSectionNum", "sectionStartNum", "startNumber",
    )
    private val endKeys = arrayOf(
        "endNumber", "end", "endSection", "sectionEnd", "nodeEnd", "endNode", "endSectionNum",
    )
    private val spanKeys = arrayOf(
        "span", "sectionSpan", "nodeNum", "sectionCount", "step", "length", "duration", "periods",
    )
    private val nestedSectionKeys = listOf(
        "section", "sections", "node", "period", "sectionInfo", "timeInfo",
    )

    fun parse(raw: String): LessonPayload {
        if (raw.isBlank()) return LessonPayload.NoLessonContainer
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull()
            ?: return LessonPayload.InvalidPayload
        val array = findLessonArray(root, depth = 0)
            ?: return if (root is JsonPrimitive) {
                // A bare string/number is not a lesson payload (lenient Json would otherwise
                // accept "not-json" as a JSON string).
                LessonPayload.InvalidPayload
            } else {
                LessonPayload.NoLessonContainer
            }
        val lessons = array.mapNotNull { element ->
            (element as? JsonObject)?.let(::toLesson)
        }.filter { !it.title.isNullOrBlank() }
        return LessonPayload.Lessons(lessons)
    }

    private fun findLessonArray(element: JsonElement, depth: Int): JsonArray? {
        if (depth > 6) return null
        return when (element) {
            is JsonArray -> element

            is JsonPrimitive -> if (element.isString) {
                parseNested(element.content)?.let { findLessonArray(it, depth + 1) }
            } else {
                null
            }

            is JsonObject -> {
                for (key in preferredContainerKeys) {
                    val child = element[key] ?: continue
                    val found = resolveContainer(child, depth)
                    if (found != null) return found
                }
                // Schema drift fallback: accept the first NON-EMPTY array that looks like lessons.
                // Empty arrays from unrelated fields (for example data.schools == []) must not
                // be mistaken for an empty timetable.
                for (child in element.values) {
                    val candidate = resolveContainer(child, depth) ?: continue
                    if (candidate.isNotEmpty() && looksLikeLessonArray(candidate)) return candidate
                }
                null
            }
        }
    }

    private fun resolveContainer(element: JsonElement, depth: Int): JsonArray? = when (element) {
        is JsonArray -> element
        is JsonPrimitive -> if (element.isString) {
            parseNested(element.content)?.let { findLessonArray(it, depth + 1) }
        } else {
            null
        }

        is JsonObject -> findLessonArray(element, depth + 1)
        else -> null
    }

    private fun looksLikeLessonArray(array: JsonArray): Boolean {
        val firstObject = array.firstOrNull() as? JsonObject ?: return false
        return firstObject.hasAnyKey(titleKeys) ||
            (firstObject.hasAnyKey(startKeys) && firstObject.hasAnyKey(zeroBasedDayKeys + oneBasedDayKeys))
    }

    private fun parseNested(text: String): JsonElement? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{" ) && !trimmed.startsWith("[")) return null
        return runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
    }

    private fun toLesson(obj: JsonObject): ChaoxingLessonDto {
        val sectionObject = nestedSectionKeys.firstNotNullOfOrNull { key -> obj[key] as? JsonObject }
        val sectionNumbers = (obj["sections"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }
            ?.sorted()
            .orEmpty()

        val start = obj.intOf(*startKeys)
            ?: sectionObject?.intOf(*startKeys)
            ?: sectionNumbers.firstOrNull()
        val end = obj.intOf(*endKeys)
            ?: sectionObject?.intOf(*endKeys)
            ?: sectionNumbers.lastOrNull()
        val span = obj.intOf(*spanKeys)?.takeIf { it > 0 }
            ?: sectionObject?.intOf(*spanKeys)?.takeIf { it > 0 }
            ?: sectionNumbers.size.takeIf { it > 0 }

        val dayIndex = obj.intOf(*oneBasedDayKeys)?.minus(1)
            ?: obj.intOf(*zeroBasedDayKeys)?.let { if (it == 7) 6 else it }

        return ChaoxingLessonDto(
            id = obj.textOf(*idKeys),
            title = obj.textOf(*titleKeys),
            teacher = obj.textOf(*teacherKeys) ?: obj.arrayTextOf(*teacherKeys),
            location = obj.textOf(*locationKeys)
                ?: obj.objectTextOf("locationInfo", "classroom", "place"),
            dayIndex = dayIndex?.takeIf { it in 0..6 },
            start = start,
            end = end,
            span = span,
            timeText = obj.textOf(*timeTextKeys),
        )
    }

    private fun JsonObject.hasAnyKey(keys: Array<String>): Boolean = keys.any { this[it] != null }

    private fun JsonObject.textOf(vararg keys: String): String? {
        for (key in keys) {
            val text = (this[key] as? JsonPrimitive)?.content?.trim()
            if (!text.isNullOrEmpty()) return text
        }
        return null
    }

    private fun JsonObject.intOf(vararg keys: String): Int? {
        for (key in keys) {
            val raw = (this[key] as? JsonPrimitive)?.content?.trim() ?: continue
            raw.toIntOrNull()?.let { return it }
        }
        return null
    }

    private fun JsonObject.arrayTextOf(vararg keys: String): String? {
        for (key in keys) {
            val array = this[key] as? JsonArray ?: continue
            val names = array.mapNotNull { item ->
                when (item) {
                    is JsonPrimitive -> item.content.trim().takeIf { it.isNotEmpty() }
                    is JsonObject -> item.textOf("name", "teacherName", "teacher", "title")
                    else -> null
                }
            }
            if (names.isNotEmpty()) return names.joinToString("、")
        }
        return null
    }

    private fun JsonObject.objectTextOf(vararg keys: String): String? {
        for (key in keys) {
            val value = this[key]
            when (value) {
                is JsonPrimitive -> {
                    val text = value.content.trim()
                    if (text.isNotEmpty()) return text
                }

                is JsonObject -> {
                    value.textOf("name", "title", "address", "place", "location")?.let { return it }
                }

                else -> Unit
            }
        }
        return null
    }
}
