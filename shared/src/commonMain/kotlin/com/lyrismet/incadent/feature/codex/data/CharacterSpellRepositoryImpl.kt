package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.SpellRest
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CharacterSpellRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : CharacterSpellRepository {
    private val queries = database.characterSpellQueries

    override fun observe(memberId: Long): Flow<List<CharacterSpell>> =
        queries.selectByMember(memberId, ::toDomain).asFlow().mapToList(DatabaseDispatcher)

    override suspend fun upsert(
        memberId: Long,
        spell: CharacterSpell,
    ): Long =
        withContext(DatabaseDispatcher) {
            val value = spell.clamped()
            val material = if (value.hasMaterial) value.material else ""
            val stored = if (value.id == 0L) null else queries.selectById(value.id).executeAsOneOrNull()
            if (stored != null) {
                queries.update(
                    value.name,
                    value.level.toLong(),
                    value.school,
                    value.castingTime,
                    value.range,
                    value.verbal.toLong(),
                    value.somatic.toLong(),
                    value.hasMaterial.toLong(),
                    material,
                    value.duration,
                    value.concentration.toLong(),
                    value.ritual.toLong(),
                    value.text,
                    value.freeMax.toLong(),
                    value.freeRest.name,
                    value.position.toLong(),
                    value.id,
                )
                value.id
            } else {
                queries.insert(
                    memberId,
                    value.name,
                    value.level.toLong(),
                    value.school,
                    value.castingTime,
                    value.range,
                    value.verbal.toLong(),
                    value.somatic.toLong(),
                    value.hasMaterial.toLong(),
                    material,
                    value.duration,
                    value.concentration.toLong(),
                    value.ritual.toLong(),
                    value.text,
                    value.freeMax.toLong(),
                    value.freeLeft.toLong(),
                    value.freeRest.name,
                    value.position.toLong(),
                    current.requireId(),
                    value.originId.orNewOriginId(),
                )
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun changeFree(
        id: Long,
        delta: Int,
    ) {
        withContext(DatabaseDispatcher) { queries.changeFree(delta.toLong(), id) }
    }

    override suspend fun delete(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }

    private fun Boolean.toLong() = if (this) 1L else 0L

    // one param per column, the shape SQLDelight's generated mapper dictates
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        level: Long,
        school: String,
        castingTime: String,
        spellRange: String,
        verbal: Long,
        somatic: Long,
        hasMaterial: Long,
        material: String,
        duration: String,
        concentration: Long,
        ritual: Long,
        body: String,
        freeMax: Long,
        freeLeft: Long,
        freeRest: String,
        position: Long,
        originId: String,
    ) = CharacterSpell(
        id = id,
        name = name,
        level = level.toInt(),
        school = school,
        castingTime = castingTime,
        range = spellRange,
        verbal = verbal != 0L,
        somatic = somatic != 0L,
        hasMaterial = hasMaterial != 0L,
        material = material,
        duration = duration,
        concentration = concentration != 0L,
        ritual = ritual != 0L,
        text = body,
        freeMax = freeMax.toInt(),
        freeLeft = freeLeft.toInt(),
        freeRest = SpellRest.entries.firstOrNull { it.name == freeRest } ?: SpellRest.LONG,
        position = position.toInt(),
        originId = originId,
    )
}
