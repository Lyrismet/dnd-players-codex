package com.lyrismet.incadent.feature.combat.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
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
class HitObservationRepositoryImpl(
    database: AppDatabase,
    private val current: CurrentCampaign,
) : HitObservationRepository {
    private val queries = database.speciesHitObservationQueries

    override fun observeAll(): Flow<Map<Long, List<SpeciesHitObservation>>> =
        current.observe { campaignId ->
            queries.selectByCampaign(campaignId).asFlow().mapToList(DatabaseDispatcher).map { rows ->
                rows.groupBy({ it.species_id }, { SpeciesHitObservation(it.damage.toInt(), it.died != 0L) })
            }
        }

    override suspend fun add(
        speciesId: Long,
        combatId: Long,
        observation: SpeciesHitObservation,
    ): Long =
        withContext(DatabaseDispatcher) {
            queries.transactionWithResult {
                queries.insert(speciesId, combatId, observation.damage.toLong(), if (observation.died) 1L else 0L)
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun remove(id: Long) {
        withContext(DatabaseDispatcher) { queries.deleteById(id) }
    }
}
