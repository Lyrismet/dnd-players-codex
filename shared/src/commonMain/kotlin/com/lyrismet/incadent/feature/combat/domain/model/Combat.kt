package com.lyrismet.incadent.feature.combat.domain.model

import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import kotlinx.datetime.LocalDateTime

enum class CombatOutcome { VICTORY, RETREAT, PEACE, INTERRUPTED }

/** a combat is prepared, then fought round by round, and finished with an outcome */
enum class CombatPhase { PREPARING, ONGOING, FINISHED }

enum class CombatSide { PARTY, ALLY, ENEMY }

enum class CombatEventKind { DAMAGE, HEAL, STATE }

/** how a hit was corrected after the fact, half for resistance, none for immunity, double for vulnerability */
enum class HitEffect { FULL, HALF, NONE, DOUBLE }

data class Combat(
    val id: Long,
    val sessionNoteId: Long?,
    val phase: CombatPhase,
    val startedAt: LocalDateTime,
    val round: Int,
    val outcome: CombatOutcome?,
    val combatants: List<Combatant>,
    val events: List<CombatEvent>,
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
)
