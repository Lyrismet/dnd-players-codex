package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.core.undo.UndoFollowUp
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.domain.round.NpcPromotion
import com.lyrismet.incadent.feature.combat.domain.round.PromotionTrace
import com.lyrismet.incadent.feature.combat.domain.round.damageTaken
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_name_linked_title
import incadent.shared.generated.resources.combat_name_made_title
import incadent.shared.generated.resources.combat_name_more
import incadent.shared.generated.resources.combat_round_took
import org.jetbrains.compose.resources.getString

/** what is being typed over a participant */
internal data class NameDraft(
    val memberId: Long,
    val text: String,
)

/** a participant that was just named, waiting for the master to look at what the naming did */
internal data class PromotionDraft(
    val trace: PromotionTrace,
    val name: String,
    val isNew: Boolean,
)

/** the species of the codex, as the naming reads them */
internal class NamingSnapshot(
    val species: List<Species>,
)

/** names a participant of a species with a name of the codex, in the middle of the fight */
@Suppress("LongParameterList")
internal class NamingHandler(
    private val editor: RoundEditor,
    private val selected: MutableState<Long?>,
    private val typed: MutableState<NameDraft?>,
    private val promotion: MutableState<PromotionDraft?>,
    private val promotions: NpcPromotion,
    private val undo: UndoController,
) {
    fun onEvent(
        event: CombatRoundEvent,
        snapshot: NamingSnapshot,
    ) {
        when (event) {
            is CombatRoundEvent.NameChanged -> selected.value?.let { typed.value = NameDraft(it, event.text) }
            is CombatRoundEvent.NameChosen -> selected.value?.let { choose(it, event.choice, snapshot) }
            CombatRoundEvent.PromotionConfirmed -> promotion.value?.let { confirm(it, snapshot) }
            CombatRoundEvent.PromotionRejected -> promotion.value?.let(::reject)
            else -> Unit
        }
    }

    private fun choose(
        memberId: Long,
        choice: NameChoice,
        snapshot: NamingSnapshot,
    ) = editor.withMember(memberId) { combat, member ->
        typed.value = null
        val trace = promotions.promote(member, choice)
        if (trace == null) return@withMember
        val species = snapshot.species.firstOrNull { it.id == member.speciesId }
        val draft = PromotionDraft(trace, namedAs(choice), choice is NameChoice.New)
        val title = madeTitle(draft)
        val sub = subtitle(combat, member, species)
        val more = UndoFollowUp(getString(Res.string.combat_name_more)) { promotion.value = draft }
        undo.showMade(title, sub, more) { promotions.undo(trace) }
    }

    private fun confirm(
        draft: PromotionDraft,
        snapshot: NamingSnapshot,
    ) {
        promotion.value = null
        editor.withMember(draft.trace.memberId) { combat, member ->
            val species = snapshot.species.firstOrNull { it.id == member.speciesId }
            undo.show(madeTitle(draft), subtitle(combat, member, species)) {
                promotions.undo(draft.trace)
            }
        }
    }

    private fun reject(draft: PromotionDraft) {
        promotion.value = null
        editor.edit { promotions.undo(draft.trace) }
    }

    private suspend fun madeTitle(draft: PromotionDraft) =
        getString(
            if (draft.isNew) Res.string.combat_name_made_title else Res.string.combat_name_linked_title,
            draft.name,
        )

    private suspend fun subtitle(
        combat: Combat,
        member: Combatant,
        species: Species?,
    ): String {
        val taken = combat.damageTaken(member)
        return listOfNotNull(
            species?.name?.lowercase(),
            if (taken > 0) getString(Res.string.combat_round_took, taken) else null,
        ).joinToString(" · ")
    }

    private fun namedAs(choice: NameChoice) =
        when (choice) {
            is NameChoice.New -> choice.name
            is NameChoice.Existing -> choice.name
            is NameChoice.Plain -> choice.name
        }
}
