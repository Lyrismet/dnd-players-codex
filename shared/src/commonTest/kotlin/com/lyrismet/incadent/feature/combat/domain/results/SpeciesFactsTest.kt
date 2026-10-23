package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeciesFactsTest {
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)

    private fun finished() = combatOf(listOf(goblin, goblin.copy(id = 4))).copy(phase = CombatPhase.FINISHED)

    @Test
    fun `a fight counts once however many of the species stood in it`() {
        assertEquals(1, speciesCombatFacts(listOf(finished()), emptyMap()).getValue(7).fights)
    }

    @Test
    fun `a fight that is not finished does not count`() {
        val ongoing = finished().copy(phase = CombatPhase.ONGOING)
        assertEquals(emptyMap(), speciesCombatFacts(listOf(ongoing), emptyMap()))
    }

    @Test
    fun `the range sits between the survived and the fallen damage`() {
        val seen =
            mapOf(
                7L to
                    listOf(
                        SpeciesHitObservation(5, false),
                        SpeciesHitObservation(12, true),
                        SpeciesHitObservation(9, true),
                    ),
            )
        assertEquals(HitRange(6, 9), speciesCombatFacts(emptyList(), seen).getValue(7).hitRange)
    }

    @Test
    fun `only falls give only an upper bound`() {
        val seen = mapOf(7L to listOf(SpeciesHitObservation(9, true)))
        assertEquals(HitRange(null, 9), speciesCombatFacts(emptyList(), seen).getValue(7).hitRange)
    }

    @Test
    fun `no observations give no range`() {
        assertNull(speciesCombatFacts(listOf(finished()), emptyMap()).getValue(7).hitRange)
    }
}
