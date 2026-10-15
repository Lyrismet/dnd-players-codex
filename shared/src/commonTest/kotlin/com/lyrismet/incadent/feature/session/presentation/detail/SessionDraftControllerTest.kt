package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.token
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val moment = LocalDateTime(2026, 1, 1, 12, 0)
private val gromm = MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE)

class SessionDraftControllerTest {
    private val repository = FakeSessionEntryRepository()

    private fun TestScope.controller() =
        SessionDraftController(
            sessionNoteId = 3,
            entryRepository = repository,
            undoController = UndoController(backgroundScope),
            draft = mutableStateOf(TextFieldValue("")),
            editingEntryId = mutableStateOf<Long?>(null),
            selectedEntryId = mutableStateOf<Long?>(null),
        )

    private fun entry(
        id: Long,
        body: String,
    ) = SessionEntry(id, 3, moment, body)

    @Test
    fun `the at button appends a trigger and puts the cursor at the end`() =
        runTest {
            val composer = controller()
            composer.draft.value = TextFieldValue("встретили")

            composer.insertMentionTrigger()

            assertEquals("встретили @", composer.draft.value.text)
            assertEquals(TextRange(11), composer.draft.value.selection)
        }

    @Test
    fun `a picked mention replaces the trailing query with its token`() =
        runTest {
            val composer = controller()
            composer.draft.value = TextFieldValue("видели @Гр")

            composer.pickMention(gromm.token())

            assertEquals("видели ${gromm.token()} ", composer.draft.value.text)
        }

    @Test
    fun `a typed name turns into a token on send and the draft clears`() =
        runTest {
            val composer = controller()
            composer.draft.value = TextFieldValue("пришёл @Громм")

            composer.submit(this, emptyList(), MentionIndex(listOf(gromm)))
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("пришёл ${gromm.token()}"), repository.all.map { it.body })
            assertEquals("", composer.draft.value.text)
        }

    @Test
    fun `a blank draft sends nothing`() =
        runTest {
            val composer = controller()
            composer.draft.value = TextFieldValue("   ")

            composer.submit(this, emptyList(), MentionIndex.EMPTY)
            testScheduler.advanceUntilIdle()

            assertEquals(0, repository.all.size)
        }

    @Test
    fun `an edit rewrites the entry and an unchanged edit writes nothing`() =
        runTest {
            val existing = entry(1, "старый текст")
            val withEntry = FakeSessionEntryRepository(listOf(existing))
            val composer =
                SessionDraftController(
                    3,
                    withEntry,
                    UndoController(backgroundScope),
                    mutableStateOf(TextFieldValue("")),
                    mutableStateOf<Long?>(null),
                    mutableStateOf<Long?>(null),
                )

            composer.edit(1, listOf(existing))
            assertEquals("старый текст", composer.draft.value.text)

            composer.submit(this, listOf(existing), MentionIndex.EMPTY)
            testScheduler.advanceUntilIdle()
            assertEquals(false, withEntry.all.single().edited)

            composer.edit(1, listOf(existing))
            composer.draft.value = TextFieldValue("новый текст")
            composer.submit(this, listOf(existing), MentionIndex.EMPTY)
            testScheduler.advanceUntilIdle()
            assertEquals("новый текст", withEntry.all.single().body)
            assertEquals(true, withEntry.all.single().edited)
        }

    @Test
    fun `deleting the edited entry drops the edit and offers undo`() =
        runTest {
            val existing = entry(1, "текст")
            val withEntry = FakeSessionEntryRepository(listOf(existing))
            val undo = UndoController(backgroundScope)
            val composer =
                SessionDraftController(
                    3,
                    withEntry,
                    undo,
                    mutableStateOf(TextFieldValue("")),
                    mutableStateOf<Long?>(null),
                    mutableStateOf<Long?>(null),
                )
            composer.edit(1, listOf(existing))

            composer.delete(this, 1, listOf(existing), "Запись удалена")
            testScheduler.advanceUntilIdle()

            assertEquals(0, withEntry.all.size)
            assertNull(composer.editingEntryId.value)
            assertEquals("", composer.draft.value.text)
            assertEquals("Запись удалена", undo.current.value?.title)
        }

    @Test
    fun `tapping the selected entry again deselects it`() =
        runTest {
            val composer = controller()

            composer.toggleSelected(5)
            assertEquals(5L, composer.selectedEntryId.value)

            composer.toggleSelected(5)
            assertNull(composer.selectedEntryId.value)
        }

    @Test
    fun `a cut token is removed whole and the caret stays after the text before it`() =
        runTest {
            val composer = controller()
            val cut = "ждали @[Громм|n:1 там"

            composer.onDraftChanged(cut, TextRange(cut.length))

            assertEquals("ждали  там", composer.draft.value.text)
        }
}
