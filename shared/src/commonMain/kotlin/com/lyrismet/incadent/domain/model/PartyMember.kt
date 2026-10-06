package com.lyrismet.incadent.domain.model

data class PartyMember(
    val id: Long,
    val name: String,
    val characterClass: String,
    val race: String,
    val level: Int,
    val playerName: String,
    val isPlayerCharacter: Boolean,
    val presence: PartyPresence,
    val hpMax: Int,
    val hpCurrent: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val description: String,
    val portraitUri: String?,
)

enum class PartyPresence {
    IN,
    AWAY,
}

/** the bounds the party form's steppers enforce - also what [clampedToRanges] pins values to */
object PartyStatRanges {
    val level = 1..20
    val hpMax = 1..999
    val armorClass = 1..30
    val initiativeBonus = -5..15
}

/** pins every numeric stat to its range and keeps current hp between zero and the max */
fun PartyMember.clampedToRanges(): PartyMember {
    val hpMaxClamped = hpMax.coerceIn(PartyStatRanges.hpMax)
    return copy(
        level = level.coerceIn(PartyStatRanges.level),
        hpMax = hpMaxClamped,
        hpCurrent = hpCurrent.coerceIn(0, hpMaxClamped),
        armorClass = armorClass.coerceIn(PartyStatRanges.armorClass),
        initiativeBonus = initiativeBonus.coerceIn(PartyStatRanges.initiativeBonus),
    )
}
