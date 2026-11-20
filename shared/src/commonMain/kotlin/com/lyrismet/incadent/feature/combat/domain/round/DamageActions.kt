package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.damage.PlannedHit
import com.lyrismet.incadent.feature.combat.domain.damage.effectiveDamage
import com.lyrismet.incadent.feature.combat.domain.damage.hurt
import com.lyrismet.incadent.feature.combat.domain.damage.missingHp
import com.lyrismet.incadent.feature.combat.domain.damage.takeDamage
import com.lyrismet.incadent.feature.combat.domain.damage.takeHeal
import com.lyrismet.incadent.feature.combat.domain.damage.takeTemp
import com.lyrismet.incadent.feature.combat.domain.forecast.ledgerOf
import com.lyrismet.incadent.feature.combat.domain.forecast.permilleOf
import com.lyrismet.incadent.feature.combat.domain.forecast.permilleOfExact
import com.lyrismet.incadent.feature.combat.domain.forecast.stateAtPermille
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
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
        val members = mutableListOf<Combatant>()
        val told = mutableListOf<CombatEvent>()
        strikes.filter { it.hit.total > 0 }.forEach { strike ->
            val target = strike.target
            val total = strike.hit.total
            val fallen = hurt(target, total)
            members += fallen.member
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
                told += event
            }
            if (fallen.wentDown) told += plain(combat, CombatEventKind.DOWN, target.id, sourceId)
            if (fallen.wounded) {
                val taken = combat.damageTaken(target) + total
                told +=
                    plain(combat, CombatEventKind.STATE, target.id, sourceId, taken).copy(state = CombatState.WOUNDED)
            }
        }
        if (members.isNotEmpty()) repository.record(combat.id, members, told)
    }

    suspend fun heal(
        combat: Combat,
        sourceId: Long?,
        targets: List<Combatant>,
        amount: Int,
    ) {
        val members = mutableListOf<Combatant>()
        val told = mutableListOf<CombatEvent>()
        targets.forEach { target ->
            val healed = healed(combat, sourceId, target, amount)
            members += healed.member
            told += healed.events
        }
        if (members.isNotEmpty()) repository.record(combat.id, members, told)
    }

    /** a change that can be taken back, with the participants as they were and the events it told */
    class Recorded(
        val before: List<Combatant>,
        val eventIds: List<Long>,
    )

    /** every counted target that is hurt gets back what it lacks, temporary hit points stay as they are */
    suspend fun healToMax(
        combat: Combat,
        sourceId: Long?,
        targets: List<Combatant>,
    ): Recorded? {
        val hurt = targets.hurt()
        if (hurt.isEmpty()) return null
        val healed = hurt.map { healed(combat, sourceId, it, checkNotNull(it.missingHp())) }
        val ids = repository.record(combat.id, healed.map { it.member }, healed.flatMap { it.events })
        return Recorded(hurt, ids)
    }

    /** puts back only the hit points and what a stand-up changes, so later edits of the participant stay */
    suspend fun undo(
        healed: Recorded,
        current: List<Combatant>,
    ) {
        val back =
            healed.before.mapNotNull { old ->
                current.firstOrNull { it.id == old.id }?.copy(
                    hpCurrent = old.hpCurrent,
                    state = old.state,
                    deathSuccesses = old.deathSuccesses,
                    deathFailures = old.deathFailures,
                    captured = old.captured,
                )
            }
        repository.unrecord(back, healed.eventIds)
    }

    /** the hit points typed by hand, [unrecordedHeal] is the part of the change that was a heal nobody told */
    suspend fun setExact(
        combat: Combat,
        sourceId: Long?,
        target: Combatant,
        amount: Int,
        unrecordedHeal: Int = 0,
    ): Recorded {
        val told = mutableListOf<CombatEvent>()
        if (unrecordedHeal > 0) told += plain(combat, CombatEventKind.HEAL, target.id, sourceId, unrecordedHeal)
        var member = target
        val wasDown = target.isDown() && !target.dead
        if (target.isTracked()) {
            member = member.copy(hpCurrent = amount.coerceIn(0, checkNotNull(target.hpMax)))
        } else if (target.side == CombatSide.PARTY) {
            // the damage taken and what is left give the maximum, so an ally's numbers are known from now on
            val ledger = combat.ledgerOf(target.id)
            val max = ((ledger.damage - ledger.healed - unrecordedHeal).coerceAtLeast(0) + amount).coerceAtLeast(1)
            member = member.copy(hpMax = max, hpCurrent = amount.coerceIn(0, max))
        } else if (amount > 0 && !wasDown) {
            member = member.copy(state = scaleWord(combat, target, amount))
        }
        if (wasDown && amount > 0) member = member.standing()
        told += plain(combat, CombatEventKind.SET, target.id, sourceId, amount)
        if (wasDown && amount > 0) told += plain(combat, CombatEventKind.UP, target.id, sourceId)
        val fellNow = member.isTracked() && !target.isDown() && amount == 0
        if (fellNow) {
            member = member.copy(state = CombatState.DOWN, deathSuccesses = 0, deathFailures = 0)
            told += plain(combat, CombatEventKind.DOWN, target.id, sourceId)
        }
        val ids = repository.record(combat.id, listOfNotNull(member.takeIf { it != target }), told)
        return Recorded(listOf(target), ids)
    }

    /** the word of the scale that a typed number is nearest to, for a guessed participant that is on the scale */
    private fun scaleWord(
        combat: Combat,
        target: Combatant,
        amount: Int,
    ): CombatState {
        if (permilleOf(target.state) == null) return target.state
        val net = combat.ledgerOf(target.id).net
        // a creature with hit points left is on its feet, so it never reads as down
        return stateAtPermille(
            permilleOfExact(amount, net).coerceAtLeast(
                permilleOf(CombatState.BARELY_STANDING) ?: 0,
            ),
        )
    }

    private class Healed(
        val member: Combatant,
        val events: List<CombatEvent>,
    )

    private fun healed(
        combat: Combat,
        sourceId: Long?,
        target: Combatant,
        amount: Int,
    ): Healed {
        val wasDown = target.isDown() && !target.dead
        var member = target
        var done = amount
        if (target.isTracked()) {
            val max = checkNotNull(target.hpMax)
            val change = takeHeal(target.hpCurrent ?: max, max, target.tempHp, amount)
            member = member.copy(hpCurrent = change.hp)
            done = change.effective
        }
        if (wasDown) member = member.standing()
        val events = mutableListOf(plain(combat, CombatEventKind.HEAL, target.id, sourceId, done))
        if (wasDown) events += plain(combat, CombatEventKind.UP, target.id, sourceId)
        return Healed(member, events)
    }

    /** only for those whose hit points are numbers, the larger of the old and the new number stays */
    suspend fun giveTemp(
        combat: Combat,
        sourceId: Long?,
        targets: List<Combatant>,
        amount: Int,
    ) {
        val members = mutableListOf<Combatant>()
        val told = mutableListOf<CombatEvent>()
        targets.filter { it.isTracked() }.forEach { target ->
            val max = checkNotNull(target.hpMax)
            val change = takeTemp(target.hpCurrent ?: max, target.tempHp, amount)
            members += target.copy(tempHp = change.temp)
            if (change.effective > 0) told += plain(combat, CombatEventKind.TEMP, target.id, sourceId, amount)
        }
        if (members.isNotEmpty()) repository.record(combat.id, members, told)
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

    private fun plain(
        combat: Combat,
        kind: CombatEventKind,
        targetId: Long,
        sourceId: Long?,
        amount: Int = 0,
    ) = CombatEvent(0, combat.round, kind, sourceId, targetId, amount, null, HitEffect.FULL, null, turn = combat.turn)
}
