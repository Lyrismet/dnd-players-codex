package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.grouping.withClasses
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges

/** whether another level can be taken, the first level is taken when none is set */
val PartyMember.canLevelUp: Boolean get() = (level ?: 0) < PartyStatRanges.level.last

/** how many levels can still be taken */
val PartyMember.levelsLeft: Int get() = PartyStatRanges.level.last - (level ?: 0)

/**
 * [levels] higher with [gain] more hit points for each, null at the top level, the levels stop at the top.
 * [perClass] says how many of the levels each class takes, it counts only when the character has that many classes
 */
fun PartyMember.leveledUp(
    gain: Int,
    levels: Int = 1,
    perClass: List<Int> = emptyList(),
): PartyMember? {
    if (!canLevelUp) return null
    val taken = levels.coerceIn(1, levelsLeft)
    val add = gain.coerceAtLeast(0) * taken
    val max = hpMax?.let { (it + add).coerceAtMost(PartyStatRanges.hpMax.last) }
    val shared = perClass.takeIf { classes.size >= 2 && it.size == classes.size && it.sum() == taken }
    val raised =
        shared?.let { withClasses(classes.mapIndexed { index, entry -> entry.copy(level = entry.level + it[index]) }) }
            ?: copy(level = (level ?: 0) + taken)
    return raised.copy(
        hpMax = max,
        hpCurrent = if (max != null) hpCurrent?.let { minOf(it + add, max) } else hpCurrent,
        lastLevelGain = gain.coerceAtLeast(0),
    )
}
