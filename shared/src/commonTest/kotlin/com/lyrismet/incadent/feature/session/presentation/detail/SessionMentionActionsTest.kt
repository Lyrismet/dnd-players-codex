package com.lyrismet.incadent.feature.session.presentation.detail

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionToken
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

private val moment = LocalDateTime(2026, 1, 1, 12, 0)

class SessionMentionActionsTest {
    private val from = MentionRef(EntityKind.NPC, 1_000_000_000_001)
    private val to = MentionRef(EntityKind.QUEST, 4)

    @Test
    fun `relinkAll rewrites only the notes that hold the tag and the way back restores them`() =
        runTest {
            val tagged = SessionEntry(1, 3, moment, "видели ${mentionToken(from, "Xyz")}")
            val other = SessionEntry(2, 3, moment, "ничего")
            val repository = FakeSessionEntryRepository(listOf(tagged, other))
            val actions = SessionMentionActions(repository, UndoController(backgroundScope))

            val revert = actions.relinkAll(from, to, "Xyz", listOf(tagged, other))

            assertEquals("видели ${mentionToken(to, "Xyz")}", repository.all.first { it.id == 1L }.body)
            assertEquals("ничего", repository.all.first { it.id == 2L }.body)
            revert()
            assertEquals(tagged.body, repository.all.first { it.id == 1L }.body)
        }
}
