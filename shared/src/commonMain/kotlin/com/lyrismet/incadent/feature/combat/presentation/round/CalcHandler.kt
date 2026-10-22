package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.damage.parseTerms
import com.lyrismet.incadent.feature.combat.domain.damage.planHit
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.Strike
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_done_many
import dndplayerscodex.shared.generated.resources.combat_calc_done_one
import dndplayerscodex.shared.generated.resources.combat_calc_recorded_sub
import dndplayerscodex.shared.generated.resources.combat_calc_recorded_title
import org.jetbrains.compose.resources.getString

/** what the calculator may use of the fight at the moment of an event */
internal class CalcSnapshot(
    val ordered: List<Combatant>,
    val moverId: Long?,
    val resistances: Map<Long, Map<DamageType, DamageState>>,
    val speciesOf: Map<Long, Species>,
    // the words for a damage type and for what a hit proved about it, ready to put in a message
    val typeNames: Map<DamageType, String>,
    val stateTitles: Map<DamageState, String>,
)

internal class CalcHandler(
    private val editor: RoundEditor,
    private val calc: MutableState<CalcDraft?>,
    private val learner: SpeciesLearner,
) {
    fun onEvent(
        event: CalcEvent,
        snapshot: CalcSnapshot,
        chosenId: Long?,
    ) {
        val draft = calc.value
        when (event) {
            is CalcEvent.Opened -> calc.value = open(event.mode, snapshot, chosenId)
            CalcEvent.Closed -> calc.value = null
            else -> if (draft != null) onDraft(event, draft, snapshot)
        }
    }

    private fun open(
        mode: CalcMode,
        snapshot: CalcSnapshot,
        chosenId: Long?,
    ): CalcDraft {
        // a blow on yourself is rarer than one on somebody else, so the mover starts without a target
        val targets =
            if (mode == CalcMode.DAMAGE &&
                chosenId == snapshot.moverId
            ) {
                emptyList()
            } else {
                listOfNotNull(chosenId)
            }
        return CalcDraft(mode = mode, targets = targets, sourceId = snapshot.moverId)
    }

    private fun onDraft(
        event: CalcEvent,
        draft: CalcDraft,
        snapshot: CalcSnapshot,
    ) {
        when (event) {
            is CalcEvent.ModeSelected ->
                calc.value =
                    draft.copy(mode = event.mode, collapsed = false, selectedTarget = null)
            is CalcEvent.SourceSelected -> calc.value = draft.copy(sourceId = event.id)
            CalcEvent.CollapseToggled ->
                if (draft.mode ==
                    CalcMode.DAMAGE
                ) {
                    calc.value = draft.copy(collapsed = !draft.collapsed)
                }
            CalcEvent.QuickRecorded -> recordQuick(draft, snapshot)
            is CalcEvent.ReactionToggled ->
                editor.withMember(
                    event.memberId,
                ) { _, m -> editor.actions.toggleReaction(m) }
            CalcEvent.MoreHit -> apply(draft, snapshot, more = true)
            CalcEvent.Applied -> apply(draft, snapshot, more = false)
            else -> calc.value = CalcEditing.edit(draft, event)
        }
    }

    private fun apply(
        draft: CalcDraft,
        snapshot: CalcSnapshot,
        more: Boolean,
    ) {
        val terms = parseTerms(draft.expr.raw, draft.termTypes)
        val amount = terms.sumOf { it.amount }
        if (draft.targets.isEmpty() || amount <= 0) return
        editor.edit { combat ->
            val targets = draft.targets.mapNotNull { id -> combat.combatants.firstOrNull { it.id == id } }
            when (draft.mode) {
                CalcMode.DAMAGE -> {
                    val strikes =
                        targets.map {
                            Strike(
                                it,
                                planHit(
                                    terms,
                                    draft.scales[it.id] ?: HitScale.ONE,
                                    snapshot.resistances[it.id].orEmpty(),
                                ),
                            )
                        }
                    editor.damage.damage(combat, draft.sourceId, strikes)
                    val dealt = strikes.sumOf { it.hit.total }
                    calc.value = if (more) nextHit(draft, targets, dealt) else null
                }
                CalcMode.HEAL -> editor.damage.heal(combat, draft.sourceId, targets, amount)
                CalcMode.TEMP -> editor.damage.giveTemp(combat, draft.sourceId, targets, amount)
            }
            if (draft.mode != CalcMode.DAMAGE) calc.value = null
        }
    }

    /** the same calculator for the next blow: the sum is empty again and those who went down are no longer targets */
    private suspend fun nextHit(
        draft: CalcDraft,
        targets: List<Combatant>,
        dealt: Int,
    ): CalcDraft {
        val label =
            if (targets.size > 1) {
                getString(Res.string.combat_calc_done_many, draft.hit, targets.size)
            } else {
                getString(Res.string.combat_calc_done_one, draft.hit, targets.first().name)
            }
        val fresh = editor.fresh()?.combatants.orEmpty()
        val standing =
            draft.targets.filter { id ->
                fresh.firstOrNull { it.id == id }?.let { !it.isDown() && !it.dead } ==
                    true
            }
        return draft.copy(
            expr = draft.expr.clear(),
            termTypes = emptyList(),
            scales = emptyMap(),
            selectedTarget = null,
            hit = draft.hit + 1,
            done = draft.done + DoneHit(label, dealt),
            targets = standing,
        )
    }

    /** what "it is a resistance" would write: the species, the type, the answer and who it was learned from */
    private class QuickPlan(
        val targetId: Long,
        val scale: HitScale,
        val species: Species,
        val type: DamageType,
        val state: DamageState,
    )

    private fun planQuick(
        draft: CalcDraft,
        snapshot: CalcSnapshot,
    ): QuickPlan? {
        val target = quickTarget(draft) ?: return null
        val species = snapshot.speciesOf[target.first]
        val type = parseTerms(draft.expr.raw, draft.termTypes).mapNotNull { it.type }.distinct().singleOrNull()
        val state = stateOf(target.second)
        return if (species != null &&
            type != null &&
            state != null
        ) {
            QuickPlan(target.first, target.second, species, type, state)
        } else {
            null
        }
    }

    /** the target whose answer was given by hand: the chosen one, or the only one */
    private fun quickTarget(draft: CalcDraft): Pair<Long, HitScale>? {
        val id = draft.selectedTarget ?: draft.targets.singleOrNull()
        val scale = id?.let { draft.scales[it] }
        return if (id != null && scale != null) id to scale else null
    }

    private fun recordQuick(
        draft: CalcDraft,
        snapshot: CalcSnapshot,
    ) {
        val plan = planQuick(draft, snapshot) ?: return
        val name =
            snapshot.ordered
                .firstOrNull { it.id == plan.targetId }
                ?.name
                .orEmpty()
        editor.edit {
            val note =
                UndoNote(
                    getString(
                        Res.string.combat_calc_recorded_title,
                        snapshot.stateTitles.getValue(plan.state),
                        snapshot.typeNames.getValue(plan.type),
                    ),
                    getString(Res.string.combat_calc_recorded_sub, name),
                )
            learner.record(plan.species.id, plan.type, plan.state, note)
        }
        val sameKind =
            draft.targets.filter {
                snapshot.speciesOf[it]?.id == plan.species.id &&
                    draft.scales[it] == plan.scale
            }
        calc.value = draft.copy(scales = draft.scales - sameKind.toSet())
    }
}
