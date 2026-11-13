package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val GLOW_RINGS = 4
private const val GLOW_RING_ALPHA = 0.06f

/** the colours of the squares: lavender for spell slots, gold for the uses of a resource */
enum class CellTone(
    internal val lit: Color,
    internal val off: Color,
    internal val offBorder: Color,
) {
    Arcane(AppPalette.ArcaneCell, AppPalette.ArcaneCellOff, AppPalette.ArcaneCellBorder),
    Gold(AppPalette.GoldBright, AppPalette.GoldCellOff, AppPalette.GoldCellBorder),
}

/** [Small] is only drawn (a level tile), [Medium] is a line of a resource, [Big] is the cells of an open level */
enum class CellSize(
    internal val side: Dp,
    internal val radius: Dp,
    internal val glow: Dp,
    internal val tapWidth: Dp,
    internal val tapHeight: Dp,
    internal val gap: Dp,
) {
    Small(13.dp, 3.dp, 4.dp, 13.dp, 13.dp, 4.dp),
    Medium(16.dp, 4.dp, 5.dp, 30.dp, 44.dp, 0.dp),
    Big(30.dp, 7.dp, 9.dp, 52.dp, 48.dp, 4.dp),
}

/** [total] squares in a row, the first [filled] lit, with [onPress] each gives its index and a hold calls [onHold] */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppCells(
    total: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    size: CellSize = CellSize.Small,
    tone: CellTone = CellTone.Arcane,
    onPress: ((Int) -> Unit)? = null,
    onHold: (() -> Unit)? = null,
    // the motion of a free cell, a slot level and the shared clock, none of it by default
    effect: CellEffect = CellEffect.None,
    clock: State<Float>? = null,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(size.gap)) {
        repeat(total) { index ->
            if (onPress == null) {
                AppCell(index < filled, size, tone, effect, clock)
            } else {
                Box(
                    Modifier
                        .size(size.tapWidth, size.tapHeight)
                        .combinedClickable(onClick = { onPress(index) }, onLongClick = onHold),
                    contentAlignment = Alignment.Center,
                ) { AppCell(index < filled, size, tone, effect, clock) }
            }
        }
    }
}

@Composable
private fun AppCell(
    lit: Boolean,
    size: CellSize,
    tone: CellTone,
    effect: CellEffect,
    clock: State<Float>?,
) {
    val shape = remember(size) { RoundedCornerShape(size.radius) }
    Box(
        modifier =
            Modifier
                .size(size.side)
                .then(if (lit) Modifier.cellEffect(effect, clock, tone.lit) else Modifier)
                .then(if (lit) Modifier.cellGlow(tone.lit, size.glow, size.radius) else Modifier)
                .clip(shape)
                .background(if (lit) tone.lit else tone.off)
                .then(if (lit) Modifier else Modifier.border(1.5.dp, tone.offBorder, shape)),
    )
}

// a soft halo from a few growing rings, Compose has no box shadow with a colour and a blur on every platform
private fun Modifier.cellGlow(
    color: Color,
    reach: Dp,
    radius: Dp,
): Modifier =
    drawBehind {
        val grow = reach.toPx()
        for (ring in GLOW_RINGS downTo 1) {
            val extra = grow * ring / GLOW_RINGS
            drawRoundRect(
                color = color.copy(alpha = GLOW_RING_ALPHA),
                topLeft = Offset(-extra, -extra),
                size = Size(this.size.width + extra * 2, this.size.height + extra * 2),
                cornerRadius = CornerRadius(radius.toPx() + extra),
            )
        }
    }
