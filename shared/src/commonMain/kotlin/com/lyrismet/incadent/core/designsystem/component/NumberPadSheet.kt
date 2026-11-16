package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.icons.CalculatorIcon

private val IconBadgeSize = 42.dp
private val KeyHeight = 48.dp
private val ResultBlockShape = AppShapes.Card
private val KeyShape = AppShapes.Field
private val IconBadgeShape = AppShapes.Field

/** a quick numeric keypad over a form field - the stepper's "КОДЕКС" calculator (v6.dc.html's `pad`) */
@Composable
fun NumberPadSheet(
    overline: String,
    title: String,
    expr: String,
    result: String,
    rangeHint: String,
    applyLabel: String,
    applyEnabled: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // a row under the header, e.g. the mode switch of a pad that edits two numbers
    modes: (@Composable () -> Unit)? = null,
    // tints the typed number and the range under it, e.g. green for a gain and red for a loss
    accent: Color? = null,
    // a button left of the apply one, e.g. "to maximum" of a pad that heals
    secondary: NumberPadAction? = null,
    // a quiet line under both buttons of a pad that has a [secondary] one
    secondaryNote: String? = null,
) {
    Column(
        modifier =
            modifier
                .background(AppPalette.Surface)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NumberPadHeader(overline, title, onClose)
        modes?.invoke()
        NumberPadResultBlock(expr, result, rangeHint, accent)
        NumberPadKeyGrid(onDigit, onBackspace, onClear)
        if (secondary == null) {
            FormPrimaryButton(text = applyLabel, onClick = onApply, enabled = applyEnabled)
        } else {
            NumberPadActions(secondary, secondaryNote) { width ->
                FormPrimaryButton(text = applyLabel, onClick = onApply, enabled = applyEnabled, modifier = width)
            }
        }
    }
}

/** the extra button of a pad: its [label] and what it will do as a [caption] */
data class NumberPadAction(
    val label: String,
    val caption: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/** the outlined button of a [NumberPadAction]: its label over what it will do */
@Composable
fun NumberPadSecondaryButton(
    action: NumberPadAction,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .defaultMinSize(minHeight = 52.dp)
                .appCard(
                    shape = AppShapes.Card,
                    background = Color.Transparent,
                    border = AppPalette.BorderHover,
                    onClick = if (action.enabled) action.onClick else null,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val color = if (action.enabled) AppPalette.TextPrimary else AppPalette.TextTertiary
        Text(action.label, color = color, fontSize = AppFontSize.Body, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(action.caption, color = AppPalette.TextMuted, fontSize = AppFontSize.Caption, maxLines = 1)
    }
}

/** the extra button beside the apply one, drawn by [apply] with the width it is given, and the [note] under both */
@Composable
fun NumberPadActions(
    secondary: NumberPadAction,
    note: String?,
    apply: @Composable (Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberPadSecondaryButton(secondary, Modifier.weight(1f))
            apply(Modifier.weight(1f))
        }
        note?.let { Text(it, color = AppPalette.TextMuted, fontSize = AppFontSize.Caption) }
    }
}

@Composable
private fun NumberPadHeader(
    overline: String,
    title: String,
    onClose: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            size = IconBadgeSize,
            shape = IconBadgeShape,
            background = AppPalette.Background,
            border = AppPalette.Gold.copy(alpha = 0.5f),
        ) {
            CalculatorIcon()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionOverline(text = overline, color = AppPalette.GoldBright)
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SheetCloseButton(onClick = onClose, size = 40.dp)
    }
}

@Composable
private fun NumberPadResultBlock(
    expr: String,
    result: String,
    rangeHint: String,
    accent: Color?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = ResultBlockShape, background = AppPalette.Background, border = AppPalette.BorderSubtle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            expr,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium),
            color = AppPalette.TextSecondary,
            modifier = Modifier.heightIn(min = 20.dp),
        )
        Text(
            result,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = AppFontSize.Number,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = accent ?: AppPalette.GoldBright,
        )
        Text(
            rangeHint,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.SmallPlus),
            color = accent ?: AppPalette.TextMuted,
        )
    }
}

/** one key of the 4-column grid (Players Codex v6.dc.html `pad.keys`) - [emphasized] is the ⌫/C gold styling */
private data class NumberPadKey(
    val label: String,
    val onClick: () -> Unit,
    val emphasized: Boolean = false,
)

/**
 * the four-column grid of digits "7 8 9 ⌫ / 4 5 6 + / 1 2 3 / C 0 00", the "+" starting the next term of a sum is there
 * only when [onPlus] is given, which is the layout of the damage calculator
 */
@Composable
fun NumberPadKeyGrid(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onPlus: (() -> Unit)? = null,
    keyHeight: Dp = KeyHeight,
) {
    val rows =
        listOf(
            listOf(
                digitKey(7, onDigit),
                digitKey(8, onDigit),
                digitKey(9, onDigit),
                NumberPadKey(label = "⌫", onClick = onBackspace, emphasized = true),
            ),
            listOf(
                digitKey(4, onDigit),
                digitKey(5, onDigit),
                digitKey(6, onDigit),
                onPlus?.let { NumberPadKey(label = "+", onClick = it, emphasized = true) },
            ),
            listOf(digitKey(1, onDigit), digitKey(2, onDigit), digitKey(3, onDigit), null),
            listOf(
                NumberPadKey(label = "C", onClick = onClear, emphasized = true),
                digitKey(0, onDigit),
                NumberPadKey(label = "00", onClick = { repeat(DOUBLE_ZERO) { onDigit(0) } }),
                null,
            ),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    if (key != null) {
                        NumberPadKeyButton(key, keyHeight, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private const val DOUBLE_ZERO = 2

private fun digitKey(
    digit: Int,
    onDigit: (Int) -> Unit,
) = NumberPadKey(label = digit.toString(), onClick = { onDigit(digit) })

@Composable
private fun NumberPadKeyButton(
    key: NumberPadKey,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val background = if (key.emphasized) AppPalette.Surface else AppPalette.SurfacePopover
    val border = if (key.emphasized) AppPalette.BorderPopover else AppPalette.Border
    val foreground = if (key.emphasized) AppPalette.GoldBright else AppPalette.TextPrimary
    Box(
        modifier =
            modifier
                .height(height)
                .appCard(shape = KeyShape, background = background, border = border, onClick = key.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            key.label,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = AppFontSize.Heading,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = foreground,
        )
    }
}
