package com.lyrismet.incadent.feature.codex.domain.model

/** every sheet formula of D&D 5e (SRD 5.1) in one place, so another system can replace it later */
object FifthEditionRules {
    private const val ABILITY_BASELINE = 10
    private const val SPELL_DC_BASE = 8
    private const val PASSIVE_BASE = 10
    private const val LEVELS_PER_BONUS_STEP = 4

    /** the score minus 10, halved and rounded down */
    fun abilityBonus(score: Int): Int = (score - ABILITY_BASELINE).floorDiv(2)

    /** +2 on levels 1-4 up to +6 on 17-20 */
    fun proficiencyBonus(level: Int): Int = 2 + (level.coerceIn(PartyStatRanges.level) - 1) / LEVELS_PER_BONUS_STEP

    fun abilityBonus(
        sheet: CharacterSheet,
        ability: Ability,
    ): Int? = sheet.abilities[ability]?.let(::abilityBonus)

    fun saveBonus(
        sheet: CharacterSheet,
        ability: Ability,
        level: Int,
    ): Int? =
        abilityBonus(sheet, ability)?.let { bonus ->
            bonus + if (ability in sheet.saves) proficiencyBonus(level) else 0
        }

    fun skillBonus(
        sheet: CharacterSheet,
        skill: Skill,
        level: Int,
    ): Int? =
        abilityBonus(sheet, skill.ability)?.let { bonus ->
            bonus + proficiencyBonus(level) * sheet.mark(skill).multiplier + sheet.adjustment(skill)
        }

    fun initiative(sheet: CharacterSheet): Int? =
        abilityBonus(sheet, Ability.DEXTERITY)?.let { it + sheet.initiativeAdjustment }

    fun passivePerception(
        sheet: CharacterSheet,
        level: Int,
    ): Int? = skillBonus(sheet, Skill.PERCEPTION, level)?.let { PASSIVE_BASE + it + sheet.passivePerceptionAdjustment }

    fun spellSaveDc(
        sheet: CharacterSheet,
        level: Int,
    ): Int? =
        sheet.spellAbility?.let { ability ->
            abilityBonus(sheet, ability)?.let {
                SPELL_DC_BASE +
                    proficiencyBonus(level) +
                    it
            }
        }

    fun spellAttack(
        sheet: CharacterSheet,
        level: Int,
    ): Int? = sheet.spellAbility?.let { ability -> abilityBonus(sheet, ability)?.let { proficiencyBonus(level) + it } }
}
