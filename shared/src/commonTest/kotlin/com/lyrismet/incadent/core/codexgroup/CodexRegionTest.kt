package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CodexRegionTest {
    @Test
    fun `a place without a region has no region`() {
        val place = location(1, "Город", region = "")

        assertNull(place.resolvedRegion(listOf(place)))
    }

    @Test
    fun `the dash placeholder counts as no region`() {
        val place = location(1, "Город", region = "—")

        assertNull(place.resolvedRegion(listOf(place)))
    }

    @Test
    fun `a region that names no other place stays as the region`() {
        val place = location(1, "Таверна", region = "Нижний Город")

        assertEquals("Нижний Город", place.resolvedRegion(listOf(place)))
    }

    @Test
    fun `a region that names another place inherits that place's region`() {
        val north = location(1, "Север", region = "")
        val castle = location(2, "Замок", region = "Север")
        val all = listOf(north, castle)

        assertEquals("Север", castle.resolvedRegion(all))
    }

    @Test
    fun `nested places inherit the region of the top-level place recursively`() {
        val north = location(1, "Север", region = "")
        val castle = location(2, "Замок", region = "Север")
        val tavern = location(3, "Таверна", region = "Замок")
        val all = listOf(north, castle, tavern)

        assertEquals("Север", tavern.resolvedRegion(all))
    }

    @Test
    fun `a place never resolves its region through itself`() {
        val forest = location(1, "Лес", region = "Лес")

        assertEquals("Лес", forest.resolvedRegion(listOf(forest)))
    }

    @Test
    fun `a region loop still terminates with a region`() {
        val first = location(1, "A", region = "B")
        val second = location(2, "B", region = "A")

        assertNotNull(first.resolvedRegion(listOf(first, second)))
        assertEquals("A", first.resolvedRegion(listOf(first, second)))
    }

    @Test
    fun `an npc takes the region of its place`() {
        val north = location(1, "Север", region = "")
        val castle = location(2, "Замок", region = "Север")
        val npc = npc(1, locationId = castle.id)

        assertEquals("Север", npcRegion(npc, listOf(north, castle)))
    }

    @Test
    fun `an npc without a place has no region`() {
        val castle = location(2, "Замок", region = "Север")

        assertNull(npcRegion(npc(1, locationId = null), listOf(castle)))
    }

    @Test
    fun `an npc whose place has no region has no region`() {
        val castle = location(2, "Замок", region = "")

        assertNull(npcRegion(npc(1, locationId = castle.id), listOf(castle)))
    }

    @Test
    fun `a quest takes the region of its where place`() {
        val south = location(1, "Юг", region = "")
        val mill = location(2, "Мельница", region = "Юг")

        assertEquals("Юг", questRegion(quest(1, locationId = mill.id), emptyList(), listOf(south, mill)))
    }

    @Test
    fun `a quest without a where place falls back to the place of its giver`() {
        val south = location(1, "Юг", region = "")
        val mill = location(2, "Мельница", region = "Юг")
        val giver = npc(1, locationId = mill.id)

        assertEquals(
            "Юг",
            questRegion(quest(1, givenByNpcId = giver.id), listOf(giver), listOf(south, mill)),
        )
    }

    @Test
    fun `a quest whose where place has no region does not fall back to the giver`() {
        val mill = location(1, "Мельница", region = "")
        val tavern = location(2, "Таверна", region = "Юг")
        val giver = npc(1, locationId = tavern.id)

        assertNull(
            questRegion(
                quest(1, locationId = mill.id, givenByNpcId = giver.id),
                listOf(giver),
                listOf(mill, tavern),
            ),
        )
    }

    @Test
    fun `CodexRegions resolves the same regions as the direct lookups`() {
        val north = location(1, "Север", region = "")
        val castle = location(2, "Замок", region = "Север")
        val giver = npc(1, locationId = castle.id)
        val quest = quest(1, givenByNpcId = giver.id)
        val all = listOf(north, castle)
        val regions = CodexRegions(listOf(giver), listOf(quest), all)

        assertEquals(npcRegion(giver, all), regions.regionOfNpc(giver))
        assertEquals(questRegion(quest, listOf(giver), all), regions.regionOfQuest(quest))
        assertEquals(castle.resolvedRegion(all), regions.regionOfLocation(castle))
    }
}

internal fun location(
    id: Long,
    name: String,
    region: String = "",
    type: String = "Город",
) = Location(id = id, name = name, type = type, description = "", region = region)

internal fun npc(
    id: Long,
    name: String = "NPC $id",
    status: NpcStatus = NpcStatus.FRIEND,
    lifeState: NpcLifeState = NpcLifeState.ALIVE,
    locationId: Long? = null,
    race: String = "",
    faction: String = "",
) = Npc(
    id = id,
    name = name,
    status = status,
    lifeState = lifeState,
    description = "",
    locationId = locationId,
    race = race,
    faction = faction,
)

internal fun quest(
    id: Long,
    title: String = "Quest $id",
    status: QuestStatus = QuestStatus.ACTIVE,
    givenByNpcId: Long? = null,
    locationId: Long? = null,
) = Quest(
    id = id,
    title = title,
    status = status,
    reward = "",
    givenByNpcId = givenByNpcId,
    locationId = locationId,
)
