package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette

/** background/border are callbacks, not a flag, since the status picker tints each item by its own status color */
@Composable
fun <T> SegmentedControl(
    items: List<T>,
    onSelected: (T) -> Unit,
    itemBackground: (T) -> Color,
    modifier: Modifier = Modifier,
    containerShape: Shape = RoundedCornerShape(12.dp),
    containerBackground: Color = AppPalette.Surface,
    containerBorder: Color = AppPalette.BorderSubtle,
    itemShape: Shape = RoundedCornerShape(9.dp),
    itemHeight: Dp = 34.dp,
    itemSpacing: Dp = 3.dp,
    itemBorder: ((T) -> Color)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(containerBackground)
                .border(1.dp, containerBorder, containerShape)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items.forEach { item ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(itemHeight)
                        .clip(itemShape)
                        .background(itemBackground(item))
                        .then(
                            itemBorder?.let { border -> Modifier.border(1.dp, border(item), itemShape) }
                                ?: Modifier,
                        ).clickable { onSelected(item) },
                contentAlignment = Alignment.Center,
            ) {
                itemContent(item)
            }
        }
    }
}
