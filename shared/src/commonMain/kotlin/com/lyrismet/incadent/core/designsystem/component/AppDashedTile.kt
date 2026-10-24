package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the tappable dashed tile of an "add one more" or "create" action - the caller fills it with its glyph and text */
@Composable
fun AppDashedTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppPalette.BorderHover,
    cornerRadius: Dp = 12.dp,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .dashedBorder(color, cornerRadius = cornerRadius)
                .clickable(onClick = onClick)
                .defaultMinSize(minHeight = minHeight)
                .padding(contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
