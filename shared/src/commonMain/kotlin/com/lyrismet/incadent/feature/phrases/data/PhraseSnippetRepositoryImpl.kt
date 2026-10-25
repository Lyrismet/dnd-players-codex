package com.lyrismet.incadent.feature.phrases.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.phrases.domain.repository.PhraseSnippetRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PhraseSnippetRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : PhraseSnippetRepository {
    private val queries = database.phraseSnippetQueries

    override fun observeAll(): Flow<List<String>> =
        current.observe { campaignId ->
            queries
                .selectAll(campaignId)
                .asFlow()
                .mapToList(DatabaseDispatcher)
                .map { rows -> rows.map { it.text } }
        }

    override suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(DatabaseDispatcher) { queries.insert(trimmed, current.requireId()) }
    }

    override suspend fun remove(text: String) {
        withContext(DatabaseDispatcher) { queries.deleteByText(text, current.requireId()) }
    }
}
