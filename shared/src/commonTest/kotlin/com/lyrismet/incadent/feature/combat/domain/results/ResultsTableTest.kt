package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultsTableTest {
    private val hero = member(1, "Кира", CombatSide.PARTY)
    private val goblin = member(2, "Гоблин 1", CombatSide.ENEMY, speciesId = 7, state = CombatState.DOWN)
    private val wolf = member(3, "Волк", CombatSide.ENEMY, speciesId = 8)

    private val combat =
        combatOf(
            listOf(wolf, goblin, hero),
            listOf(
                strike(1, 1, 2, 9),
                marker(2, CombatEventKind.DOWN, 1, 2),
                strike(3, 3, 1, 4),
            ),
        )

    @Test
    fun `rows go party first then enemies`() {
        assertEquals(listOf("Кира", "Волк", "Гоблин 1"), resultRows(combat).map { it.name })
    }

    @Test
    fun `dealt and taken sum the damage`() {
        val rows = resultRows(combat).associateBy { it.name }
        assertEquals(9, rows.getValue("Кира").dealt)
        assertEquals(4, rows.getValue("Кира").taken)
        assertEquals(9, rows.getValue("Гоблин 1").taken)
    }

    @Test
    fun `a downed enemy names who put it down`() {
        assertEquals(ParticipantResult.Down("Кира"), resultRows(combat).first { it.name == "Гоблин 1" }.result)
    }

    @Test
    fun `a standing enemy without numbers shows how it looks`() {
        assertEquals(ParticipantResult.Looks(CombatState.WHOLE), resultRows(combat).first { it.name == "Волк" }.result)
    }
}
