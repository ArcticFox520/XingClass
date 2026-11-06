package com.xingclass.app.ui.components.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingclass.app.R
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicIconButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicStyle
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing

/**
 * Compact teaching-week selector between the header and the weekday tabs.
 *
 * The week number shown is the *selected* week. The server's `currentWeek` remains the real
 * current week, so "回到本周" stays correct while browsing other weeks.
 */
@Composable
fun WeekSwitcher(
    week: Int?,
    isCurrentWeek: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeumorphicIconButton(
            onClick = onPrevious,
            icon = Icons.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.schedule_previous_week),
            enabled = enabled && canGoPrevious,
            size = 30.dp,
            iconSize = 18.dp,
        )
        Spacer(Modifier.width(XingSpacing.sm))

        NeumorphicSurface(
            style = NeumorphicStyle.INSET,
            shape = RoundedCornerShape(XingRadii.pill),
            elevation = NeumorphicElevation.SMALL,
            contentColor = colors.textPrimary,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (week != null) {
                    stringResource(R.string.schedule_week_label, week)
                } else {
                    stringResource(R.string.schedule_week_unknown)
                },
                style = XingClassType.label,
                color = if (enabled) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(XingSpacing.sm))
        NeumorphicIconButton(
            onClick = onNext,
            icon = Icons.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.schedule_next_week),
            enabled = enabled && canGoNext,
            size = 30.dp,
            iconSize = 18.dp,
        )

        if (week != null && !isCurrentWeek) {
            Spacer(Modifier.width(10.dp))
            NeumorphicButton(
                onClick = onCurrent,
                enabled = enabled,
                shape = RoundedCornerShape(XingRadii.pill),
                elevation = NeumorphicElevation.SMALL,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.schedule_week_back_to_current),
                    style = XingClassType.caption,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
