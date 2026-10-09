package com.lyrismet.incadent.core.quickedit

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import com.lyrismet.incadent.core.entitysummary.FakeLocationRepository
import com.lyrismet.incadent.core.entitysummary.FakeNpcRepository
import com.lyrismet.incadent.core.entitysummary.FakePartyRepository
import com.lyrismet.incadent.core.entitysummary.FakeQuestRepository
import com.lyrismet.incadent.core.entitysummary.RecordingNavigator
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.repository.MentionRepositories
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickEditInteractionsTest {
    @Test
    fun `holding a field opens its editor with the current text and marks the hint seen`() =
        runTest {
            val harness = Harness()

            harness.interactions.onEvent(this, QuickEditUiEvent.FieldHeld(QuickEditField.NAME, "Кассиан"))

            assertEquals(InlineEdit(QuickEditField.NAME, "Кассиан"), harness.inlineEdit.value)
            assertTrue(harness.hintSeen)
        }

    @Test
    fun `cancelling closes the editor and writes nothing`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.NAME, "Кассиан")

            harness.interactions.onEvent(this, QuickEditUiEvent.DraftChanged("Новое"))
            harness.interactions.onEvent(this, QuickEditUiEvent.EditCancelled)
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals("Кассиан", harness.npcs.stored(1)?.name)
        }

    @Test
    fun `done saves the trimmed draft and closes the editor`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.NAME, "Кассиан")

            harness.interactions.onEvent(this, QuickEditUiEvent.DraftChanged("  Ворон "))
            harness.interactions.onEvent(this, QuickEditUiEvent.EditSaved)
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals("Ворон", harness.npcs.stored(1)?.name)
        }

    @Test
    fun `done with a blank name closes the editor and keeps the old name`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.NAME, "Кассиан")

            harness.interactions.onEvent(this, QuickEditUiEvent.DraftChanged("   "))
            harness.interactions.onEvent(this, QuickEditUiEvent.EditSaved)
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals("Кассиан", harness.npcs.stored(1)?.name)
        }

    @Test
    fun `picking a link writes it at once and closes the picker`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.PLACE, "")

            harness.interactions.onEvent(this, QuickEditUiEvent.LinkChosen(9))
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals(9L, harness.npcs.stored(1)?.locationId)
        }

    @Test
    fun `closing the sheet drops an open editor`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.NAME, "Кассиан")

            harness.interactions.onSheetClosed()

            assertNull(harness.inlineEdit.value)
        }

    private class Harness {
        val npcs = FakeNpcRepository(Npc(1, "Кассиан", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", null, "", ""))
        val inlineEdit = mutableStateOf<InlineEdit?>(null)
        val selected = mutableStateOf<EntityRef?>(EntityRef.Npc(1))
        var hintSeen = false
        private val entitySheet =
            EntitySheetInteractions(
                selected,
                MentionRepositories(npcs, FakeLocationRepository(), FakeQuestRepository()),
                FakePartyRepository(),
                RecordingNavigator(),
            )
        val interactions =
            QuickEditInteractions(inlineEdit, selected, entitySheet, ImageCompressor()) { hintSeen = true }

        fun open(
            field: QuickEditField,
            draft: String,
        ) {
            inlineEdit.value = InlineEdit(field, draft)
        }
    }
}
