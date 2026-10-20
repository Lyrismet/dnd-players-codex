package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.Photo

data class PartyMember(
    val id: Long,
    val name: String,
    val characterClass: String,
    val race: String,
    val level: Int?,
    val playerName: String,
    val isPlayerCharacter: Boolean,
    val presence: PartyPresence,
    val hpMax: Int?,
    val hpCurrent: Int?,
    val description: String,
    val portrait: Photo? = null,
)

enum class PartyPresence {
    IN,
    AWAY,
}

/** the bounds the party form's number fields enforce - also what [clampedToRanges] pins values to */
object PartyStatRanges {
    val level = 1..20
    val hpMax = 1..999
}

/** pins every set stat to its range and keeps current hp between zero and the max, an unset stat stays unset */
fun PartyMember.clampedToRanges(): PartyMember {
    val hpMaxClamped = hpMax?.coerceIn(PartyStatRanges.hpMax)
    return copy(
        level = level?.coerceIn(PartyStatRanges.level),
        hpMax = hpMaxClamped,
        hpCurrent = if (hpMaxClamped != null) hpCurrent?.coerceIn(0, hpMaxClamped) else hpCurrent?.coerceAtLeast(0),
    )
}
