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
    val fromCache: Boolean = false,
    val offline: Boolean = false,
    val error: AppError? = null,
    val lastSyncedAtMillis: Long? = null,
    val selectedDay: Int? = null,
    val todayIndex: Int = 0,
    val todayDate: LocalDate = LocalDate.now(),
    val message: UiText? = null,
    val sessionExpired: Boolean = false,
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
}
