package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SheetStripsTest {
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
            skillMarks = mapOf(Skill.HISTORY to Proficiency.EXPERTISE, Skill.ARCANA to Proficiency.PROFICIENT),
            speed = 30,
        )

    @Test
    fun `six strips come in the order of the paper sheet`() {
        assertEquals(Ability.entries, abilityStrips(lira, 5).map { it.ability })
    }

    @Test
    fun `a strip starts with the saving throw and then its skills`() {
        val intelligence = abilityStrips(lira, 5).first { it.ability == Ability.INTELLIGENCE }
        assertNull(intelligence.rows.first().skill)
        assertEquals(7, intelligence.rows.first().bonus)
        assertEquals(
            listOf(Skill.ARCANA, Skill.HISTORY, Skill.INVESTIGATION, Skill.NATURE, Skill.RELIGION),
            intelligence.rows.drop(1).mapNotNull { it.skill },
        )
    }

    @Test
    fun `constitution has only the saving throw`() {
        assertEquals(1, abilityStrips(lira, 5).first { it.ability == Ability.CONSTITUTION }.rows.size)
    }

    @Test
    fun `the bonuses of the strips are the ones of Lira`() {
        assertEquals(listOf(-1, 2, 1, 4, 1, 0), abilityStrips(lira, 5).map { it.bonus })
    }

    @Test
    fun `a search drops the saving throws and the strips with no match`() {
        val found = abilityStrips(lira, 5) { it == Skill.HISTORY || it == Skill.STEALTH }
        assertEquals(listOf(Ability.DEXTERITY, Ability.INTELLIGENCE), found.map { it.ability })
        assertEquals(listOf(Skill.HISTORY), found.last().rows.mapNotNull { it.skill })
    }

    @Test
    fun `an empty sheet shows dashes and no made up numbers`() {
        val strip = abilityStrips(CharacterSheet(), 1).first()
        assertNull(strip.bonus)
        assertNull(strip.score)
        assertNull(strip.rows.first().bonus)
    }

    @Test
    fun `header numbers come from the sheet and the level`() {
        val withExpertise = lira.copy(skillMarks = lira.skillMarks + (Skill.PERCEPTION to Proficiency.PROFICIENT))
        val numbers = headerNumbers(withExpertise, armorClass = 12, level = 5)
        assertEquals(12, numbers.armorClass)
        assertEquals(2, numbers.initiative)
        assertEquals(30, numbers.speed)
        assertEquals(3, numbers.proficiency)
        assertEquals(14, numbers.passivePerception)
    }

    @Test
    fun `a skill mark cycles through none proficient and expertise`() {
        assertEquals(Proficiency.PROFICIENT, Proficiency.NONE.next())
        assertEquals(Proficiency.EXPERTISE, Proficiency.PROFICIENT.next())
        assertEquals(Proficiency.NONE, Proficiency.EXPERTISE.next())
    }
}
