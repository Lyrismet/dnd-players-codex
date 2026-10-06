package com.lyrismet.incadent.core.quickedit

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private const val SHIELD_REGIONS = 4

/**
 * the four rectangles that cover the sheet except [hole] - top, bottom, left and right of it.
 * Without a hole the first rectangle covers everything and the others are empty.
 */
fun editorShieldRegions(
    hole: Rect?,
    width: Float,
    height: Float,
): List<Rect> {
    val empty = Rect.Zero
    if (hole == null) return listOf(Rect(0f, 0f, width, height), empty, empty, empty)
    val left = hole.left.coerceIn(0f, width)
    val top = hole.top.coerceIn(0f, height)
    val right = hole.right.coerceIn(left, width)
    val bottom = hole.bottom.coerceIn(top, height)
    return listOf(
        Rect(0f, 0f, width, top),
        Rect(0f, bottom, width, height),
        Rect(0f, top, left, bottom),
        Rect(right, top, width, bottom),
    )
}

/**
 * the transparent layer over the whole sheet while an editor is open - it takes every touch except the ones inside
 * the open editor, and a tap on it reports [onOutsideTap] (the same path as "Отмена").
 */
@Composable
fun BoxScope.EditorShield(
    hole: Rect?,
    onOutsideTap: () -> Unit,
) {
    val currentOnTap by rememberUpdatedState(onOutsideTap)
    Layout(
        content = {
            repeat(SHIELD_REGIONS) {
                Box(Modifier.pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) })
            }
        },
        modifier = Modifier.matchParentSize(),
    ) { measurables, constraints ->
        val regions = editorShieldRegions(hole, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val placeables =
            measurables.mapIndexed { index, measurable ->
                val region = regions[index]
                measurable.measure(Constraints.fixed(region.width.roundToInt(), region.height.roundToInt()))
            }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(regions[index].left.roundToInt(), regions[index].top.roundToInt())
            }
        }
    }
}
