package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppShapes

/** the card of a notice over a list - a [tint] fill and outline around a row of content, 10dp apart */
@Composable
fun AppNoticeBar(
    tint: Color,
    modifier: Modifier = Modifier,
    fillAlpha: Float = 0.08f,
    borderAlpha: Float = 0.35f,
    endPadding: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = tint.copy(alpha = fillAlpha),
                    border = tint.copy(alpha = borderAlpha),
                ).padding(start = 14.dp, top = 12.dp, end = endPadding, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
