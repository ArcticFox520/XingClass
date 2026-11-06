package com.xingclass.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xingclass.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Week math and localized date labels. Weeks start on Monday. */
object DateLabels {
    fun weekStart(today: LocalDate): LocalDate =
        today.minusDays((today.dayOfWeek.value - 1).toLong())

    fun weekDates(today: LocalDate): List<LocalDate> {
        val monday = weekStart(today)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun weekdayShortRes(dayIndex: Int): Int = when (dayIndex.coerceIn(0, 6)) {
        0 -> R.string.weekday_short_1
        1 -> R.string.weekday_short_2
        2 -> R.string.weekday_short_3
        3 -> R.string.weekday_short_4
        4 -> R.string.weekday_short_5
        5 -> R.string.weekday_short_6
        else -> R.string.weekday_short_7
    }

    fun weekdayFullRes(dayIndex: Int): Int = when (dayIndex.coerceIn(0, 6)) {
        0 -> R.string.weekday_full_1
        1 -> R.string.weekday_full_2
        2 -> R.string.weekday_full_3
        3 -> R.string.weekday_full_4
        4 -> R.string.weekday_full_5
        5 -> R.string.weekday_full_6
        else -> R.string.weekday_full_7
    }
}

@Composable
fun weekdayShort(dayIndex: Int): String = stringResource(DateLabels.weekdayShortRes(dayIndex))

@Composable
fun weekdayFull(dayIndex: Int): String = stringResource(DateLabels.weekdayFullRes(dayIndex))

/** "11月6日 · 周五" */
@Composable
fun dateLabel(date: LocalDate): String {
    val weekday = stringResource(DateLabels.weekdayFullRes(date.dayOfWeek.value - 1))
    return stringResource(R.string.date_format, date.monthValue, date.dayOfMonth, weekday)
}

/** "今天 10:32" or "11月5日 10:32" */
@Composable
fun syncTimeLabel(epochMillis: Long, today: LocalDate): String {
    val dateTime = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    return if (dateTime.toLocalDate() == today) {
        stringResource(R.string.sync_time_today, time)
    } else {
        stringResource(
            R.string.sync_time_other,
            dateTime.monthValue,
            dateTime.dayOfMonth,
            time,
        )
    }
}

