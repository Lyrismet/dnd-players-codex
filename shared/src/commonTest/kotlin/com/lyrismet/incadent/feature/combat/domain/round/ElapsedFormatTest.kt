package com.lyrismet.incadent.feature.combat.domain.round

import kotlin.test.Test
import kotlin.test.assertEquals

class ElapsedFormatTest {
    @Test
    fun `the first round is zero seconds`() {
        assertEquals(GameTime(0, 0), gameTime(1))
    }

    @Test
    fun `round four is eighteen seconds`() {
        assertEquals(GameTime(0, 18), gameTime(4))
    }

    @Test
    fun `round twelve is a minute and six seconds`() {
        assertEquals(GameTime(1, 6), gameTime(12))
    }

    @Test
    fun `round a hundred is nine minutes and fifty four seconds`() {
        assertEquals(GameTime(9, 54), gameTime(100))
    }

    @Test
    fun `a round below one reads as the start`() {
        assertEquals(GameTime(0, 0), gameTime(0))
    }
}
