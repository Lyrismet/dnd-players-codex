package com.lyrismet.incadent.feature.combat.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.newOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import com.lyrismet.incadent.db.Combat as CombatRow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Suppress("TooManyFunctions")
class CombatRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : CombatRepository {
    private val combats = database.combatQueries
    private val combatants = database.combatantQueries
    private val events = database.combatEventQueries
    private val learned = database.combatLearnedQueries

    override fun observeAll(): Flow<List<Combat>> =
        current.observe { campaignId ->
            combine(
                combats.selectAll(campaignId).asFlow().mapToList(DatabaseDispatcher),
                combatants.selectByCampaign(campaignId).asFlow().mapToList(DatabaseDispatcher),
                events.selectByCampaign(campaignId).asFlow().mapToList(DatabaseDispatcher),
            ) { combatRows, combatantRows, eventRows ->
                val combatantsByCombat = combatantRows.groupBy { it.combat_id }
                val eventsByCombat = eventRows.groupBy { it.combat_id }
                combatRows.map { row ->
                    row.toDomain(
                        combatantsByCombat[row.id].orEmpty().map { it.toDomain() },
                        eventsByCombat[row.id].orEmpty().map { it.toDomain() },
                    )
                }
                // the grouping and the mapping run here, off the thread that collects
            }.flowOn(DatabaseDispatcher)
        }

    override fun observe(id: Long): Flow<Combat?> =
        current.observe { campaignId ->
            combine(
                combats.selectById(id, campaignId).asFlow().mapToOneOrNull(DatabaseDispatcher),
                combatants.selectByCombat(id).asFlow().mapToList(DatabaseDispatcher),
                events.selectByCombat(id).asFlow().mapToList(DatabaseDispatcher),
            ) { row: CombatRow?, combatantRows, eventRows ->
                row?.toDomain(combatantRows.map { it.toDomain() }, eventRows.map { it.toDomain() })
            }.flowOn(DatabaseDispatcher)
        }

