package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.quickedit.EntityChange
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditValue
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.CodexRepositories
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitySheetInteractionsTest {
    @Test
    fun `tapping an entity selects it and dismissing clears the selection`() {
        val selected = mutableStateOf<EntityRef?>(null)
        val interactions = interactions(selected = selected)

        interactions.onEntityClicked(EntityRef.Npc(id = 1))
        assertEquals(EntityRef.Npc(id = 1), selected.value)

        interactions.onDismissed()
        assertNull(selected.value)
    }

    @Test
    fun `npc status change is written through the repository`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val interactions = interactions(npcs = npcs)

            interactions.onNpcStatusSelected(this, npcId = 1, status = NpcStatus.ENEMY)
            advanceUntilIdle()

            assertEquals(NpcStatus.ENEMY, npcs.stored(1)?.status)
        }

    @Test
    fun `a status change reports its field and restoring the change puts the old record back`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val changes = mutableListOf<EntityChange>()
            val interactions = interactions(npcs = npcs, onChanged = { changes += it })

            interactions.onNpcStatusSelected(this, npcId = 1, status = NpcStatus.FRIEND)
            advanceUntilIdle()

            assertEquals(QuickEditField.RELATION, changes.single().field)
            changes.single().restore()
            assertEquals(NpcStatus.NEUTRAL, npcs.stored(1)?.status)
        }

    @Test
    fun `a quick edit writes the new text and reports the old name for the undo toast`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val changes = mutableListOf<EntityChange>()
            val interactions = interactions(npcs = npcs, onChanged = { changes += it })

            interactions.onQuickEdit(this, EntityRef.Npc(1), QuickEditField.NAME, QuickEditValue.Text("Ворон"))
            advanceUntilIdle()

            assertEquals("Ворон", npcs.stored(1)?.name)
            assertEquals("Кассиан", changes.single().entityName)
            changes.single().restore()
            assertEquals("Кассиан", npcs.stored(1)?.name)
        }

    @Test
    fun `a quick edit with the same value writes nothing and offers no undo`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val changes = mutableListOf<EntityChange>()
            val interactions = interactions(npcs = npcs, onChanged = { changes += it })

            interactions.onQuickEdit(this, EntityRef.Npc(1), QuickEditField.NAME, QuickEditValue.Text("Кассиан"))
            advanceUntilIdle()

            assertTrue(changes.isEmpty())
        }

    @Test
    fun `a field the entity does not have is ignored`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val changes = mutableListOf<EntityChange>()
            val interactions = interactions(npcs = npcs, onChanged = { changes += it })

            interactions.onQuickEdit(this, EntityRef.Npc(1), QuickEditField.REWARD, QuickEditValue.Text("200 зм"))
            advanceUntilIdle()

            assertTrue(changes.isEmpty())
        }

    @Test
    fun `a location edit is written through the location repository`() =
        runTest {
            val locations =
                FakeLocationRepository(
                    Location(id = 4, name = "Башня", description = ""),
                )
            val interactions = interactions(locations = locations)

            interactions.onQuickEdit(
                this,
                EntityRef.Location(4),
                QuickEditField.NAME,
                QuickEditValue.Text("Юг"),
            )
            advanceUntilIdle()

            assertEquals("Юг", locations.stored(4)?.name)
        }

    @Test
    fun `npc condition change is written through the repository`() =
        runTest {
            val npcs = FakeNpcRepository(npc(id = 1, status = NpcStatus.NEUTRAL))
            val interactions = interactions(npcs = npcs)

            interactions.onNpcLifeSelected(this, npcId = 1, lifeState = NpcLifeState.DEAD)
            advanceUntilIdle()

            assertEquals(NpcLifeState.DEAD, npcs.stored(1)?.lifeState)
            assertEquals(NpcStatus.NEUTRAL, npcs.stored(1)?.status)
        }

    @Test
    fun `party presence change is written through the repository`() =
        runTest {
            val party = FakePartyRepository(member(id = 3, presence = PartyPresence.IN))
            val interactions = interactions(party = party)

            interactions.onPartyPresenceSelected(this, partyId = 3, presence = PartyPresence.AWAY)
            advanceUntilIdle()

            assertEquals(PartyPresence.AWAY, party.stored(3)?.presence)
        }

    @Test
    fun `quest status change is written through the repository`() =
        runTest {
            val quests = FakeQuestRepository(quest(id = 2, status = QuestStatus.ACTIVE))
            val interactions = interactions(quests = quests)

            interactions.onQuestStatusSelected(this, questId = 2, status = QuestStatus.COMPLETED)
            advanceUntilIdle()

            assertEquals(QuestStatus.COMPLETED, quests.stored(2)?.status)
        }

    @Test
    fun `following a related note closes the sheet and opens its session`() {
        val selected = mutableStateOf<EntityRef?>(EntityRef.Npc(id = 1))
        val opened = mutableListOf<Pair<Long, Long>>()
        val interactions = interactions(selected = selected, openSession = { id, entry -> opened += id to entry })

        interactions.onRelatedNoteClicked(sessionNoteId = 7, entryId = 70)

        assertNull(selected.value)
        assertEquals(listOf(7L to 70L), opened)
    }

    @Test
    fun `an entity opened from another one keeps that one underneath until it is closed`() {
        val selected = mutableStateOf<EntityRef?>(null)
        val interactions = interactions(selected = selected)

        interactions.onEntityClicked(EntityRef.Npc(id = 1))
        interactions.onEntityClicked(EntityRef.Location(id = 2))
        assertEquals(EntityRef.Location(2), selected.value)

        interactions.onDismissed()
        assertEquals(EntityRef.Npc(1), selected.value)

        interactions.onDismissed()
        assertNull(selected.value)
    }

    @Test
    fun `clicking the entity that is already open does not stack it on itself`() {
        val selected = mutableStateOf<EntityRef?>(null)
        val interactions = interactions(selected = selected)

        interactions.onEntityClicked(EntityRef.Npc(id = 1))
        interactions.onEntityClicked(EntityRef.Npc(id = 1))
        interactions.onDismissed()

        assertNull(selected.value)
    }

    private fun interactions(
        selected: androidx.compose.runtime.MutableState<EntityRef?> = mutableStateOf(null),
        npcs: FakeNpcRepository = FakeNpcRepository(),
        quests: FakeQuestRepository = FakeQuestRepository(),
        party: FakePartyRepository = FakePartyRepository(),
        locations: FakeLocationRepository = FakeLocationRepository(),
        openSession: (Long, Long) -> Unit = { _, _ -> },
        history: androidx.compose.runtime.MutableState<List<EntityRef>> = mutableStateOf(emptyList()),
        onChanged: (EntityChange) -> Unit = {},
    ) = EntitySheetInteractions(
        selected,
        CodexRepositories(party, npcs, quests, locations),
        openSession,
        history,
        onChanged,
    )

    private fun member(
        id: Long,
        presence: PartyPresence,
    ) = PartyMember(
        id = id,
        name = "Торин",
        characterClass = "Паладин",
        race = "Человек",
        level = 5,
        playerName = "Максим",
        isPlayerCharacter = false,
        presence = presence,
        hpMax = 52,
        hpCurrent = 43,
        description = "",
        portrait = null,
    )

    private fun npc(
        id: Long,
        status: NpcStatus,
    ) = Npc(
        id = id,
        name = "Кассиан",
        status = status,
        lifeState = NpcLifeState.ALIVE,
        description = "",
        locationIds = emptyList(),
        race = "",
        faction = "",
    )

    private fun quest(
        id: Long,
        status: QuestStatus,
    ) = Quest(
        id = id,
        title = "Пропавший обоз",
        status = status,
        reward = "",
        givenByNpcId = null,
        locationId = null,
    )
}
