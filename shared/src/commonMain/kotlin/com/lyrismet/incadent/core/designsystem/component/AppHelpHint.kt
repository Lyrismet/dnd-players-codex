package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.help_got_it
import org.jetbrains.compose.resources.stringResource

private val HintTapArea = 48.dp
private val HintIconSize = 18.dp
private val SheetIconSize = 28.dp
private val MarkerSlot = 30.dp
private val MarkerSize = 18.dp
private val HelpLegendShape = RoundedCornerShape(14.dp)

/** the round marks the legend of a hint draws, the same circles the sheet uses for skills, pips and saving throws */
enum class HelpMarker { Empty, Filled, Double, Success, Danger }

/** one legend line of a hint: a [marker], what it means and an optional second line */
data class HelpRow(
    val marker: HelpMarker,
    val title: String,
    val subtitle: String? = null,
)

/**
 * the "?" next to a section title: a tap opens a short explanation sheet with an optional legend and a "Понятно"
 * button. The tap area is 48dp, the drawn mark 18dp, and nothing pops up on its own.
 */
@Composable
fun AppHelpHint(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    rows: List<HelpRow> = emptyList(),
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .size(HintTapArea)
                .clip(CircleShape)
                .clickable(role = Role.Button) { open = true },
        contentAlignment = Alignment.Center,
    ) {
        HintMark(size = HintIconSize, fontSize = AppFontSize.Tiny)
    }
    if (open) {
        AppBottomSheet(onDismissRequest = { open = false }) {
            HelpSheetBody(title, body, rows, onClose = { open = false })
        }
    }
}

@Composable
private fun HintMark(
    size: Dp,
    fontSize: TextUnit,
) {
    Box(
        modifier = Modifier.size(size).border(1.5.dp, AppPalette.RadioIdle, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = AppPalette.TextDescription,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HelpSheetBody(
    title: String,
    body: String,
    rows: List<HelpRow>,
    onClose: () -> Unit,
) {
    AppSheetColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            HintMark(size = SheetIconSize, fontSize = AppFontSize.Medium)
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 29.sp),
                color = AppPalette.TextHeading,
            )
        }
        if (rows.isNotEmpty()) HelpLegend(rows)
        Text(
            text = body,
            fontSize = AppFontSize.MediumPlus,
            lineHeight = 22.sp,
            color = AppPalette.TextDescription,
        )
        AppButton(
            text = stringResource(Res.string.help_got_it),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Solid,
            size = ButtonSize.Primary,
        )
    }
}

@Composable
private fun HelpLegend(rows: List<HelpRow>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(HelpLegendShape)
                .background(AppPalette.Background)
                .border(1.dp, AppPalette.Border, HelpLegendShape)
                .padding(horizontal = 14.dp),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) AppDivider(color = AppPalette.SurfacePopover)
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(MarkerSlot), contentAlignment = Alignment.Center) { HelpMarkerDot(row.marker) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.title,
                        fontSize = AppFontSize.MediumPlus,
                        fontWeight = FontWeight.SemiBold,
                        color = AppPalette.TextPrimary,
                    )
                    if (!row.subtitle.isNullOrBlank()) {
                        Text(
                            text = row.subtitle,
                            fontSize = AppFontSize.SmallPlus,
                            lineHeight = 18.sp,
                            color = AppPalette.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpMarkerDot(marker: HelpMarker) {
    val fill: Color =
        when (marker) {
            HelpMarker.Empty -> Color.Transparent
            HelpMarker.Filled, HelpMarker.Double -> AppPalette.GoldBright
            HelpMarker.Success -> AppPalette.Emerald
            HelpMarker.Danger -> AppPalette.Maroon
        }
    Canvas(Modifier.size(MarkerSize + 8.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = MarkerSize.toPx() / 2
        if (marker == HelpMarker.Empty) {
            val stroke = 1.5.dp.toPx()
            drawCircle(AppPalette.RadioIdle, radius - stroke / 2, center, style = Stroke(stroke))
        } else {
            drawCircle(fill, radius, center)
        }
        if (marker == HelpMarker.Double) {
            val gap = 2.dp.toPx()
            val ring = 1.5.dp.toPx()
            drawCircle(AppPalette.GoldBright, radius + gap + ring / 2, center, style = Stroke(ring))
        }
    }
}
