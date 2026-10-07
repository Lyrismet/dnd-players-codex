package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.icons.CalculatorIcon

private val IconBadgeSize = 42.dp
private val KeyHeight = 54.dp
private val ResultBlockShape = RoundedCornerShape(14.dp)
private val KeyShape = RoundedCornerShape(12.dp)
private val IconBadgeShape = RoundedCornerShape(12.dp)

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
        NumberPadResultBlock(expr, result, rangeHint)
        NumberPadKeyGrid(onDigit, onBackspace, onClear)
        FormPrimaryButton(text = applyLabel, onClick = onApply, enabled = applyEnabled)
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = AppPalette.TextSecondary,
            modifier = Modifier.heightIn(min = 20.dp),
        )
        Text(
            result,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 46.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = AppPalette.GoldBright,
        )
        Text(
            rangeHint,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = AppPalette.TextMuted,
        )
    }
}

/** one key of the 4-column grid (Players Codex v6.dc.html `pad.keys`) - [emphasized] is the ⌫/C gold styling */
private data class NumberPadKey(
    val label: String,
    val onClick: () -> Unit,
    val emphasized: Boolean = false,
)

@Composable
private fun NumberPadKeyGrid(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val rows =
        listOf(
            listOf(
                NumberPadKey(label = "7", onClick = { onDigit(7) }),
                NumberPadKey(label = "8", onClick = { onDigit(8) }),
                NumberPadKey(label = "9", onClick = { onDigit(9) }),
                NumberPadKey(label = "⌫", onClick = onBackspace, emphasized = true),
            ),
            listOf(
                NumberPadKey(label = "4", onClick = { onDigit(4) }),
                NumberPadKey(label = "5", onClick = { onDigit(5) }),
                NumberPadKey(label = "6", onClick = { onDigit(6) }),
                NumberPadKey(label = "C", onClick = onClear, emphasized = true),
            ),
            listOf(
                NumberPadKey(label = "1", onClick = { onDigit(1) }),
                NumberPadKey(label = "2", onClick = { onDigit(2) }),
                NumberPadKey(label = "3", onClick = { onDigit(3) }),
                NumberPadKey(label = "0", onClick = { onDigit(0) }),
            ),
        )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key -> NumberPadKeyButton(key, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NumberPadKeyButton(
    key: NumberPadKey,
    modifier: Modifier = Modifier,
) {
    val background = if (key.emphasized) AppPalette.Surface else AppPalette.SurfacePopover
    val border = if (key.emphasized) AppPalette.BorderPopover else AppPalette.Border
    val foreground = if (key.emphasized) AppPalette.GoldBright else AppPalette.TextPrimary
    Box(
        modifier =
            modifier
                .height(KeyHeight)
                .appCard(shape = KeyShape, background = background, border = border, onClick = key.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            key.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            color = foreground,
        )
    }
}
