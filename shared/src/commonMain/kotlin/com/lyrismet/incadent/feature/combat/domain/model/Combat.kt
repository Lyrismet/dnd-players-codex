package com.lyrismet.incadent.feature.combat.domain.model

import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import kotlinx.datetime.LocalDateTime

enum class CombatOutcome { VICTORY, RETREAT, PEACE, INTERRUPTED }

/** a combat is prepared, then fought round by round, and finished with an outcome */
enum class CombatPhase { PREPARING, ONGOING, FINISHED }

enum class CombatSide { PARTY, ALLY, ENEMY }

enum class CombatEventKind { DAMAGE, HEAL, TEMP, SET, STATE, DOWN, UP, DEAD, CAPTURED, STANCE, CONDITION, JOINED }

/** how an enemy stands to the party now, a change of it is written back to the npc it is made of */
enum class CombatStance { ENEMY, NEUTRAL, FRIEND, FLED }

enum class ConditionGroup { RESTRAINT, WEAKENING, EFFECT }

/** the conditions a participant can carry, told apart by what they do */
enum class CombatCondition(
    val group: ConditionGroup,
) {
    GRAPPLED(ConditionGroup.RESTRAINT),
    RESTRAINED(ConditionGroup.RESTRAINT),
    PRONE(ConditionGroup.RESTRAINT),
    STUNNED(ConditionGroup.RESTRAINT),
    PARALYZED(ConditionGroup.RESTRAINT),
    INCAPACITATED(ConditionGroup.RESTRAINT),
    PETRIFIED(ConditionGroup.RESTRAINT),
    BLINDED(ConditionGroup.WEAKENING),
    DEAFENED(ConditionGroup.WEAKENING),
    POISONED(ConditionGroup.WEAKENING),
    FRIGHTENED(ConditionGroup.WEAKENING),
    CHARMED(ConditionGroup.WEAKENING),
    CONCENTRATION(ConditionGroup.EFFECT),
    INVISIBLE(ConditionGroup.EFFECT),
}

/** how a hit was corrected after the fact, half for resistance, none for immunity, double for vulnerability */
enum class HitEffect { FULL, HALF, NONE, DOUBLE }

data class Combat(
    val id: Long,
    val sessionNoteId: Long?,
    val phase: CombatPhase,
    val startedAt: LocalDateTime,
    val round: Int,
    // whose turn it is, a position in the participants as they stand in the order
    val turn: Int,
    val outcome: CombatOutcome?,
    val combatants: List<Combatant>,
    val events: List<CombatEvent>,
    // when the results were saved, none while the fight is not over
    val finishedAt: LocalDateTime? = null,
)

data class Combatant(
    val id: Long,
    val name: String,
    val side: CombatSide,
    val npcId: Long?,
    val speciesId: Long?,
    val partyMemberId: Long?,
    val enteredWhole: Boolean,
    val state: CombatState,
    val joinedRound: Int,
    val deathSuccesses: Int,
    val deathFailures: Int,
    // set before the fight for a party member, damage takes it first
    val tempHp: Int = 0,
    // the place in the master's order, none while it is not given
    val turnOrder: Int? = null,
    val stance: CombatStance = CombatStance.ENEMY,
    // taken alive after going down
    val captured: Boolean = false,
    // a party member who did not come back from the death saves
    val dead: Boolean = false,
    val conditions: Set<CombatCondition> = emptySet(),
    val note: String = "",
    // the party member's own reaction, lit while it can still be used
    val reactionAvailable: Boolean = true,
    // known numbers, null while the hit points are only a guess from the master's words
    val hpCurrent: Int? = null,
    val hpMax: Int? = null,
)

/** [amount] is hits for damage and heal, and the damage taken at the mark for a state event */
data class CombatEvent(
    val id: Long,
    val round: Int,
    val kind: CombatEventKind,
    val sourceId: Long?,
    val targetId: Long,
    val amount: Int,
    val damageType: DamageType?,
    val effect: HitEffect,
    val state: CombatState?,
    // what a condition or a stance event is about, the name of the value
    val detail: String? = null,
    // the turn it happened on, a damage term is told the same way: its place in the order of the round
    val turn: Int = 0,
    // the hit a damage term belongs to, the terms of one blow on one target share it
    val hit: Int = 0,
    // the number as typed, before the master's words about the hit and the target's resistance
    val raw: Int = 0,
    val scale: HitScale = HitScale.ONE,
)
