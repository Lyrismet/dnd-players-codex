package com.lyrismet.incadent.core.entitysummary

import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.DeletedMentionRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** an id-keyed in-memory table that assigns ids on insert and re-emits on every write, like the real repositories */
internal abstract class InMemoryStore<T>(
    initial: List<T>,
    private val idOf: (T) -> Long,
    private val withId: (T, Long) -> T,
) {
    private val items = MutableStateFlow(initial.associateBy(idOf))
    private var nextId = (initial.maxOfOrNull(idOf) ?: 0L) + 1

    /** every stored record, oldest first */
    val all: List<T> get() = items.value.values.toList()

    fun stored(id: Long): T? = items.value[id]

    protected fun observe(): Flow<List<T>> = items.map { it.values.toList() }

    protected fun put(item: T): Long {
        val id = if (idOf(item) == 0L) nextId++ else idOf(item)
        items.update { it + (id to withId(item, id)) }
        return id
    }

    protected fun remove(id: Long) {
        items.update { it - id }
    }
}

internal class FakeNpcRepository(
    vararg initial: Npc,
) : InMemoryStore<Npc>(initial.toList(), { it.id }, { npc, id -> npc.copy(id = id) }),
    NpcRepository {
    override fun observeAll(): Flow<List<Npc>> = observe()

    override suspend fun getById(id: Long): Npc? = stored(id)

    override suspend fun upsert(npc: Npc): Long = put(npc)

    override suspend fun delete(id: Long) = remove(id)
}

internal class FakePartyRepository(
    vararg initial: PartyMember,
) : InMemoryStore<PartyMember>(initial.toList(), { it.id }, { member, id -> member.copy(id = id) }),
    PartyRepository {
    override fun observeAll(): Flow<List<PartyMember>> = observe()

    override suspend fun getById(id: Long): PartyMember? = stored(id)

    override suspend fun upsert(member: PartyMember): Long = put(member)

    override suspend fun delete(id: Long) = remove(id)
}

internal class FakeLocationRepository(
    vararg initial: Location,
) : InMemoryStore<Location>(initial.toList(), { it.id }, { location, id -> location.copy(id = id) }),
    LocationRepository {
    override fun observeAll(): Flow<List<Location>> = observe()

    override suspend fun getById(id: Long): Location? = stored(id)

    override suspend fun upsert(location: Location): Long = put(location)

    override suspend fun delete(id: Long) = remove(id)
}

internal class FakeQuestRepository(
    vararg initial: Quest,
) : InMemoryStore<Quest>(initial.toList(), { it.id }, { quest, id -> quest.copy(id = id) }),
    QuestRepository {
    override fun observeAll(): Flow<List<Quest>> = observe()

    override fun observeByStatus(status: QuestStatus): Flow<List<Quest>> =
        observe().map { quests -> quests.filter { it.status == status } }

    override suspend fun getById(id: Long): Quest? = stored(id)

    override suspend fun upsert(quest: Quest): Long = put(quest)

    override suspend fun delete(id: Long) = remove(id)
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

internal class FakeDeletedMentionRepository : DeletedMentionRepository {
    val stored = mutableMapOf<Pair<String, DeletedMentionType>, DeletedMention>()

    override fun observeAll(): Flow<List<DeletedMention>> = flowOf(stored.values.toList())

    override suspend fun remember(mention: DeletedMention) {
        stored[mention.name to mention.type] = mention
    }

    override suspend fun forget(mention: DeletedMention) {
        stored.remove(mention.name to mention.type)
    }
}
