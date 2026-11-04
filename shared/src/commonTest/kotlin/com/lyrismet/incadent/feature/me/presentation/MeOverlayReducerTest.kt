package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeOverlayReducerTest {
    @Test
    fun `an ability score is held between 1 and 30`() {
        val edit = MeOverlay.AbilityEdit(Ability.WISDOM, 5, CharacterSheet(abilities = mapOf(Ability.WISDOM to 30)))
        assertEquals(30, (edit.reduce(OverlayEvent.NumberStepped(1)) as MeOverlay.AbilityEdit).score)
        val low = edit.copy(sheet = CharacterSheet(abilities = mapOf(Ability.WISDOM to 1)))
        assertEquals(1, (low.reduce(OverlayEvent.NumberStepped(-1)) as MeOverlay.AbilityEdit).score)
    }

    @Test
    fun `the numbers of an ability sheet follow the score and the save`() {
        val edit =
            MeOverlay.AbilityEdit(
                Ability.INTELLIGENCE,
                5,
                CharacterSheet(
                    abilities =
                        mapOf(
                            Ability.INTELLIGENCE to 17,
                        ),
                ),
            )
        val changed =
            edit
                .reduce(OverlayEvent.NumberStepped(1))
                .reduce(OverlayEvent.SaveToggled) as MeOverlay.AbilityEdit
        assertEquals(18, changed.score)
        assertEquals(4, changed.bonus)
        assertEquals(7, changed.saveBonus)
        assertTrue(changed.save)
    }

    @Test
    fun `a skill sheet takes a proficiency and an adjustment`() {
        val edit = MeOverlay.SkillEdit(Skill.STEALTH, 5, CharacterSheet(abilities = mapOf(Ability.DEXTERITY to 14)))
        val changed =
            edit
                .reduce(OverlayEvent.ProficiencyChosen(Proficiency.EXPERTISE))
                .reduce(OverlayEvent.NumberStepped(1)) as MeOverlay.SkillEdit
        assertEquals(9, changed.bonus)
    }

    @Test
    fun `speed and armor class stay in their ranges`() {
        assertEquals(0, (MeOverlay.Speed(0).reduce(OverlayEvent.NumberStepped(-5)) as MeOverlay.Speed).value)
        assertEquals(
            99,
            (MeOverlay.ArmorClass(99, "").reduce(OverlayEvent.NumberStepped(1)) as MeOverlay.ArmorClass).value,
        )
    }

    @Test
    fun `an overlay ignores an event that is not for it`() {
        val speed = MeOverlay.Speed(30)
        assertEquals(speed, speed.reduce(OverlayEvent.SaveToggled))
    }

    @Test
    fun `the fill moves between the steps and stops at both ends`() {
        var draft = FillDraft()
        draft = draft.reduce(OverlayEvent.FillBack)
        assertEquals(1, draft.step)
        repeat(6) { draft = draft.reduce(OverlayEvent.FillNext) }
        assertEquals(FillDraft.LAST_STEP, draft.step)
    }

    @Test
    fun `a skipped step is remembered`() {
        val draft = FillDraft().reduce(OverlayEvent.FillSkip)
        assertEquals(2, draft.step)
        assertEquals(setOf(1), draft.skipped)
    }

    @Test
    fun `the fill scores marks and saves are edited in place`() {
        val draft =
            FillDraft()
                .reduce(OverlayEvent.FillAbilityStepped(Ability.INTELLIGENCE, 8))
                .reduce(OverlayEvent.FillSaveToggled(Ability.WISDOM))
                .reduce(OverlayEvent.FillSkillTapped(Skill.HISTORY))
                .reduce(OverlayEvent.FillSkillTapped(Skill.HISTORY))
        assertEquals(18, draft.abilities[Ability.INTELLIGENCE])
        assertEquals(setOf(Ability.WISDOM), draft.saves)
        assertEquals(Proficiency.EXPERTISE, draft.marks[Skill.HISTORY])
    }
}
