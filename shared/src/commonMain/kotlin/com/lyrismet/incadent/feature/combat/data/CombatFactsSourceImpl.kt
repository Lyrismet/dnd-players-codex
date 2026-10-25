package com.lyrismet.incadent.feature.combat.data

import com.lyrismet.incadent.core.coroutines.AppCoroutineScope
import com.lyrismet.incadent.feature.combat.domain.repository.CombatFactsSource
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.combat.domain.results.combatFacts
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CombatFactsSourceImpl(
    combats: CombatRepository,
    observations: HitObservationRepository,
    @AppCoroutineScope scope: CoroutineScope,
) : CombatFactsSource {
    // one computation off the main thread for every screen, dropped a few seconds after the last one leaves
    private val shared =
        combine(combats.observeAll(), observations.observeAll(), ::combatFacts)
            .flowOn(Dispatchers.Default)
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_TIMEOUT_MS, replayExpirationMillis = 0), replay = 1)

    override fun observe(): Flow<CombatFacts> = shared

    private companion object {
        const val SHARE_TIMEOUT_MS = 5_000L
    }
}
