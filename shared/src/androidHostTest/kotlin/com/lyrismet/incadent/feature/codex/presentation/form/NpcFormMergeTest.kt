package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.data.NpcMergeRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.NpcRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NpcFormMergeTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val merge = NpcMergeRepositoryImpl(db.database, db.current, npcs)

    private val texts: suspend (String) -> Pair<String, String> = { it to "" }

    private fun npc(name: String) = Npc(0, name, NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")

    @Test
    fun `a name that is free merges nothing`() =
        runTest {
            val mine = npcs.upsert(npc("Вазгар"))
            val formMerge = NpcFormMerge(npcs, merge, UndoController(backgroundScope), texts)

            assertFalse(formMerge.run(mine, "Другой"))
            assertNotNull(npcs.getById(mine))
        }

    @Test
    fun `the edited npc goes into the one with the typed name and the bar takes it back`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар"))
            val mine = npcs.upsert(npc("Вазгар старший"))
            val undo = UndoController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
            val formMerge = NpcFormMerge(npcs, merge, undo, texts)

            assertTrue(formMerge.run(mine, " вазгар "))
            assertNull(npcs.getById(mine))
            assertNotNull(npcs.getById(survivor))

            assertNotNull(undo.current.value).onUndo()
            assertEquals(
                setOf(survivor, mine),
                npcs
                    .observeAll()
                    .first()
                    .map { it.id }
                    .toSet(),
            )
        }
}
