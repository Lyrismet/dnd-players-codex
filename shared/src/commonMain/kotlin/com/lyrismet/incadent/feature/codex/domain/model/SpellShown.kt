package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.core.format.toRomanNumeral

/** what the spell list shows - the circle the player pinned in the card, else the spell as it is */
fun CharacterSpell.atShown(characterLevel: Int): SpellAt = atRank(characterLevel, growth.shownCircle)

/** the circle pinned above the spell's own, a cantrip never has one */
val CharacterSpell.pinnedCircle: Int? get() = growth.shownCircle.takeIf { it > level && !isCantrip }

// a table gives its own text per step, otherwise the one text of the rule, the circle fills its place
internal fun GrowthRule.customNote(
    times: Int,
    step: String?,
    circle: Int,
): String? {
    val text = if (mode == GrowthMode.STEPS) step else add.takeIf { times > 0 && it.isNotBlank() }
    return text?.replace(CIRCLE_PLACEHOLDER, circle.toRomanNumeral())
}
