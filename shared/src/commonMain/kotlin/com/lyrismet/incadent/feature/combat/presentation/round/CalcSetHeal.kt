package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.ExactExpectation
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.hp_exact_as_heal
import incadent.shared.generated.resources.hp_exact_as_is
import incadent.shared.generated.resources.hp_exact_suggest
import org.jetbrains.compose.resources.stringResource

/** the healing nobody told that a typed number implies, none when it is not above what the told healing gives */
internal fun missingHeal(
    draft: CalcDraft,
    targets: List<Combatant>,
    expected: Map<Long, ExactExpectation>,
    sum: Int,
): Int {
    val known = targets.singleOrNull()?.let { expected[it.id] }
    return if (draft.mode != CalcMode.SET || draft.expr.raw.isEmpty() || known == null) {
        0
    } else {
        (sum - known.expected).coerceAtLeast(0)
    }
}

/** what the label of the apply button adds: the part of the number taken as healing, when the player chose that */
internal fun healPart(
    draft: CalcDraft,
    targets: List<Combatant>,
    ctx: CalcContext,
    sum: Int,
): Int = if (draft.asHeal) missingHeal(draft, targets, ctx.expected, sum) else 0

@Composable
internal fun setHealOf(
    draft: CalcDraft,
    targets: List<Combatant>,
    ctx: CalcContext,
    sum: Int,
): CalcSetHeal? {
    val diff = missingHeal(draft, targets, ctx.expected, sum)
    val known = targets.singleOrNull()?.let { ctx.expected[it.id] }
    return if (diff > 0 && known != null) {
        CalcSetHeal(
            suggestion = stringResource(Res.string.hp_exact_suggest, known.before, known.healed, known.expected, sum),
            asHeal = draft.asHeal,
            healLabel = stringResource(Res.string.hp_exact_as_heal, diff),
            plainLabel = stringResource(Res.string.hp_exact_as_is, sum),
        )
    } else {
        null
    }
}
