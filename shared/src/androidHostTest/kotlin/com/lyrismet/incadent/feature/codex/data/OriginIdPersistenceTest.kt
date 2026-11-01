package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OriginIdPersistenceTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)

    private fun npc(name: String) =
        Npc(
            id = 0,
            name = name,
            status = NpcStatus.NEUTRAL,
            lifeState = NpcLifeState.ALIVE,
            description = "",
            locationIds = emptyList(),
            race = "",
        )

    @Test
    fun `a new record gets its own origin id and an edit keeps it`() =
        runTest {
            val first = npcs.upsert(npc("Bob"))
            val second = npcs.upsert(npc("Ann"))
            val bob = npcs.getById(first)!!
            assertTrue(bob.originId.isNotBlank())
            assertNotEquals(bob.originId, npcs.getById(second)?.originId)

            npcs.upsert(bob.copy(name = "Robert", originId = ""))

            assertEquals(bob.originId, npcs.getById(first)?.originId)
        }

    @Test
    fun `an undone delete brings the record back with the same origin id`() =
        runTest {
            val id = npcs.upsert(npc("Bob"))
            val stored = npcs.getById(id)!!
            npcs.delete(id)

            npcs.upsert(stored)

            assertEquals(stored.originId, npcs.getById(id)?.originId)
        }
}
