package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** the motion around a free slot cell, the higher the level the livelier, levels I-III stand still */
enum class CellEffect {
    None,
    Sparks,
    SparksPulse,
    Orbit,
    ;

    companion object {
        private const val SPARKS_FROM = 4
        private const val PULSE_FROM = 7
        private const val ORBIT_AT = 9

        fun forLevel(level: Int): CellEffect =
            when {
                level >= ORBIT_AT -> Orbit
                level >= PULSE_FROM -> SparksPulse
                level >= SPARKS_FROM -> Sparks
                else -> None
            }
    }
}

private const val FULL_TURN = (2 * PI).toFloat()
private const val SEED_STEP = 2.4f
private const val DRIFT = 0.45f
private const val SPARK_RADIUS = 0.09f
private const val ORBIT_RADIUS = 0.85f
private const val PULSE_BEATS = 2
private const val PULSE_STROKE = 0.08f
private const val PULSE_CORNER = 3f
private const val PULSE_BASE = 0.2f
private const val PULSE_SWING = 0.15f
private const val PULSE_GROWTH = 0.18f

/**
 * how one cell differs from its neighbours: [phase] shifts it along the shared clock, [turn] rotates where its sparks
 * leave from and [clockwise] sets which way an orbit goes. The same seed and index always give the same look
 */
class CellVariation(
    val phase: Float,
    val turn: Float,
    val clockwise: Boolean,
) {
    companion object {
        val None = CellVariation(0f, 0f, true)
    }
}

private const val HASH_MULTIPLIER = 2654435761L
private const val HASH_MASK = 0xFFFFL
private const val HASH_RANGE = 65536f
private const val SEED_SPREAD = 31
private const val INDEX_SPREAD = 7

/** a stable, scattered-looking variation for the cell at [index] of a group named by [seed], like a slot level */
fun cellVariation(
    seed: Int,
    index: Int,
): CellVariation {
    val hash = (seed * SEED_SPREAD + index * INDEX_SPREAD + 1) * HASH_MULTIPLIER
    val a = (hash shr 8) and HASH_MASK
    val b = (hash shr 24) and HASH_MASK
    val c = (hash shr 40) and 1L
    return CellVariation(a / HASH_RANGE, b / HASH_RANGE * FULL_TURN, c == 0L)
}

// how many sparks a cell has and how many times each one rises while the shared clock goes round once
private class Rhythm(
    val sparks: Int,
    val cycles: Int,
)

private val SparksRhythm = Rhythm(sparks = 2, cycles = 2)
private val PulseRhythm = Rhythm(sparks = 4, cycles = 4)
private val OrbitRhythm = Rhythm(sparks = 3, cycles = 1)

/**
 * draws the motion of [effect] around a lit cell from the shared [clock], a null clock (motion reduced, or the screen
 * is away) leaves the cell still. Reading the clock happens while drawing, so a lap never recomposes the cell
 */
fun Modifier.cellEffect(
    effect: CellEffect,
    clock: State<Float>?,
    color: Color,
    variation: CellVariation = CellVariation.None,
): Modifier =
    if (effect == CellEffect.None || clock == null) {
        this
    } else {
        drawWithContent {
            when (effect) {
                CellEffect.SparksPulse -> pulse(shifted(clock.value, variation), color)
                else -> Unit
            }
            drawContent()
            when (effect) {
                CellEffect.Sparks -> sparks(SparksRhythm, shifted(clock.value, variation), color, variation.turn)
                CellEffect.SparksPulse -> sparks(PulseRhythm, shifted(clock.value, variation), color, variation.turn)
                CellEffect.Orbit -> orbit(OrbitRhythm, shifted(clock.value, variation), color, variation)
                CellEffect.None -> Unit
            }
        }
    }

// the lap of this cell: the shared clock moved on by its own offset, still a whole lap round
private fun shifted(
    lap: Float,
    variation: CellVariation,
): Float = (lap + variation.phase) % 1f

private fun DrawScope.pulse(
    lap: Float,
    color: Color,
) {
    val breath = (sin(lap * PULSE_BEATS * FULL_TURN) + 1f) / 2f
    val reach = size.minDimension * PULSE_GROWTH * breath
    drawRoundRect(
        color = color.copy(alpha = PULSE_BASE + PULSE_SWING * breath),
        topLeft = Offset(-reach, -reach),
        size =
            androidx.compose.ui.geometry
                .Size(size.width + reach * 2, size.height + reach * 2),
        cornerRadius = CornerRadius(size.minDimension / PULSE_CORNER),
        style = Stroke(width = size.minDimension * PULSE_STROKE),
    )
}

// each spark leaves the edge at its own angle, drifts outward and fades
private fun DrawScope.sparks(
    rhythm: Rhythm,
    lap: Float,
    color: Color,
    turn: Float,
) {
    val side = size.minDimension
    repeat(rhythm.sparks) { index ->
        val phase = (lap * rhythm.cycles + index.toFloat() / rhythm.sparks) % 1f
        val angle = index * SEED_STEP + turn
        val distance = side * (0.5f + DRIFT * phase)
        drawCircle(
            color = color.copy(alpha = sin(phase * PI.toFloat())),
            radius = side * SPARK_RADIUS * (1f - phase / 2f) + 0.5f,
            center = center + Offset(cos(angle) * distance, sin(angle) * distance),
        )
    }
}

private fun DrawScope.orbit(
    rhythm: Rhythm,
    lap: Float,
    color: Color,
    variation: CellVariation,
) {
    val side = size.minDimension
    val way = if (variation.clockwise) 1f else -1f
    repeat(rhythm.sparks) { index ->
        val angle = way * (lap * rhythm.cycles + index.toFloat() / rhythm.sparks) * FULL_TURN + variation.turn
        drawCircle(
            color = color,
            radius = side * SPARK_RADIUS + 0.5f,
            center = center + Offset(cos(angle) * side * ORBIT_RADIUS, sin(angle) * side * ORBIT_RADIUS),
        )
    }
}
