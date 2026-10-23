package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResistanceInferenceTest {
    private val kira = member(1, "Кира", CombatSide.PARTY)
    private val borin = member(2, "Борин", CombatSide.PARTY)
    private val goblin = member(3, "Гоблин", CombatSide.ENEMY, speciesId = 7)
    private val species = mapOf(7L to speciesOf(7))

    private fun guesses(vararg events: com.lyrismet.incadent.feature.combat.domain.model.CombatEvent) =
        inferResistances(combatOf(listOf(kira, borin, goblin), events.toList()), species)

    @Test
    fun `two attackers and a full hit of another type is confident`() {
        val result =
            guesses(
                strike(1, 1, 3, 4, DamageType.FIRE, HitEffect.HALF),
                strike(2, 2, 3, 3, DamageType.FIRE, HitEffect.HALF),
                strike(3, 1, 3, 8, DamageType.SLASHING),
            )
        assertEquals(1, result.size)
        assertEquals(DamageState.RESISTANCE, result.single().state)
        assertTrue(result.single().confident)
    }

    @Test
    fun `one attacker is weak`() {
        val result =
            guesses(
                strike(1, 1, 3, 4, DamageType.FIRE, HitEffect.HALF),
                strike(2, 1, 3, 3, DamageType.FIRE, HitEffect.HALF),
                strike(3, 1, 3, 8, DamageType.SLASHING),
            )
        assertEquals(false, result.single().confident)
    }

    @Test
    fun `no full hit of another type is weak`() {
        val result =
            guesses(
                strike(1, 1, 3, 4, DamageType.FIRE, HitEffect.HALF),
                strike(2, 2, 3, 3, DamageType.FIRE, HitEffect.HALF),
            )
        assertEquals(false, result.single().confident)
    }

    @Test
    fun `vulnerability is confident after one hit`() {
        val result = guesses(strike(1, 1, 3, 12, DamageType.COLD, HitEffect.DOUBLE))
        assertEquals(DamageState.VULNERABILITY, result.single().state)
        assertTrue(result.single().confident)
    }

    @Test
    fun `mixed answers for a type give nothing`() {
        val result =
            guesses(
                strike(1, 1, 3, 4, DamageType.FIRE, HitEffect.HALF),
                strike(2, 2, 3, 8, DamageType.FIRE, HitEffect.FULL),
            )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `a type the species already knows is skipped`() {
        val known = mapOf(7L to speciesOf(7, damage = mapOf(DamageType.FIRE to DamageState.RESISTANCE)))
        val combat = combatOf(listOf(kira, goblin), listOf(strike(1, 1, 3, 4, DamageType.FIRE, HitEffect.HALF)))
        assertTrue(inferResistances(combat, known).isEmpty())
    }

    @Test
    fun `immunity is read from a hit that dealt nothing`() {
        val result = guesses(strike(1, 1, 3, 0, DamageType.POISON, HitEffect.NONE))
        assertEquals(DamageState.IMMUNITY, result.single().state)
    }
}
