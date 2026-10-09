package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.codexgroup.CodexRegions
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// "Таверна" sits in "Север", which is itself a place whose region is "Королевство" - the chain must reach it
private val kingdom = Location(id = 1, name = "Королевство", type = "Регион", description = "", region = "")
private val north = Location(id = 2, name = "Север", type = "Регион", description = "", region = "Королевство")
private val tavern =
    Location(id = 3, name = "Таверна", type = "Постоялый двор", description = "Шумная", region = "Север")
private val locations = listOf(kingdom, north, tavern)

private val monk =
    Npc(
        id = 1,
        name = "Ода",
        status = NpcStatus.NEUTRAL,
        lifeState = NpcLifeState.ALIVE,
        description = "Монах",
        locationId = tavern.id,
        race = "Человек",
        faction = "Орден",
    )

private val npcs = listOf(monk)

private val errand =
    Quest(
        id = 1,
        title = "Ключ",
        status = QuestStatus.ACTIVE,
        reward = "Серебро",
        givenByNpcId = monk.id,
        locationId = null,
        description = "Найти в склепе",
    )

private val bard =
    PartyMember(
        id = 1,
        name = "Бран",
        characterClass = "Плут",
        race = "Полурослик",
        level = 3,
        playerName = "Даня",
        isPlayerCharacter = false,
        presence = PartyPresence.IN,
        hpMax = 20,
        hpCurrent = 20,
        armorClass = 13,
        initiativeBonus = 2,
        description = "Ловкий карманник",
        portraitBase64 = null,
    )

private val regions = CodexRegions(npcs, listOf(errand), locations)
private val npcsById = npcs.associateBy { it.id }

class CodexSearchMatchingTest {
    @Test
    fun `Npc search matches race faction and description`() {
        assertTrue(monk.matchesQuery("человек", regions))
        assertTrue(monk.matchesQuery("орден", regions))
        assertTrue(monk.matchesQuery("монах", regions))
    }

    @Test
    fun `Npc search matches the region of its place through the region chain`() {
        assertTrue(monk.matchesQuery("королевство", regions))
    }

    @Test
    fun `Npc search is case-insensitive across fields`() {
        assertTrue(monk.matchesQuery("ОРДЕН", regions))
    }

    @Test
    fun `Npc search rejects text found in no field`() {
        assertFalse(monk.matchesQuery("гоблин", regions))
    }

    @Test
    fun `Quest search matches reward description and giver name`() {
        assertTrue(errand.matchesQuery("серебро", npcsById, regions))
        assertTrue(errand.matchesQuery("склепе", npcsById, regions))
        assertTrue(errand.matchesQuery("ода", npcsById, regions))
    }

    @Test
    fun `Quest search matches its region through the giver place when it has no where`() {
        assertTrue(errand.matchesQuery("королевство", npcsById, regions))
    }

    @Test
    fun `Location search matches its raw and resolved region`() {
        assertTrue(tavern.matchesQuery("север", regions))
        assertTrue(tavern.matchesQuery("королевство", regions))
        assertTrue(tavern.matchesQuery("постоялый", regions))
    }

    @Test
    fun `Location search matches its description`() {
        assertTrue(tavern.matchesQuery("шумная", regions))
    }

    @Test
    fun `PartyMember search matches race player and description`() {
        assertTrue(bard.matchesQuery("полурослик"))
        assertTrue(bard.matchesQuery("даня"))
        assertTrue(bard.matchesQuery("карманник"))
        assertFalse(bard.matchesQuery("гоблин"))
    }

    @Test
    fun `every search matches everything for an empty query`() {
        assertTrue(monk.matchesQuery("", regions))
        assertTrue(errand.matchesQuery("", npcsById, regions))
        assertTrue(tavern.matchesQuery("", regions))
        assertTrue(bard.matchesQuery(""))
    }
}
