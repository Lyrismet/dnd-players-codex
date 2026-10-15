package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes

/** a segmented switch of text tabs where the selected one is raised and gold, the tabs share the width equally */
@Composable
fun <T> AppSegmentedTabs(
    tabs: List<SegmentedTab<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.labelLarge,
    containerShape: Shape = AppShapes.Field,
    containerBackground: Color = AppPalette.Surface,
    itemShape: Shape = AppShapes.Tile,
    itemHeight: Dp = 34.dp,
    itemSpacing: Dp = 3.dp,
) {
    SegmentedControl(
        items = tabs,
        onSelected = { onSelected(it.value) },
        itemBackground = { if (it.value == selected) AppPalette.SurfaceElevated else Color.Transparent },
        modifier = modifier,
        containerShape = containerShape,
        containerBackground = containerBackground,
        itemShape = itemShape,
        itemHeight = itemHeight,
        itemSpacing = itemSpacing,
    ) { tab ->
        val tint = if (tab.value == selected) AppPalette.GoldBright else AppPalette.TextSecondary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = tab.label, style = labelStyle, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tab.count != null) {
                Text(
                    text = tab.count.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = AppFontSize.TinyPlus),
                    color = tint.copy(alpha = 0.65f),
                )
            }
        }
    }
}
