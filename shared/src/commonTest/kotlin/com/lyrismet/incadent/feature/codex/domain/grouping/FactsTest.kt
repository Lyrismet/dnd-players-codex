package com.lyrismet.incadent.feature.codex.domain.grouping

import kotlin.test.Test
import kotlin.test.assertEquals

class FactsTest {
    @Test
    fun `facts survive encoding and decoding`() {
        val facts = listOf("fears fire", "owes the guild")
        assertEquals(facts, decodeFacts(facts.encodeFacts()))
        assertEquals("", emptyList<String>().encodeFacts())
        assertEquals(emptyList(), decodeFacts(""))
    }

    @Test
    fun `blank lines are no facts`() {
        assertEquals(listOf("a", "b"), decodeFacts("a\n\n  \nb"))
    }

    @Test
    fun `a fact already known in any letter case is not added again`() {
        val facts = listOf("Fears fire")
        assertEquals(facts, facts.withFact("fears FIRE"))
        assertEquals(facts, facts.withFact("   "))
        assertEquals(listOf("Fears fire", "Hates elves"), facts.withFact(" Hates elves "))
    }
}
