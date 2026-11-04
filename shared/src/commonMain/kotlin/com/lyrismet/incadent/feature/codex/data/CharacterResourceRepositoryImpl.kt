package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CharacterResourceRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : CharacterResourceRepository {
    private val queries = database.characterResourceQueries

    override fun observe(memberId: Long): Flow<List<CharacterResource>> =
        queries.selectByMember(memberId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)

    override suspend fun upsert(
        memberId: Long,
        resource: CharacterResource,
    ): Long =
        withContext(DatabaseDispatcher) {
            val value = resource.clamped()
            val stored = if (value.id == 0L) null else queries.selectById(value.id, ::toDomainRow).executeAsOneOrNull()
            if (stored != null) {
                queries.update(
                    value.name,
                    value.max.toLong(),
                    value.current.toLong(),
                    value.slotLevel?.toLong(),
                    value.position.toLong(),
                    value.id,
                )
                value.id
            } else {
                queries.insert(
                    memberId,
                    value.name,
                    value.max.toLong(),
                    value.current.toLong(),
                    value.slotLevel?.toLong(),
                    value.position.toLong(),
                    current.requireId(),
                    value.originId.orNewOriginId(),
                )
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun setCurrent(
        id: Long,
        value: Int,
    ) {
        withContext(DatabaseDispatcher) { queries.setCurrent(value.toLong(), id) }
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }

    // one param per column, the shape SQLDelight's generated mapper dictates
    @Suppress("LongParameterList", "UnusedParameter")
    private fun toDomain(
        id: Long,
        partyMemberId: Long,
        name: String,
        maxValue: Long,
        currentValue: Long,
        slotLevel: Long?,
        position: Long,
        originId: String,
    ) = CharacterResource(
        id = id,
        name = name,
        max = maxValue.toInt(),
        current = currentValue.toInt(),
        slotLevel = slotLevel?.toInt(),
        position = position.toInt(),
        originId = originId,
    )

    @Suppress("LongParameterList")
    private fun toDomainRow(
        id: Long,
        partyMemberId: Long,
        name: String,
        maxValue: Long,
        currentValue: Long,
        slotLevel: Long?,
        position: Long,
        originId: String,
    ) = toDomain(id, partyMemberId, name, maxValue, currentValue, slotLevel, position, originId)
}
