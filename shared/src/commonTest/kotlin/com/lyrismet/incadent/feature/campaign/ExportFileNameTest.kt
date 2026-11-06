package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.exportFileName
import com.lyrismet.incadent.feature.campaign.domain.exchange.typedFileName
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportFileNameTest {
    @Test
    fun `a plain name keeps its letters and gets the extension`() {
        assertEquals("Морской_дракон.incadent.json", exportFileName("Морской дракон"))
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

    @Test
    fun `the mode label follows the name after a dot`() {
        assertEquals("Тени_личная_копия.incadent.json", exportFileName("Тени", "личная копия"))
        assertEquals("Тени.incadent.json", exportFileName("Тени", ""))
    }

    @Test
    fun `a typed name without the extension gets it`() {
        assertEquals("Мой_файл.incadent.json", typedFileName("Мой файл"))
    }

    @Test
    fun `a typed name that already ends with json stays as it is`() {
        assertEquals("Мой_файл.incadent.json", typedFileName("Мой файл.incadent.json"))
        assertEquals("copy.json", typedFileName("copy.json"))
    }

    @Test
    fun `a typed name ending with the app extension or a dot gets one extension`() {
        assertEquals("My_campaign.incadent.json", typedFileName("My  campaign.incadent"))
        assertEquals("name.incadent.json", typedFileName("name."))
    }

    @Test
    fun `a typed name loses path signs and a leading dot`() {
        assertEquals("a_b.incadent.json", typedFileName("a/b"))
        assertEquals("hidden.incadent.json", typedFileName("..hidden"))
    }

    @Test
    fun `a blank typed name stays blank`() {
        assertEquals("", typedFileName("   "))
        assertEquals("", typedFileName("///"))
    }

    @Test
    fun `spaces and runs of them become single underscores`() {
        assertEquals("a_b_c.incadent.json", exportFileName("a  b", " c "))
        assertEquals("a_b.incadent.json", typedFileName("a   b"))
    }
}
