package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.LongRest
import com.lyrismet.incadent.feature.codex.domain.model.RestPlan
import com.lyrismet.incadent.feature.codex.domain.model.RestUndo
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterRestRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CharacterRestRepositoryImpl(
    private val database: AppDatabase,
) : CharacterRestRepository {
    private val party = database.partyMemberQueries
    private val resources = database.characterResourceQueries

    override suspend fun preview(memberId: Long): RestPlan? = withContext(DatabaseDispatcher) { plan(memberId) }

    override suspend fun longRest(memberId: Long): RestPlan? =
        withContext(DatabaseDispatcher) {
            database.transactionWithResult {
                plan(memberId)?.also { plan ->
                    writeVitals(party, memberId, plan.vitals)
                    plan.resources.forEach { resources.restoreCurrent(it.current.toLong(), it.id) }
                }
            }
        }

    override suspend fun undo(
        memberId: Long,
        undo: RestUndo,
    ) {
        withContext(DatabaseDispatcher) {
            database.transaction {
                // only what the rest changed goes back, an armor class edited since stays
                val now = party.selectVitals(memberId).executeAsOneOrNull()?.toDomain() ?: return@transaction
                val hpMax = now.hpMax
                writeVitals(
                    party,
                    memberId,
                    now.copy(
                        hpCurrent =
                            if (hpMax !=
                                null
                            ) {
                                undo.vitals.hpCurrent?.coerceAtMost(hpMax)
                            } else {
                                undo.vitals.hpCurrent
                            },
                        tempHp = undo.vitals.tempHp,
                        deathSavesSuccess = undo.vitals.deathSavesSuccess,
                        deathSavesFailure = undo.vitals.deathSavesFailure,
                    ),
                )
                undo.resourceCounts.forEach { (id, count) -> resources.restoreCurrent(count.toLong(), id) }
            }
        }
    }

    private fun plan(memberId: Long): RestPlan? {
        val vitals = party.selectVitals(memberId).executeAsOneOrNull()?.toDomain() ?: return null
        val own: List<CharacterResource> =
            resources
                .selectByMember(memberId) { id, _, name, max, current, slotLevel, position, originId ->
                    CharacterResource(
                        id,
                        name,
                        max.toInt(),
                        current.toInt(),
                        slotLevel?.toInt(),
                        position.toInt(),
                        originId,
                    )
                }.executeAsList()
        return LongRest.plan(vitals, own)
    }
}
