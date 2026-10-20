package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import kotlin.random.Random

private val SkyStart = Color(0xFF1C1E30)
private val SkyEnd = Color(0xFF16171F)
private val WarmStar = AppPalette.TextHeading
private val CoolStar = Color(0xFFC8D2FF)

private const val MIN_STARS = 6
private const val MAX_STARS = 10
private const val MIN_EDGE = 0.06f
private const val MAX_EDGE = 0.94f

// the sky is muted and its stars barely differ, so no single one stands out from the background
private const val MIN_RADIUS_DP = 0.9f
private const val MAX_RADIUS_DP = 1.2f
private const val MIN_ALPHA = 0.2f
private const val MAX_ALPHA = 0.35f
private const val COOL_STAR_CHANCE = 0.4f
private const val SEED_MIX = -0x61c8864680b583ebL

/** x and y as fractions of the card, the radius in dp and the star's colour */
internal class Star(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val color: Color,
)

private fun Random.between(
    from: Float,
    until: Float,
): Float = from + nextFloat() * (until - from)

/** the stars of one card - the same [seed] always gives the same sky, another seed gives another one */
internal fun cosmicStars(seed: Long): List<Star> {
    val random = Random(seed * SEED_MIX)
    return List(random.nextInt(MIN_STARS, MAX_STARS + 1)) {
        val base = if (random.nextFloat() < COOL_STAR_CHANCE) CoolStar else WarmStar
        Star(
            x = random.between(MIN_EDGE, MAX_EDGE),
            y = random.between(MIN_EDGE, MAX_EDGE),
            radiusDp = random.between(MIN_RADIUS_DP, MAX_RADIUS_DP),
            color = base.copy(alpha = random.between(MIN_ALPHA, MAX_ALPHA)),
        )
    }
}

/** a night-sky fill with a few faint stars laid out from [seed], behind the card's content - marks a top level place */
fun Modifier.cosmicBackdrop(seed: Long): Modifier =
    drawWithCache {
        // built once per size, not on every frame the card is redrawn
        val sky =
            Brush.linearGradient(
                listOf(SkyStart, SkyEnd),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        val stars = cosmicStars(seed)
        onDrawBehind {
            drawRect(sky)
            stars.forEach { star ->
                drawCircle(
                    star.color,
                    radius = star.radiusDp * density,
                    center =
                        Offset(
                            size.width * star.x,
                            size.height * star.y,
                        ),
                )
            }
        }
    }
