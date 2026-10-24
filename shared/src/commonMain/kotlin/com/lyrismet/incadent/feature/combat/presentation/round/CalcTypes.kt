package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_reaction
import dndplayerscodex.shared.generated.resources.combat_calc_target_many
import dndplayerscodex.shared.generated.resources.combat_calc_target_one
import dndplayerscodex.shared.generated.resources.combat_calc_type_for_next
import dndplayerscodex.shared.generated.resources.combat_calc_type_for_term
import dndplayerscodex.shared.generated.resources.combat_calc_type_none
import dndplayerscodex.shared.generated.resources.combat_calc_type_title
import dndplayerscodex.shared.generated.resources.combat_panel_rx_spent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun targetsHeader(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.combat_calc_target_one)
    } else {
        stringResource(Res.string.combat_calc_target_many, count)
    }

@Composable
internal fun reactionOf(
    targets: List<Combatant>,
    ctx: CalcContext,
): CalcReaction? =
    targets.firstOrNull { it.id in ctx.ownIds && !it.isDown() }?.let {
        val label =
            if (it.reactionAvailable) {
                stringResource(Res.string.combat_calc_reaction)
            } else {
                stringResource(Res.string.combat_panel_rx_spent)
            }
        CalcReaction(it.reactionAvailable, label, it.id)
    }

internal fun currentType(draft: CalcDraft): DamageType? {
    val parts = draft.expr.raw.split('+')
    val index = parts.lastIndex
    return if (parts[index].isNotEmpty() &&
        draft.termTypes.getOrNull(index) != null
    ) {
        draft.termTypes[index]
    } else {
        draft.currentType
    }
}

@Composable
internal fun typeHeader(draft: CalcDraft): String {
    val parts = draft.expr.raw.split('+')
    val last = parts.last()
    return when {
        last.isNotEmpty() -> stringResource(Res.string.combat_calc_type_for_term, last)
        parts.size > 1 -> stringResource(Res.string.combat_calc_type_for_next)
        else -> stringResource(Res.string.combat_calc_type_title)
    }
}

@Composable
internal fun typeLabel(draft: CalcDraft): String =
    currentType(draft)?.let { damageTypeLabels().getValue(it).lowercase() }
        ?: stringResource(Res.string.combat_calc_type_none)

internal fun termChips(draft: CalcDraft): List<TermChip> {
    val parts = draft.expr.raw.split('+')
    return parts.mapIndexedNotNull { index, text ->
        if (text.isEmpty()) {
            null
        } else {
            TermChip(
                text,
                draft.termTypes.getOrNull(index),
                current =
                    index == parts.lastIndex,
            )
        }
    }
}
