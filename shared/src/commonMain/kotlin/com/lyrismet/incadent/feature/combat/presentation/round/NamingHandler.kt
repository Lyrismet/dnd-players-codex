package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.core.undo.UndoFollowUp
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.domain.round.NpcPromotion
import com.lyrismet.incadent.feature.combat.domain.round.PromotionTrace
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_name_linked_title
import dndplayerscodex.shared.generated.resources.combat_name_made_title
import dndplayerscodex.shared.generated.resources.combat_name_more
import dndplayerscodex.shared.generated.resources.combat_name_plain_sub
import dndplayerscodex.shared.generated.resources.combat_name_plain_title
import dndplayerscodex.shared.generated.resources.combat_round_took
import kotlinx.coroutines.flow.first
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
    val learned: List<LearnedVulnerability>,
    val onlyNpc: Set<DamageType> = emptySet(),
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
    private val combats: CombatRepository,
    private val undo: UndoController,
) {
    fun onEvent(
        event: CombatRoundEvent,
        snapshot: NamingSnapshot,
    ) {
        when (event) {
            is CombatRoundEvent.NameChanged -> selected.value?.let { typed.value = NameDraft(it, event.text) }
            is CombatRoundEvent.NameChosen -> selected.value?.let { choose(it, event.choice, snapshot) }
            is CombatRoundEvent.PromotionScopeChanged ->
                promotion.value =
                    promotion.value?.let { draft ->
                        draft.copy(
                            onlyNpc =
                                if (event.onlyNpc) {
                                    draft.onlyNpc + event.type
                                } else {
                                    draft.onlyNpc -
                                        event.type
                                },
                        )
                    }
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
        if (trace == null) {
            plainCaption(member, choice)
            return@withMember
        }
        val species = snapshot.species.firstOrNull { it.id == member.speciesId }
        val learned = learnedOf(member, species, combats.observeLearned(editor.combatId).first())
        val draft = PromotionDraft(trace, namedAs(choice), choice is NameChoice.New, learned)
        if (learned.isNotEmpty()) {
            promotion.value = draft
        } else {
            val title = madeTitle(draft)
            val sub = subtitle(combat, member, species)
            val more = UndoFollowUp(getString(Res.string.combat_name_more)) { promotion.value = draft }
            undo.showMade(title, sub, more) { promotions.undo(trace) }
        }
    }

    private suspend fun plainCaption(
        member: Combatant,
        choice: NameChoice,
    ) {
        undo.show(
            getString(Res.string.combat_name_plain_title, member.name, namedAs(choice)),
            getString(Res.string.combat_name_plain_sub),
        ) { combats.renameCombatant(member.id, member.name) }
    }

    private fun confirm(
        draft: PromotionDraft,
        snapshot: NamingSnapshot,
    ) {
        promotion.value = null
        editor.withMember(draft.trace.memberId) { combat, member ->
            val back = promotions.moveLearned(editor.combatId, draft.trace, draft.learned, draft.onlyNpc)
            val species = snapshot.species.firstOrNull { it.id == member.speciesId }
            undo.show(madeTitle(draft), subtitle(combat, member, species)) {
                back()
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
