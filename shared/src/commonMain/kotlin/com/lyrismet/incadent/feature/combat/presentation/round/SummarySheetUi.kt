package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppCheckBox
import com.lyrismet.incadent.core.designsystem.component.AppDamageGlyph
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_summary_empty
import dndplayerscodex.shared.generated.resources.combat_summary_hint
import dndplayerscodex.shared.generated.resources.combat_summary_overline
import org.jetbrains.compose.resources.stringResource

/** the damage of one round, target by target and blow by blow, with the master's resistance named after the blow */
@Composable
internal fun SummarySheet(
    summary: SummaryState,
    send: (CombatRoundEvent) -> Unit,
) {
    val emit = { event: SummaryEvent -> send(CombatRoundEvent.Summary(event)) }
    AppBottomSheet(onDismissRequest = { emit(SummaryEvent.Closed) }) {
        AppSheetColumn(scroll = true, spacing = 12.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.combat_summary_overline),
                        color = AppPalette.Gold,
                        fontSize = AppFontSize.Caption,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                    )
                    Text(summary.title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
                }
                SheetCloseButton(onClick = { emit(SummaryEvent.Closed) }, size = 40.dp)
            }
            RoundPicker(summary.rounds, emit)
            Text(
                stringResource(Res.string.combat_summary_hint),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                lineHeight = AppLineHeight.Row,
            )
            if (summary.targets.isEmpty()) {
                Text(
                    stringResource(Res.string.combat_summary_empty),
                    color = AppPalette.TextSecondary,
                    fontSize = AppFontSize.Body,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                )
            }
            summary.targets.forEach { SummaryTargetCard(it, summary, emit) }
        }
    }
}

