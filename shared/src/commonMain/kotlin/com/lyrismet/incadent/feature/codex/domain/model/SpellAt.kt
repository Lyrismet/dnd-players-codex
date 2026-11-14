package com.lyrismet.incadent.feature.codex.domain.model

/** what a spell comes to on a given slot or character level: the base fields with the growth applied */
data class SpellAt(
    val damage: List<SpellDamage>,
    val projectiles: Int,
    val durationUnit: DurationUnit,
    val durationValue: Int,
    val extraTargets: Int,
    val range: String,
    val notes: List<String>,
) {
    val combatRounds: Int? get() = combatRoundsOf(durationUnit, durationValue)
}
