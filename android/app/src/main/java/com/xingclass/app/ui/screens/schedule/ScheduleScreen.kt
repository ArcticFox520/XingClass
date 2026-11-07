package com.xingclass.app.ui.screens.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingclass.app.domain.model.Course
import com.xingclass.app.domain.model.PeriodTimes
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.ui.components.neumorphic.NeumorphicMessageBar
import com.xingclass.app.ui.components.schedule.DayDrawer
import com.xingclass.app.ui.components.schedule.DayTabs
import com.xingclass.app.ui.components.schedule.PeriodColumn
import com.xingclass.app.ui.components.schedule.ScheduleGrid
import com.xingclass.app.ui.components.schedule.ScheduleHeader
import com.xingclass.app.ui.components.schedule.ScheduleMetrics
import com.xingclass.app.ui.components.schedule.WeekSwitcher
import com.xingclass.app.ui.schedule.ScheduleUiState
import com.xingclass.app.ui.util.DateLabels
import kotlinx.coroutines.delay

/**
 * The product's core screen: the complete week is visible at once, with no main scroll container.
 * The day drawer is an overlay on top of the grid.
 */
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onGoToCurrentWeek: () -> Unit,
    onDayTabClick: (Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    onCloseDay: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissMessage: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val periodTimes = remember(state.lessonTimes) { PeriodTimes.parse(state.lessonTimes) }
    val weekDates = remember(state.todayDate, state.week, state.realCurrentWeek) {
        DateLabels.weekDatesFor(
            selectedWeek = state.week,
            realCurrentWeek = state.realCurrentWeek,
            today = state.todayDate,
        )
    }

    // System back closes the drawer first instead of leaving the app.
    BackHandler(enabled = state.selectedDay != null, onBack = onCloseDay)
    var visibleMessage by remember { mutableStateOf(state.message) }

    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            visibleMessage = message
            delay(2800)
            onDismissMessage()
        } else {
            delay(320)
            visibleMessage = null
        }
    }

    val drawerCourses = remember(state.courses, state.selectedDay) {
        val day = state.selectedDay
        if (day == null) {
            emptyList()
        } else {
            state.courses
                .filter { it.day == day }
                .sortedWith(compareBy({ it.start }, { it.title }))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 2.dp, bottom = 8.dp),
        ) {
            ScheduleHeader(
                todayDate = state.todayDate,
                status = state.status,
                themeMode = themeMode,
                refreshing = state.refreshing,
                onCycleTheme = onCycleTheme,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(6.dp))
            WeekSwitcher(
                week = state.week,
                isCurrentWeek = state.isCurrentWeek,
                canGoPrevious = state.canGoPreviousWeek,
                canGoNext = state.canGoNextWeek,
                enabled = !state.refreshing && !state.switchingWeek,
                onPrevious = { state.week?.let { onSelectWeek(it - 1) } },
                onNext = { state.week?.let { onSelectWeek(it + 1) } },
                onCurrent = onGoToCurrentWeek,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(ScheduleMetrics.PeriodColumnWidth + ScheduleMetrics.Gap))
                DayTabs(
                    selectedDay = state.selectedDay,
                    todayIndex = state.highlightedTodayIndex,
                    weekDates = weekDates,
                    onDayClick = onDayTabClick,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                PeriodColumn(
                    modifier = Modifier
                        .width(ScheduleMetrics.PeriodColumnWidth)
                        .fillMaxHeight(),
                    times = periodTimes,
                )
                Spacer(Modifier.width(ScheduleMetrics.Gap))
                ScheduleGrid(
                    courses = state.courses,
                    status = state.status,
                    error = state.error,
                    todayIndex = state.highlightedTodayIndex,
                    onRetry = onRefresh,
                    onCourseClick = onCourseClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        NeumorphicMessageBar(
            visible = state.message != null,
            message = visibleMessage,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        )

        DayDrawer(
            visible = state.selectedDay != null,
            dayIndex = state.selectedDay ?: 0,
            date = state.selectedDay?.let { weekDates.getOrNull(it) } ?: state.todayDate,
            courses = drawerCourses,
            periodTimes = periodTimes,
            onDismiss = onCloseDay,
        )
    }
}
