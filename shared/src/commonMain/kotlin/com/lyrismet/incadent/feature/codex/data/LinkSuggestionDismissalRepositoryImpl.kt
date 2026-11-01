package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.repository.LinkSuggestionDismissalRepository
import com.lyrismet.incadent.feature.mention.domain.MentionRef
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
class LinkSuggestionDismissalRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : LinkSuggestionDismissalRepository {
    private val queries = database.linkSuggestionDismissalQueries

    override fun observeDismissed(owner: MentionRef): Flow<Set<MentionRef>> =
        current
            .observe { campaignId ->
                queries
                    .selectForOwner(campaignId, owner.type.name, owner.id) { kind, id ->
                        MentionRef(EntityKind.valueOf(kind), id)
                    }.asFlow()
                    .mapToList(DatabaseDispatcher)
            }.map { it.toSet() }

    override suspend fun dismiss(
        owner: MentionRef,
        target: MentionRef,
    ) {
        withContext(DatabaseDispatcher) {
            queries.insert(current.requireId(), owner.type.name, owner.id, target.type.name, target.id)
        }
    }

    override suspend fun restore(
        owner: MentionRef,
        target: MentionRef,
    ) {
        withContext(DatabaseDispatcher) {
            queries.delete(current.requireId(), owner.type.name, owner.id, target.type.name, target.id)
        }
    }
}
