package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FifthEditionRulesTest {
    // Lira, a wizard of level 5
    private val lira =
        CharacterSheet(
            abilities =
                mapOf(
                    Ability.STRENGTH to 8,
                    Ability.DEXTERITY to 14,
                    Ability.CONSTITUTION to 13,
                    Ability.INTELLIGENCE to 18,
                    Ability.WISDOM to 12,
                    Ability.CHARISMA to 10,
                ),
            saves = setOf(Ability.INTELLIGENCE, Ability.WISDOM),
            skillMarks =
                mapOf(
                    Skill.INVESTIGATION to Proficiency.PROFICIENT,
                    Skill.ARCANA to Proficiency.PROFICIENT,
                    Skill.HISTORY to Proficiency.EXPERTISE,
                    Skill.INSIGHT to Proficiency.PROFICIENT,
                ),
            spellAbility = Ability.INTELLIGENCE,
        )

    @Test
    fun `ability bonus rounds down from the score minus ten`() {
        val expected = mapOf(1 to -5, 8 to -1, 9 to -1, 10 to 0, 11 to 0, 20 to 5, 30 to 10)
        expected.forEach { (score, bonus) ->
            assertEquals(bonus, FifthEditionRules.abilityBonus(score), "score $score")
        }
    }

    @Test
    fun `proficiency bonus follows the level`() {
        val expected = mapOf(1 to 2, 4 to 2, 5 to 3, 8 to 3, 9 to 4, 13 to 5, 17 to 6, 20 to 6)
        expected.forEach { (level, bonus) ->
            assertEquals(bonus, FifthEditionRules.proficiencyBonus(level), "level $level")
        }
    }

    @Test
    fun `the bonuses of Lira come out of her scores`() {
        val bonuses = Ability.entries.map { FifthEditionRules.abilityBonus(lira, it) }
        assertEquals(listOf(-1, 2, 1, 4, 1, 0), bonuses)
    }

    @Test
    fun `a save adds the proficiency bonus only when proficient`() {
        assertEquals(7, FifthEditionRules.saveBonus(lira, Ability.INTELLIGENCE, 5))
        assertEquals(4, FifthEditionRules.saveBonus(lira, Ability.WISDOM, 5))
        assertEquals(2, FifthEditionRules.saveBonus(lira, Ability.DEXTERITY, 5))
    }

    @Test
    fun `a skill adds the bonus once for proficiency and twice for expertise`() {
        assertEquals(7, FifthEditionRules.skillBonus(lira, Skill.INVESTIGATION, 5))
        assertEquals(7, FifthEditionRules.skillBonus(lira, Skill.ARCANA, 5))
        assertEquals(10, FifthEditionRules.skillBonus(lira, Skill.HISTORY, 5))
        assertEquals(4, FifthEditionRules.skillBonus(lira, Skill.INSIGHT, 5))
        assertEquals(4, FifthEditionRules.skillBonus(lira, Skill.NATURE, 5))
    }

    @Test
    fun `a skill adjustment is added on top`() {
        val adjusted = lira.copy(skillAdjustments = mapOf(Skill.STEALTH to 2))
        assertEquals(4, FifthEditionRules.skillBonus(adjusted, Skill.STEALTH, 5))
    }

    @Test
    fun `initiative and passive perception come from dexterity and wisdom`() {
        assertEquals(2, FifthEditionRules.initiative(lira))
        assertEquals(11, FifthEditionRules.passivePerception(lira, 5))
        val adjusted = lira.copy(initiativeAdjustment = 1, passivePerceptionAdjustment = 5)
        assertEquals(3, FifthEditionRules.initiative(adjusted))
        assertEquals(16, FifthEditionRules.passivePerception(adjusted, 5))
    }

    @Test
    fun `spell save dc and attack use the chosen ability`() {
        assertEquals(15, FifthEditionRules.spellSaveDc(lira, 5))
        assertEquals(7, FifthEditionRules.spellAttack(lira, 5))
    }

    @Test
    fun `level 9 raises every number that depends on the proficiency bonus`() {
        assertEquals(8, FifthEditionRules.saveBonus(lira, Ability.INTELLIGENCE, 9))
        assertEquals(12, FifthEditionRules.skillBonus(lira, Skill.HISTORY, 9))
        assertEquals(16, FifthEditionRules.spellSaveDc(lira, 9))
    }

    @Test
    fun `a missing score gives no bonus at all`() {
        val empty = CharacterSheet()
        assertNull(FifthEditionRules.abilityBonus(empty, Ability.DEXTERITY))
        assertNull(FifthEditionRules.initiative(empty))
        assertNull(FifthEditionRules.passivePerception(empty, 5))
        assertNull(FifthEditionRules.spellSaveDc(empty.copy(spellAbility = Ability.WISDOM), 5))
        assertNull(FifthEditionRules.spellSaveDc(lira.copy(spellAbility = null), 5))
    }

    @Test
    fun `scores outside 1 to 30 are pinned`() {
        val sheet = CharacterSheet(abilities = mapOf(Ability.STRENGTH to 0, Ability.DEXTERITY to 99)).clampedToRanges()
        assertEquals(1, sheet.abilities[Ability.STRENGTH])
        assertEquals(30, sheet.abilities[Ability.DEXTERITY])
    }

    @Test
    fun `every skill belongs to one ability and the SRD has 18`() {
        assertEquals(18, Skill.entries.size)
        assertEquals(Ability.WISDOM, Skill.PERCEPTION.ability)
        assertEquals(Ability.DEXTERITY, Skill.SLEIGHT_OF_HAND.ability)
    }
}
