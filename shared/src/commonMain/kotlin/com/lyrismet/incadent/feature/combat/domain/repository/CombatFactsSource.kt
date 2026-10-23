package com.lyrismet.incadent.feature.combat.domain.repository

import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import kotlinx.coroutines.flow.Flow

/** what the saved fights say of the species and the party, for the codex to show */
interface CombatFactsSource {
    fun observe(): Flow<CombatFacts>
}
