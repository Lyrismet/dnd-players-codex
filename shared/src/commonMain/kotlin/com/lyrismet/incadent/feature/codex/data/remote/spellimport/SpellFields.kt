package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource

/** what every source hands over once its own format is read, the rest is the same for all of them */
@Suppress("LongParameterList")
internal class SpellFields(
    val source: SpellSource,
    val name: String,
    val level: Int,
    val schoolText: String,
    val castingText: String,
    val rangeText: String,
    val verbal: Boolean,
    val somatic: Boolean,
    val material: String?,
    val durationText: String,
    val concentration: Boolean,
    val ritual: Boolean,
    val description: String,
    val damage: DamageRead = NoDamage,
)

/** the name, the level and the description are what a spell cannot go without, a page without them is not read */
internal fun SpellFields.toImportedSpell(): ImportedSpell? {
    val title = name.trim()
    val body = description.trim()
    if (title.isEmpty() || body.isEmpty() || level !in 0..SPELL_LEVELS.last) return null
    val (kind, castingTime) = readCasting(castingText)
    return ImportedSpell(
        source = source,
        name = title,
        level = level,
        school = readSchool(schoolText),
        castKind = kind,
        castingTime = castingTime,
        range = readRange(rangeText),
        verbal = verbal,
        somatic = somatic,
        material = material?.trim(),
        duration = readDuration(durationText),
        concentration = concentration,
        ritual = ritual,
        text = body,
        dice = findDice(body),
        damage = damage.damage,
        growth = damage.growth,
    )
}
