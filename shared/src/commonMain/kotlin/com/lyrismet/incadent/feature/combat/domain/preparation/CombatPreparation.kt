package com.lyrismet.incadent.feature.combat.domain.preparation

import com.lyrismet.incadent.feature.combat.domain.grouping.autonumber
import com.lyrismet.incadent.feature.combat.domain.grouping.baseName
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import kotlinx.datetime.LocalDateTime

/** a party member standing in the preparation */
data class PartySeat(
    val partyMemberId: Long,
    val name: String,
)

/** what a prepared member is made of: a codex record, or only a name for this fight (a summon) */
data class MemberSource(
    val npcId: Long?,
    val speciesId: Long?,
    val baseName: String,
)

/** edits of a combat that is still being prepared, a name taken by several members is numbered after each change */
class CombatPreparation(
    private val repository: CombatRepository,
) {
    /** a new preparation with [party] on our side */
    suspend fun start(
        startedAt: LocalDateTime,
        party: List<PartySeat>,
    ): Long {
        val combatId = repository.create(sessionNoteId = null, startedAt = startedAt)
        party.forEach { seat ->
            repository.addCombatant(combatId, newMember(seat.name, CombatSide.PARTY, null, null, seat.partyMemberId))
        }
        return combatId
    }

    suspend fun setPartyIncluded(
        combat: Combat,
        seat: PartySeat,
        included: Boolean,
    ) {
        val present = combat.combatants.firstOrNull { it.partyMemberId == seat.partyMemberId }
        when {
            included && present == null ->
                repository.addCombatant(
                    combat.id,
                    newMember(seat.name, CombatSide.PARTY, null, null, seat.partyMemberId),
                )
            !included && present != null -> repository.removeCombatant(present.id)
        }
    }

    /**
     * a named npc is one creature and is added once, a species or a summon can be added again. A reinforcement is
     * given its [turnOrder] and the round it came in, the id of the one added comes back
     */
    suspend fun add(
        combat: Combat,
        side: CombatSide,
        source: MemberSource,
        turnOrder: Int? = null,
        joinedRound: Int = 1,
    ): Long? {
        val members = combat.membersOf(side)
        if (source.npcId != null && members.any { it.isMadeOf(source) }) return null
        val added =
            newMember(source.baseName, side, source.npcId, source.speciesId, null)
                .copy(turnOrder = turnOrder, joinedRound = joinedRound)
        val id = repository.addCombatant(combat.id, added)
        renumber(members + added.copy(id = id))
        return id
    }

    suspend fun removeOne(
        combat: Combat,
        side: CombatSide,
        source: MemberSource,
    ) {
        val members = combat.membersOf(side)
        val last = members.lastOrNull { it.isMadeOf(source) } ?: return
        repository.removeCombatant(last.id)
        renumber(members - last)
    }

    suspend fun removeAll(
        combat: Combat,
        side: CombatSide,
        source: MemberSource,
    ) {
        val members = combat.membersOf(side)
        val gone = members.filter { it.isMadeOf(source) }
        gone.forEach { repository.removeCombatant(it.id) }
        renumber(members - gone.toSet())
    }

    private suspend fun renumber(members: List<Combatant>) {
        val names = autonumber(members.map { it.name.baseName() })
        members.zip(names).forEach { (member, name) ->
            if (member.name != name) repository.renameCombatant(member.id, name)
        }
    }

    private fun Combat.membersOf(side: CombatSide): List<Combatant> = combatants.filter { it.side == side }

    private fun newMember(
        name: String,
        side: CombatSide,
        npcId: Long?,
        speciesId: Long?,
        partyMemberId: Long?,
    ) = Combatant(
        id = 0,
        name = name,
        side = side,
        npcId = npcId,
        speciesId = speciesId,
        partyMemberId = partyMemberId,
        enteredWhole = true,
        state = CombatState.WHOLE,
        joinedRound = 1,
        deathSuccesses = 0,
        deathFailures = 0,
    )
}

/** whether this member was made from [source] */
fun Combatant.isMadeOf(source: MemberSource): Boolean =
    npcId == source.npcId &&
        speciesId == source.speciesId &&
        (npcId != null || speciesId != null || name.baseName() == source.baseName)
