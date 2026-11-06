package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val GLOW_MILLIS = 1100
private const val GLOW_REACH = 2.1f
private const val GLOW_ALPHA = 0.55f

/** the "recording" dot: a solid [color] core with a halo that swells and fades, for something that is live right now */
@Composable
fun AppLiveDot(
    modifier: Modifier = Modifier,
    color: Color = AppPalette.MaroonBright,
    size: Dp = 10.dp,
) {
    val pulse by
        rememberInfiniteTransition(label = "live-dot")
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        tween(GLOW_MILLIS, easing = FastOutSlowInEasing),
                        RepeatMode.Restart,
                    ),
                label = "live-dot-pulse",
            )
    Box(modifier = modifier.size(size * GLOW_REACH), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(1f + (GLOW_REACH - 1f) * pulse)
                .alpha(GLOW_ALPHA * (1f - pulse))
                .clip(CircleShape)
                .background(color),
        )
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}
