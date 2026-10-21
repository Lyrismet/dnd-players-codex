package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import kotlin.test.Test
import kotlin.test.assertEquals

class DamageMapTest {
    private val sample =
        mapOf(
            DamageType.FIRE to DamageState.RESISTANCE,
            DamageType.COLD to DamageState.VULNERABILITY,
            DamageType.POISON to DamageState.IMMUNITY,
            DamageType.ACID to DamageState.NORMAL,
        )

    @Test
    fun `shields list vulnerability first then immunity then resistance`() {
        assertEquals(
            listOf(
                DamageType.COLD to DamageState.VULNERABILITY,
                DamageType.POISON to DamageState.IMMUNITY,
                DamageType.FIRE to DamageState.RESISTANCE,
            ),
            sample.shields(),
        )
    }

    @Test
    fun `a normal or unknown type earns no shield`() {
        assertEquals(emptyList(), mapOf(DamageType.ACID to DamageState.NORMAL).shields())
        assertEquals(emptyList(), emptyMap<DamageType, DamageState>().shields())
    }

    @Test
    fun `a missing type reads as unknown`() {
        assertEquals(DamageState.UNKNOWN, sample.stateOf(DamageType.FORCE))
    }

    @Test
    fun `setting a type to unknown drops its entry`() {
        assertEquals(sample - DamageType.FIRE, sample.with(DamageType.FIRE, DamageState.UNKNOWN))
        assertEquals(DamageState.IMMUNITY, sample.with(DamageType.FIRE, DamageState.IMMUNITY).stateOf(DamageType.FIRE))
    }

    @Test
    fun `encoding then decoding gives the same map`() {
        assertEquals(sample, decodeDamage(sample.encodeDamage()))
        assertEquals("", emptyMap<DamageType, DamageState>().encodeDamage())
        assertEquals(emptyMap(), decodeDamage(""))
    }

    @Test
    fun `decoding skips pairs this build does not know`() {
        assertEquals(
            mapOf(DamageType.FIRE to DamageState.IMMUNITY),
            decodeDamage("FIRE:IMMUNITY,SONIC:IMMUNITY,COLD:WEIRD,broken,COLD:UNKNOWN"),
        )
    }
}
