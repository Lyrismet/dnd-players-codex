package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecast
import com.lyrismet.incadent.feature.combat.domain.forecast.HitForecastInput
import com.lyrismet.incadent.feature.combat.domain.forecast.HitLedger
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.forecast.forecastHits
import com.lyrismet.incadent.feature.combat.domain.forecast.ledgerOf
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant

fun Combat.damageTaken(member: Combatant): Int = ledgerOf(member.id).damage

/** the damage taken at the last "bloodied" or "barely standing" mark, the mark is a snapshot */
fun Combat.markAt(member: Combatant): Int? =
    events
        .lastOrNull { it.targetId == member.id && it.kind == CombatEventKind.STATE && it.state == member.state }
        ?.amount
        ?.takeIf { member.state == CombatState.BLOODIED || member.state == CombatState.BARELY_STANDING }

/** the hit points typed by hand, the healing told after them and what they add up to */
data class ExactExpectation(
    val before: Int,
    val healed: Int,
    val expected: Int,
)

fun Combat.exactExpectations(members: List<Combatant>): Map<Long, ExactExpectation> =
    members.mapNotNull { m -> exactExpectation(m)?.let { m.id to it } }.toMap()

private fun Combat.exactExpectation(member: Combatant): ExactExpectation? {
    val ledger = ledgerOf(member.id)
    val exact = ledger.exact
    val forecast = forecastOf(member, ledger) as? HitForecast.Exact
    return if (exact != null && forecast != null && ledger.healedSinceSet > 0) {
        ExactExpectation(exact.remaining, ledger.healedSinceSet, forecast.remaining)
    } else {
        null
    }
}

/** what is left of an enemy whose hit points are only guessed: what was typed, the scale, then earlier fights */
fun Combat.forecast(
    member: Combatant,
    seen: List<SpeciesHitObservation> = emptyList(),
): HitForecast = forecastOf(member, ledgerOf(member.id), seen)

private fun forecastOf(
    member: Combatant,
    ledger: HitLedger,
    seen: List<SpeciesHitObservation> = emptyList(),
): HitForecast =
    if (member.isTracked() || member.isDown()) {
        HitForecast.Unknown
    } else {
        forecastHits(HitForecastInput(ledger.damage, ledger.marks, seen, ledger.healed, ledger.exact))
    }
