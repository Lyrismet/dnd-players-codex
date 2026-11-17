package com.lyrismet.incadent.feature.session.presentation.list.downtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.designsystem.component.icons.sheet.SheetIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.downtime_card_empty
import incadent.shared.generated.resources.downtime_card_more
import incadent.shared.generated.resources.downtime_gap_after_format
import incadent.shared.generated.resources.downtime_gap_format
import incadent.shared.generated.resources.downtime_gold_format
import incadent.shared.generated.resources.downtime_item_format
import org.jetbrains.compose.resources.stringResource

/** "Between sessions I and II", or "After session I" when nothing follows the gap */
@Composable
internal fun gapText(
    afterLabel: String,
    nextLabel: String?,
): String =
    if (nextLabel == null) {
        stringResource(Res.string.downtime_gap_after_format, afterLabel)
    } else {
        stringResource(Res.string.downtime_gap_format, afterLabel, nextLabel)
    }

@Composable
internal fun DowntimeLine.text(): String = stringResource(Res.string.downtime_item_format, name, quantity)

@Composable
internal fun goldText(delta: Int): String = stringResource(Res.string.downtime_gold_format, signedGold(delta))

private val CardShape = RoundedCornerShape(12.dp)

/** the card of the gap between two sessions, hanging on a dotted line with the hourglass in a ring */
@Composable
fun DowntimeCardRow(
    card: DowntimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Timeline()
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .appCard(
                        shape = CardShape,
                        background = AppPalette.Parchment.copy(alpha = 0.04f),
                        border = null,
                        onClick = onClick,
                    ).dashedBorder(AppPalette.Parchment.copy(alpha = 0.35f), cornerRadius = 12.dp)
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CardHeadline(card)
            CardBody(card)
        }
    }
}

@Composable
private fun CardHeadline(card: DowntimeCard) {
    SectionOverline(
        text = gapText(card.afterLabel, card.nextLabel).uppercase(),
        color = AppPalette.Parchment,
        style = OverlineStyle.Field,
    )
}

@Composable
private fun CardBody(card: DowntimeCard) {
    val text = card.shortText.ifBlank { card.gainedText }
    if (text.isNotBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (card.longText.isNotBlank()) {
        Text(
            stringResource(Res.string.downtime_card_more),
            style = MaterialTheme.typography.labelMedium,
            color = AppPalette.Parchment,
        )
    }
    LootChips(card, compact = true)
    if (card.isEmpty) {
        Text(
            stringResource(Res.string.downtime_card_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
        )
    }
}

/** the dotted line down the left of the card with the hourglass in a ring on its middle */
@Composable
private fun Timeline() {
    val line = AppPalette.RadioIdle
    Column(
        modifier =
            Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val ring = 11.dp.toPx()
                    val top = size.height / 2 - ring
                    val bottom = size.height / 2 + ring
                    val dots = PathEffect.dashPathEffect(floatArrayOf(0.1f, 4.dp.toPx()))
                    drawLine(line, Offset(x, 0f), Offset(x, top), 1.5.dp.toPx(), StrokeCap.Round, dots)
                    drawLine(line, Offset(x, bottom), Offset(x, size.height), 1.5.dp.toPx(), StrokeCap.Round, dots)
                },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            size = 22.dp,
            shape = CircleShape,
            background = AppPalette.Background,
            border = AppPalette.Parchment,
        ) {
            Icon(SheetIcons.Hourglass, null, tint = AppPalette.Parchment, modifier = Modifier.size(12.dp))
        }
    }
}

/** the gold chip and the thing chips, one clipped row on the card and a wrapping one in the window */
@Composable
internal fun LootChips(
    card: DowntimeCard,
    compact: Boolean = false,
) {
    if (!card.hasLoot) return
    val chips: @Composable () -> Unit = {
        if (card.goldDelta != 0) {
            TagChip(
                text = goldText(card.goldDelta),
                foreground = AppPalette.GoldBright,
                background = AppPalette.Gold.copy(alpha = 0.12f),
            )
        }
        card.items.forEach { TagChip(text = it.text(), foreground = AppPalette.TextDescription) }
    }
    if (compact) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) { chips() }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips()
        }
    }
}

/** whichever downtime window is open - the full view or the editor */
@Composable
fun DowntimeSheets(
    state: DowntimeSheetState,
    onEvent: (DowntimeEvent) -> Unit,
) {
    when (state) {
        DowntimeSheetState.Hidden -> Unit
        is DowntimeSheetState.Detail -> DowntimeDetailSheet(state, onEvent)
        is DowntimeSheetState.Edit -> DowntimeEditSheet(state.draft, state.canApply, onEvent)
    }
}