@Composable
private fun RoundPicker(
    rounds: List<SummaryRound>,
    emit: (SummaryEvent) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rounds.reversed().forEach { round ->
            Column(
                modifier =
                    Modifier
                        .size(width = 44.dp, height = 52.dp)
                        .appCard(
                            shape = RoundedCornerShape(11.dp),
                            background = if (round.selected) AppPalette.SurfaceElevated else Color.Transparent,
                            border = if (round.selected) AppPalette.Gold else AppPalette.Border,
                            onClick = { emit(SummaryEvent.RoundSelected(round.number)) },
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    round.number.toString(),
                    color = if (round.selected) AppPalette.TextHeading else AppPalette.TextDescription,
                    fontSize = AppFontSize.MediumPlus,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    round.sum,
                    color = AppPalette.MaroonBright,
                    fontSize = AppFontSize.Caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SummaryTargetCard(
    target: SummaryTarget,
    summary: SummaryState,
    emit: (SummaryEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Card, background = AppPalette.Background, border = AppPalette.BorderSubtle)
                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                target.name,
                color = RoundColors.side(target.side),
                fontSize = AppFontSize.MediumPlus,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(target.state, color = AppPalette.TextSecondary, fontSize = AppFontSize.Small)
            Text(
                target.sum,
                color = AppPalette.MaroonBright,
                fontSize = AppFontSize.Heading,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        target.hits.forEach { SummaryHitRow(it, summary, emit) }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SummaryHitRow(
    hit: SummaryHit,
    summary: SummaryState,
    emit: (SummaryEvent) -> Unit,
) {
    AppDivider(color = AppPalette.SurfacePopover)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            hit.source,
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Caption,
            lineHeight = AppLineHeight.Row,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                hit.chips.forEach { TermButton(it, emit) }
                hit.scaleLabel?.let {
                    Text(
                        it,
                        color = AppPalette.TextPrimary,
                        fontSize = AppFontSize.Small,
                        fontWeight = FontWeight.ExtraBold,
                        modifier =
                            Modifier
                                .appCard(
                                    AppShapes.Badge,
                                    AppPalette.SurfaceElevated,
                                    null,
                                ).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                hit.total,
                color = AppPalette.TextHeading,
                fontSize = AppFontSize.MediumPlus,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        hit.popup?.let { PopupCard(it, hit.canRecord, summary, emit) }
    }
}

@Composable
private fun TermButton(
    chip: SummaryChip,
    emit: (SummaryEvent) -> Unit,
) {
    val border =
        when {
            chip.open -> AppPalette.GoldBright
            chip.changed -> AppPalette.AmberOutline
            else -> AppPalette.BorderHover
        }
    val base =
        Modifier
            .defaultMinSize(minHeight = 34.dp)
            .then(
                if (chip.tappable &&
                    !chip.open &&
                    !chip.changed
                ) {
                    Modifier.dashedBorder(border, cornerRadius = 9.dp)
                } else {
                    Modifier.border(1.dp, border, AppShapes.Tile)
                },
            ).then(
                if (chip.tappable) {
                    Modifier.clickable {
                        emit(SummaryEvent.TermTapped(chip.eventId))
                    }
                } else {
                    Modifier
                },
            ).padding(horizontal = 9.dp)
    Row(
        modifier = base,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chip.type?.let { AppDamageGlyph(it.shieldIconPath(), it.shieldColor()) }
        chip.old?.let {
            Text(
                it,
                color = AppPalette.TextTertiary,
                fontSize = AppFontSize.Body,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        Text(chip.now, color = AppPalette.TextPrimary, fontSize = AppFontSize.Body, fontWeight = FontWeight.SemiBold)
        if (chip.effect != HitEffect.FULL) {
            AppDamageShield(
                chip.type?.shieldIconPath().orEmpty(),
                chip.type?.shieldColor() ?: AppPalette.TextMuted,
                chip.effect.shieldStyle(),
                width = 14.dp,
                crackColor = AppPalette.Background,
            )
        }
    }
}

@Composable
private fun PopupCard(
    popup: SummaryPopup,
    canRecord: Boolean,
    summary: SummaryState,
    emit: (SummaryEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.SurfacePopover,
                    border = AppPalette.BorderPopover,
                ).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            popup.title,
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Caption,
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 6.dp),
        )
        popup.options.forEach { option -> PopupOption(option, popup, emit) }
        if (canRecord) RecordRow(summary, emit)
    }
}

@Composable
private fun PopupOption(
    option: SummaryOption,
    popup: SummaryPopup,
    emit: (SummaryEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 42.dp)
                .appCard(
                    shape = AppShapes.Small,
                    background = if (option.selected) AppPalette.Gold.copy(alpha = 0.12f) else Color.Transparent,
                    border = null,
                    onClick = { emit(SummaryEvent.EffectSelected(popup.eventId, option.effect)) },
                ).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.effect != HitEffect.FULL) {
            AppDamageShield(
                popup.type?.shieldIconPath().orEmpty(),
                popup.type?.shieldColor() ?: AppPalette.TextMuted,
                option.effect.shieldStyle(),
                width = 14.dp,
                crackColor = AppPalette.SurfacePopover,
            )
        }
        Text(
            text = option.label + if (option.selected) " ✓" else "",
            color = optionColor(option.effect),
            fontSize = AppFontSize.Body,
        )
    }
}

private fun optionColor(effect: HitEffect): Color =
    when (effect) {
        HitEffect.FULL -> AppPalette.TextMuted
        HitEffect.HALF -> AppPalette.ResistanceText
        HitEffect.NONE -> AppPalette.ImmunityText
        HitEffect.DOUBLE -> AppPalette.VulnerabilityText
    }

@Composable
private fun RecordRow(
    summary: SummaryState,
    emit: (SummaryEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        AppDivider(color = AppPalette.BorderPopover)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { emit(SummaryEvent.RecordToggled) }
                    .defaultMinSize(minHeight = 42.dp)
                    .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppCheckBox(summary.record)
            Text(summary.recordLabel, color = AppPalette.TextPrimary, fontSize = AppFontSize.SmallPlus)
        }
    }
}
