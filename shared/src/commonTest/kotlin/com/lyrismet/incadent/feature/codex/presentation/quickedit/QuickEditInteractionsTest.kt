package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeLocationRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeNpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakePartyRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeQuestRepository
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

            harness.interactions.onEvent(this, QuickEditUiEvent.PlacesChosen(listOf(9)))
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals(listOf(9L), harness.npcs.stored(1)?.locationIds)
        }

    @Test
    fun `closing the sheet drops an open editor`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.NAME, "Кассиан")

            harness.interactions.onSheetClosed()

            assertNull(harness.inlineEdit.value)
        }

    @Test
    fun `toggling a relation links the quest to the held npc and keeps the editor open`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.GIVEN_QUESTS, "")

            harness.interactions.onEvent(this, QuickEditUiEvent.RelationToggled(5))
            advanceUntilIdle()

            assertEquals(1L, harness.quests.stored(5)?.givenByNpcId)
            assertEquals(QuickEditField.GIVEN_QUESTS, harness.inlineEdit.value?.field)

            harness.interactions.onEvent(this, QuickEditUiEvent.RelationToggled(5))
            advanceUntilIdle()

            assertNull(harness.quests.stored(5)?.givenByNpcId)
        }

    @Test
    fun `removing the portrait writes a null photo and cancelling a crop writes nothing`() =
        runTest {
            val harness = Harness()
            harness.photoCrop.value = QuickPhotoCrop(PhotoCropDraft(ByteArray(0), PhotoSlot.PORTRAIT), EntityRef.Npc(1))

            harness.interactions.onEvent(this, QuickEditUiEvent.PhotoCropCancelled)
            advanceUntilIdle()
            assertNull(harness.photoCrop.value)

            harness.interactions.onEvent(this, QuickEditUiEvent.PhotoRemoved(PhotoSlot.PORTRAIT))
            advanceUntilIdle()
            assertNull(harness.npcs.stored(1)?.portrait)
        }

    private class Harness {
        val npcs = FakeNpcRepository(Npc(1, "Кассиан", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "", ""))
        val quests = FakeQuestRepository(Quest(5, "Меч", QuestStatus.ACTIVE, "", null, null, ""))
        val inlineEdit = mutableStateOf<InlineEdit?>(null)
        val photoCrop = mutableStateOf<QuickPhotoCrop?>(null)
        val selected = mutableStateOf<EntityRef?>(EntityRef.Npc(1))
        var hintSeen = false
        private val entitySheet =
            EntitySheetInteractions(
                selected,
                CodexRepositories(FakePartyRepository(), npcs, quests, FakeLocationRepository()),
                { _, _ -> },
            )
        val interactions =
            QuickEditInteractions(inlineEdit, photoCrop, selected, entitySheet, ImageCompressor()) { hintSeen = true }

        fun open(
            field: QuickEditField,
            draft: String,
        ) {
            inlineEdit.value = InlineEdit(field, draft)
        }
    }
}
