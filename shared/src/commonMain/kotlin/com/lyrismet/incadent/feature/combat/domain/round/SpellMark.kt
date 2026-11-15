package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.atRank
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind

/** the counted mark a cast puts on the caster, [rounds] is null for a concentration with no set time */
data class SpellMark(
    val kind: MarkKind,
    val rounds: Int?,
)

/** a concentration spell always marks the caster, any other one only when it lasts */
fun CharacterSpell.markFor(
    circle: Int,
    characterLevel: Int,
): SpellMark? {
    val rounds = atRank(characterLevel, circle).combatRounds
    return when {
        concentration -> SpellMark(MarkKind.CONCENTRATION, rounds)
        rounds != null -> SpellMark(MarkKind.SPELL, rounds)
        else -> null
    }
}
