package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatMark
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.CustomCondition
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import com.lyrismet.incadent.feature.combat.domain.round.isExpired
import com.lyrismet.incadent.feature.combat.domain.round.isLastRound
import com.lyrismet.incadent.feature.combat.domain.round.remaining
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_mark_concentration
import incadent.shared.generated.resources.combat_mark_field_custom
import incadent.shared.generated.resources.combat_mark_field_label
import incadent.shared.generated.resources.combat_mark_field_spell
import incadent.shared.generated.resources.combat_mark_sheet_custom_title
import incadent.shared.generated.resources.combat_mark_sheet_title
import org.jetbrains.compose.resources.stringResource

private const val CONCENTRATION_SIGN = "◎ "
private const val EXPIRED_SIGN = " ×"

internal fun CombatMark.tone(round: Int): MarkTone =
    when {
        isExpired(round) -> MarkTone.EXPIRED
        isLastRound(round) -> MarkTone.LAST
        else -> MarkTone.NORMAL
    }

/** the text of a mark as a chip tells it: the sign of a concentration, the name, the rounds left */
@Composable
internal fun CombatMark.chipText(round: Int): String {
    val name =
        if (kind == MarkKind.CONCENTRATION) {
            CONCENTRATION_SIGN + text.ifBlank { stringResource(Res.string.combat_mark_concentration) }
        } else {
            text
        }
    val left = remaining(round) ?: return name
    return "$name · $left" + if (left == 0) EXPIRED_SIGN else ""
}

@Composable
internal fun panelTags(
    member: Combatant,
    round: Int,
): List<PanelMark> =
    member.conditions.map { PanelMark(0, it.label(), MarkTone.NORMAL, ChipKind.MARK, condition = it) } +
        member.marks.inRowOrder(round).map {
            PanelMark(
                it.id,
                it.chipText(round),
                it.tone(round),
                it.kind.chip(),
            )
        }

internal fun panelCustoms(
    member: Combatant,
    customs: List<CustomCondition>,
): List<CustomItem> = customs.map { CustomItem(it.name, it.isOn(member.marks)) }

@Composable
internal fun markRowChips(
    member: Combatant,
    round: Int,
): List<RowChip> =
    member.marks.inRowOrder(round).map { mark ->
        val kind =
            when (mark.tone(round)) {
                MarkTone.NORMAL -> mark.kind.chip()
                MarkTone.LAST -> ChipKind.MARK_LAST
                MarkTone.EXPIRED -> ChipKind.MARK_EXPIRED
            }
        RowChip(mark.chipText(round), kind)
    }

@Composable
internal fun markSheetState(
    draft: MarkDraft,
    member: Combatant,
): MarkSheet {
    val concentration = draft.concentration && !draft.custom
    val label =
        stringResource(
            when {
                draft.custom -> Res.string.combat_mark_field_custom
                concentration -> Res.string.combat_mark_field_spell
                else -> Res.string.combat_mark_field_label
            },
        )
    return MarkSheet(
        name = member.name,
        title =
            stringResource(
                if (draft.custom) Res.string.combat_mark_sheet_custom_title else Res.string.combat_mark_sheet_title,
            ),
        fieldLabel = label,
        text = draft.text,
        custom = draft.custom,
        concentration = concentration,
        roundsText = draft.roundsText,
        canAdd = draft.text.isNotBlank() || concentration,
        // a participant of the party or the allies has no enemy presets
        showPresets = member.side == CombatSide.ENEMY && !draft.custom && !concentration,
    )
}

@Composable
internal fun markSheetFor(
    draft: MarkDraft?,
    combat: Combat,
): MarkSheet? = draft?.let { d -> combat.combatants.firstOrNull { it.id == d.memberId }?.let { markSheetState(d, it) } }

internal fun MarkKind.chip(): ChipKind =
    when (this) {
        MarkKind.CONCENTRATION -> ChipKind.EFFECT
        MarkKind.SPELL -> ChipKind.SPELL
        else -> ChipKind.MARK
    }
