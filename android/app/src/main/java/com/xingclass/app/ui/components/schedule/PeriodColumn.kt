package com.xingclass.app.ui.components.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.theme.LocalNeumorphicColors
import com.xingclass.app.ui.theme.XingClassType
import com.xingclass.app.ui.theme.XingRadii

/**
 * Left hand period rail: ten independent raised blocks, one per period (see the three-layer
 * nesting rule in the UI guide). No scrolling: every block gets 1/10 of the available height.
 */
@Composable
fun PeriodColumn(
    modifier: Modifier = Modifier,
    periodCount: Int = 10,
) {
    val colors = LocalNeumorphicColors.current
    Column(modifier = modifier) {
        repeat(periodCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                NeumorphicSurface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(XingRadii.small),
                    elevation = NeumorphicElevation.SMALL,
                    contentColor = colors.textSecondary,
                    contentPadding = PaddingValues(0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = XingClassType.periodNumber,
                    )
                }
            }
        }
    }
}
