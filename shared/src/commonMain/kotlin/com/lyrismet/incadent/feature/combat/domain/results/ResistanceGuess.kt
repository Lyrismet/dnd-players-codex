package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect

/** what a fight suggests about how a species takes one damage type, [confident] ones are ticked from the start */
data class ResistanceGuess(
    val speciesId: Long,
    val speciesName: String,
    val type: DamageType,
    val state: DamageState,
    val confident: Boolean,
)

private const val CONFIDENT_STRIKES = 2

private fun HitEffect.toState(): DamageState? =
    when (this) {
        HitEffect.FULL -> null
        HitEffect.HALF -> DamageState.RESISTANCE
        HitEffect.NONE -> DamageState.IMMUNITY
        HitEffect.DOUBLE -> DamageState.VULNERABILITY
    }

/** a type the species already has a known answer for is never guessed again, a mixed answer is no answer */
fun inferResistances(
    combat: Combat,
    species: Map<Long, Species>,
): List<ResistanceGuess> {
    val speciesOf = combat.combatants.mapNotNull { m -> m.speciesId?.let { m.id to it } }.toMap()
    val blows = combat.events.filter { it.kind == CombatEventKind.DAMAGE && it.damageType != null }
    val bySpecies = blows.groupBy { speciesOf[it.targetId] }
    return bySpecies
        .flatMap { (speciesId, events) ->
            val known = speciesId?.let { species[it] }
            if (speciesId == null || known == null) {
                emptyList()
            } else {
                guessesFor(known, events)
            }
        }.sortedWith(compareBy({ it.speciesName }, { it.type.ordinal }))
}

private fun guessesFor(
    species: Species,
    events: List<CombatEvent>,
): List<ResistanceGuess> {
    val fullTypes = events.filter { it.effect == HitEffect.FULL }.mapNotNull { it.damageType }.toSet()
    return events
        .groupBy { checkNotNull(it.damageType) }
        .mapNotNull { (type, strikes) ->
            val state =
                strikes
                    .map { it.effect }
                    .distinct()
                    .singleOrNull()
                    ?.toState()
            val known = species.vulnerabilities[type]
            if (state == null || (known != null && known != DamageState.UNKNOWN)) {
                null
            } else {
                val attackers = strikes.mapNotNull { it.sourceId }.distinct().size
                val strong =
                    state == DamageState.VULNERABILITY ||
                        (attackers >= CONFIDENT_STRIKES && (fullTypes - type).isNotEmpty())
                ResistanceGuess(species.id, species.name, type, state, strong)
            }
        }
}
