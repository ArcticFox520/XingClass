package com.xingclass.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingclass.app.R
import com.xingclass.app.core.AppClock
import com.xingclass.app.core.AppError
import com.xingclass.app.core.AppResult
import com.xingclass.app.domain.model.CourseCacheSnapshot
import com.xingclass.app.domain.repository.CourseRepository
import com.xingclass.app.ui.schedule.ScheduleUiState
import com.xingclass.app.ui.util.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val courseRepository: CourseRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScheduleUiState(
            todayIndex = clock.now().dayOfWeek.value - 1,
            todayDate = clock.now().toLocalDate(),
        ),
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            val cached = runCatching { courseRepository.getCachedCourses() }.getOrNull()
            if (cached != null) {
                applySnapshot(cached, fromCache = true)
            }
            // Online startup always opens the real current week. The cached week is only a
            // fallback until this request succeeds (or permanently when offline).
            launchRefresh(targetWeek = null, manual = false, switching = false)
        }
        viewModelScope.launch {
            while (true) {
                delay(DATE_TICK_MS)
                val today = clock.now().toLocalDate()
                if (today != _state.value.todayDate) {
                    _state.update {
                        it.copy(todayDate = today, todayIndex = today.dayOfWeek.value - 1)
                    }
                }
            }
        }
    }

    /** Refreshes the week currently on screen. */
    fun refresh(manual: Boolean = false) {
        launchRefresh(targetWeek = _state.value.week, manual = manual, switching = false)
    }

    /** Switches to another teaching week and fetches it. */
    fun selectWeek(week: Int) {
        val current = _state.value
        if (current.refreshing || current.switchingWeek) return
        val maxWeek = current.maxWeek ?: return
        val target = week.coerceIn(1, maxWeek)
        if (target == current.week) return
        launchRefresh(targetWeek = target, manual = false, switching = true)
    }

    /** Jumps back to the server's real current week (fetched without a week parameter). */
    fun goToCurrentWeek() {
        val current = _state.value
        if (current.refreshing || current.switchingWeek || current.isCurrentWeek) return
        launchRefresh(targetWeek = null, manual = false, switching = true)
    }

    fun toggleDay(day: Int) {
        _state.update {
            it.copy(selectedDay = if (it.selectedDay == day) null else day.coerceIn(0, 6))
        }
    }

    fun openDay(day: Int) {
        _state.update { it.copy(selectedDay = day.coerceIn(0, 6)) }
    }

    fun closeDay() {
        _state.update { it.copy(selectedDay = null) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun launchRefresh(targetWeek: Int?, manual: Boolean, switching: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            performRefresh(targetWeek = targetWeek, manual = manual, switching = switching)
        }
    }

    private suspend fun performRefresh(targetWeek: Int?, manual: Boolean, switching: Boolean) {
        _state.update {
            it.copy(
                refreshing = true,
                switchingWeek = switching,
                error = null,
                message = null,
                selectedDay = if (switching) null else it.selectedDay,
            )
        }

        when (val result = courseRepository.refreshCourses(targetWeek)) {
            is AppResult.Success -> {
                applySnapshot(result.value, fromCache = false)
                _state.update {
                    it.copy(
                        refreshing = false,
                        switchingWeek = false,
                        offline = false,
                        error = null,
                        message = if (manual) UiText.res(R.string.schedule_refresh_success) else null,
                    )
                }
            }

            is AppResult.Failure -> {
                if (result.error == AppError.SessionExpired) {
                    _state.update {
                        it.copy(
                            refreshing = false,
                            switchingWeek = false,
                            loading = false,
                            sessionExpired = true,
                        )
                    }
                    return
                }

                val networkError = result.error == AppError.Network
                if (switching) {
                    // Keep showing the previous week; only report that the switch failed.
                    _state.update {
                        it.copy(
                            refreshing = false,
                            switchingWeek = false,
                            offline = networkError,
                            message = if (networkError) {
                                UiText.res(R.string.schedule_switch_week_offline)
                            } else {
                                UiText.res(R.string.schedule_switch_week_failed)
                            },
                        )
                    }
                    return
                }

                _state.update { current ->
                    val keepData = current.courses.isNotEmpty()
                    current.copy(
                        refreshing = false,
                        loading = false,
                        offline = networkError,
                        error = if (keepData) null else result.error,
                        fromCache = keepData,
                        message = when {
                            keepData && networkError -> UiText.res(R.string.schedule_offline_hint)
                            manual && keepData -> UiText.res(R.string.schedule_refresh_failed_kept_cache)
                            else -> null
                        },
                    )
                }
            }
        }
    }

    private fun applySnapshot(snapshot: CourseCacheSnapshot, fromCache: Boolean) {
        _state.update {
            it.copy(
                courses = snapshot.courses,
                lastSyncedAtMillis = snapshot.savedAtMillis,
                week = snapshot.week,
                realCurrentWeek = snapshot.realCurrentWeek,
                maxWeek = snapshot.maxWeek,
                loading = false,
                fromCache = fromCache,
            )
        }
    }

    companion object {
        private const val DATE_TICK_MS = 60_000L

        fun factory(
            courseRepository: CourseRepository,
            clock: AppClock,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScheduleViewModel(courseRepository, clock) }
        }
    }
}
