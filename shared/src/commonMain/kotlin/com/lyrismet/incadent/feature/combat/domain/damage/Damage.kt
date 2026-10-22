package com.lyrismet.incadent.feature.combat.domain.damage

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlin.math.floor

/** what the master said about a hit as a whole: it landed in full, half (saved), not at all or doubled */
private const val NO_CHANGE = 1.0
private const val HALVED = 0.5
private const val DOUBLED = 2.0

enum class HitScale(
    val factor: Double,
) {
    ONE(NO_CHANGE),
    HALF(HALVED),
    ZERO(0.0),
    DOUBLE(DOUBLED),
}

val HitEffect.factor: Double
    get() =
        when (this) {
            HitEffect.FULL -> NO_CHANGE
            HitEffect.HALF -> HALVED
            HitEffect.NONE -> 0.0
            HitEffect.DOUBLE -> DOUBLED
        }

/** the effect a known state of a damage type has on a hit, an unknown or a normal one changes nothing */
fun DamageState?.toHitEffect(): HitEffect =
    when (this) {
        DamageState.RESISTANCE -> HitEffect.HALF
        DamageState.IMMUNITY -> HitEffect.NONE
        DamageState.VULNERABILITY -> HitEffect.DOUBLE
        else -> HitEffect.FULL
    }

/** one number of the sum that makes a hit, with the damage type it was typed for */
data class DamageTerm(
    val amount: Int,
    val type: DamageType? = null,
)

data class PlannedTerm(
    val term: DamageTerm,
    val effect: HitEffect,
    val effective: Int,
)

/** a hit as it lands on one target: every term after its resistance and the master's scale */
data class PlannedHit(
    val terms: List<PlannedTerm>,
    val scale: HitScale,
) {
    val total: Int get() = terms.sumOf { it.effective }
}

/** the hit points a term takes off once the scale and the effect are applied, rounded down */
fun effectiveDamage(
    raw: Int,
    scale: HitScale,
    effect: HitEffect,
): Int = floor(raw * scale.factor * effect.factor).toInt()

/** the terms of a hit on a target whose damage states are [resistances], a state it does not know changes nothing */
fun planHit(
    terms: List<DamageTerm>,
    scale: HitScale,
    resistances: Map<DamageType, DamageState>,
): PlannedHit =
    PlannedHit(
        terms =
            terms.map { term ->
                val effect = term.type?.let { resistances[it] }.toHitEffect()
                PlannedTerm(term, effect, effectiveDamage(term.amount, scale, effect))
            },
        scale = scale,
    )

/** the terms of the typed sum, "8+6" with the type typed for each: a zero or an empty term is not a hit */
fun parseTerms(
    raw: String,
    types: List<DamageType?>,
): List<DamageTerm> =
    raw
        .split('+')
        .mapIndexed { index, text -> text.toIntOrNull()?.let { DamageTerm(it, types.getOrNull(index)) } }
        .filterNotNull()
        .filter { it.amount > 0 }
