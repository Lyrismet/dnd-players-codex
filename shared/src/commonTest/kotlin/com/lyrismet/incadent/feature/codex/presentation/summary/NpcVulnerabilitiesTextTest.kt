package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import kotlin.test.Test
import kotlin.test.assertEquals

class NpcVulnerabilitiesTextTest {
    private val types = DamageType.entries.associateWith { it.name.lowercase() }
    private val states = DamageState.entries.associateWith { it.name.lowercase() }

    @Test
    fun `shields are grouped by state and the facts come last`() {
        val features =
            NpcVulnerabilities(
                own =
                    mapOf(
                        DamageType.entries[0] to DamageState.RESISTANCE,
                        DamageType.entries[1] to DamageState.RESISTANCE,
                    ),
                facts = listOf("a", "b"),
            )
        val first = DamageType.entries[0].name.lowercase()
        val second = DamageType.entries[1].name.lowercase()
        assertEquals("resistance: $first, $second\nfacts: a, b", features.asText(types, states, "facts"))
    }

    @Test
    fun `nothing to copy gives an empty text`() {
        assertEquals("", NpcVulnerabilities().asText(types, states, "facts"))
    }
}
