package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope

/** writes the fights of a file, every link follows the maps of the records the import made or matched */
@Suppress("LongParameterList")
internal class FightImport(
    private val database: AppDatabase,
    private val campaignId: String,
    private val sessionMap: Map<Long, Long>,
    private val speciesMap: Map<Long, Long>,
    private val npcMap: Map<Long, Long>,
    private val partyMap: Map<Long, Long>,
) {
    private val origins =
        OriginSet(
            database.combatQueries
                .selectAll(campaignId)
                .executeAsList()
                .map { it.origin_id },
        )
    private val combatMap = mutableMapOf<Long, Long>()
    private val fighterMap = mutableMapOf<Pair<Long, Long>, Long>()

    /** a fight comes with its session, the rest of its records find their fight and are skipped without one */
    fun isWanted(
        record: FightRecord,
        scope: RecordScope?,
    ): Boolean = record !is FileRecord.Combat || scope?.sessions?.contains(record.sessionId) != false

    fun accept(record: FightRecord) {
        when (record) {
            is FileRecord.Combat -> addCombat(record)
            is FileRecord.Fighter -> addFighter(record)
            is FileRecord.Blow -> addBlow(record)
            is FileRecord.Observation -> addObservation(record)
        }
    }

    private fun addCombat(record: FileRecord.Combat) {
        val sessionId = sessionMap[record.sessionId] ?: return
        database.combatQueries.insertFinished(
            sessionId,
            record.startedAt,
            record.rounds.toLong(),
            record.turn.toLong(),
            record.outcome,
            record.finishedAt,
            campaignId,
            origins.claim(record.originId),
        )
        combatMap[record.id] = database.combatQueries.lastInsertRowId().executeAsOne()
    }

    private fun addFighter(record: FileRecord.Fighter) {
        val combatId = combatMap[record.combatId] ?: return
        database.combatantQueries.insert(
            combat_id = combatId,
            name = record.name,
            side = record.side,
            npc_id = record.npcId?.let(npcMap::get),
            species_id = record.speciesId?.let(speciesMap::get),
            party_member_id = record.partyId?.let(partyMap::get),
            entered_whole = if (record.enteredWhole) 1L else 0L,
            state = record.state,
            joined_round = record.joinedRound.toLong(),
            death_successes = record.deathSuccesses.toLong(),
            death_failures = record.deathFailures.toLong(),
            temp_hp = record.tempHp.toLong(),
            turn_order = null,
            stance = record.stance,
            captured = if (record.captured) 1L else 0L,
            dead = if (record.dead) 1L else 0L,
            conditions = record.conditions.toSet(),
            note = record.note,
            reaction_available = 1L,
            hp_current = record.hpCurrent?.toLong(),
            hp_max = record.hpMax?.toLong(),
        )
        fighterMap[record.combatId to record.id] = database.combatantQueries.lastInsertRowId().executeAsOne()
    }

    private fun addBlow(record: FileRecord.Blow) {
        val combatId = combatMap[record.combatId] ?: return
        val target = fighterMap[record.combatId to record.targetId] ?: return
        database.combatEventQueries.insert(
            combat_id = combatId,
            round = record.round.toLong(),
            kind = record.kind,
            source_id = record.sourceId?.let { fighterMap[record.combatId to it] },
            target_id = target,
            amount = record.amount.toLong(),
            damage_type = record.damageType,
            effect = record.effect,
            state = record.state,
            detail = record.detail,
            turn = record.turn.toLong(),
            hit = record.hit.toLong(),
            raw = record.raw.toLong(),
            scale = record.scale,
        )
    }

    private fun addObservation(record: FileRecord.Observation) {
        val combatId = combatMap[record.combatId] ?: return
        val speciesId = speciesMap[record.speciesId] ?: return
        database.speciesHitObservationQueries.insert(
            speciesId,
            combatId,
            record.damage.toLong(),
            if (record.died) 1L else 0L,
        )
    }
}
