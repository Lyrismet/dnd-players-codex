package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.FifthEditionRules
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.Skill

/** one line of a strip: the saving throw (no [skill]) or a skill of the strip's ability */
data class SheetRow(
    val skill: Skill?,
    val bonus: Int?,
    val proficiency: Proficiency,
)

/** an ability with its score and bonus and the lines that use it, the six strips are the body of the sheet */
data class AbilityStrip(
    val ability: Ability,
    val score: Int?,
    val bonus: Int?,
    val rows: List<SheetRow>,
)

/**
 * the six strips in sheet order, every number from [FifthEditionRules]. While searching ([matches] set) the saving
 * throw lines go away and a strip without a matching skill is left out
 */
fun abilityStrips(
    sheet: CharacterSheet,
    level: Int,
    matches: ((Skill) -> Boolean)? = null,
): List<AbilityStrip> =
    Ability.entries
        .map { ability ->
            val save =
                SheetRow(
                    skill = null,
                    bonus = FifthEditionRules.saveBonus(sheet, ability, level),
                    proficiency = if (ability in sheet.saves) Proficiency.PROFICIENT else Proficiency.NONE,
                )
            val skills =
                Skill.entries
                    .filter { it.ability == ability && (matches == null || matches(it)) }
                    .map { SheetRow(it, FifthEditionRules.skillBonus(sheet, it, level), sheet.mark(it)) }
            AbilityStrip(
                ability = ability,
                score = sheet.abilities[ability],
                bonus = FifthEditionRules.abilityBonus(sheet, ability),
                rows = if (matches == null) listOf(save) + skills else skills,
            )
        }.filter { it.rows.isNotEmpty() }

/** the numbers under the name: armor class and speed are typed, the rest comes from the sheet */
data class HeaderNumbers(
    val armorClass: Int?,
    val initiative: Int?,
    val speed: Int?,
    val proficiency: Int,
    val passivePerception: Int?,
)

fun headerNumbers(
    sheet: CharacterSheet,
    armorClass: Int?,
    level: Int,
): HeaderNumbers =
    HeaderNumbers(
        armorClass = armorClass,
        initiative = FifthEditionRules.initiative(sheet),
        speed = sheet.speed,
        proficiency = FifthEditionRules.proficiencyBonus(level),
        passivePerception = FifthEditionRules.passivePerception(sheet, level),
    )

/** the next mark when a skill circle is pressed: none, proficient, expertise, none */
fun Proficiency.next(): Proficiency = Proficiency.entries[(ordinal + 1) % Proficiency.entries.size]
