package com.lyrismet.incadent.feature.combat.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import com.lyrismet.incadent.db.Combat as CombatRow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CombatRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : CombatRepository {
    private val combats = database.combatQueries
    private val combatants = database.combatantQueries
    private val events = database.combatEventQueries

    override fun observeAll(): Flow<List<Combat>> =
        current.observe { campaignId ->
            combine(
                combats.selectAll(campaignId).asFlow().mapToList(Dispatchers.Default),
                combatants.selectByCampaign(campaignId).asFlow().mapToList(Dispatchers.Default),
                events.selectByCampaign(campaignId).asFlow().mapToList(Dispatchers.Default),
            ) { combatRows, combatantRows, eventRows ->
                val combatantsByCombat = combatantRows.groupBy { it.combat_id }
                val eventsByCombat = eventRows.groupBy { it.combat_id }
                combatRows.map { row ->
                    row.toDomain(
                        combatantsByCombat[row.id].orEmpty().map { it.toDomain() },
                        eventsByCombat[row.id].orEmpty().map { it.toDomain() },
                    )
                }
            }
        }

    override fun observe(id: Long): Flow<Combat?> =
        current.observe { campaignId ->
            combine(
                combats.selectById(id, campaignId).asFlow().mapToOneOrNull(Dispatchers.Default),
                combatants.selectByCombat(id).asFlow().mapToList(Dispatchers.Default),
                events.selectByCombat(id).asFlow().mapToList(Dispatchers.Default),
            ) { row: CombatRow?, combatantRows, eventRows ->
                row?.toDomain(combatantRows.map { it.toDomain() }, eventRows.map { it.toDomain() })
            }
        }

    override suspend fun create(
        sessionNoteId: Long?,
        startedAt: LocalDateTime,
    ): Long =
        withContext(Dispatchers.Default) {
            combats.transactionWithResult {
                combats.insert(sessionNoteId, startedAt, current.requireId())
                combats.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun addCombatant(
        combatId: Long,
        combatant: Combatant,
    ): Long =
        withContext(Dispatchers.Default) {
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
        withContext(Dispatchers.Default) {
            events.transactionWithResult {
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
                events.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun removeCombatant(combatantId: Long) {
        withContext(Dispatchers.Default) { combatants.deleteById(combatantId) }
    }

    override suspend fun setTempHp(
        combatantId: Long,
        tempHp: Int,
    ) {
        withContext(Dispatchers.Default) { combatants.updateTempHp(tempHp.toLong(), combatantId) }
    }

    override suspend fun renameCombatant(
        combatantId: Long,
        name: String,
    ) {
        withContext(Dispatchers.Default) { combatants.updateName(name, combatantId) }
    }

    override suspend fun updateTerm(
        eventId: Long,
        amount: Int,
        effect: HitEffect,
    ) {
        withContext(Dispatchers.Default) { events.updateTerm(amount.toLong(), effect, eventId) }
    }

    override suspend fun start(
        combatId: Long,
        startedAt: LocalDateTime,
    ) {
        withContext(Dispatchers.Default) { combats.start(startedAt, combatId) }
    }

    override suspend fun setTurn(
        combatId: Long,
        round: Int,
        turn: Int,
    ) {
        withContext(Dispatchers.Default) { combats.updateTurn(round.toLong(), turn.toLong(), combatId) }
    }

    override suspend fun updateCombatant(combatant: Combatant) {
        withContext(Dispatchers.Default) {
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
    }

    override suspend fun setTurnOrder(
        combatantId: Long,
        order: Int?,
    ) {
        withContext(Dispatchers.Default) { combatants.updateTurnOrder(order?.toLong(), combatantId) }
    }

    override suspend fun clearTurnOrders(combatId: Long) {
        withContext(Dispatchers.Default) { combatants.clearTurnOrders(combatId) }
    }

    override suspend fun attachToSession(
        combatId: Long,
        sessionNoteId: Long?,
    ) {
        withContext(Dispatchers.Default) { combats.updateSession(sessionNoteId, combatId) }
    }

    override suspend fun finish(
        combatId: Long,
        outcome: CombatOutcome,
    ) {
        withContext(Dispatchers.Default) { combats.updateOutcome(outcome, combatId) }
    }

    override suspend fun delete(combatId: Long) {
        withContext(Dispatchers.Default) { combats.deleteById(combatId, current.requireId()) }
    }
}
