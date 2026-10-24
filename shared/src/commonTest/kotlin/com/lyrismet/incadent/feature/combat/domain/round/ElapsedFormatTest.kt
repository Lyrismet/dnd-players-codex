package com.lyrismet.incadent.feature.combat.domain.round

import kotlin.test.Test
import kotlin.test.assertEquals

class ElapsedFormatTest {
    @Test
    fun `under a minute is padded`() {
        assertEquals("00:07", formatElapsed(7))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("03:07", formatElapsed(187))
    }

    @Test
    fun `an hour adds the hours place`() {
        assertEquals("1:02:09", formatElapsed(3729))
    }

    @Test
    fun `a negative span reads as zero`() {
        assertEquals("00:00", formatElapsed(-5))
    }
}
