package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.isDown
import com.lyrismet.incadent.feature.combat.domain.round.isTracked

/** how a participant came out of the fight, the words are the screen's */
sealed interface ParticipantResult {
    data object Dead : ParticipantResult

    data object Dying : ParticipantResult

    data class Hp(
        val current: Int,
        val max: Int,
    ) : ParticipantResult

    data class Looks(
        val state: CombatState,
    ) : ParticipantResult

    data object Captured : ParticipantResult

    data object Fled : ParticipantResult

    data object Surrendered : ParticipantResult

    data object Joined : ParticipantResult

    data class Down(
        val by: String?,
    ) : ParticipantResult
}

/** a line of the table: who, what they dealt and took, how it ended for them */
data class ResultRow(
    val memberId: Long,
    val name: String,
    val side: CombatSide,
    val stance: CombatStance,
    val dealt: Int,
    val taken: Int,
    val result: ParticipantResult,
)

private val SideOrder = listOf(CombatSide.PARTY, CombatSide.ALLY, CombatSide.ENEMY)

/** the participants in the order party, allies, enemies, with the damage each dealt and took */
fun resultRows(combat: Combat): List<ResultRow> {
    val damage = combat.events.filter { it.kind == CombatEventKind.DAMAGE }
    return SideOrder.flatMap { side -> combat.combatants.filter { it.side == side } }.map { member ->
        ResultRow(
            memberId = member.id,
            name = member.name,
            side = member.side,
            stance = member.stance,
            dealt = damage.filter { it.sourceId == member.id }.sumOf { it.amount },
            taken = damage.filter { it.targetId == member.id }.sumOf { it.amount },
            result = resultOf(combat, member),
        )
    }
}

private fun resultOf(
    combat: Combat,
    member: Combatant,
): ParticipantResult =
    when {
        member.side == CombatSide.PARTY -> partyResult(member)
        member.captured -> ParticipantResult.Captured
        member.stance == CombatStance.FLED -> ParticipantResult.Fled
        member.stance == CombatStance.NEUTRAL -> ParticipantResult.Surrendered
        member.stance == CombatStance.FRIEND -> ParticipantResult.Joined
        member.isDown() -> ParticipantResult.Down(killerOf(combat, member))
        else -> standing(member)
    }

private fun partyResult(member: Combatant): ParticipantResult =
    when {
        member.dead -> ParticipantResult.Dead
        member.isDown() -> ParticipantResult.Dying
        else -> standing(member)
    }

private fun standing(member: Combatant): ParticipantResult =
    if (member.isTracked()) {
        ParticipantResult.Hp(member.hpCurrent ?: checkNotNull(member.hpMax), checkNotNull(member.hpMax))
    } else {
        ParticipantResult.Looks(member.state)
    }

/** the one whose blow put the participant down for the last time, none for the environment or a mark by hand */
private fun killerOf(
    combat: Combat,
    member: Combatant,
): String? {
    val down = combat.events.lastOrNull { it.kind == CombatEventKind.DOWN && it.targetId == member.id }
    val by = down?.sourceId?.takeIf { it != member.id }
    return combat.combatants.firstOrNull { it.id == by }?.name
}

/** the damage an event told, the terms of one blow on one target are one line */
internal fun List<CombatEvent>.damageHits(): List<List<CombatEvent>> =
    filter { it.kind == CombatEventKind.DAMAGE }.groupBy { it.hit to it.targetId }.values.toList()
