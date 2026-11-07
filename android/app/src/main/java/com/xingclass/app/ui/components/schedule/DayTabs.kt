package com.xingclass.app.ui.components.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingclass.app.domain.model.GridSpec
import com.xingclass.app.ui.components.neumorphic.NeumorphicTab
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.weekdayShort
import java.time.LocalDate

/**
 * Weekday tabs above the grid. Tapping a tab opens the day drawer for that day; while the drawer
 * is open the corresponding tab is highlighted with an inset shadow.
 */
@Composable
fun DayTabs(
    selectedDay: Int?,
    todayIndex: Int,
    weekDates: List<LocalDate>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    Row(modifier = modifier) {
        repeat(GridSpec.DAYS) { day ->
            val selected = selectedDay == day
            NeumorphicTab(
                selected = selected,
                onClick = { onDayClick(day) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .padding(horizontal = 1.dp),
                contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = weekdayShort(day),
                        style = XingClassType.label,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = weekDates.getOrNull(day)?.dayOfMonth?.toString().orEmpty(),
                        style = XingClassType.micro,
                        color = if (selected) colors.accent else colors.textSecondary,
                        maxLines = 1,
                    )
                    if (day == todayIndex) {
                        Box(
                            Modifier
                                .padding(top = 1.dp)
                                .size(3.dp)
                                .background(colors.accent, CircleShape),
                        )
                    }
                }
            }
        }
    }
}
