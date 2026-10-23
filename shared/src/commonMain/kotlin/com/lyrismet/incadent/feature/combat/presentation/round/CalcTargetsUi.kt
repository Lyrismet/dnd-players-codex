package com.lyrismet.incadent.feature.combat.presentation.round

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.ShieldStyle
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_add_target
import org.jetbrains.compose.resources.stringResource

internal fun HitEffect.shieldStyle(): ShieldStyle =
    when (this) {
        HitEffect.HALF -> ShieldStyle.Resist
        HitEffect.NONE -> ShieldStyle.Immune
        HitEffect.DOUBLE -> ShieldStyle.Vulnerable
        HitEffect.FULL -> ShieldStyle.Plain
    }

@Composable
internal fun Shield(
    shield: CalcShield,
    width: androidx.compose.ui.unit.Dp,
) {
    val type = shield.type
    AppDamageShield(
        iconPath = type?.shieldIconPath().orEmpty(),
        iconColor = type?.shieldColor() ?: AppPalette.TextMuted,
        style = shield.effect.shieldStyle(),
        width = width,
    )
}

@Composable
internal fun TargetsBlock(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FormFieldLabel(calc.targetsHeader, Modifier.weight(1f))
            calc.reaction?.let { ReactionChip(it) { emit(CalcEvent.ReactionToggled(it.memberId)) } }
        }
        if (calc.rowMode) {
            calc.targets.forEach { TargetRow(it, emit) }
        } else {
            TargetChips(calc, emit)
        }
        if (calc.offTargets.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                calc.offTargets.forEach { off ->
                    Text(
                        text = stringResource(Res.string.combat_calc_add_target, off.name),
                        color = AppPalette.TextSecondary,
                        fontSize = AppFontSize.Body,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .dashedBorder(AppPalette.BorderHover, cornerRadius = 10.dp)
                                .clickable { emit(CalcEvent.TargetAdded(off.id)) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionChip(
    reaction: CalcReaction,
    onClick: () -> Unit,
) {
    val tint = if (reaction.available) RoundColors.Reaction else AppPalette.TextTertiary
    Text(
        text = "⚡ ${reaction.label}",
        color = tint,
        fontSize = AppFontSize.Small,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .appCard(
                    shape = AppShapes.Pill,
                    background = if (reaction.available) tint.copy(alpha = 0.12f) else Color.Transparent,
                    border = if (reaction.available) tint.copy(alpha = 0.55f) else AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TargetChips(
    calc: CalcState,
    emit: (CalcEvent) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        calc.targets.forEach { target ->
            val tint = RoundColors.side(target.side)
            Row(
                modifier =
                    Modifier
                        .appCard(
                            shape = AppShapes.Control,
                            background = tint.copy(alpha = 0.12f),
                            border = if (target.selected) tint else tint.copy(alpha = 0.45f),
                        ).defaultMinSize(minHeight = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .clickable {
                                emit(
                                    CalcEvent.TargetToggled(target.id),
                                )
                            }.padding(start = 12.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(target.name, color = tint, fontSize = AppFontSize.BodyPlus, fontWeight = FontWeight.SemiBold)
                    target.shield?.let { Shield(it, 16.dp) }
                    ScaleBadge(target)
                    target.result?.let {
                        Text(
                            it,
                            color = AppPalette.TextHeading,
                            fontSize = AppFontSize.MediumPlus,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Text(
                    text = "✕",
                    color = AppPalette.TextSecondary,
                    fontSize = AppFontSize.Small,
                    modifier =
                        Modifier
                            .clickable { emit(CalcEvent.TargetRemoved(target.id)) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
    calc.selectedTarget?.let { SelectedTarget(it, emit) }
}

@Composable
private fun ScaleBadge(target: CalcTarget) {
    if (target.scale != com.lyrismet.incadent.feature.combat.domain.damage.HitScale.ONE) {
        val label = target.scales.first { it.selected }.label
        Text(
            text = label,
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.Small,
            fontWeight = FontWeight.ExtraBold,
            modifier =
                Modifier
                    .appCard(
                        AppShapes.Badge,
                        AppPalette.SurfaceElevated,
                        null,
                    ).padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TargetRow(
    target: CalcTarget,
    emit: (CalcEvent) -> Unit,
) {
    val tint = RoundColors.side(target.side)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(
                        shape = AppShapes.Control,
                        background = tint.copy(alpha = 0.12f),
                        border = if (target.selected) tint else tint.copy(alpha = 0.45f),
                    ).defaultMinSize(minHeight = 48.dp)
                    .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).clickable { emit(CalcEvent.TargetToggled(target.id)) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        target.name,
                        color = tint,
                        fontSize = AppFontSize.BodyPlus,
                        lineHeight = AppLineHeight.Row,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    target.shield?.let { Shield(it, 16.dp) }
                    ScaleBadge(target)
                }
                PartsLine(target.parts)
            }
            target.result?.let {
                Text(
                    it,
                    color = AppPalette.TextHeading,
                    fontSize = AppFontSize.MediumPlus,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                text = "✕",
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                modifier =
                    Modifier
                        .clickable {
                            emit(CalcEvent.TargetRemoved(target.id))
                        }.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        if (target.selected) SelectedTarget(target, emit, showParts = false)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun PartsLine(parts: List<CalcPart>) {
    if (parts.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.forEach { part ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(part.prefix, color = AppPalette.TextMuted, fontSize = AppFontSize.Small)
                part.type?.let { Shield(CalcShield(it, HitEffect.FULL), 12.dp) }
                Text(
                    part.text,
                    color = AppPalette.TextMuted,
                    fontSize = AppFontSize.Small,
                    lineHeight = AppLineHeight.Row,
                )
            }
        }
    }
}
