package com.xingclass.app.ui.schedule

import com.xingclass.app.core.AppError
import com.xingclass.app.domain.model.Course
import com.xingclass.app.ui.util.UiText
import java.time.LocalDate

/** Presentation status of the schedule screen. Derived, never stored twice. */
enum class ScheduleStatus {
    LOADING,
    CACHED,
    REFRESHING,
    SUCCESS,
    EMPTY,
    ERROR,
    OFFLINE,
}

data class ScheduleUiState(
    val courses: List<Course> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val switchingWeek: Boolean = false,
    val fromCache: Boolean = false,
    val offline: Boolean = false,
    val error: AppError? = null,
    val lastSyncedAtMillis: Long? = null,
    val selectedDay: Int? = null,
    val todayIndex: Int = 0,
    val todayDate: LocalDate = LocalDate.now(),
    val message: UiText? = null,
    val sessionExpired: Boolean = false,
    /** Week currently displayed (1-based). Null until the first successful load. */
    val week: Int? = null,
    /** The server's real current week; independent from [week] when browsing another week. */
    val realCurrentWeek: Int? = null,
    val maxWeek: Int? = null,
    val lessonTimes: List<String> = emptyList(),
) {
    val status: ScheduleStatus
        get() = when {
            loading -> ScheduleStatus.LOADING
            refreshing -> ScheduleStatus.REFRESHING
            error != null -> ScheduleStatus.ERROR
            offline -> ScheduleStatus.OFFLINE
            courses.isEmpty() -> ScheduleStatus.EMPTY
            fromCache -> ScheduleStatus.CACHED
            else -> ScheduleStatus.SUCCESS
        }

    val isCurrentWeek: Boolean
        get() = week != null && realCurrentWeek != null && week == realCurrentWeek

    val canGoPreviousWeek: Boolean
        get() = !refreshing && !switchingWeek && week != null && week > 1

    val canGoNextWeek: Boolean
        get() = !refreshing && !switchingWeek && week != null &&
            maxWeek != null && week < maxWeek

    /** Today's column is only meaningful while looking at the real current week. */
    val highlightedTodayIndex: Int
        get() = if (isCurrentWeek || week == null) todayIndex else -1
}
