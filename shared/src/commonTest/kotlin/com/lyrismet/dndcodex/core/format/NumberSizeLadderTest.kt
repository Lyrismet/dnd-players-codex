package com.lyrismet.dndcodex.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberSizeLadderTest {
    @Test
    fun `sizeFor steps down as the label gets longer`() {
        assertEquals(24, NumberSizeLadder.ARCHIVE_SESSION.sizeFor("XI"))
        assertEquals(21, NumberSizeLadder.ARCHIVE_SESSION.sizeFor("XII"))
        assertEquals(18, NumberSizeLadder.ARCHIVE_SESSION.sizeFor("XIII"))
        assertEquals(15, NumberSizeLadder.ARCHIVE_SESSION.sizeFor("XVIII"))
        assertEquals(13, NumberSizeLadder.ARCHIVE_SESSION.sizeFor("XXVIII"))
    }

    @Test
    fun `live ladder starts bigger than the archive one`() {
        assertEquals(46, NumberSizeLadder.LIVE_SESSION.sizeFor("V"))
        assertEquals(24, NumberSizeLadder.LIVE_SESSION.sizeFor("XXXVIII"))
    }
}
