package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodexChecklistTest {
    private val kira = member(1, "Кира", CombatSide.PARTY)
    private val species = mapOf(7L to speciesOf(7))

    @Test
    fun `a downed named enemy is proposed as dead and ticked`() {
        val boss = member(3, "Вожак", CombatSide.ENEMY, npcId = 5, state = CombatState.DOWN)
        val entry = codexChecklist(combatOf(listOf(kira, boss)), species).single()
        assertEquals(CodexItem.MarkDead(5, "Вожак"), entry.item)
        assertTrue(entry.checked)
    }

    @Test
    fun `a downed named ally is proposed as fallen`() {
        val friend = member(4, "Лис", CombatSide.ALLY, npcId = 6, state = CombatState.DOWN)
        assertEquals(
            CodexItem.MarkFallen(6, "Лис"),
            codexChecklist(combatOf(listOf(kira, friend)), species).single().item,
        )
    }

    @Test
    fun `a captured enemy is not marked dead`() {
        val boss = member(3, "Вожак", CombatSide.ENEMY, npcId = 5, state = CombatState.DOWN).copy(captured = true)
        assertTrue(codexChecklist(combatOf(listOf(kira, boss)), species).isEmpty())
    }

    @Test
    fun `a temporary ally is offered but not ticked`() {
        val ally = member(4, "Стражник", CombatSide.ALLY)
        val entry = codexChecklist(combatOf(listOf(kira, ally)), species).single()
        assertEquals(CodexItem.KeepAlly(4, "Стражник"), entry.item)
        assertEquals(false, entry.checked)
    }

    @Test
    fun `hit observations come first and are ticked`() {
        val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7, state = CombatState.DOWN)
        val events = listOf(strike(1, 1, 3, 8), marker(2, CombatEventKind.DOWN, 1, 3))
        val entry = codexChecklist(combatOf(listOf(kira, goblin), events), species).first()
        assertTrue(entry.item is CodexItem.HitObserved)
        assertTrue(entry.checked)
    }

    @Test
    fun `a party member who died is proposed as fallen and ticked`() {
        val hero = member(1, "Кира", CombatSide.PARTY).copy(dead = true, partyMemberId = 4)
        val entry = codexChecklist(combatOf(listOf(hero)), species).single()
        assertEquals(CodexItem.MarkPartyFallen(4, "Кира"), entry.item)
        assertTrue(entry.checked)
    }

    @Test
    fun `a party member who only went down is not proposed`() {
        val hero = member(1, "Кира", CombatSide.PARTY, state = CombatState.DOWN).copy(partyMemberId = 4)
        assertTrue(codexChecklist(combatOf(listOf(hero)), species).isEmpty())
    }
}
