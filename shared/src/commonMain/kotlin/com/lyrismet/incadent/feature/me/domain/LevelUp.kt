package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges

/** whether another level can be taken, the first level is taken when none is set */
val PartyMember.canLevelUp: Boolean get() = (level ?: 0) < PartyStatRanges.level.last

/** how many levels can still be taken */
val PartyMember.levelsLeft: Int get() = PartyStatRanges.level.last - (level ?: 0)

/** [levels] higher with [gain] more hit points for each, null at the top level, the levels stop at the top */
fun PartyMember.leveledUp(
    gain: Int,
    levels: Int = 1,
): PartyMember? {
    if (!canLevelUp) return null
    val taken = levels.coerceIn(1, levelsLeft)
    val add = gain.coerceAtLeast(0) * taken
    val max = hpMax?.let { (it + add).coerceAtMost(PartyStatRanges.hpMax.last) }
    return copy(
        level = (level ?: 0) + taken,
        hpMax = max,
        hpCurrent = if (max != null) hpCurrent?.let { minOf(it + add, max) } else hpCurrent,
        lastLevelGain = gain.coerceAtLeast(0),
    )
}
