package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitySheetEventTest {
    private val gromm = Npc(1, "Громм", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")
    private val npcs = FakeNpcRepository(gromm)
    private val selected = mutableStateOf<EntityRef?>(null)
    private val opened = mutableListOf<Pair<Long, Long>>()
    private val interactions =
        EntitySheetInteractions(
            selected,
            CodexRepositories(
                FakePartyRepository(),
                npcs,
                FakeQuestRepository(),
                FakeLocationRepository(),
                FakeSpeciesRepository(),
            ),
            { id, entry -> opened += id to entry },
        )

    @Test
    fun `a tapped entity opens the card and dismissing closes it`() =
        runTest {
            interactions.onEvent(this, EntitySheetEvent.EntityClicked(EntityRef.Npc(1)))
            assertEquals(EntityRef.Npc(1), selected.value)

            interactions.onEvent(this, EntitySheetEvent.Dismissed)
            assertNull(selected.value)
        }

    @Test
    fun `a chosen status is written to the record`() =
        runTest {
            interactions.onEvent(this, EntitySheetEvent.NpcStatusSelected(1, NpcStatus.ENEMY))
            testScheduler.advanceUntilIdle()

            assertEquals(NpcStatus.ENEMY, npcs.stored(1)?.status)
        }

    @Test
    fun `a related note opens its session at that note`() =
        runTest {
            interactions.onEvent(this, EntitySheetEvent.RelatedNoteClicked(sessionNoteId = 7, entryId = 70))

            assertEquals(listOf(7L to 70L), opened)
        }

    @Test
    fun `only the events that change a record count as writes`() {
        assertTrue(EntitySheetEvent.NpcStatusSelected(1, NpcStatus.FRIEND).writesCampaignData())
        assertTrue(EntitySheetEvent.NpcLifeSelected(1, NpcLifeState.DEAD).writesCampaignData())
        assertTrue(EntitySheetEvent.PartyPresenceSelected(1, PartyPresence.AWAY).writesCampaignData())
        assertTrue(EntitySheetEvent.QuestStatusSelected(1, QuestStatus.FAILED).writesCampaignData())
        assertFalse(EntitySheetEvent.EntityClicked(EntityRef.Npc(1)).writesCampaignData())
        assertFalse(EntitySheetEvent.RelatedNoteClicked(1, 1).writesCampaignData())
        assertFalse(EntitySheetEvent.Dismissed.writesCampaignData())
    }
}
