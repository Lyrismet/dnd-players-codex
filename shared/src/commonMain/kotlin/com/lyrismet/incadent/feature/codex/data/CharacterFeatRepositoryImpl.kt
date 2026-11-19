package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import com.lyrismet.incadent.feature.codex.domain.model.FEAT_NAME_MAX
import com.lyrismet.incadent.feature.codex.domain.model.FeatSource
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterFeatRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CharacterFeatRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : CharacterFeatRepository {
    private val queries = database.characterFeatQueries

    override fun observe(memberId: Long): Flow<List<CharacterFeat>> =
        queries.selectByMember(memberId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)

    override suspend fun getById(id: Long): CharacterFeat? =
        withContext(DatabaseDispatcher) { queries.selectById(id, ::toDomain).executeAsOneOrNull() }

    // a feat of a known id that is gone comes back under that id, so an undo of a delete lands where it was
    override suspend fun upsert(
        memberId: Long,
        feat: CharacterFeat,
    ): Long =
        withContext(DatabaseDispatcher) {
            database.transactionWithResult {
                val value = feat.copy(name = feat.name.trim().take(FEAT_NAME_MAX))
                val stored = if (value.id == 0L) null else queries.selectById(value.id).executeAsOneOrNull()
                if (stored != null) {
                    queries.update(value.name, value.source.stored(), value.description, value.id)
                    value.id
                } else {
                    val knownId = value.id.takeIf { it != 0L }
                    queries.insert(
                        knownId,
                        memberId,
                        value.name,
                        value.source.stored(),
                        value.description,
                        if (knownId != null) value.position.toLong() else queries.nextPosition(memberId).executeAsOne(),
                        current.requireId(),
                        value.originId.orNewOriginId(),
                    )
                    knownId ?: queries.lastInsertRowId().executeAsOne()
                }
            }
        }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }

    private fun FeatSource?.stored() = this?.name.orEmpty()

    // one param per column, the shape SQLDelight's generated mapper dictates
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        source: String,
        description: String,
        position: Long,
        originId: String,
    ) = CharacterFeat(
        id = id,
        name = name,
        source = FeatSource.entries.firstOrNull { it.name == source },
        description = description,
        position = position.toInt(),
        originId = originId,
    )
}
