package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.exportFileName
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportFileNameTest {
    @Test
    fun `a plain name keeps its letters and gets the extension`() {
        assertEquals("Морской дракон.incadent.json", exportFileName("Морской дракон"))
    }

    @Test
    fun `characters a file system refuses become underscores`() {
        assertEquals("a_b_c.incadent.json", exportFileName("a/b:c"))
    }

    @Test
    fun `a name that is only symbols falls back`() {
        assertEquals("incadent.incadent.json", exportFileName("///"))
        assertEquals("incadent.incadent.json", exportFileName("  "))
    }

    @Test
    fun `dots at the start cannot make a hidden file or a path climb`() {
        assertEquals("hidden.incadent.json", exportFileName("../hidden"))
    }
}
