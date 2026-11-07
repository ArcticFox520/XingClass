package com.xingclass.app.ui.components.schedule

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xingclass.app.R
import com.xingclass.app.domain.model.ThemeMode
import com.xingclass.app.ui.components.neumorphic.NeumorphicBadge
import com.xingclass.app.ui.components.neumorphic.NeumorphicButton
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicIconButton
import com.xingclass.app.ui.schedule.ScheduleStatus
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingControlSize
import com.xingclass.app.ui.theme.XingRadii
import com.xingclass.app.ui.theme.XingSpacing
import com.xingclass.app.ui.util.dateLabel
import com.xingclass.app.ui.util.themeModeLabel
import java.time.LocalDate

/**
 * Top bar: wordmark, date, sync status, theme shortcut, refresh and settings.
 * Nothing here scrolls or changes height, so the grid below always gets the same space.
 */
@Composable
fun ScheduleHeader(
    todayDate: LocalDate,
    status: ScheduleStatus,
    themeMode: ThemeMode,
    refreshing: Boolean,
    onCycleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalNeumorphicColors.current
    val transition = rememberInfiniteTransition(label = "refresh")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "refreshAngle",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = XingClassType.wordmark,
                color = colors.textPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(1.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(XingSpacing.sm),
            ) {
                Text(
                    text = dateLabel(todayDate),
                    style = XingClassType.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
                ScheduleStatusBadge(status)
            }
        }

        NeumorphicButton(
            onClick = onCycleTheme,
            shape = RoundedCornerShape(XingRadii.pill),
            elevation = NeumorphicElevation.SMALL,
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
        ) {
            Text(
                text = themeModeLabel(themeMode),
                style = XingClassType.caption,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(XingSpacing.sm))
        NeumorphicIconButton(
            onClick = onRefresh,
            icon = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.schedule_refresh),
            modifier = Modifier.rotate(if (refreshing) angle else 0f),
            size = XingControlSize.iconButton,
            iconSize = XingControlSize.iconGlyph,
        )
        Spacer(Modifier.width(XingSpacing.sm))
        NeumorphicIconButton(
            onClick = onOpenSettings,
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.schedule_settings),
            size = XingControlSize.iconButton,
            iconSize = XingControlSize.iconGlyph,
        )
    }
}

@Composable
private fun ScheduleStatusBadge(status: ScheduleStatus) {
    val colors = LocalNeumorphicColors.current
    val labelRes = when (status) {
        ScheduleStatus.LOADING -> R.string.schedule_status_loading
        ScheduleStatus.CACHED -> R.string.schedule_status_cached
        ScheduleStatus.REFRESHING -> R.string.schedule_status_refreshing
        ScheduleStatus.SUCCESS -> R.string.schedule_status_synced
        ScheduleStatus.EMPTY -> R.string.schedule_status_empty
        ScheduleStatus.ERROR -> R.string.schedule_status_error
        ScheduleStatus.OFFLINE -> R.string.schedule_status_offline
    }
    val contentColor = when (status) {
        ScheduleStatus.ERROR, ScheduleStatus.OFFLINE -> colors.danger
        else -> colors.textSecondary
    }
    NeumorphicBadge(
        text = stringResource(labelRes),
        contentColor = contentColor,
    )
}
