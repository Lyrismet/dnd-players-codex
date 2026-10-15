package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.domain.grouping.CodexRegions
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// "Таверна" sits in "Север", which sits in "Королевство" - a search has to reach every place above
private val kingdom = Location(id = 1, name = "Королевство", description = "")
private val north = Location(id = 2, name = "Север", description = "", parentId = 1)
private val tavern = Location(id = 3, name = "Таверна", description = "Шумная", parentId = 2)
private val locations = listOf(kingdom, north, tavern)

private val monk =
    Npc(
        id = 1,
        name = "Ода",
        status = NpcStatus.NEUTRAL,
        lifeState = NpcLifeState.ALIVE,
        description = "Монах",
        locationIds = listOf(tavern.id),
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
        description = "Ловкий карманник",
        portrait = null,
    )

private val regions = CodexRegions(npcs, locations)

class CodexSearchMatchingTest {
    @Test
    fun `Npc search matches the name only`() {
        assertTrue(monk.matchesQuery("ода", regions))
        assertFalse(monk.matchesQuery("человек", regions))
        assertFalse(monk.matchesQuery("орден", regions))
        assertFalse(monk.matchesQuery("монах", regions))
    }

    @Test
    fun `Npc search matches the place it is met at and the places above it`() {
        assertTrue(monk.matchesQuery("таверна", regions))
        assertTrue(monk.matchesQuery("королевство", regions))
    }

    @Test
    fun `search is case-insensitive and treats yo as ye`() {
        assertTrue(monk.matchesQuery("ОДА", regions))
        val yo = monk.copy(name = "Артём")
        assertTrue(yo.matchesQuery("артем", regions))
        assertTrue(yo.matchesQuery("АРТЁМ", regions))
    }

    @Test
    fun `Npc search rejects text found in no title`() {
        assertFalse(monk.matchesQuery("гоблин", regions))
    }

    @Test
    fun `Quest search matches the title and the places above its giver and not reward description or giver name`() {
        assertTrue(errand.matchesQuery("ключ", regions))
        assertTrue(errand.matchesQuery("королевство", regions))
        assertFalse(errand.matchesQuery("серебро", regions))
        assertFalse(errand.matchesQuery("склепе", regions))
        assertFalse(errand.matchesQuery("ода", regions))
    }

    @Test
    fun `Location search matches its name and the places above it and not its description`() {
        assertTrue(tavern.matchesQuery("таверна", regions))
        assertTrue(tavern.matchesQuery("север", regions))
        assertTrue(tavern.matchesQuery("королевство", regions))
        assertFalse(kingdom.matchesQuery("север", regions))
        assertFalse(tavern.matchesQuery("шумная", regions))
    }

    @Test
    fun `PartyMember search matches the name only`() {
        assertTrue(bard.matchesQuery("бран"))
        assertFalse(bard.matchesQuery("полурослик"))
        assertFalse(bard.matchesQuery("даня"))
        assertFalse(bard.matchesQuery("карманник"))
    }

    @Test
    fun `every search matches everything for an empty query`() {
        assertTrue(monk.matchesQuery("", regions))
        assertTrue(errand.matchesQuery("", regions))
        assertTrue(tavern.matchesQuery("", regions))
        assertTrue(bard.matchesQuery(""))
    }
}
