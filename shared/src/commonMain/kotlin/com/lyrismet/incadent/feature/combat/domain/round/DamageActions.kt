package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.damage.PlannedHit
import com.lyrismet.incadent.feature.combat.domain.damage.effectiveDamage
import com.lyrismet.incadent.feature.combat.domain.damage.takeDamage
import com.lyrismet.incadent.feature.combat.domain.damage.takeHeal
import com.lyrismet.incadent.feature.combat.domain.damage.takeTemp
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository

/** a hit planned for one target */
data class Strike(
    val target: Combatant,
    val hit: PlannedHit,
)

/** damage, healing and temporary hit points: what they do to the participants and what is told of them */
class DamageActions(
    private val repository: CombatRepository,
) {
    /** the terms of every strike are told one by one, those of one blow on one target share a hit number */
    suspend fun damage(
        combat: Combat,
        sourceId: Long?,
        strikes: List<Strike>,
    ) {
        val hitNumber = (combat.events.maxOfOrNull { it.hit } ?: 0) + 1
        strikes.filter { it.hit.total > 0 }.forEach { strike ->
            val target = strike.target
            val total = strike.hit.total
            val fallen = hurt(target, total)
            repository.updateCombatant(fallen.member)
            strike.hit.terms.forEach { planned ->
                val term = planned.term
                val event =
                    CombatEvent(
                        id = 0,
                        round = combat.round,
                        kind = CombatEventKind.DAMAGE,
                        sourceId = sourceId,
                        targetId = target.id,
                        amount = planned.effective,
                        damageType = term.type,
                        effect = planned.effect,
                        state = null,
                        turn = combat.turn,
                        hit = hitNumber,
                        raw = term.amount,
                        scale = strike.hit.scale,
                    )
                repository.addEvent(combat.id, event)
            }
            if (fallen.wentDown) {
                repository.addEvent(
                    combat.id,
                    plain(combat, CombatEventKind.DOWN, target.id, sourceId),
                )
            }
            if (fallen.wounded) {
                val taken = combat.damageTakenBy(target) + total
                repository.addEvent(
                    combat.id,
                    plain(combat, CombatEventKind.STATE, target.id, sourceId, taken).copy(state = CombatState.WOUNDED),
                )
            }
        }
    }

    suspend fun heal(
        combat: Combat,
        sourceId: Long?,
        targets: List<Combatant>,
        amount: Int,
    ) {
        targets.forEach { target ->
            val wasDown = target.isDown() && !target.dead
            var healed = target
            var done = amount
            if (target.isTracked()) {
                val max = checkNotNull(target.hpMax)
                val change = takeHeal(target.hpCurrent ?: max, max, target.tempHp, amount)
                healed = healed.copy(hpCurrent = change.hp)
                done = change.effective
            }
            if (wasDown) healed = healed.standing()
            repository.updateCombatant(healed)
            repository.addEvent(combat.id, plain(combat, CombatEventKind.HEAL, target.id, sourceId, done))
            if (wasDown) repository.addEvent(combat.id, plain(combat, CombatEventKind.UP, target.id, sourceId))
        }
    }

    /** only for those whose hit points are numbers, the larger of the old and the new number stays */
    suspend fun giveTemp(
        combat: Combat,
        sourceId: Long?,
        targets: List<Combatant>,
        amount: Int,
    ) {
        targets.filter { it.isTracked() }.forEach { target ->
            val max = checkNotNull(target.hpMax)
            val change = takeTemp(target.hpCurrent ?: max, target.tempHp, amount)
            repository.updateCombatant(target.copy(tempHp = change.temp))
            if (change.effective > 0) {
                repository.addEvent(combat.id, plain(combat, CombatEventKind.TEMP, target.id, sourceId, amount))
            }
        }
    }

    /** the resistance was named after the blow: the term is recounted, the hit points follow by the difference */
    suspend fun correct(
        combat: Combat,
        eventId: Long,
        effect: HitEffect,
    ): Int {
        val event = combat.events.firstOrNull { it.id == eventId && it.kind == CombatEventKind.DAMAGE } ?: return 0
        val recounted = effectiveDamage(event.raw, event.scale, effect)
        val delta = recounted - event.amount
        repository.updateTerm(eventId, recounted, effect)
        val target = combat.combatants.firstOrNull { it.id == event.targetId }
        if (target != null && target.isTracked()) {
            val max = checkNotNull(target.hpMax)
            val hp = ((target.hpCurrent ?: max) - delta).coerceIn(0, max)
            repository.updateCombatant(target.copy(hpCurrent = hp))
        }
        return delta
    }

    private class Hurt(
        val member: Combatant,
        val wentDown: Boolean,
        val wounded: Boolean,
    )

    /** what damage does to the target: numbers go down, a guessed one is marked hurt at the first blow */
    private fun hurt(
        target: Combatant,
        total: Int,
    ): Hurt {
        if (!target.isTracked()) {
            val wounded = target.state == CombatState.WHOLE
            return Hurt(
                if (wounded) target.copy(state = CombatState.WOUNDED) else target,
                wentDown = false,
                wounded = wounded,
            )
        }
        val max = checkNotNull(target.hpMax)
        val now = target.hpCurrent ?: max
        val change = takeDamage(now, target.tempHp, total)
        val down = change.hp == 0 && now > 0
        val moved = target.copy(hpCurrent = change.hp, tempHp = change.temp)
        val member = if (down) moved.copy(state = CombatState.DOWN, deathSuccesses = 0, deathFailures = 0) else moved
        return Hurt(member, wentDown = down, wounded = false)
    }

    /** back on the feet: a guessed participant barely stands, a counted one is well again */
    private fun Combatant.standing(): Combatant =
        copy(
            state = if (isTracked()) CombatState.WHOLE else CombatState.BARELY_STANDING,
            deathSuccesses = 0,
            deathFailures = 0,
            captured = false,
        )

    private fun Combat.damageTakenBy(member: Combatant): Int =
        events.filter { it.kind == CombatEventKind.DAMAGE && it.targetId == member.id }.sumOf { it.amount }

    private fun plain(
        combat: Combat,
        kind: CombatEventKind,
        targetId: Long,
        sourceId: Long?,
        amount: Int = 0,
    ) = CombatEvent(0, combat.round, kind, sourceId, targetId, amount, null, HitEffect.FULL, null, turn = combat.turn)
}
