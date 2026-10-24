package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private val TrackWidth = 44.dp
private val TrackHeight = 26.dp
private val KnobSize = 20.dp
private val KnobInset = 3.dp

/** the on/off switch: a gold track with the knob on the right when on, a grey one with the knob on the left when off */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(if (checked) TrackWidth - KnobSize - KnobInset * 2 else 0.dp)
    Box(
        modifier =
            modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .size(TrackWidth, TrackHeight)
                .background(if (checked) AppPalette.Gold else AppPalette.BorderHover, CircleShape)
                .padding(KnobInset),
    ) {
        // read while placing, so the knob sliding does not recompose the switch every frame
        Box(
            Modifier
                .offset {
                    IntOffset(
                        knobOffset.roundToPx(),
                        0,
                    )
                }.size(KnobSize)
                .background(Color.White, CircleShape),
        )
    }
}
