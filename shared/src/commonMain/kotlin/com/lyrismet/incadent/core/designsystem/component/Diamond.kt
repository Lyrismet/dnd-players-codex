package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the turn that makes a square a diamond - every ornament and glyph of the design is built from it */
const val DIAMOND_ROTATION_DEGREES = 45f

/** a rotated square drawn as an outline, [size] is the side of the square before the turn */
@Composable
fun DiamondOutline(
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.Gold,
    strokeWidth: Dp = 1.dp,
) {
    Box(modifier.size(size).rotate(DIAMOND_ROTATION_DEGREES).border(strokeWidth, color))
}

/** a rotated square filled with [color], [size] is the side of the square before the turn */
@Composable
fun DiamondFill(
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.Gold,
) {
    Box(modifier.size(size).rotate(DIAMOND_ROTATION_DEGREES).background(color))
}
