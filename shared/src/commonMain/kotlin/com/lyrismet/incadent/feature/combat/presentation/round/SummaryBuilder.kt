package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.damage.effectiveDamage
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isTracked
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_source_environment
import dndplayerscodex.shared.generated.resources.combat_summary_hit
import dndplayerscodex.shared.generated.resources.combat_summary_option_immunity
import dndplayerscodex.shared.generated.resources.combat_summary_option_normal
import dndplayerscodex.shared.generated.resources.combat_summary_option_resistance
import dndplayerscodex.shared.generated.resources.combat_summary_option_vulnerability
import dndplayerscodex.shared.generated.resources.combat_summary_pop_title
import dndplayerscodex.shared.generated.resources.combat_summary_record_species
import dndplayerscodex.shared.generated.resources.combat_summary_state_down
import dndplayerscodex.shared.generated.resources.combat_summary_state_hp
import dndplayerscodex.shared.generated.resources.combat_summary_title
import dndplayerscodex.shared.generated.resources.combat_summary_title_now
import dndplayerscodex.shared.generated.resources.combat_wound_dying
import org.jetbrains.compose.resources.stringResource

/** the damage of one round, target by target and blow by blow */
@Composable
internal fun summaryState(
    draft: SummaryDraft,
    combat: Combat,
    learners: Set<Long>,
): SummaryState {
    val shown = (draft.round ?: combat.round).coerceIn(1, combat.round)
    val damage = combat.events.filter { it.kind == CombatEventKind.DAMAGE }
    val inRound = damage.filter { it.round == shown }
    val env = stringResource(Res.string.combat_calc_source_environment)
    val typeNames = damageTypeLabels()
    return SummaryState(
        title =
            if (shown == combat.round) {
                stringResource(Res.string.combat_summary_title_now, shown)
            } else {
                stringResource(Res.string.combat_summary_title, shown)
            },
        rounds =
            (combat.round downTo 1).map { n ->
                val sum = damage.filter { it.round == n }.sumOf { it.amount }
                SummaryRound(n, if (sum > 0) "−$sum" else "—", n == shown)
            },
        targets =
            inRound
                .map { it.targetId }
                .distinct()
                .mapNotNull { id -> combat.combatants.firstOrNull { it.id == id } }
                .map { target ->
                    val own = inRound.filter { it.targetId == target.id }
                    SummaryTarget(
                        name = target.name,
                        side = target.rowSide(),
                        sum = "−${own.sumOf { it.amount }}",
                        state = targetState(target),
                        hits = hitsOf(own, combat, target, draft, learners, env, typeNames),
                    )
                },
        recordLabel = stringResource(Res.string.combat_summary_record_species),
        record = draft.record,
    )
}

@Composable
private fun targetState(target: Combatant): String =
    when {
        target.isDown() && target.side == CombatSide.PARTY -> stringResource(Res.string.combat_wound_dying).lowercase()
        target.isDown() -> stringResource(Res.string.combat_summary_state_down)
        target.isTracked() ->
            stringResource(
                Res.string.combat_summary_state_hp,
                target.hpCurrent ?: checkNotNull(target.hpMax),
                checkNotNull(target.hpMax),
            )
        else -> woundWord(target).first.lowercase()
    }

@Composable
private fun hitsOf(
    events: List<CombatEvent>,
    combat: Combat,
    target: Combatant,
    draft: SummaryDraft,
    learners: Set<Long>,
    env: String,
    typeNames: Map<com.lyrismet.incadent.feature.codex.domain.model.DamageType, String>,
): List<SummaryHit> {
    val sequence = mutableMapOf<Long?, Int>()
    return events.groupBy { it.hit }.values.map { terms ->
        val first = terms.first()
        val number = (sequence[first.sourceId] ?: 0) + 1
        sequence[first.sourceId] = number
        val source = first.sourceId?.let { id -> combat.combatants.firstOrNull { it.id == id }?.name } ?: env
        val open = terms.firstOrNull { it.id == draft.openEventId }
        SummaryHit(
            source = stringResource(Res.string.combat_summary_hit, source, number),
            chips = terms.map { chipOf(it, it.id == draft.openEventId) },
            scaleLabel = first.scale.takeIf { it != HitScale.ONE }?.let { scaleLabel(it) },
            total = terms.sumOf { it.amount }.toString(),
            popup = open?.let { popupOf(it, target, typeNames) },
            canRecord = target.side != CombatSide.PARTY && target.id in learners,
        )
    }
}

private fun chipOf(
    event: CombatEvent,
    open: Boolean,
): SummaryChip {
    val changed = event.effect != HitEffect.FULL
    return SummaryChip(
        eventId = event.id,
        type = event.damageType,
        old = if (changed) event.raw.toString() else null,
        now = effectiveDamage(event.raw, HitScale.ONE, event.effect).toString(),
        effect = event.effect,
        changed = changed,
        tappable = event.damageType != null,
        open = open,
    )
}

private fun scaleLabel(scale: HitScale): String =
    when (scale) {
        HitScale.HALF -> "½"
        HitScale.ZERO -> "0"
        HitScale.DOUBLE -> "×2"
        HitScale.ONE -> ""
    }

@Composable
private fun popupOf(
    event: CombatEvent,
    target: Combatant,
    typeNames: Map<com.lyrismet.incadent.feature.codex.domain.model.DamageType, String>,
): SummaryPopup {
    val typeName = event.damageType?.let { typeNames.getValue(it) }.orEmpty()
    return SummaryPopup(
        title = stringResource(Res.string.combat_summary_pop_title, typeName, target.name),
        eventId = event.id,
        options =
            listOf(
                SummaryOption(
                    HitEffect.FULL,
                    stringResource(Res.string.combat_summary_option_normal),
                    event.effect == HitEffect.FULL,
                ),
                SummaryOption(
                    HitEffect.HALF,
                    stringResource(Res.string.combat_summary_option_resistance),
                    event.effect == HitEffect.HALF,
                ),
                SummaryOption(
                    HitEffect.NONE,
                    stringResource(Res.string.combat_summary_option_immunity),
                    event.effect == HitEffect.NONE,
                ),
                SummaryOption(
                    HitEffect.DOUBLE,
                    stringResource(Res.string.combat_summary_option_vulnerability),
                    event.effect == HitEffect.DOUBLE,
                ),
            ),
    )
}
