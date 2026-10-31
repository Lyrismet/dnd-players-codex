package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private val BarShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/**
 * the main action of a screen or sheet on a rounded backdrop with a soft shadow, glued to the bottom edge. It lifts
 * above the keyboard and the gesture bar by itself (an inset an outer container already took is not taken twice).
 * Put it after the scrolling content in a Column, or in a Box aligned to the bottom, so no content sits under it.
 */
@Composable
fun PinnedActionBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = BarShape)
                .background(AppPalette.Surface, BarShape)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
    ) {
        content()
    }
}
