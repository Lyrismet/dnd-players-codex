package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.quickNpc
import com.lyrismet.incadent.feature.codex.domain.repository.quickSpecies
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.DamageActions
import com.lyrismet.incadent.feature.combat.domain.round.RoundActions
import com.lyrismet.incadent.feature.combat.domain.round.TurnPosition
import com.lyrismet.incadent.feature.combat.domain.round.inTurnOrder
import com.lyrismet.incadent.feature.combat.domain.round.nextTurn
import com.lyrismet.incadent.feature.combat.presentation.prep.FoeAdder
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_prep_toast_npc_created
import dndplayerscodex.shared.generated.resources.combat_prep_toast_species_created
import dndplayerscodex.shared.generated.resources.combat_round_toast_joined
import dndplayerscodex.shared.generated.resources.combat_round_toast_removed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/** the writes of a fight in progress, each reads the combat again under a lock so quick taps stay in order */
@Suppress("LongParameterList")
internal class RoundEditor(
    val combatId: Long,
    private val combatRepository: CombatRepository,
    private val codexRepositories: CodexRepositories,
    private val sessionNoteRepository: SessionNoteRepository,
    private val lock: Mutex,
    private val showToast: (String) -> Unit,
    private val launch: (suspend () -> Unit) -> Unit,
) : FoeAdder {
    val actions = RoundActions(combatRepository)
    val damage = DamageActions(combatRepository)

    /** the combat as it stands now, after the writes before it */
    suspend fun fresh(): Combat? = combatRepository.observe(combatId).first()

    fun edit(block: suspend (Combat) -> Unit) {
        launch { lock.withLock { combatRepository.observe(combatId).first()?.let { block(it) } } }
    }

    fun withMember(
        id: Long,
        block: suspend (Combat, Combatant) -> Unit,
    ) = edit { combat -> combat.combatants.firstOrNull { it.id == id }?.let { block(combat, it) } }

    /** the turn passes on, [onMoved] gets the one who moves now */
    fun next(
        ownIds: Set<Long>,
        onMoved: (Long?) -> Unit,
    ) = edit { combat ->
        val ordered = combat.combatants.inTurnOrder()
        actions.next(combat, ownIds)
        onMoved(ordered.getOrNull(nextTurn(ordered, TurnPosition(combat.round, combat.turn)).turn)?.id)
    }

    fun remove(
        id: Long,
        onRemoved: () -> Unit,
    ) = withMember(id) { combat, member ->
        actions.remove(combat, member)
        showToast(getString(Res.string.combat_round_toast_removed, member.name))
        onRemoved()
    }

    /** an enemy tied to an npc of the codex takes its new stance there too */
    fun setStance(
        id: Long,
        stance: CombatStance,
    ) = withMember(id) { combat, member ->
        actions.setStance(combat, member, stance)
        val status =
            when (stance) {
                CombatStance.ENEMY -> NpcStatus.ENEMY
                CombatStance.NEUTRAL -> NpcStatus.NEUTRAL
                CombatStance.FRIEND -> NpcStatus.FRIEND
                CombatStance.FLED -> null
            }
        val npc = member.npcId?.let { codexRepositories.npcRepository.getById(it) }
        if (npc != null && status != null) codexRepositories.npcRepository.upsert(npc.copy(status = status))
    }

    /** the fight is over: it goes under the running session when there is one and closes with [outcome] */
    fun finish(
        outcome: CombatOutcome,
        onDone: () -> Unit,
    ) {
        launch {
            val live = sessionNoteRepository.observeAll().first().firstOrNull { it.isLive }
            if (live != null) combatRepository.attachToSession(combatId, live.id)
            combatRepository.finish(combatId, outcome)
            onDone()
        }
    }

    private fun reinforce(
        side: CombatSide,
        source: MemberSource,
    ) = edit { combat ->
        if (actions.reinforce(
                combat,
                side,
                source,
            )
        ) {
            showToast(getString(Res.string.combat_round_toast_joined, source.baseName))
        }
    }

    override fun add(
        side: CombatSide,
        source: MemberSource,
    ) = reinforce(side, source)

    override fun toggle(
        side: CombatSide,
        source: MemberSource,
    ) = reinforce(side, source)

    // a reinforcement cannot be taken back out of the search, the panel has "Убрать из боя"
    override fun removeOne(
        side: CombatSide,
        source: MemberSource,
    ) = Unit

    override fun createNpc(
        side: CombatSide,
        name: String,
    ) = edit { combat ->
        val status = if (side == CombatSide.ENEMY) NpcStatus.ENEMY else NpcStatus.FRIEND
        val id = codexRepositories.npcRepository.upsert(quickNpc(name).copy(status = status))
        if (actions.reinforce(combat, side, MemberSource(id, null, name))) {
            showToast(getString(Res.string.combat_prep_toast_npc_created, name))
        }
    }

    override fun createSpecies(
        side: CombatSide,
        name: String,
    ) = edit { combat ->
        val id = codexRepositories.speciesRepository.upsert(quickSpecies(name))
        if (actions.reinforce(combat, side, MemberSource(null, id, name))) {
            showToast(getString(Res.string.combat_prep_toast_species_created, name))
        }
    }
}
