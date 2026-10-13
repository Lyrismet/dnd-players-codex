package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Photo
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
            locationIds = emptyList(),
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
            description = "",
            portrait = null,
        )

    @Test
    fun `a text edit writes its field and leaves the rest of the npc alone`() {
        val edited = npc.withQuickEdit(QuickEditField.FACTION, QuickEditValue.Text("Гильдия"))

        assertEquals("Гильдия", edited?.faction)
        assertEquals(npc.name, edited?.name)
    }

    @Test
    fun `names and short labels are trimmed and capitalized but the description is kept as typed`() {
        val edited = npc.withQuickEdit(QuickEditField.RACE, QuickEditValue.Text("  эльф "))

        assertEquals("Эльф", edited?.race)
        assertEquals("Гильдия", npc.withQuickEdit(QuickEditField.FACTION, QuickEditValue.Text("гильдия"))?.faction)
        assertEquals(
            "Воин",
            member.withQuickEdit(QuickEditField.CHARACTER_CLASS, QuickEditValue.Text("воин"))?.characterClass,
        )
        assertEquals(
            "  заметка ",
            npc.withQuickEdit(QuickEditField.DESCRIPTION, QuickEditValue.Text("  заметка "))?.description,
        )
    }

    @Test
    fun `a picked portrait is written for npc and party and rejected elsewhere`() {
        val photo = Photo(thumb = byteArrayOf(1), full = byteArrayOf(1, 2, 3))
        val portrait = QuickEditValue.Picture(photo)

        assertEquals(photo, npc.withQuickEdit(QuickEditField.PORTRAIT, portrait)?.portrait)
        assertEquals(photo, member.withQuickEdit(QuickEditField.PORTRAIT, portrait)?.portrait)
        assertNull(npc.withQuickEdit(QuickEditField.NAME, portrait))
    }

    @Test
    fun `a removed photo clears the portrait and a place takes a cover`() {
        val old = Photo(byteArrayOf(9), byteArrayOf(9, 9))
        val fresh = Photo(byteArrayOf(1), byteArrayOf(1, 1))
        val withPortrait = npc.copy(portrait = old)
        val place = Location(id = 4, name = "Храм", description = "", cover = old)

        assertNull(withPortrait.withQuickEdit(QuickEditField.PORTRAIT, QuickEditValue.Picture(null))?.portrait)
        assertEquals(fresh, place.withQuickEdit(QuickEditField.COVER, QuickEditValue.Picture(fresh))?.cover)
        assertNull(place.withQuickEdit(QuickEditField.COVER, QuickEditValue.Picture(null))?.cover)
        assertNull(npc.withQuickEdit(QuickEditField.COVER, QuickEditValue.Picture(fresh)))
    }

    @Test
    fun `a field the entity does not own is rejected instead of written`() {
        assertNull(npc.withQuickEdit(QuickEditField.REWARD, QuickEditValue.Text("200")))
        assertNull(member.withQuickEdit(QuickEditField.PLACE, QuickEditValue.Places(listOf(3))))
    }

    @Test
    fun `an emptied place choice clears the npc locations`() {
        val placed = npc.copy(locationIds = listOf(7, 8))

        assertEquals(
            emptyList(),
            placed.withQuickEdit(QuickEditField.PLACE, QuickEditValue.Places(emptyList()))?.locationIds,
        )
    }

    @Test
    fun `an npc keeps every chosen place while a quest keeps the first one`() {
        val quest =
            Quest(
                id = 3,
                title = "Ключ",
                status = QuestStatus.ACTIVE,
                reward = "",
                givenByNpcId = null,
                locationId = null,
            )
        val places = QuickEditValue.Places(listOf(4, 5))

        assertEquals(listOf(4L, 5L), npc.withQuickEdit(QuickEditField.PLACE, places)?.locationIds)
        assertEquals(4L, quest.withQuickEdit(QuickEditField.PLACE, places)?.locationId)
    }

    @Test
    fun `a cleared number empties the stat`() {
        assertNull(member.withQuickEdit(QuickEditField.HP_MAX, QuickEditValue.Number(null))?.hpMax)
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

    private val quest =
        Quest(7, "Меч", QuestStatus.ACTIVE, "", givenByNpcId = null, locationId = null, description = "")

    @Test
    fun `toggling a giver sets it and clears it again and takes a quest over from another npc`() {
        val given = quest.withGiverToggled(1)

        assertEquals(1L, given.givenByNpcId)
        assertNull(given.withGiverToggled(1).givenByNpcId)
        assertEquals(2L, given.withGiverToggled(2).givenByNpcId)
    }

    @Test
    fun `toggling a quest place sets it and clears it only for the same place`() {
        val placed = quest.withPlaceToggled(3)

        assertEquals(3L, placed.locationId)
        assertNull(placed.withPlaceToggled(3).locationId)
        assertEquals(4L, placed.withPlaceToggled(4).locationId)
    }

    @Test
    fun `toggling an npc place adds it once and drops it again`() {
        val met = npc.withPlaceToggled(3).withPlaceToggled(4)

        assertEquals(listOf(3L, 4L), met.locationIds)
        assertEquals(listOf(4L), met.withPlaceToggled(3).locationIds)
    }
}
