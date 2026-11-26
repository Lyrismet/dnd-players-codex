package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.grouping.withFact
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.entityWriteLock
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_features_added
import incadent.shared.generated.resources.combat_features_undo
import incadent.shared.generated.resources.features_removed_title
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/** the sheet of the features of the chosen participant: the facts written to its record, or to the fight without one */
internal class FeaturesHandler(
    private val editor: RoundEditor,
    private val selected: MutableState<Long?>,
    private val draft: MutableState<FeaturesDraft?>,
    private val npcRepository: NpcRepository,
    private val own: OwnFeatures,
    private val undo: UndoController,
) {
    fun onEvent(event: CombatRoundEvent.FeaturesEvent) {
        val current = draft.value
        when (event) {
            is CombatRoundEvent.FeaturesOpened ->
                selected.value?.let {
                    draft.value =
                        FeaturesDraft(
                            it,
                            text = event.fact.orEmpty(),
                            editing = event.fact,
                        )
                }
            is CombatRoundEvent.FactTextChanged -> draft.value = current?.copy(text = event.text)
            CombatRoundEvent.FeaturesClosed -> draft.value = null
            CombatRoundEvent.FactAdded -> current?.let { add(it) }
            is CombatRoundEvent.FactRemoved -> current?.let { write(it.memberId, event.fact, null) }
            is CombatRoundEvent.ShieldTapped,
            CombatRoundEvent.ShieldsEditStarted,
            CombatRoundEvent.ShieldsEditDone,
            -> Unit
        }
    }

    private fun add(d: FeaturesDraft) {
        val text = d.text.trim()
        if (text.isEmpty()) return
        draft.value = d.copy(text = "", editing = null)
        write(d.memberId, d.editing, text)
    }

    /** takes [removed] out of the facts and puts [added] in, a fact written to a record can be taken back */
    private fun write(
        memberId: Long,
        removed: String?,
        added: String?,
    ) = editor.withMember(memberId) { _, member ->
        val before = own.facts(member)
        val after = change(before, removed, added)
        if (after == before) return@withMember
        own.writeFacts(member, after)
        val npcId = member.npcId
        if (removed != null && added == null) tellRemoved(member.id, removed, before, after)
        if (added != null &&
            npcId != null
        ) {
            tellAdded(npcId, npcRepository.getById(npcId)?.name ?: member.name, before, after)
        }
    }

    private suspend fun tellRemoved(
        memberId: Long,
        fact: String,
        before: List<String>,
        after: List<String>,
    ) = undo.show(getString(Res.string.features_removed_title), fact) {
        editor.withMember(memberId) { _, member ->
            val now = own.facts(member)
            own.writeFacts(member, if (now == after) before else now.withFact(fact))
        }
    }

    private suspend fun tellAdded(
        npcId: Long,
        name: String,
        before: List<String>,
        after: List<String>,
    ) = undo.show(getString(Res.string.combat_features_added, name), getString(Res.string.combat_features_undo)) {
        val added = after.filterNot { it in before }
        entityWriteLock.withLock {
            npcRepository.getById(npcId)?.let {
                npcRepository.upsert(
                    it.copy(
                        facts =
                            it.facts.filterNot { f ->
                                f in
                                    added
                            },
                    ),
                )
            }
        }
    }

    private fun change(
        facts: List<String>,
        removed: String?,
        added: String?,
    ): List<String> {
        val without = if (removed == null) facts else facts.filterNot { it == removed }
        return if (added == null) without else without.withFact(added)
    }
}
