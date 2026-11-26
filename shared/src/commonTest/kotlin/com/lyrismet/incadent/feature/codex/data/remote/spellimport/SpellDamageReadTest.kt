package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.CantripGrowth
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.GrowthMode
import com.lyrismet.incadent.feature.codex.domain.model.GrowthStep
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpellDamageReadTest {
    private fun api(
        level: Int,
        extra: String,
    ) = """{"name":"S","level":$level,"desc":["Text."],"school":{"name":"Evocation"},"components":["V"],
        "casting_time":"1 action","range":"60 feet","duration":"Instantaneous",$extra}"""

    @Test
    fun `slot table of an even growth becomes one dice rule`() {
        val spell =
            assertNotNull(
                readDnd5eApiSpell(
                    api(
                        3,
                        """"damage":[{"damage_type":{"index":"fire"},"damage_at_slot_level":
                        {"3":"8d6","4":"9d6","5":"10d6","6":"11d6","7":"12d6","8":"13d6","9":"14d6"}}]""",
                    ),
                ),
            )

        assertEquals("8d6", spell.damage.single().dice)
        assertEquals(SpellEffect.Damage(DamageType.FIRE), spell.damage.single().effect)
        assertTrue(spell.growth.enabled)
        val rule = spell.growth.rules.single()
        assertEquals(GrowthMode.EVERY, rule.mode)
        assertEquals("1d6", rule.add)
    }

    @Test
    fun `slot table with a flat bonus becomes steps`() {
        val spell =
            assertNotNull(
                readDnd5eApiSpell(
                    api(
                        1,
                        """"damage":[{"damage_type":{"index":"force"},"damage_at_slot_level":
                        {"1":"3d4 + 3","2":"4d4 + 4","3":"5d4 + 5"}}]""",
                    ),
                ),
            )

        assertEquals("3d4+3", spell.damage.single().dice)
        val rule = spell.growth.rules.single()
        assertEquals(GrowthMode.STEPS, rule.mode)
        assertEquals(listOf(GrowthStep(2, "4d4+4"), GrowthStep(3, "5d4+5")), rule.steps)
    }

    @Test
    fun `healing table becomes a healing row`() {
        val spell =
            assertNotNull(
                readDnd5eApiSpell(
                    api(1, """"heal_at_slot_level":{"1":"1d8 + MOD","2":"2d8 + MOD","3":"3d8 + MOD"}"""),
                ),
            )

        assertEquals(SpellEffect.Healing, spell.damage.single().effect)
        assertEquals("1d8+MOD", spell.damage.single().dice)
    }

    @Test
    fun `cantrip that gains a die each tier grows by dice`() {
        val spell =
            assertNotNull(
                readDnd5eApiSpell(
                    api(
                        0,
                        """"damage":[{"damage_type":{"index":"fire"},"damage_at_character_level":
                        {"1":"1d10","5":"2d10","11":"3d10","17":"4d10"}}]""",
                    ),
                ),
            )

        assertEquals("1d10", spell.damage.single().dice)
        assertEquals(CantripGrowth.DICE, spell.growth.cantrip)
        assertFalse(spell.growth.enabled)
    }

    @Test
    fun `spell without tables keeps the damage and the growth empty`() {
        val spell = assertNotNull(readDnd5eApiSpell(api(2, """"damage":[]""")))

        assertTrue(spell.damage.isEmpty())
        assertFalse(spell.growth.enabled)
    }

    @Test
    fun `open5e v2 casting options become the growth`() {
        val body =
            """{"name":"Fireball","level":3,"desc":"Text.","school":{"name":"Evocation"},"verbal":true,
            "casting_time":"action","range_text":"150 feet","duration":"instantaneous","damage_roll":"8d6","damage_types":["fire"],
            "casting_options":[{"type":"default","damage_roll":null},{"type":"slot_level_4","damage_roll":"9d6"},
            {"type":"slot_level_5","damage_roll":"10d6"},{"type":"slot_level_6","damage_roll":"11d6"},
            {"type":"slot_level_7","damage_roll":"12d6"},{"type":"slot_level_8","damage_roll":"13d6"},
            {"type":"slot_level_9","damage_roll":"14d6"}]}"""

        val spell = assertNotNull(readOpen5eV2Spell(body))

        assertEquals("8d6", spell.damage.single().dice)
        assertEquals(
            "1d6",
            spell.growth.rules
                .single()
                .add,
        )
    }
}
