package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppShapes

/** a tappable list row that opens with a tick box - the row owns the tap, [mark] swaps the box for a look of its own */
@Composable
fun AppCheckRow(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 56.dp,
    verticalPadding: Dp = 6.dp,
    gap: Dp = 12.dp,
    mark: @Composable () -> Unit = { AppCheckBox(checked) },
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Control, Color.Transparent, null, onClick = onClick)
                .defaultMinSize(minHeight = minHeight)
                .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        mark()
        content()
    }
}
