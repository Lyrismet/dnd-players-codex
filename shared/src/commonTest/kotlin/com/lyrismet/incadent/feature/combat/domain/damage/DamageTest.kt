package com.lyrismet.incadent.feature.combat.domain.damage

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class DamageTest {
    @Test
    fun `a typed sum becomes terms with their types`() {
        val terms = parseTerms("8+6", listOf(DamageType.FIRE, null))
        assertEquals(listOf(DamageTerm(8, DamageType.FIRE), DamageTerm(6, null)), terms)
    }

    @Test
    fun `an empty or zero term is dropped`() {
        assertEquals(listOf(DamageTerm(5)), parseTerms("5+0+", emptyList()))
    }

    @Test
    fun `resistance halves a term and rounds down`() {
        val hit =
            planHit(
                listOf(DamageTerm(7, DamageType.FIRE)),
                HitScale.ONE,
                mapOf(
                    DamageType.FIRE to DamageState.RESISTANCE,
                ),
            )
        assertEquals(3, hit.total)
        assertEquals(HitEffect.HALF, hit.terms.single().effect)
    }

    @Test
    fun `immunity takes everything and vulnerability doubles`() {
        val states = mapOf(DamageType.COLD to DamageState.IMMUNITY, DamageType.FIRE to DamageState.VULNERABILITY)
        val hit = planHit(listOf(DamageTerm(9, DamageType.COLD), DamageTerm(4, DamageType.FIRE)), HitScale.ONE, states)
        assertEquals(listOf(0, 8), hit.terms.map { it.effective })
    }

    @Test
    fun `an unknown or untyped term changes nothing`() {
        val hit =
            planHit(
                listOf(DamageTerm(5, DamageType.ACID), DamageTerm(4)),
                HitScale.ONE,
                mapOf(
                    DamageType.ACID to DamageState.UNKNOWN,
                ),
            )
        assertEquals(9, hit.total)
    }

    @Test
    fun `a saved hit halves every term on top of the resistance`() {
        val hit =
            planHit(
                listOf(DamageTerm(8, DamageType.FIRE)),
                HitScale.HALF,
                mapOf(
                    DamageType.FIRE to DamageState.RESISTANCE,
                ),
            )
        assertEquals(2, hit.total)
    }

    @Test
    fun `each term is rounded on its own`() {
        val hit = planHit(listOf(DamageTerm(3), DamageTerm(3)), HitScale.HALF, emptyMap())
        assertEquals(2, hit.total)
    }

    @Test
    fun `a correction recomputes a term from the raw number`() {
        assertEquals(6, effectiveDamage(raw = 12, scale = HitScale.ONE, effect = HitEffect.HALF))
        assertEquals(24, effectiveDamage(raw = 12, scale = HitScale.ONE, effect = HitEffect.DOUBLE))
    }

    @Test
    fun `damage takes the temporary hit points first`() {
        assertEquals(HpChange(hp = 17, temp = 0, effective = 8), takeDamage(hp = 20, temp = 5, amount = 8))
    }

    @Test
    fun `damage stops at zero and reports what it really did`() {
        assertEquals(HpChange(hp = 0, temp = 0, effective = 12), takeDamage(hp = 7, temp = 5, amount = 40))
    }

    @Test
    fun `healing stops at the maximum`() {
        assertEquals(HpChange(hp = 30, temp = 0, effective = 4), takeHeal(hp = 26, max = 30, temp = 0, amount = 10))
    }

    @Test
    fun `temporary hit points keep the larger number`() {
        assertEquals(HpChange(hp = 9, temp = 8, effective = 0), takeTemp(hp = 9, temp = 8, amount = 5))
        assertEquals(HpChange(hp = 9, temp = 12, effective = 4), takeTemp(hp = 9, temp = 8, amount = 12))
    }
}
