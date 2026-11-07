package com.xingclass.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingclass.app.R
import com.xingclass.app.ui.components.neumorphic.NeumorphicElevation
import com.xingclass.app.ui.components.neumorphic.NeumorphicSurface
import com.xingclass.app.ui.theme.LocalNeumorphicColors

/** Circular brand mark used by the boot and login screens. */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    fontSize: TextUnit = 26.sp,
) {
    val colors = LocalNeumorphicColors.current
    NeumorphicSurface(
        modifier = modifier.size(size),
        shape = CircleShape,
        elevation = NeumorphicElevation.LARGE,
        contentColor = colors.accent,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.brand_letter),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
        )
    }
}
