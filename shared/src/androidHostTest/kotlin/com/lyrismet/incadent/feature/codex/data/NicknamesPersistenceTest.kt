package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NicknamesPersistenceTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val quests = QuestRepositoryImpl(db.database, db.current)
    private val locations = LocationRepositoryImpl(db.database, db.current)

    @Test
    fun `a party member keeps its nicknames apart from the notes`() =
        runTest {
            val member =
                PartyMember(
                    id = 0,
                    name = "Лира",
                    characterClass = "Бард",
                    race = "Эльф",
                    level = 3,
                    playerName = "Анна",
                    isPlayerCharacter = false,
                    presence = PartyPresence.IN,
                    hpMax = 20,
                    hpCurrent = 20,
                    description = "заметка",
                    nicknames = listOf("Певунья", "Лири"),
                )
            val id = party.upsert(member)
            assertEquals(member.copy(id = id), party.getById(id))

            party.upsert(member.copy(id = id, nicknames = listOf("Тень"), description = "другая"))
            assertEquals(listOf("Тень"), party.getById(id)?.nicknames)
            assertEquals("другая", party.getById(id)?.description)
        }

    @Test
    fun `an npc keeps its nicknames apart from the notes`() =
        runTest {
            val npc =
                Npc(
                    id = 0,
                    name = "Рагнар",
                    status = NpcStatus.NEUTRAL,
                    lifeState = NpcLifeState.ALIVE,
                    description = "заметка",
                    locationIds = emptyList(),
                    race = "Гном",
                    nicknames = listOf("Седой", "Кузнец"),
                )
            val id = npcs.upsert(npc)
            assertEquals(npc.copy(id = id), npcs.getById(id))

            npcs.upsert(npc.copy(id = id, nicknames = emptyList(), description = "другая"))
            assertEquals(emptyList(), npcs.getById(id)?.nicknames)
            assertEquals("другая", npcs.getById(id)?.description)
        }

    @Test
    fun `a quest keeps its nicknames apart from the description`() =
        runTest {
            val quest = Quest(0, "Меч", QuestStatus.ACTIVE, "50", null, null, "описание", listOf("Клинок", "Реликвия"))
            val id = quests.upsert(quest)
            assertEquals(quest.copy(id = id), quests.getById(id))

            quests.upsert(quest.copy(id = id, nicknames = listOf("Дар"), description = "другое"))
            assertEquals(listOf("Дар"), quests.getById(id)?.nicknames)
            assertEquals("другое", quests.getById(id)?.description)
        }

    @Test
    fun `a place keeps its nicknames apart from the description`() =
        runTest {
            val place = Location(0, "Гавань", "описание", nicknames = listOf("Порт", "Ворота"))
            val id = locations.upsert(place)
            assertEquals(place.copy(id = id), locations.getById(id))

            locations.upsert(place.copy(id = id, nicknames = listOf("Док"), description = "другое"))
            assertEquals(listOf("Док"), locations.getById(id)?.nicknames)
            assertEquals("другое", locations.getById(id)?.description)
        }
}
