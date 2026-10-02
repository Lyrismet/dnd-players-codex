package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** the surface-card clip/tint/border/click chrome - callers still own their own layout and padding */
fun Modifier.appCard(
    shape: Shape = RoundedCornerShape(14.dp),
    background: Color = AppPalette.Surface,
    border: Color? = AppPalette.BorderSubtle,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
): Modifier =
    clip(shape)
        .background(background)
        .then(if (border != null) Modifier.border(borderWidth, border, shape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
