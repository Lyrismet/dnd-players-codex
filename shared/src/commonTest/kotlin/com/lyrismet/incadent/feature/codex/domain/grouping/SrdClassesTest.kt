package com.lyrismet.incadent.feature.codex.domain.grouping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SrdClassesTest {
    @Test
    fun `nothing typed lists all twelve classes sorted by name`() {
        val russian = srdClassesMatching("", russian = true).map { it.nameRu }
        assertEquals(12, russian.size)
        assertEquals(russian.sorted(), russian)
        val english = srdClassesMatching("  ", russian = false).map { it.nameEn }
        assertEquals(english.sorted(), english)
    }

    @Test
    fun `a typed part matches in either language ignoring case`() {
        assertEquals(listOf("Паладин"), srdClassesMatching("пал", russian = true).map { it.nameRu })
        assertEquals(listOf("Паладин"), srdClassesMatching("PALAD", russian = true).map { it.nameRu })
    }

    @Test
    fun `an unknown class matches nothing`() {
        assertTrue(srdClassesMatching("Иллюзионист", russian = true).isEmpty())
    }

    @Test
    fun `a class of the srd is known in both languages`() {
        assertTrue(isSrdClass("воин"))
        assertTrue(isSrdClass(" Fighter "))
        assertFalse(isSrdClass("Иллюзионист"))
        assertFalse(isSrdClass(""))
    }

    @Test
    fun `the class line keeps only the filled parts`() {
        assertEquals("Паладин · Клятва преданности", classLine("Паладин", "Клятва преданности"))
        assertEquals("Паладин", classLine("Паладин", ""))
        assertEquals("Клятва преданности", classLine("", "Клятва преданности"))
        assertEquals("", classLine("", ""))
    }
}
