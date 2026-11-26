package com.lyrismet.incadent.feature.codex.domain.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellGrowth

/** the eight schools of magic, the importer reads a site's own words into one of them */
enum class SpellSchool {
    ABJURATION,
    CONJURATION,
    DIVINATION,
    ENCHANTMENT,
    EVOCATION,
    ILLUSION,
    NECROMANCY,
    TRANSMUTATION,
}

/** a range as the sites write it, the form shows it in its own words and unit */
sealed interface ImportedRange {
    data class Feet(
        val feet: Int,
    ) : ImportedRange

    data object Touch : ImportedRange

    data object Self : ImportedRange

    /** a range that is none of these, kept as the site wrote it */
    data class Text(
        val text: String,
    ) : ImportedRange
}

/** [unit] is [DurationUnit.NONE] when the site's words fit none, then [text] keeps them */
data class ImportedDuration(
    val unit: DurationUnit,
    val value: Int,
    val text: String,
)

/**
 * a spell read from a site, the [damage] and the [growth] are set only by a source with tables of them,
 * [dice] are the dice of the description in the order they appear
 */
data class ImportedSpell(
    val source: SpellSource,
    val name: String,
    val level: Int,
    val school: SpellSchool?,
    val castKind: CastKind,
    val castingTime: String,
    val range: ImportedRange,
    val verbal: Boolean,
    val somatic: Boolean,
    // null when the spell has no material component, empty when it has one with no words about it
    val material: String?,
    val duration: ImportedDuration,
    val concentration: Boolean,
    val ritual: Boolean,
    val text: String,
    val dice: List<String>,
    val damage: List<SpellDamage> = emptyList(),
    val growth: SpellGrowth = SpellGrowth(),
)
