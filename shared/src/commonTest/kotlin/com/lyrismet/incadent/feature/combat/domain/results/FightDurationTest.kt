package com.lyrismet.incadent.feature.combat.domain.results

import kotlin.test.Test
import kotlin.test.assertEquals

class FightDurationTest {
    @Test
    fun `four rounds are twenty four seconds`() {
        assertEquals(FightDuration.Seconds(24), fightDuration(4))
    }

    @Test
    fun `one round is six seconds`() {
        assertEquals(FightDuration.Seconds(6), fightDuration(1))
    }

    @Test
    fun `twelve rounds are about a minute`() {
        assertEquals(FightDuration.AboutMinutes(1), fightDuration(12))
    }

    @Test
    fun `a hundred rounds are about ten minutes`() {
        assertEquals(FightDuration.AboutMinutes(10), fightDuration(100))
    }
}
