package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDamageGlyph
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath

private val ActionColor = AppPalette.EmeraldBright
private val BonusColor = Color(0xFFE8A15A)
private val ReactionColor = AppPalette.Azure
private val HealingColor = Color(0xFF8DE0A8)
private val TempHpColor = Color(0xFF7DB8FF)
private const val GRID = 14f
private const val HEART_PATH =
    "M12 21s-7-4.5-9-9a5 5 0 0 1 9-3 5 5 0 0 1 9 3c-2 4.5-9 9-9 9z M12 9.5v5 M9.5 12h5"
private const val SHIELD_PATH = "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"

/** the casting time as a small shape: a circle for the action, a triangle for the bonus, a diamond, a clock */
@Suppress("MagicNumber") // the numbers are the points of the mockup's 14 grid
@Composable
internal fun CastKindGlyph(
    kind: CastKind,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    val clock = AppPalette.TextSecondary
    Canvas(modifier.size(size)) {
        val k = this.size.width / GRID

        fun point(
            x: Float,
            y: Float,
        ) = Offset(x * k, y * k)
        when (kind) {
            CastKind.ACTION -> drawCircle(ActionColor, radius = 5.5f * k, center = point(7f, 7f))
            CastKind.BONUS ->
                drawPath(
                    Path().apply {
                        moveTo(7f * k, 1.5f * k)
                        lineTo(13f * k, 12.5f * k)
                        lineTo(1f * k, 12.5f * k)
                        close()
                    },
                    BonusColor,
                )
            CastKind.REACTION ->
                drawPath(
                    Path().apply {
                        moveTo(7f * k, 1f * k)
                        lineTo(13f * k, 7f * k)
                        lineTo(7f * k, 13f * k)
                        lineTo(1f * k, 7f * k)
                        close()
                    },
                    ReactionColor,
                )
            CastKind.LONG -> {
                val line = Stroke(1.5f * k, cap = StrokeCap.Round)
                drawCircle(clock, radius = 5.5f * k, center = point(7f, 7f), style = line)
                drawPath(
                    Path().apply {
                        moveTo(7f * k, 4f * k)
                        lineTo(7f * k, 7.2f * k)
                        lineTo(9f * k, 8.5f * k)
                    },
                    clock,
                    style = line,
                )
            }
        }
    }
}

/** the icon of what a damage row does, a type has its own, nothing is drawn for a row without a type */
@Composable
internal fun SpellEffectGlyph(
    effect: SpellEffect?,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    when (effect) {
        null -> Unit
        SpellEffect.Healing -> AppDamageGlyph(HEART_PATH, HealingColor, modifier, size)
        SpellEffect.TempHp -> AppDamageGlyph(SHIELD_PATH, TempHpColor, modifier, size)
        is SpellEffect.Damage -> AppDamageGlyph(effect.type.shieldIconPath(), effect.type.shieldColor(), modifier, size)
    }
}
