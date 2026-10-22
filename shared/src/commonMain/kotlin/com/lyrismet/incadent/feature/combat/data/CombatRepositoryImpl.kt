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
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
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
                )
                events.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun removeEvent(eventId: Long) {
        withContext(Dispatchers.Default) { events.deleteById(eventId) }
    }

    override suspend fun setState(
        combatantId: Long,
        state: CombatState,
    ) {
        withContext(Dispatchers.Default) { combatants.updateState(state, combatantId) }
    }

    override suspend fun setDeathSaves(
        combatantId: Long,
        successes: Int,
        failures: Int,
    ) {
        withContext(Dispatchers.Default) {
            combatants.updateDeathSaves(successes.toLong(), failures.toLong(), combatantId)
        }
    }

    override suspend fun setRound(
        combatId: Long,
        round: Int,
    ) {
        withContext(Dispatchers.Default) { combats.updateRound(round.toLong(), combatId) }
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
