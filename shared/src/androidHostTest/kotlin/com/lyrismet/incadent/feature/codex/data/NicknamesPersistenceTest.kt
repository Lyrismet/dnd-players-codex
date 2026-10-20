package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NicknamesPersistenceTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)

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
}
