package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val SWEEP_DURATION_MILLIS = 600
private const val SWEEP_BAND_HEIGHT_DP = 90f
private const val SWEEP_TRAVEL_MARGIN_DP = 60f
private const val PARTICLE_COUNT = 14
private const val PARTICLE_LIFE_FRACTION = 0.28f
private const val PARTICLE_RISE_DP = 30f
private const val PARTICLE_SPAN_DP = 260f
private const val PARTICLE_MIN_RADIUS_DP = 1.5f
private const val PARTICLE_RADIUS_JITTER_DP = 2f
private const val PARTICLE_SEED = 1206

private val FlameColors =
    listOf(Color(0xFFFFF3B0), Color(0xFFFFB347), Color(0xFFFF6A3D), Color(0xFFE4572E))

/**
 * recolors [content] to flame tones in a band that sweeps bottom-to-top once [play] turns true, tracing
 * [content]'s own silhouette (it's a color mask over the existing pixels, not a separate flame shape) via
 * a `SrcIn`-blended overlay, shedding a few ember particles along the band's leading edge as it goes.
 * [content] is composed twice (once plain, once inside the tinted/clipped overlay) so both line up exactly.
 */
@Composable
fun FlameSweepEffect(
    play: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var started by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (play && !started) {
            started = true
            progress.animateTo(1f, tween(SWEEP_DURATION_MILLIS, easing = LinearEasing))
        }
    }

    Box(modifier = modifier) {
        content()
        if (started) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            val bandHeight = SWEEP_BAND_HEIGHT_DP.dp.toPx()
                            val margin = SWEEP_TRAVEL_MARGIN_DP.dp.toPx()
                            val travel = size.height + bandHeight + margin * 2
                            val bandBottom = size.height + margin - progress.value * travel
                            val bandTop = bandBottom - bandHeight
                            clipRect(top = bandTop, bottom = bandBottom) {
                                this@drawWithContent.drawContent()
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = FlameColors,
                                            startY = bandBottom,
                                            endY = bandTop,
                                        ),
                                    blendMode = BlendMode.SrcIn,
                                )
                            }
                            drawEmberParticles(progress.value, bandBottom)
                        },
            ) {
                content()
            }
        }
    }
}

private fun DrawScope.drawEmberParticles(
    progress: Float,
    bandLeadingEdgeY: Float,
) {
    val spanPx = PARTICLE_SPAN_DP.dp.toPx()
    val risePx = PARTICLE_RISE_DP.dp.toPx()
    val centerX = size.width / 2f
    val random = Random(PARTICLE_SEED)
    val particles =
        List(PARTICLE_COUNT) {
            Triple(random.nextFloat(), random.nextFloat(), random.nextInt(FlameColors.size))
        }
    particles.forEachIndexed { i, (xSeed, radiusSeed, colorIndex) ->
        val spawnAt = i / PARTICLE_COUNT.toFloat()
        val localT = (progress - spawnAt) / PARTICLE_LIFE_FRACTION
        if (localT in 0f..1f) {
            val xJitter = (xSeed - 0.5f) * spanPx
            val radius = PARTICLE_MIN_RADIUS_DP.dp.toPx() + radiusSeed * PARTICLE_RADIUS_JITTER_DP.dp.toPx()
            drawCircle(
                color = FlameColors[colorIndex].copy(alpha = 1f - localT),
                radius = radius,
                center = Offset(centerX + xJitter, bandLeadingEdgeY - localT * risePx),
            )
        }
    }
}