    override suspend fun create(
        sessionNoteId: Long?,
        startedAt: LocalDateTime,
    ): Long =
        withContext(DatabaseDispatcher) {
            combats.transactionWithResult {
                combats.insert(sessionNoteId, startedAt, current.requireId(), newOriginId())
                combats.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun addCombatant(
        combatId: Long,
        combatant: Combatant,
    ): Long =
        withContext(DatabaseDispatcher) {
            combatants.transactionWithResult {
                combatants.insert(
                    combat_id = combatId,
                    name = combatant.name,
                    side = combatant.side,
                    npc_id = combatant.npcId,
                    species_id = combatant.speciesId,
                    party_member_id = combatant.partyMemberId,
                    entered_whole = if (combatant.enteredWhole) 1L else 0L,
                    state = combatant.state,
                    joined_round = combatant.joinedRound.toLong(),
                    death_successes = combatant.deathSuccesses.toLong(),
                    death_failures = combatant.deathFailures.toLong(),
                    temp_hp = combatant.tempHp.toLong(),
                    turn_order = combatant.turnOrder?.toLong(),
                    stance = combatant.stance,
                    captured = if (combatant.captured) 1L else 0L,
                    dead = if (combatant.dead) 1L else 0L,
                    conditions = combatant.conditions,
                    note = combatant.note,
                    reaction_available = if (combatant.reactionAvailable) 1L else 0L,
                    hp_current = combatant.hpCurrent?.toLong(),
                    hp_max = combatant.hpMax?.toLong(),
                )
                combatants.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun addEvent(
        combatId: Long,
        event: CombatEvent,
    ): Long =
        withContext(DatabaseDispatcher) {
            events.transactionWithResult {
                writeEvent(combatId, event)
                events.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun removeCombatant(combatantId: Long) {
        withContext(DatabaseDispatcher) { combatants.deleteById(combatantId) }
    }

    override suspend fun setTempHp(
        combatantId: Long,
        tempHp: Int,
    ) {
        withContext(DatabaseDispatcher) { combatants.updateTempHp(tempHp.toLong(), combatantId) }
    }

    override suspend fun linkNpc(
        combatantId: Long,
        npcId: Long?,
        name: String,
    ) {
        withContext(DatabaseDispatcher) { combatants.updateNpc(npcId, name, combatantId) }
    }

    override fun observeLearned(combatId: Long): Flow<List<LearnedVulnerability>> =
        learned
            .selectByCombat(combatId)
            .asFlow()
            .mapToList(DatabaseDispatcher)
            .map { rows ->
                rows.mapNotNull { row ->
                    val type = DamageType.entries.firstOrNull { it.name == row.damage_type } ?: return@mapNotNull null
                    val previous = DamageState.entries.firstOrNull { it.name == row.previous_state }
                    LearnedVulnerability(row.species_id, type, previous)
                }
            }

    override suspend fun addLearned(
        combatId: Long,
        learned: LearnedVulnerability,
    ) {
        withContext(DatabaseDispatcher) {
            this@CombatRepositoryImpl.learned.insertIfNew(
                combatId,
                learned.speciesId,
                learned.type.name,
                learned.previous?.name,
            )
        }
    }

    override suspend fun removeLearned(
        combatId: Long,
        speciesId: Long,
        type: DamageType,
    ) {
        withContext(DatabaseDispatcher) { learned.deleteOne(combatId, speciesId, type.name) }
    }

    override suspend fun renameCombatant(
        combatantId: Long,
        name: String,
    ) {
        withContext(DatabaseDispatcher) { combatants.updateName(name, combatantId) }
    }

    override suspend fun updateTerm(
        eventId: Long,
        amount: Int,
        effect: HitEffect,
    ) {
        withContext(DatabaseDispatcher) { events.updateTerm(amount.toLong(), effect, eventId) }
    }

    override suspend fun start(
        combatId: Long,
        startedAt: LocalDateTime,
    ) {
        withContext(DatabaseDispatcher) { combats.start(startedAt, combatId) }
    }

    override suspend fun setTurn(
        combatId: Long,
        round: Int,
        turn: Int,
    ) {
        withContext(DatabaseDispatcher) { combats.updateTurn(round.toLong(), turn.toLong(), combatId) }
    }

    override suspend fun updateCombatant(combatant: Combatant) {
        withContext(DatabaseDispatcher) { writeMember(combatant) }
    }

    override suspend fun record(
        combatId: Long,
        members: List<Combatant>,
        events: List<CombatEvent>,
    ) {
        withContext(DatabaseDispatcher) {
            // one commit for the whole change, so there is one sync and one round of announcements to the listeners
            combatants.transaction {
                members.forEach { writeMember(it) }
                events.forEach { writeEvent(combatId, it) }
            }
        }
    }

    private fun writeMember(combatant: Combatant) {
        combatants.updateMember(
            name = combatant.name,
            entered_whole = if (combatant.enteredWhole) 1L else 0L,
            state = combatant.state,
            death_successes = combatant.deathSuccesses.toLong(),
            death_failures = combatant.deathFailures.toLong(),
            temp_hp = combatant.tempHp.toLong(),
            turn_order = combatant.turnOrder?.toLong(),
            stance = combatant.stance,
            captured = if (combatant.captured) 1L else 0L,
            dead = if (combatant.dead) 1L else 0L,
            conditions = combatant.conditions,
            note = combatant.note,
            reaction_available = if (combatant.reactionAvailable) 1L else 0L,
            hp_current = combatant.hpCurrent?.toLong(),
            hp_max = combatant.hpMax?.toLong(),
            id = combatant.id,
        )
    }

    private fun writeEvent(
        combatId: Long,
        event: CombatEvent,
    ) {
        events.insert(
            combat_id = combatId,
            round = event.round.toLong(),
            kind = event.kind,
            source_id = event.sourceId,
            target_id = event.targetId,
            amount = event.amount.toLong(),
            damage_type = event.damageType,
            effect = event.effect,
            state = event.state,
            detail = event.detail,
            turn = event.turn.toLong(),
            hit = event.hit.toLong(),
            raw = event.raw.toLong(),
            scale = event.scale,
        )
    }

    override suspend fun setTurnOrder(
        combatantId: Long,
        order: Int?,
    ) {
        withContext(DatabaseDispatcher) { combatants.updateTurnOrder(order?.toLong(), combatantId) }
    }

    override suspend fun clearTurnOrders(combatId: Long) {
        withContext(DatabaseDispatcher) { combatants.clearTurnOrders(combatId) }
    }

    override suspend fun attachToSession(
        combatId: Long,
        sessionNoteId: Long?,
    ) {
        withContext(DatabaseDispatcher) { combats.updateSession(sessionNoteId, combatId) }
    }

    override suspend fun finish(
        combatId: Long,
        outcome: CombatOutcome,
        finishedAt: LocalDateTime,
    ) {
        withContext(DatabaseDispatcher) { combats.updateOutcome(outcome, finishedAt, combatId) }
    }

    override suspend fun delete(combatId: Long) {
        withContext(DatabaseDispatcher) { combats.deleteById(combatId, current.requireId()) }
    }
}
