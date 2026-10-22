package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

// lets a scrolling row run to the edges of a padded parent while its first and last item still sit at the padding
fun Modifier.bleedHorizontally(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val bleedPx = amount.roundToPx()
        val placeable =
            measurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth + bleedPx * 2))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-bleedPx, 0) }
    }
