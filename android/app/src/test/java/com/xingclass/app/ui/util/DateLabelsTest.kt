package com.xingclass.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateLabelsTest {

    private val today = LocalDate.of(2026, 11, 6) // Friday of teaching week 10

    @Test
    fun `current week starts on Monday`() {
        val dates = DateLabels.weekDates(today)
        assertEquals(LocalDate.of(2026, 11, 2), dates.first())
        assertEquals(LocalDate.of(2026, 11, 8), dates.last())
        assertEquals(7, dates.size)
    }

    @Test
    fun `selected week dates follow the current week offset`() {
        val previous = DateLabels.weekDatesFor(
            selectedWeek = 9,
            realCurrentWeek = 10,
            today = today,
        )
        assertEquals(LocalDate.of(2026, 10, 26), previous.first())
        assertEquals(LocalDate.of(2026, 11, 1), previous.last())

        val next = DateLabels.weekDatesFor(
            selectedWeek = 11,
            realCurrentWeek = 10,
            today = today,
        )
        assertEquals(LocalDate.of(2026, 11, 9), next.first())
        assertEquals(LocalDate.of(2026, 11, 15), next.last())
    }

    @Test
    fun `missing week info falls back to the current week`() {
        val dates = DateLabels.weekDatesFor(selectedWeek = null, realCurrentWeek = null, today = today)
        assertEquals(LocalDate.of(2026, 11, 2), dates.first())
    }
}
