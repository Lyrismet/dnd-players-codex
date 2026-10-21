package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityEmblem
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityEmblemShape
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.parseMentions
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val moment = LocalDateTime(2026, 1, 1, 12, 0)
private val goblin = MentionEntity.SpeciesMention(7, "Гоблин")

class SpeciesNoteControllerTest {
    private val body = "@[Гоблин|s:7|4] и @[Волк|s:8] и @[Гоблин|s:7]"
    private val entries = listOf(SessionEntry(1, 3, moment, body))
    private val repository = FakeSessionEntryRepository(entries)

    private fun controller(point: SpeciesNotePoint?) = SpeciesNoteController(mutableStateOf(point), repository)

    private fun card(id: Long) =
        EntitySummaryItem.SpeciesSummary(
            ref = EntityRef.Species(id),
            name = "Гоблин",
            creatureType = CreatureType.HUMANOID,
            notes = "",
            emblem = EntityEmblem("Г", EntityEmblemShape.CIRCLE, SpeciesMentionColor),
            damage = emptyMap(),
            relatedNotes = emptyList(),
            seenInSessions = 1,
        )

    @Test
    fun `the card gets the count of the chip it was opened from`() {
        val second = controller(SpeciesNotePoint(1, 1, 7))
        val first = controller(SpeciesNotePoint(1, 0, 7))

        assertEquals(1, (second.withNoteQuantity(card(7), entries) as EntitySummaryItem.SpeciesSummary).noteQuantity)
        assertEquals(4, (first.withNoteQuantity(card(7), entries) as EntitySummaryItem.SpeciesSummary).noteQuantity)
    }

    @Test
    fun `a card of another species or no chip keeps no count`() {
        val other = card(9)

        val otherCard = controller(SpeciesNotePoint(1, 0, 7)).withNoteQuantity(other, entries)
        val noChip = controller(null).withNoteQuantity(card(7), entries)

        assertNull((otherCard as EntitySummaryItem.SpeciesSummary).noteQuantity)
        assertNull((noChip as EntitySummaryItem.SpeciesSummary).noteQuantity)
    }

    @Test
    fun `a step rewrites the count of that chip only and leaves the note unedited`() =
        runTest {
            controller(SpeciesNotePoint(1, 0, 7)).step(this, entries, delta = 1)
            advanceUntilIdle()

            val saved = repository.all.single()
            assertEquals("@[Гоблин|s:7|5] и @[Волк|s:8] и @[Гоблин|s:7]", saved.body)
            assertEquals(false, saved.edited)
        }

    @Test
    fun `a step below one stays at one`() =
        runTest {
            controller(SpeciesNotePoint(1, 1, 7)).step(this, entries, delta = -1)
            advanceUntilIdle()

            assertEquals(body, repository.all.single().body)
        }

    @Test
    fun `a chip knows which token of its species it is`() {
        val index = MentionIndex(listOf(goblin, MentionEntity.SpeciesMention(8, "Волк")))
        val segments = parseMentions(body, index).toSegments().filterIsInstance<SessionEntrySegment.Mention>()

        assertEquals(listOf(0, 0, 1), segments.map { it.occurrence })
        assertEquals(listOf(4, 1, 1), segments.map { it.chip.quantity })
        assertEquals(MentionRef(EntityKind.SPECIES, 7), goblin.ref)
    }
}
