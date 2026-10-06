package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickEditApplyTest {
    private val npc =
        Npc(
            id = 1,
            name = "Кассиан",
            status = NpcStatus.NEUTRAL,
            lifeState = NpcLifeState.ALIVE,
            description = "",
            locationId = null,
            race = "Человек",
            faction = "Орден",
        )

    private val member =
        PartyMember(
            id = 2,
            name = "Кайра",
            characterClass = "Воин",
            race = "Эльф",
            level = 5,
            playerName = "Макс",
            isPlayerCharacter = false,
            presence = PartyPresence.IN,
            hpMax = 40,
            hpCurrent = 38,
            armorClass = 16,
            initiativeBonus = 2,
            description = "",
            portraitUri = null,
        )

    @Test
    fun `a text edit writes its field and leaves the rest of the npc alone`() {
        val edited = npc.withQuickEdit(QuickEditField.FACTION, QuickEditValue.Text("Гильдия"))

        assertEquals("Гильдия", edited?.faction)
        assertEquals(npc.name, edited?.name)
    }

    @Test
    fun `a field the entity does not own is rejected instead of written`() {
        assertNull(npc.withQuickEdit(QuickEditField.REWARD, QuickEditValue.Text("200")))
        assertNull(member.withQuickEdit(QuickEditField.PLACE, QuickEditValue.Link(3)))
    }

    @Test
    fun `an unlinked place clears the npc location`() {
        val placed = npc.copy(locationId = 7)

        assertNull(placed.withQuickEdit(QuickEditField.PLACE, QuickEditValue.Link(null))?.locationId)
    }

    @Test
    fun `lowering max hp pulls the current hp down with it`() {
        val edited = member.withQuickEdit(QuickEditField.HP_MAX, QuickEditValue.Number(30))

        assertEquals(30, edited?.hpMax)
        assertEquals(30, edited?.hpCurrent)
    }

    @Test
    fun `a stat written out of range is clamped by the party rules`() {
        val edited = member.withQuickEdit(QuickEditField.LEVEL, QuickEditValue.Number(40))

        assertEquals(20, edited?.level)
    }

    @Test
    fun `a quest giver link is written and can be cleared`() {
        val quest =
            Quest(id = 3, title = "Ключ", status = QuestStatus.ACTIVE, reward = "", givenByNpcId = 1, locationId = null)

        assertEquals(2L, quest.withQuickEdit(QuickEditField.GIVER, QuickEditValue.Link(2))?.givenByNpcId)
        assertNull(quest.withQuickEdit(QuickEditField.GIVER, QuickEditValue.Link(null))?.givenByNpcId)
    }

    @Test
    fun `the one-tap status functions change only their own field`() {
        assertEquals(NpcStatus.ENEMY, npc.withStatus(NpcStatus.ENEMY).status)
        assertEquals(NpcLifeState.DEAD, npc.withLifeState(NpcLifeState.DEAD).lifeState)
        assertEquals(PartyPresence.AWAY, member.withPresence(PartyPresence.AWAY).presence)
    }
}
