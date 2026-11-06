package com.xingclass.app.ui.util

/**
 * Building / room split for a Chaoxing location string.
 *
 * Real examples: "示例楼C201" -> building "示例楼", room "C201";
 * "综合楼A-101" -> "综合楼" + "A-101"; "线上" -> "线上" + null.
 * The parser is deliberately heuristic and always keeps [raw] as a fallback, so an unexpected
 * format is still displayed (just not split).
 */
data class LocationParts(
    val building: String?,
    val room: String?,
    val raw: String?,
) {
    val hasSplit: Boolean get() = building != null && room != null
    val hasLocation: Boolean get() = building != null || room != null
}

private val roomPattern = Regex(
    "^(.*?)[\\s·]?([A-Za-z]{0,2}-?\\d{1,5}(?:-\\d{1,3})?[A-Za-z]?)$",
)

fun parseLocation(raw: String?): LocationParts {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return LocationParts(null, null, null)

    val match = roomPattern.find(trimmed)
    if (match != null) {
        val building = match.groupValues[1]
            .trim()
            .trimEnd(' ', '-', '·', ',', '，')
            .ifEmpty { null }
        val room = match.groupValues[2]
            .trim()
            .trimStart('-')
            .ifEmpty { null }
        if (room != null) {
            return LocationParts(building = building, room = room, raw = trimmed)
        }
    }
    return LocationParts(building = trimmed, room = null, raw = trimmed)
}
