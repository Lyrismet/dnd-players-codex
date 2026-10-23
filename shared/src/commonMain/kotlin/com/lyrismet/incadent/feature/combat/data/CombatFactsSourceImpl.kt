package com.lyrismet.incadent.feature.combat.data

import com.lyrismet.incadent.feature.combat.domain.repository.CombatFactsSource
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.combat.domain.results.combatFacts
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CombatFactsSourceImpl(
    private val combats: CombatRepository,
    private val observations: HitObservationRepository,
) : CombatFactsSource {
    override fun observe(): Flow<CombatFacts> = combine(combats.observeAll(), observations.observeAll(), ::combatFacts)
}
