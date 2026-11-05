package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassPersistenceTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val party = PartyRepositoryImpl(db.database, db.current)

    @Test
    fun `an npc keeps its class and subclass and changes them`() =
        runTest {
            val npc =
                Npc(
                    id = 0,
                    name = "Рагнар",
                    status = NpcStatus.NEUTRAL,
                    lifeState = NpcLifeState.ALIVE,
                    description = "",
                    locationIds = emptyList(),
                    race = "Гном",
                    characterClass = "Воин",
                    subclass = "Мастер боя",
                )
            val id = npcs.upsert(npc)
            assertEquals("Воин", npcs.getById(id)?.characterClass)
            assertEquals("Мастер боя", npcs.getById(id)?.subclass)

            npcs.upsert(npc.copy(id = id, characterClass = "Жрец", subclass = ""))
            assertEquals("Жрец", npcs.getById(id)?.characterClass)
            assertEquals("", npcs.getById(id)?.subclass)
        }

    @Test
    fun `a party member keeps its subclass next to the class`() =
        runTest {
            val member =
                PartyMember(
                    id = 0,
                    name = "Торин",
                    characterClass = "Паладин",
                    race = "Человек",
                    level = 5,
                    playerName = "Максим",
                    isPlayerCharacter = false,
                    presence = PartyPresence.IN,
                    hpMax = 52,
                    hpCurrent = 43,
                    description = "",
                    subclass = "Клятва преданности",
                )
            val id = party.upsert(member)
            assertEquals("Клятва преданности", party.getById(id)?.subclass)

            party.upsert(assertNotNull(party.getById(id)).copy(subclass = "Клятва древних"))
            assertEquals("Паладин", party.getById(id)?.characterClass)
            assertEquals("Клятва древних", party.getById(id)?.subclass)
        }
}
