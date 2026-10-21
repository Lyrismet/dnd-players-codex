package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SrdSpeciesTest {
    @Test
    fun `an empty query offers nothing`() {
        assertTrue(srdSuggestions("  ", russian = true).isEmpty())
    }

    @Test
    fun `a prefix match comes before a match inside the name`() {
        val names = srdSuggestions("гоб", russian = true).map { it.nameRu }
        assertEquals(listOf("Гоблин", "Хобгоблин"), names)
    }

    @Test
    fun `the english names are searched in english`() {
        assertEquals(CreatureType.HUMANOID, srdSuggestions("gobl", russian = false).first().type)
        assertTrue(srdSuggestions("гоб", russian = false).isEmpty())
    }

    @Test
    fun `yo and ye are the same letter`() {
        assertEquals("Орёл", srdSuggestions("оре", russian = true).first().nameRu)
    }

    @Test
    fun `a name typed in full is not offered again`() {
        assertTrue(srdSuggestions("Гоблин", russian = true).none { it.nameRu == "Гоблин" })
    }

    @Test
    fun `the list stops at the limit`() {
        assertEquals(SRD_SUGGESTION_LIMIT, srdSuggestions("о", russian = true).size)
    }

    @Test
    fun `every name is unique in both languages`() {
        assertEquals(
            SrdCatalog.species.size,
            SrdCatalog.species
                .map { it.nameRu }
                .toSet()
                .size,
        )
        assertEquals(
            SrdCatalog.species.size,
            SrdCatalog.species
                .map { it.nameEn }
                .toSet()
                .size,
        )
    }

    @Test
    fun `an added species hides its srd twin in either language`() {
        val ru = srdNotAdded(listOf("гоблин")).map { it.second.nameRu }
        val en = srdNotAdded(listOf("Goblin")).map { it.second.nameEn }
        assertFalse("Гоблин" in ru)
        assertFalse("Goblin" in en)
        assertTrue(srdNotAdded(emptyList()).all { it.first < 0 })
    }
}
