package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.damage.PlannedHit
import com.lyrismet.incadent.feature.combat.domain.damage.parseTerms
import com.lyrismet.incadent.feature.combat.domain.damage.planHit
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_manual
import dndplayerscodex.shared.generated.resources.combat_calc_quick
import dndplayerscodex.shared.generated.resources.combat_damage_state_immunity
import dndplayerscodex.shared.generated.resources.combat_damage_state_resistance
import dndplayerscodex.shared.generated.resources.combat_damage_state_vulnerability
import org.jetbrains.compose.resources.stringResource

private val ScaleOrder = listOf(HitScale.ONE, HitScale.HALF, HitScale.ZERO, HitScale.DOUBLE)

private fun HitScale.label(): String =
    when (this) {
        HitScale.ONE -> "×1"
        HitScale.HALF -> "½"
        HitScale.ZERO -> "0"
        HitScale.DOUBLE -> "×2"
    }

@Composable
internal fun calcState(
    draft: CalcDraft,
    ctx: CalcContext,
    round: Int,
): CalcState {
    val damage = draft.mode == CalcMode.DAMAGE
    val targets = ctx.ordered.filter { it.id in draft.targets }
    val terms = parseTerms(draft.expr.raw, draft.termTypes)
    val sum = terms.sumOf { it.amount }
    val rowMode = damage && (targets.size == 1 || draft.collapsed)
    val selected = selectedTargetId(draft, targets)
    val items = targets.map { calcTarget(it, draft, ctx, terms, sum, selected == it.id, damage) }
    return CalcState(
        mode = draft.mode,
        modes = modesFor(targets),
        overline = overline(draft, ctx, round, sourceName(draft.sourceId, ctx)),
        title = stringResource(titleOf(draft.mode)),
        sources = sourceChips(draft, ctx),
        sourceHint = sourceHint(draft, ctx),
        done = draft.done,
        targetsHeader = targetsHeader(targets.size),
        reaction = if (damage) reactionOf(targets, ctx) else null,
        targets = items,
        offTargets = offTargetsOf(draft, ctx),
        rowMode = rowMode,
        selectedTarget = if (!rowMode) items.firstOrNull { it.selected } else null,
        typeHeader = typeHeader(draft),
        typeLabel = typeLabel(draft),
        types = DamageType.entries.map { TypeChip(it, it == currentType(draft)) },
        terms = termChips(draft),
        result = sum.toString(),
        preview = if (damage) emptyList() else previewLines(draft, targets, sum),
        keysShown = !(damage && draft.collapsed),
        canMore = damage && targets.isNotEmpty() && sum > 0,
        applyLabel = applyLabel(draft, targets.size, sum),
        applyEnabled = canApply(draft.mode, targets, sum),
    )
}

/** a lone target is always the chosen one, among several it is the one pressed */
private fun selectedTargetId(
    draft: CalcDraft,
    targets: List<Combatant>,
): Long? =
    if (targets.size == 1) targets.first().id else draft.selectedTarget?.takeIf { id -> targets.any { it.id == id } }

/** temporary hit points need counted ones, so a guessed target takes damage and healing only */
private fun modesFor(targets: List<Combatant>): List<CalcMode> =
    if (targets.any { !it.isTracked() }) listOf(CalcMode.DAMAGE, CalcMode.HEAL) else CalcMode.entries.toList()

private fun offTargetsOf(
    draft: CalcDraft,
    ctx: CalcContext,
): List<OffTarget> =
    ctx.ordered.filter { it.id !in draft.targets && !it.isDown() && !it.dead }.map { OffTarget(it.id, it.name) }

private fun canApply(
    mode: CalcMode,
    targets: List<Combatant>,
    sum: Int,
): Boolean = targets.isNotEmpty() && sum > 0 && (mode != CalcMode.TEMP || targets.all { it.isTracked() })

@Composable
private fun calcTarget(
    member: Combatant,
    draft: CalcDraft,
    ctx: CalcContext,
    terms: List<com.lyrismet.incadent.feature.combat.domain.damage.DamageTerm>,
    sum: Int,
    selected: Boolean,
    damage: Boolean,
): CalcTarget {
    val scale = draft.scales[member.id] ?: HitScale.ONE
    val hit = planHit(terms, scale, ctx.resistances[member.id].orEmpty())
    val parts = if (damage) partsOf(hit, scale) else emptyList()
    val shown = damage && (sum > 0 || scale == HitScale.ZERO)
    return CalcTarget(
        id = member.id,
        name = member.name,
        side = member.rowSide(),
        scale = scale,
        shield = hit.terms.firstOrNull { it.effect != HitEffect.FULL }?.let { CalcShield(it.term.type, it.effect) },
        result = if (shown) hit.total.toString() else null,
        parts = parts,
        selected = selected,
        scales = ScaleOrder.map { ScaleOption(it, it.label(), it == scale) },
        quick = if (selected) quickOf(member, scale, hit, ctx) else null,
    )
}

@Composable
private fun partsOf(
    hit: PlannedHit,
    scale: HitScale,
): List<CalcPart> {
    val parts =
        hit.terms.mapIndexed { index, planned ->
            val raw = planned.term.amount
            val text =
                when (planned.effect) {
                    HitEffect.HALF -> "$raw ½ → ${planned.effective}"
                    HitEffect.NONE -> "$raw → 0"
                    HitEffect.DOUBLE -> "$raw ×2 → ${planned.effective}"
                    HitEffect.FULL -> raw.toString()
                }
            CalcPart(if (index > 0) "+ " else "", planned.term.type, text)
        }
    return if (scale ==
        HitScale.ONE
    ) {
        parts
    } else {
        parts +
            CalcPart("", null, stringResource(Res.string.combat_calc_manual, scale.label()))
    }
}

@Composable
private fun quickOf(
    member: Combatant,
    scale: HitScale,
    hit: PlannedHit,
    ctx: CalcContext,
): QuickResistance? {
    if (scale == HitScale.ONE || member.id !in ctx.learners) return null
    val type =
        hit.terms
            .map { it.term.type }
            .distinct()
            .singleOrNull()
    val state = stateOf(scale)
    val known = type?.let { ctx.resistances[member.id]?.get(it) }
    val unknown = known == null || known == DamageState.UNKNOWN
    return if (type == null || state == null || !unknown) {
        null
    } else {
        val label =
            stringResource(
                Res.string.combat_calc_quick,
                stateWord(state),
                damageTypeLabels().getValue(type).lowercase(),
            )
        QuickResistance(label, type, state)
    }
}

internal fun stateOf(scale: HitScale): DamageState? =
    when (scale) {
        HitScale.HALF -> DamageState.RESISTANCE
        HitScale.ZERO -> DamageState.IMMUNITY
        HitScale.DOUBLE -> DamageState.VULNERABILITY
        HitScale.ONE -> null
    }

@Composable
internal fun stateWord(state: DamageState): String =
    stringResource(
        when (state) {
            DamageState.RESISTANCE -> Res.string.combat_damage_state_resistance
            DamageState.IMMUNITY -> Res.string.combat_damage_state_immunity
            DamageState.VULNERABILITY -> Res.string.combat_damage_state_vulnerability
            else -> Res.string.combat_damage_state_resistance
        },
    )
