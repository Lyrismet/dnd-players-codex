package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.preparation.CombatPreparation
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource
import com.lyrismet.incadent.feature.combat.domain.preparation.PartyHp
import com.lyrismet.incadent.feature.combat.domain.preparation.setCurrentHp
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import kotlinx.datetime.LocalDateTime

/** what a fight in progress changes, each change is written to the participant and told in an event */
class RoundActions(
    private val repository: CombatRepository,
) {
    private val preparation = CombatPreparation(repository)

    /** the fight starts, the party members whose hit points are known are followed by the numbers */
    suspend fun begin(
        combat: Combat,
        startedAt: LocalDateTime,
        partyHp: Map<Long, PartyHp>,
    ) {
        combat.combatants.forEach { member ->
            val hp = member.partyMemberId?.let(partyHp::get)
            if (hp != null) repository.updateCombatant(member.copy(hpCurrent = hp.current, hpMax = hp.max))
        }
        repository.start(combat.id, startedAt)
    }

    /** the master's words for how the participant looks, "Упал" is the same as going down */
    suspend fun setMark(
        combat: Combat,
        member: Combatant,
        state: CombatState,
    ) {
        if (member.state == state) return
        if (state == CombatState.DOWN) {
            setDown(combat, member)
            return
        }
        val wasDown = member.isDown()
        val back =
            if (wasDown) {
                member.copy(
                    deathSuccesses = 0,
                    deathFailures = 0,
                    captured = false,
                    dead = false,
                )
            } else {
                member
            }
        repository.updateCombatant(back.copy(state = state))
        if (wasDown) combat.tell(CombatEventKind.UP, member.id)
        combat.tell(CombatEventKind.STATE, member.id, amount = damageTaken(combat, member), state = state)
    }

    suspend fun setDown(
        combat: Combat,
        member: Combatant,
    ) {
        repository.updateCombatant(
            member.copy(
                state = CombatState.DOWN,
                deathSuccesses = 0,
                deathFailures = 0,
                hpCurrent = if (member.isTracked()) 0 else null,
            ),
        )
        combat.tell(CombatEventKind.DOWN, member.id)
        val actor = combat.combatants.inTurnOrder().getOrNull(combat.turn)
        if (actor?.id == member.id &&
            member.side != CombatSide.PARTY
        ) {
            next(combat.withState(member, CombatState.DOWN), emptySet())
        }
    }

    suspend fun standUp(
        combat: Combat,
        member: Combatant,
    ) {
        val tracked = member.isTracked()
        repository.updateCombatant(
            member.copy(
                state = if (tracked) CombatState.WHOLE else CombatState.BARELY_STANDING,
                hpCurrent = if (tracked) 1 else null,
                deathSuccesses = 0,
                deathFailures = 0,
                captured = false,
                dead = false,
            ),
        )
        combat.tell(CombatEventKind.UP, member.id)
    }

    suspend fun setDeathSaves(
        member: Combatant,
        successes: Int,
        failures: Int,
    ) = repository.updateCombatant(member.copy(deathSuccesses = successes, deathFailures = failures))

    suspend fun toggleDead(
        combat: Combat,
        member: Combatant,
    ) {
        repository.updateCombatant(member.copy(dead = !member.dead))
        if (!member.dead) combat.tell(CombatEventKind.DEAD, member.id)
    }

    suspend fun toggleCaptured(
        combat: Combat,
        member: Combatant,
    ) {
        repository.updateCombatant(member.copy(captured = !member.captured))
        if (!member.captured) combat.tell(CombatEventKind.CAPTURED, member.id)
    }

    suspend fun toggleReaction(member: Combatant) =
        repository.updateCombatant(member.copy(reactionAvailable = !member.reactionAvailable))

    suspend fun toggleCondition(
        combat: Combat,
        member: Combatant,
        condition: CombatCondition,
    ) {
        val on = condition in member.conditions
        repository.updateCombatant(
            member.copy(
                conditions =
                    if (on) {
                        member.conditions - condition
                    } else {
                        member.conditions +
                            condition
                    },
            ),
        )
        combat.tell(CombatEventKind.CONDITION, member.id, detail = (if (on) "-" else "+") + condition.name)
    }

    suspend fun setStance(
        combat: Combat,
        member: Combatant,
        stance: CombatStance,
    ) {
        if (member.stance == stance) return
        repository.updateCombatant(member.copy(stance = stance))
        combat.tell(CombatEventKind.STANCE, member.id, detail = stance.name)
    }

    suspend fun setEnteredWhole(
        member: Combatant,
        whole: Boolean,
    ) = repository.updateCombatant(member.copy(enteredWhole = whole))

    suspend fun setNote(
        member: Combatant,
        note: String,
    ) = repository.updateCombatant(member.copy(note = note))

    /** the hit points the master named out loud, a participant without a maximum gets it as its maximum too */
    suspend fun setHp(
        member: Combatant,
        value: Int,
    ) {
        val hp = setCurrentHp(member.hpMax, value)
        repository.updateCombatant(member.copy(hpCurrent = hp.current, hpMax = hp.max))
    }

    /** takes the participant out of the fight, the last one cannot leave, what it took is kept to put it back */
    suspend fun remove(
        combat: Combat,
        member: Combatant,
    ): RemovedMember? {
        val ordered = combat.combatants.inTurnOrder()
        val index = ordered.indexOfFirst { it.id == member.id }
        if (ordered.size <= 1 || index < 0) return null
        val turn = turnAfterRemoval(combat.turn, index, ordered.size - 1)
        repository.removeCombatant(member.id)
        repository.setTurn(combat.id, combat.round, turn)
        return RemovedMember(
            member = member,
            blows = combat.events.filter { it.targetId == member.id },
            round = combat.round,
            turn = combat.turn,
            roundAfter = combat.round,
            turnAfter = turn,
        )
    }

    /** puts a removed participant back with the blows it took, the turn only returns if the fight has not moved on */
    suspend fun restore(
        removed: RemovedMember,
        now: Combat,
    ) {
        val id = repository.addCombatant(now.id, removed.member)
        removed.blows.forEach { repository.addEvent(now.id, it.copy(targetId = id)) }
        if (now.round == removed.roundAfter && now.turn == removed.turnAfter) {
            repository.setTurn(now.id, removed.round, removed.turn)
        }
    }

    /** the next one to move, the party member whose turn it becomes has the reaction back */
    suspend fun next(
        combat: Combat,
        ownCharacterIds: Set<Long>,
    ) {
        val ordered = combat.combatants.inTurnOrder()
        val position = nextTurn(ordered, TurnPosition(combat.round, combat.turn))
        repository.setTurn(combat.id, position.round, position.turn)
        ordered.getOrNull(position.turn)?.takeIf { it.id in ownCharacterIds }?.let {
            repository.updateCombatant(it.copy(reactionAvailable = true))
        }
    }

    /** a reinforcement joins and moves last in the round */
    suspend fun reinforce(
        combat: Combat,
        side: CombatSide,
        source: MemberSource,
    ): Boolean {
        val order = reinforcementOrder(combat.combatants)
        val id = preparation.add(combat, side, source, turnOrder = order, joinedRound = combat.round)
        if (id != null) combat.tell(CombatEventKind.JOINED, id)
        return id != null
    }

    private fun damageTaken(
        combat: Combat,
        member: Combatant,
    ): Int = combat.events.filter { it.kind == CombatEventKind.DAMAGE && it.targetId == member.id }.sumOf { it.amount }

    private suspend fun Combat.tell(
        kind: CombatEventKind,
        memberId: Long,
        amount: Int = 0,
        state: CombatState? = null,
        detail: String? = null,
    ) {
        repository.addEvent(
            id,
            CombatEvent(0, round, kind, null, memberId, amount, null, HitEffect.FULL, state, detail),
        )
    }

    private fun Combat.withState(
        member: Combatant,
        state: CombatState,
    ): Combat = copy(combatants = combatants.map { if (it.id == member.id) it.copy(state = state) else it })
}

/** a participant taken out of the fight, what [RoundActions.restore] needs to bring it back */
class RemovedMember(
    val member: Combatant,
    val blows: List<CombatEvent>,
    val round: Int,
    val turn: Int,
    val roundAfter: Int,
    val turnAfter: Int,
)
