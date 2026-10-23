package com.lyrismet.incadent.feature.combat.presentation.results

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
import com.lyrismet.incadent.feature.combat.domain.results.CodexItem
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime

/** the session the results went to and the one step that takes every codex change back */
internal class SavedResults(
    val sessionId: Long,
    val undo: suspend () -> Unit,
)

/** puts the ticked results into the codex, closes the fight under a session and keeps the way back */
internal class ResultsSaver(
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val hitObservations: HitObservationRepository,
    private val codex: CodexRepositories,
) {
    @Suppress("LongParameterList")
    suspend fun save(
        combat: Combat,
        outcome: CombatOutcome,
        ticked: List<CodexItem>,
        newSessionTitle: String,
        now: LocalDateTime,
    ): SavedResults {
        val rollbacks = mutableListOf<suspend () -> Unit>()
        writePartyHp(combat, rollbacks)
        ticked.forEach { apply(it, combat.id, rollbacks) }
        val sessionId = sessionFor(combat, newSessionTitle, now)
        combatRepository.attachToSession(combat.id, sessionId)
        combatRepository.finish(combat.id, outcome, now)
        return SavedResults(sessionId) { rollbacks.asReversed().forEach { it() } }
    }

    private suspend fun sessionFor(
        combat: Combat,
        title: String,
        now: LocalDateTime,
    ): Long =
        combat.sessionNoteId
            ?: sessionNoteRepository
                .observeAll()
                .first()
                .firstOrNull { it.isLive }
                ?.id
            ?: newSession(title, now)

    private suspend fun newSession(
        title: String,
        now: LocalDateTime,
    ): Long {
        val id = sessionNoteRepository.upsert(SessionNote(0, title, now, null))
        sessionNoteRepository.makeOnlyLive(id, now)
        return id
    }

    private suspend fun writePartyHp(
        combat: Combat,
        rollbacks: MutableList<suspend () -> Unit>,
    ) {
        combat.combatants
            .filter { it.side == CombatSide.PARTY && it.hpMax != null && it.hpCurrent != null }
            .forEach { member ->
                val before = member.partyMemberId?.let { codex.partyRepository.getById(it) } ?: return@forEach
                val now = member.hpCurrent?.coerceIn(0, member.hpMax ?: return@forEach) ?: return@forEach
                codex.partyRepository.upsert(before.copy(hpMax = member.hpMax, hpCurrent = now))
                rollbacks += { codex.partyRepository.upsert(before) }
            }
    }

    private suspend fun apply(
        item: CodexItem,
        combatId: Long,
        rollbacks: MutableList<suspend () -> Unit>,
    ) {
        when (item) {
            is CodexItem.HitObserved -> {
                val draft = item.draft
                val id = hitObservations.add(draft.speciesId, combatId, SpeciesHitObservation(draft.damage, draft.fell))
                rollbacks += { hitObservations.remove(id) }
            }
            is CodexItem.MarkPartyFallen -> {
                val before = codex.partyRepository.getById(item.partyMemberId) ?: return
                codex.partyRepository.setFallen(before.id, true, combatId)
                rollbacks += { codex.partyRepository.setFallen(before.id, before.fallen, before.fellCombatId) }
            }
            is CodexItem.MarkDead -> markDead(item.npcId, rollbacks)
            is CodexItem.MarkFallen -> markDead(item.npcId, rollbacks)
            is CodexItem.Resistance -> {
                val guess = item.guess
                val before = codex.speciesRepository.getById(guess.speciesId) ?: return
                codex.speciesRepository.upsert(before.copy(damage = before.damage + (guess.type to guess.state)))
                rollbacks += {
                    codex.speciesRepository.getById(guess.speciesId)?.let { current ->
                        val old = before.damage[guess.type]
                        val restored =
                            if (old ==
                                null
                            ) {
                                current.damage - guess.type
                            } else {
                                current.damage + (guess.type to old)
                            }
                        codex.speciesRepository.upsert(current.copy(damage = restored))
                    }
                }
            }
            is CodexItem.KeepAlly -> {
                val npc = Npc(0, item.name, NpcStatus.FRIEND, NpcLifeState.ALIVE, "", emptyList(), "")
                val id = codex.npcRepository.upsert(npc)
                rollbacks += { codex.npcRepository.delete(id) }
            }
        }
    }

    private suspend fun markDead(
        npcId: Long,
        rollbacks: MutableList<suspend () -> Unit>,
    ) {
        val before = codex.npcRepository.getById(npcId) ?: return
        codex.npcRepository.upsert(before.copy(lifeState = NpcLifeState.DEAD))
        rollbacks +=
            {
                codex.npcRepository
                    .getById(
                        npcId,
                    )?.let { codex.npcRepository.upsert(it.copy(lifeState = before.lifeState)) }
            }
    }
}
