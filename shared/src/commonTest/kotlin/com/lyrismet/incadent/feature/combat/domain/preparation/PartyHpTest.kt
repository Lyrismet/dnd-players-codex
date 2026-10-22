package com.lyrismet.incadent.feature.combat.domain.preparation

import kotlin.test.Test
import kotlin.test.assertEquals

class PartyHpTest {
    @Test
    fun `no maximum makes the number both the current and the maximum`() {
        assertEquals(PartyHp(current = 24, max = 24), setCurrentHp(max = null, value = 24))
    }

    @Test
    fun `a number above the maximum is cut to it`() {
        assertEquals(PartyHp(current = 30, max = 30), setCurrentHp(max = 30, value = 45))
    }

    @Test
    fun `a number inside the range is kept`() {
        assertEquals(PartyHp(current = 12, max = 30), setCurrentHp(max = 30, value = 12))
    }

    @Test
    fun `zero is a valid current value`() {
        assertEquals(PartyHp(current = 0, max = 30), setCurrentHp(max = 30, value = 0))
    }
}
