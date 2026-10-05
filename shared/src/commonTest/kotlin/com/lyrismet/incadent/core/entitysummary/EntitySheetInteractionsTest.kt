package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.lyrismet.incadent.presentation.sessiondetail.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
        val navigator = RecordingNavigator()
        val interactions = interactions(selected = selected, navigator = navigator)

        interactions.onRelatedNoteClicked(sessionNoteId = 7)

        assertNull(selected.value)
        assertEquals(listOf<Screen>(SessionDetailScreen(7)), navigator.goneTo)
    }

    private fun interactions(
        selected: androidx.compose.runtime.MutableState<EntityRef?> = mutableStateOf(null),
        npcs: FakeNpcRepository = FakeNpcRepository(),
        quests: FakeQuestRepository = FakeQuestRepository(),
        party: FakePartyRepository = FakePartyRepository(),
        navigator: RecordingNavigator = RecordingNavigator(),
    ) = EntitySheetInteractions(selected, npcs, quests, party, navigator)

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
        armorClass = 18,
        initiativeBonus = 0,
        description = "",
        portraitUri = null,
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
        locationId = null,
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

private class FakeNpcRepository(
    vararg initial: Npc,
) : NpcRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()

    fun stored(id: Long): Npc? = store[id]

    override fun observeAll(): Flow<List<Npc>> = flowOf(store.values.toList())

    override fun observeByLocation(locationId: Long): Flow<List<Npc>> = flowOf(emptyList())

    override suspend fun getById(id: Long): Npc? = store[id]

    override suspend fun upsert(npc: Npc): Long {
        store[npc.id] = npc
        return npc.id
    }

    override suspend fun delete(id: Long) {
        store.remove(id)
    }
}

private class FakePartyRepository(
    vararg initial: PartyMember,
) : PartyRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()

    fun stored(id: Long): PartyMember? = store[id]

    override fun observeAll(): Flow<List<PartyMember>> = flowOf(store.values.toList())

    override suspend fun getById(id: Long): PartyMember? = store[id]

    override suspend fun upsert(member: PartyMember): Long {
        store[member.id] = member
        return member.id
    }

    override suspend fun delete(id: Long) {
        store.remove(id)
    }
}

private class FakeQuestRepository(
    vararg initial: Quest,
) : QuestRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()

    fun stored(id: Long): Quest? = store[id]

    override fun observeAll(): Flow<List<Quest>> = flowOf(store.values.toList())

    override fun observeByStatus(status: QuestStatus): Flow<List<Quest>> = flowOf(emptyList())

    override suspend fun getById(id: Long): Quest? = store[id]

    override suspend fun upsert(quest: Quest): Long {
        store[quest.id] = quest
        return quest.id
    }

    override suspend fun delete(id: Long) {
        store.remove(id)
    }
}

private class RecordingNavigator : Navigator {
    val goneTo = mutableListOf<Screen>()

    override fun goTo(screen: Screen): Boolean {
        goneTo += screen
        return true
    }

    override fun forward(): Boolean = false

    override fun backward(): Boolean = false

    override fun pop(result: PopResult?): Screen? = null

    override fun peek(): Screen? = null

    override fun peekBackStack(): List<Screen> = emptyList()

    override fun peekNavStack(): NavStackList<Screen>? = null

    override fun resetRoot(
        newRoot: Screen,
        options: StateOptions,
    ): List<Screen> = emptyList()
}
