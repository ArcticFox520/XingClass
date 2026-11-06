package com.xingclass.app.ui.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleUiStateTest {

    @Test
    fun `current week enables both arrows inside the range`() {
        val state = ScheduleUiState(
            week = 10,
            realCurrentWeek = 10,
            maxWeek = 22,
            todayIndex = 4,
        )
        assertTrue(state.isCurrentWeek)
        assertTrue(state.canGoPreviousWeek)
        assertTrue(state.canGoNextWeek)
        assertEquals(4, state.highlightedTodayIndex)
    }

    @Test
    fun `past week still can go back to the current week`() {
        val state = ScheduleUiState(
            week = 1,
            realCurrentWeek = 10,
            maxWeek = 22,
            todayIndex = 4,
        )
        assertFalse(state.isCurrentWeek)
        assertFalse(state.canGoPreviousWeek)
        assertTrue(state.canGoNextWeek)
        assertEquals(-1, state.highlightedTodayIndex)
    }

    @Test
    fun `last week disables next`() {
        val state = ScheduleUiState(week = 22, realCurrentWeek = 10, maxWeek = 22)
        assertFalse(state.canGoNextWeek)
        assertTrue(state.canGoPreviousWeek)
    }

    @Test
    fun `refreshing and switching disable the arrows`() {
        val refreshing = ScheduleUiState(week = 10, realCurrentWeek = 10, maxWeek = 22, refreshing = true)
        assertFalse(refreshing.canGoPreviousWeek)
        assertFalse(refreshing.canGoNextWeek)

        val switching = ScheduleUiState(
            week = 10,
            realCurrentWeek = 10,
            maxWeek = 22,
            switchingWeek = true,
        )
        assertFalse(switching.canGoPreviousWeek)
        assertFalse(switching.canGoNextWeek)
    }
}
