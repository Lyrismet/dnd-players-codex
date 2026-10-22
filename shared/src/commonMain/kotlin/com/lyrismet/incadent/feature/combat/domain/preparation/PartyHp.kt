package com.lyrismet.incadent.feature.combat.domain.preparation

/** a party member's hit points as the preparation edits them */
data class PartyHp(
    val current: Int,
    val max: Int,
)

/** the number typed for the current hit points, a character with no maximum yet gets it as the maximum too */
fun setCurrentHp(
    max: Int?,
    value: Int,
): PartyHp = if (max == null) PartyHp(value, value) else PartyHp(value.coerceIn(0, max), max)
