package com.lyrismet.incadent.core.entitysummary

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeNpcRepository(
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

internal class FakePartyRepository(
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

internal class FakeLocationRepository(
    vararg initial: Location,
) : LocationRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()

    fun stored(id: Long): Location? = store[id]

    override fun observeAll(): Flow<List<Location>> = flowOf(store.values.toList())

    override suspend fun getById(id: Long): Location? = store[id]

    override suspend fun upsert(location: Location): Long {
        store[location.id] = location
        return location.id
    }

    override suspend fun delete(id: Long) {
        store.remove(id)
    }
}

internal class FakeQuestRepository(
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

internal class RecordingNavigator : Navigator {
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
