package com.lyrismet.dndcodex.core.designsystem.component.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val SECOND_LINE_WIDTH_FRACTION = 0.6f

/** hand-drawn "document with 2 lines" glyph - matches players codex v4.dc.html's sessions tab icon exactly */
@Composable
fun SessionsTabIcon(tint: Color) {
    TabIconSlot {
        Box(
            modifier =
                Modifier
                    .width(15.dp)
                    .height(18.dp)
                    .border(
                        width = TAB_ICON_STROKE_WIDTH,
                        color = tint,
                        shape =
                            RoundedCornerShape(
                                topStart = 2.dp,
                                topEnd = 4.dp,
                                bottomEnd = 4.dp,
                                bottomStart = 2.dp,
                            ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(1.5.dp).background(tint))
                Box(Modifier.fillMaxWidth(SECOND_LINE_WIDTH_FRACTION).height(1.5.dp).background(tint))
            }
        }
    }
}
