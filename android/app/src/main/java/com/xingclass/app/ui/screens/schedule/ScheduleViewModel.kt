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
                _state.update {
                    it.copy(
                        courses = cached.courses,
                        lastSyncedAtMillis = cached.savedAtMillis,
                        loading = false,
                        fromCache = true,
                    )
                }
            }
            refresh(manual = false)
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

    fun refresh(manual: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch { performRefresh(manual) }
    }

    private suspend fun performRefresh(manual: Boolean) {
        _state.update {
            it.copy(
                refreshing = true,
                error = null,
                message = null,
            )
        }

        when (val result = courseRepository.refreshCourses()) {
            is AppResult.Success -> {
                val snapshot = result.value
                _state.update {
                    it.copy(
                        courses = snapshot.courses,
                        lastSyncedAtMillis = snapshot.savedAtMillis,
                        loading = false,
                        refreshing = false,
                        fromCache = false,
                        offline = false,
                        error = null,
                        message = if (manual) {
                            UiText.res(R.string.schedule_refresh_success)
                        } else {
                            null
                        },
                    )
                }
            }

            is AppResult.Failure -> {
                if (result.error == AppError.SessionExpired) {
                    _state.update {
                        it.copy(
                            refreshing = false,
                            loading = false,
                            sessionExpired = true,
                        )
                    }
                    return
                }

                val networkError = result.error == AppError.Network
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
