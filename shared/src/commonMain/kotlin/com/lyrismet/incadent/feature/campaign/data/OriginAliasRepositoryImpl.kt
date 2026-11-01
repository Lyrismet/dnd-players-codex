package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.repository.OriginAliasRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class OriginAliasRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : OriginAliasRepository {
    private val queries = database.originAliasQueries

    override suspend fun add(
        kind: ExchangeKind,
        survivorOriginId: String,
        mergedOriginId: String,
    ) {
        withContext(DatabaseDispatcher) {
            queries.insert(current.requireId(), kind, mergedOriginId, survivorOriginId)
        }
    }

    override suspend fun resolve(
        kind: ExchangeKind,
        originId: String,
    ): String =
        withContext(DatabaseDispatcher) {
            queries.selectOriginId(current.requireId(), kind, originId).executeAsOneOrNull() ?: originId
        }
}
