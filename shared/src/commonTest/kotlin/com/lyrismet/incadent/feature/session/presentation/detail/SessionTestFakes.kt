package com.lyrismet.incadent.feature.session.presentation.detail

import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

private val FIXED_MOMENT = LocalDateTime(2026, 1, 1, 12, 0)

/** an in-memory entry store that numbers new entries and records nothing else - enough for controller tests */
internal class FakeSessionEntryRepository(
    initial: List<SessionEntry> = emptyList(),
) : SessionEntryRepository {
    private val items = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val all: List<SessionEntry> get() = items.value

    /** makes the next add or update throw, like a failed disk write */
    var failWrites = false

    override fun observeForSession(sessionNoteId: Long): Flow<List<SessionEntry>> =
        items.map { list -> list.filter { it.sessionNoteId == sessionNoteId } }

    override fun observeAll(): Flow<List<SessionEntry>> = items

    override suspend fun add(
        sessionNoteId: Long,
        body: String,
    ): Long {
        check(!failWrites) { "write failed" }
        val id = nextId++
        items.value = items.value + SessionEntry(id, sessionNoteId, FIXED_MOMENT, body, mentionCheck = true)
        return id
    }

    override suspend fun restore(entry: SessionEntry): Long {
        items.value = items.value + entry
        return entry.id
    }

    override suspend fun update(
        id: Long,
        body: String,
    ) {
        check(!failWrites) { "write failed" }
        items.value = items.value.map { if (it.id == id) it.copy(body = body, edited = true) else it }
    }

    override suspend fun rewriteBody(
        id: Long,
        body: String,
    ) {
        items.value = items.value.map { if (it.id == id) it.copy(body = body) else it }
    }

    override suspend fun setMentionCheck(
        id: Long,
        check: Boolean,
    ) {
        items.value = items.value.map { if (it.id == id) it.copy(mentionCheck = check) else it }
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
}

internal class FakeTagRepository : TagRepository {
    private val assigned = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    fun tagsOf(sessionNoteId: Long): List<String> = assigned.value[sessionNoteId].orEmpty()

    override fun observeCatalog(): Flow<List<String>> = assigned.map { it.values.flatten().distinct() }

    override fun observeTagsFor(sessionNoteId: Long): Flow<List<String>> = assigned.map { it[sessionNoteId].orEmpty() }

    override fun observeAllSessionTags(): Flow<Map<Long, List<String>>> = assigned

    override suspend fun addTag(
        sessionNoteId: Long,
        name: String,
    ) {
        val current = assigned.value[sessionNoteId].orEmpty()
        if (current.none { it.equals(name, ignoreCase = true) }) {
            assigned.value = assigned.value + (sessionNoteId to current + name)
        }
    }

    override suspend fun removeTag(
        sessionNoteId: Long,
        name: String,
    ) {
        assigned.value =
            assigned.value +
            (sessionNoteId to assigned.value[sessionNoteId].orEmpty().filterNot { it.equals(name, true) })
    }
}
