package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
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
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeSpeciesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    fun `picking a species links the npc and picking none clears the link`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.SPECIES, "")

            harness.interactions.onEvent(this, QuickEditUiEvent.LinkChosen(4))
            advanceUntilIdle()

            assertNull(harness.inlineEdit.value)
            assertEquals(4L, harness.npcs.stored(1)?.speciesId)

            harness.open(QuickEditField.SPECIES, "")
            harness.interactions.onEvent(this, QuickEditUiEvent.LinkChosen(null))
            advanceUntilIdle()

            assertNull(harness.npcs.stored(1)?.speciesId)
        }

    @Test
    fun `a species made in the sheet is saved and linked to the npc`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.SPECIES, "")

            harness.interactions.onEvent(this, QuickEditUiEvent.SpeciesCreated("  гоблин ", CreatureType.HUMANOID))
            advanceUntilIdle()

            val linked = harness.npcs.stored(1)?.speciesId
            assertEquals("Гоблин", linked?.let { harness.species.stored(it)?.name })
            assertNull(harness.inlineEdit.value)
        }

    @Test
    fun `a place made in the sheet joins the ticked ones and leads the recent places`() =
        runTest {
            val harness = Harness()
            harness.open(QuickEditField.PLACE, "")

            harness.interactions.onEvent(this, QuickEditUiEvent.PlaceCreated(" рынок ", listOf(9)))
            advanceUntilIdle()

            val ids =
                harness.npcs
                    .stored(1)
                    ?.locationIds
                    .orEmpty()
            assertEquals(2, ids.size)
            assertEquals(9L, ids.first())
            assertEquals("Рынок", harness.locations.stored(ids.last())?.name)
            assertEquals(ids.last(), harness.recentPlaces.value.first())
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
        val npcs = FakeNpcRepository(Npc(1, "Кассиан", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), ""))
        val quests = FakeQuestRepository(Quest(5, "Меч", QuestStatus.ACTIVE, "", null, null, ""))
        val inlineEdit = mutableStateOf<InlineEdit?>(null)
        val photoCrop = mutableStateOf<QuickPhotoCrop?>(null)
        val selected = mutableStateOf<EntityRef?>(EntityRef.Npc(1))
        var hintSeen = false
        val locations = FakeLocationRepository()
        val species = FakeSpeciesRepository()
        val recentPlaces = mutableStateOf(emptyList<Long>())
        private val repositories = CodexRepositories(FakePartyRepository(), npcs, quests, locations, species)
        private val entitySheet = EntitySheetInteractions(selected, repositories, { _, _ -> })
        val interactions =
            QuickEditInteractions(
                inlineEdit,
                photoCrop,
                selected,
                entitySheet,
                ImageCompressor(),
                recentPlaces,
                repositories,
                UndoController(CoroutineScope(Job())),
            ) { hintSeen = true }

        fun open(
            field: QuickEditField,
            draft: String,
        ) {
            inlineEdit.value = InlineEdit(field, draft)
        }
    }
}
