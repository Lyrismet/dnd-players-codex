package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberTieTest {
    private fun member(
        name: String,
        side: CombatSide = CombatSide.ENEMY,
        npcId: Long? = null,
        speciesId: Long? = null,
        isLabel: Boolean = false,
    ) = Combatant(0, name, side, npcId, speciesId, null, true, CombatState.WHOLE, 1, 0, 0, isLabel = isLabel)

    @Test
    fun `a creature of a species is unnamed whatever it is called`() {
        assertEquals(MemberTie.UNNAMED, member("Скелет I", speciesId = 1).tie())
        assertEquals(MemberTie.UNNAMED, member("Совсем другое", speciesId = 1).tie())
    }

    @Test
    fun `the label flag and not the name makes a creature a label`() {
        assertEquals(MemberTie.LABELLED, member("Скелет", speciesId = 1, isLabel = true).tie())
    }

    @Test
    fun `a record id wins over the label flag`() {
        assertEquals(MemberTie.CODEX, member("Ронин", npcId = 5, speciesId = 1, isLabel = true).tie())
    }

    @Test
    fun `a participant made by hand is a label and a party member is neither`() {
        assertEquals(MemberTie.LABELLED, member("Страж").tie())
        assertEquals(MemberTie.PARTY, member("Лира", side = CombatSide.PARTY).tie())
    }
}
