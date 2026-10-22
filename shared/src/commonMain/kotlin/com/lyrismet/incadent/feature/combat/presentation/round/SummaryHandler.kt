package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_summary_undo_back
import dndplayerscodex.shared.generated.resources.combat_summary_undo_recorded
import dndplayerscodex.shared.generated.resources.combat_summary_undo_same
import dndplayerscodex.shared.generated.resources.combat_summary_undo_title
import dndplayerscodex.shared.generated.resources.combat_summary_undo_took
import org.jetbrains.compose.resources.getString

internal class SummaryHandler(
    private val editor: RoundEditor,
    private val summary: MutableState<SummaryDraft?>,
    private val learner: SpeciesLearner,
) {
    fun onEvent(
        event: SummaryEvent,
        snapshot: SummarySnapshot,
    ) {
        val draft = summary.value
        when (event) {
            SummaryEvent.Opened -> summary.value = SummaryDraft()
            SummaryEvent.Closed -> summary.value = null
            is SummaryEvent.RoundSelected -> summary.value = draft?.copy(round = event.number, openEventId = null)
            is SummaryEvent.TermTapped ->
                summary.value =
                    draft?.copy(openEventId = if (draft.openEventId == event.eventId) null else event.eventId)
            SummaryEvent.RecordToggled -> summary.value = draft?.copy(record = !draft.record)
            is SummaryEvent.EffectSelected -> draft?.let { correct(it, event, snapshot) }
        }
    }

    /** the resistance was named after the blow: the term is recounted, the answer may go to the bestiary */
    private fun correct(
        draft: SummaryDraft,
        event: SummaryEvent.EffectSelected,
        snapshot: SummarySnapshot,
    ) = editor.edit { combat ->
        val term = combat.events.firstOrNull { it.id == event.eventId }
        val target = term?.let { t -> combat.combatants.firstOrNull { it.id == t.targetId } }
        if (term != null && target != null && term.effect != event.effect) {
            val delta = editor.damage.correct(combat, term.id, event.effect)
            summary.value = draft.copy(openEventId = null)
            val species = snapshot.speciesOf[target.id]
            val type = term.damageType
            val record = draft.record && species != null && type != null && target.side != CombatSide.PARTY
            val note = describe(Correction(target.name, event.effect, type, delta, record), snapshot)
            val previous = term.effect
            val revert: suspend () -> Unit = { editor.fresh()?.let { editor.damage.correct(it, term.id, previous) } }
            if (record && species != null && type != null) {
                learner.record(species.id, type, event.effect.toDamageState(), note, revert)
            } else {
                learner.offerUndo(note, revert)
            }
        }
    }

    private suspend fun describe(
        correction: Correction,
        snapshot: SummarySnapshot,
    ): UndoNote {
        val delta = correction.delta
        val change =
            when {
                delta < 0 -> getString(Res.string.combat_summary_undo_back, -delta)
                delta > 0 -> getString(Res.string.combat_summary_undo_took, delta)
                else -> getString(Res.string.combat_summary_undo_same)
            }
        val title =
            getString(
                Res.string.combat_summary_undo_title,
                correction.name,
                snapshot.effectWords.getValue(correction.effect),
                correction.type?.let { snapshot.typeNames.getValue(it) }.orEmpty(),
            )
        return UndoNote(
            title,
            if (correction.recorded) getString(Res.string.combat_summary_undo_recorded, change) else change,
        )
    }
}

/** the blow whose resistance was named, as the bar that offers to take it back tells it */
private class Correction(
    val name: String,
    val effect: HitEffect,
    val type: DamageType?,
    val delta: Int,
    val recorded: Boolean,
)
