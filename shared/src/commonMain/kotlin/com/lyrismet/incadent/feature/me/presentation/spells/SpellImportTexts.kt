package com.lyrismet.incadent.feature.me.presentation.spells

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedRange
import com.lyrismet.incadent.feature.codex.domain.spellimport.ImportedSpell
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSchool
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_range_near
import incadent.shared.generated.resources.me_spells_range_self
import incadent.shared.generated.resources.me_spells_range_unit_ft
import org.jetbrains.compose.resources.getString

/** the words of the app that an imported spell is written in: its school, its range and the unit of the range */
internal class SpellImportTexts(
    val schools: Map<SpellSchool, String>,
    val touch: String,
    val self: String,
    val feet: String,
)

internal suspend fun loadSpellImportTexts(): SpellImportTexts =
    SpellImportTexts(
        schools =
            // Schools lists the same eight in the same order as the enum
            SpellSchool.entries.zip(Schools).associate { (school, shape) -> school to getString(shape.name) },
        touch = getString(Res.string.me_spells_range_near),
        self = getString(Res.string.me_spells_range_self),
        feet = getString(Res.string.me_spells_range_unit_ft),
    )

/** the form's draft of an imported spell: the damage and the growth are empty unless the source has tables of them */
internal fun ImportedSpell.toDraft(
    texts: SpellImportTexts,
    position: Int,
): CharacterSpell =
    CharacterSpell(
        id = 0,
        name = name,
        level = level,
        school = school?.let { texts.schools[it] }.orEmpty(),
        castKind = castKind,
        castingTime = castingTime,
        range =
            when (range) {
                is ImportedRange.Feet -> "${range.feet} ${texts.feet}"
                ImportedRange.Touch -> texts.touch
                ImportedRange.Self -> texts.self
                is ImportedRange.Text -> range.text
            },
        verbal = verbal,
        somatic = somatic,
        hasMaterial = material != null,
        material = material.orEmpty(),
        duration = duration.text,
        durationUnit = duration.unit,
        durationValue = duration.value,
        concentration = concentration,
        ritual = ritual,
        text = text,
        damage = damage,
        growth = growth,
        position = position,
    )
