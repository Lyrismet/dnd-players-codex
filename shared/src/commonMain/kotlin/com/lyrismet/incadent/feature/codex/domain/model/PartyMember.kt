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
    // fell in a fight and is out of the party until brought back, [fellCombatId] is that fight
    val fallen: Boolean = false,
    val fellCombatId: Long? = null,
    val originId: String = "",
    val subclass: String = "",
    // what the character resists or is weak to, written on its own so saving a form never wipes it
    val vulnerabilities: Map<DamageType, DamageState> = emptyMap(),
    // the level rose after the maximum was typed, so it is a guess until a new one is typed
    val hpMaxApprox: Boolean = false,
    // hit points a level adds beyond the roll, a feat like Tough gives 2
    val hpBonusPerLevel: Int = 0,
    // what the last level up added, offered again as the next one
    val lastLevelGain: Int? = null,
    // two or more classes with a level each, empty while the character has one class (the summary fields then hold it)
    val classes: List<PartyClass> = emptyList(),
)

/** one class of a character that has several, the subclass may be empty */
data class PartyClass(
    val characterClass: String,
    val subclass: String,
    val level: Int,
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
