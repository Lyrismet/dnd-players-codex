package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val VIEW_WIDTH = 24f
private const val VIEW_HEIGHT = 28f
private const val ICON_SCALE = 0.5f
private const val ICON_STROKE = 2.8f
private const val ICON_OFFSET_X = 6f
private const val ICON_OFFSET_Y = 7f
private const val UNKNOWN_ICON_ALPHA = 0.45f
private const val HALF_RIM_WIDTH = 2.6f
private const val CRACK_WIDTH = 2.4f
private const val IMMUNE_RIM_WIDTH = 2.6f
private const val VULNERABLE_RIM_WIDTH = 2.4f
private const val PLAIN_RIM_WIDTH = 1.4f

private val ShieldBody = parse("M12 1.5 L21.5 5 V12.5 C21.5 19 17.5 23.5 12 26.5 C6.5 23.5 2.5 19 2.5 12.5 V5 Z")
private val ShieldLeftHalf = parse("M12 1.5 L2.5 5 V12.5 C2.5 19 6.5 23.5 12 26.5")
private val ShieldCrack = parse("M17.6 2.6 L15.1 7.4 L18.6 9.9 L16.4 13.6")

private val ShieldFill = AppPalette.SurfaceFooter
private val PlainRim = AppPalette.RadioIdle
private val ImmuneRim = Color(0xFFF1F3F7)
private val ResistHalf = Color(0xFFF0B43C)
private val VulnerableRim = Color(0xFFE5534B)
private val UnknownRim = AppPalette.BorderHover
private val UnknownIcon = AppPalette.TextSecondary

// the icon paths are a fixed set, so each is parsed once for every shield that draws it
private val parsedPaths = mutableMapOf<String, Path>()

private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()

/** the bare icon of a damage type, a stroked path of the 24 grid */
@Composable
fun AppDamageGlyph(
    iconPath: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    val icon = remember(iconPath) { parsedPaths.getOrPut(iconPath) { parse(iconPath) } }
    Canvas(modifier.size(size)) {
        scale(this.size.width / VIEW_WIDTH, pivot = Offset.Zero) {
            drawPath(icon, color, style = Stroke(ICON_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** a heater shield with [iconPath] inside, the rim telling the [style] and [count] drawn to the right */
@Composable
fun AppDamageShield(
    iconPath: String,
    iconColor: Color,
    style: ShieldStyle,
    modifier: Modifier = Modifier,
    width: Dp = 30.dp,
    count: Int? = null,
    crackColor: Color = AppPalette.Surface,
) {
    val icon =
        remember(iconPath) { if (iconPath.isBlank()) null else parsedPaths.getOrPut(iconPath) { parse(iconPath) } }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(Modifier.size(width, width * VIEW_HEIGHT / VIEW_WIDTH)) {
            scale(size.width / VIEW_WIDTH, pivot = Offset.Zero) {
                drawShield(style, icon, iconColor, crackColor)
            }
        }
        if (count != null) {
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = AppPalette.TextSecondary)
        }
    }
}

private fun DrawScope.drawShield(
    style: ShieldStyle,
    icon: Path?,
    iconColor: Color,
    crackColor: Color,
) {
    val round = { width: Float -> Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    drawPath(ShieldBody, ShieldFill)
    val rimWidth =
        when (style) {
            ShieldStyle.Immune -> IMMUNE_RIM_WIDTH
            ShieldStyle.Vulnerable -> VULNERABLE_RIM_WIDTH
            else -> PLAIN_RIM_WIDTH
        }
    val rim =
        when (style) {
            ShieldStyle.Immune -> ImmuneRim
            ShieldStyle.Vulnerable -> VulnerableRim
            ShieldStyle.Unknown -> UnknownRim
            else -> PlainRim
        }
    val dash = if (style == ShieldStyle.Unknown) PathEffect.dashPathEffect(floatArrayOf(2.2f, 2f)) else null
    drawPath(ShieldBody, rim, style = Stroke(rimWidth, join = StrokeJoin.Round, pathEffect = dash))
    if (style == ShieldStyle.Resist) drawPath(ShieldLeftHalf, ResistHalf, style = round(HALF_RIM_WIDTH))
    if (style == ShieldStyle.Vulnerable) drawPath(ShieldCrack, crackColor, style = round(CRACK_WIDTH))
    if (icon != null) {
        val known = style != ShieldStyle.Unknown
        translate(ICON_OFFSET_X, ICON_OFFSET_Y) {
            scale(ICON_SCALE, pivot = Offset.Zero) {
                drawPath(
                    icon,
                    if (known) iconColor else UnknownIcon,
                    alpha = if (known) 1f else UNKNOWN_ICON_ALPHA,
                    style = round(ICON_STROKE),
                )
            }
        }
    }
}
