package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.repository.PhraseSnippetRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PhraseSnippetRepositoryImpl(
    database: AppDatabase,
) : PhraseSnippetRepository {
    private val queries = database.phraseSnippetQueries

    override fun observeAll(): Flow<List<String>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.text } }

    override suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(Dispatchers.Default) { queries.insert(trimmed) }
    }

    override suspend fun remove(text: String) {
        withContext(Dispatchers.Default) { queries.deleteByText(text) }
    }
}
