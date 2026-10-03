package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * two-column grid (`grid-template-columns: auto 1fr` in the design) - children alternate label, value, the label
 * column is as wide as the longest label and each row is top-aligned, so values line up under each other.
 */
@Composable
fun LabelValueGrid(
    modifier: Modifier = Modifier,
    rowSpacing: Dp = 10.dp,
    columnSpacing: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val rowSpacingPx = rowSpacing.roundToPx()
        val columnSpacingPx = columnSpacing.roundToPx()
        val pairs = measurables.chunked(2)
        // capped at half the row so an unusually long label can't starve the value column to zero width
        val labelMaxWidth = ((constraints.maxWidth - columnSpacingPx).coerceAtLeast(0)) / 2
        val labels = pairs.map { it.first().measure(Constraints(maxWidth = labelMaxWidth)) }
        val labelWidth = labels.maxOfOrNull { it.width } ?: 0
        val valueMaxWidth = (constraints.maxWidth - labelWidth - columnSpacingPx).coerceAtLeast(0)
        val valueConstraints = Constraints(maxWidth = valueMaxWidth)
        val values = pairs.map { it.getOrNull(1)?.measure(valueConstraints) }
        val rowHeights = labels.indices.map { max(labels[it].height, values[it]?.height ?: 0) }
        val height = rowHeights.sum() + rowSpacingPx * (rowHeights.size - 1).coerceAtLeast(0)
        layout(constraints.maxWidth, height) {
            var y = 0
            rowHeights.forEachIndexed { index, rowHeight ->
                labels[index].placeRelative(0, y)
                values[index]?.placeRelative(labelWidth + columnSpacingPx, y)
                y += rowHeight + rowSpacingPx
            }
        }
    }
}
