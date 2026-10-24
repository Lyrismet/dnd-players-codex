package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import kotlin.test.Test
import kotlin.test.assertEquals

class CalcTargetOrderTest {
    private fun member(
        id: Long,
        side: CombatSide,
    ) = Combatant(
        id = id,
        name = "М$id",
        side = side,
        npcId = null,
        speciesId = null,
        partyMemberId = null,
        enteredWhole = true,
        state = CombatState.WHOLE,
        joinedRound = 1,
        deathSuccesses = 0,
        deathFailures = 0,
    )

    private val enemy = member(1, CombatSide.ENEMY)
    private val ally = member(2, CombatSide.ALLY)
    private val friend = member(3, CombatSide.PARTY)
    private val me = member(4, CombatSide.PARTY)
    private val all = listOf(enemy, ally, friend, me)
    private val own = setOf(4L)

    private fun ids(
        actor: Combatant,
        mode: CalcMode,
    ) = all.inTargetOrder(actor, mode, own).map { it.id }

    @Test
    fun `an enemy hits me first then the party then allies and enemies last`() {
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(enemy, CalcMode.DAMAGE))
    }

    @Test
    fun `the party hits enemies first then allies then the party`() {
        assertEquals(listOf(1L, 2L, 4L, 3L), ids(me, CalcMode.DAMAGE))
        assertEquals(listOf(1L, 2L, 4L, 3L), ids(friend, CalcMode.DAMAGE))
    }

    @Test
    fun `an enemy heals enemies first`() {
        assertEquals(listOf(1L, 4L, 3L, 2L), ids(enemy, CalcMode.HEAL))
    }

    @Test
    fun `a player heals himself first then the party then allies then enemies`() {
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(me, CalcMode.HEAL))
        assertEquals(listOf(3L, 4L, 2L, 1L), ids(friend, CalcMode.HEAL))
    }

    @Test
    fun `no actor keeps the turn order`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), all.inTargetOrder(null, CalcMode.DAMAGE, own).map { it.id })
    }
}
