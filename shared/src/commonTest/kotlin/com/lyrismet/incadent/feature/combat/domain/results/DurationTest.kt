package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationTest {
    private val combat = combatOf(listOf(member(1, "Кира", CombatSide.PARTY)))

    @Test
    fun `minutes run from the start to the end`() {
        assertEquals(12, combat.minutesUntil(LocalDateTime(2026, 1, 1, 12, 12)))
    }

    @Test
    fun `a quick fight is one minute at least`() {
        assertEquals(1, combat.minutesUntil(LocalDateTime(2026, 1, 1, 12, 0)))
    }
}
