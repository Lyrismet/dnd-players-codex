package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SpellScalingTest {
    private val cold = SpellEffect.Damage(DamageType.COLD)
    private val piercing = SpellEffect.Damage(DamageType.PIERCING)

    private fun spell(
        level: Int,
        growth: SpellGrowth,
        damage: List<SpellDamage> = emptyList(),
        projectiles: Int = 1,
    ) = CharacterSpell(1, "S", level, damage = damage, projectiles = projectiles, growth = growth)

    @Test
    fun `dice add up by their count`() {
        assertEquals("3d8", addDice("1d8", "1d8", 2))
        assertEquals("2d6+3", addDice("1d6+3", "1d6", 1))
        assertEquals("15", addDice("5", "5", 2))
        assertEquals("1d10+1d6", addDice("1d10", "1d6", 1))
        assertEquals("1d8", addDice("1d8", "1d8", 0))
    }

    @Test
    fun `a spell with no growth stays as it is on any circle`() {
        val s = spell(1, SpellGrowth(), listOf(SpellDamage("2d6", cold)))
        assertEquals(
            "2d6",
            s
                .atCircle(5)
                .damage
                .single()
                .dice,
        )
    }

    @Test
    fun `only the row that grows gets stronger every circle`() {
        val rule = GrowthRule(GrowthTarget.DICE, damageIndex = 1, add = "1d6")
        val s =
            spell(
                1,
                SpellGrowth(true, rules = listOf(rule)),
                listOf(SpellDamage("1d10", piercing), SpellDamage("2d6", cold)),
            )
        val at = s.atCircle(3)
        assertEquals(listOf("1d10", "4d6"), at.damage.map { it.dice })
        assertEquals(listOf("1d10", "2d6"), s.atCircle(1).damage.map { it.dice })
    }

    @Test
    fun `every N circles counts whole periods`() {
        val rule = GrowthRule(GrowthTarget.DICE, GrowthMode.EVERY_N, every = 2, add = "1d8")
        val s = spell(2, SpellGrowth(true, rules = listOf(rule)), listOf(SpellDamage("1d8")))
        assertEquals(
            listOf("1d8", "1d8", "2d8", "2d8", "3d8"),
            (2..6).map {
                s
                    .atCircle(it)
                    .damage
                    .single()
                    .dice
            },
        )
    }

    @Test
    fun `steps give the value of the last circle reached`() {
        val rule =
            GrowthRule(
                GrowthTarget.DURATION,
                GrowthMode.STEPS,
                steps =
                    listOf(
                        GrowthStep(3, durationStep(DurationUnit.HOUR, 8)),
                        GrowthStep(5, durationStep(DurationUnit.HOUR, 24)),
                    ),
            )
        val s =
            CharacterSpell(
                1,
                "Hex",
                1,
                durationUnit = DurationUnit.HOUR,
                durationValue = 1,
                growth = SpellGrowth(true, rules = listOf(rule)),
            )
        assertEquals(1, s.atCircle(2).durationValue)
        assertEquals(8, s.atCircle(4).durationValue)
        assertEquals(24, s.atCircle(9).durationValue)
    }

    @Test
    fun `projectiles and targets grow by a number`() {
        val rules = listOf(GrowthRule(GrowthTarget.PROJECTILES, add = "1"), GrowthRule(GrowthTarget.TARGETS, add = "1"))
        val s = spell(1, SpellGrowth(true, rules = rules), listOf(SpellDamage("1d4+1")), projectiles = 3)
        val at = s.atCircle(3)
        assertEquals(5, at.projectiles)
        assertEquals(2, at.extraTargets)
    }

    @Test
    fun `a cantrip grows by the tier of the character level`() {
        val dice =
            CharacterSpell(
                1,
                "Ray",
                0,
                damage = listOf(SpellDamage("1d8", cold)),
                growth = SpellGrowth(cantrip = CantripGrowth.DICE),
            )
        assertEquals(
            listOf("1d8", "2d8", "2d8", "3d8", "4d8"),
            listOf(1, 5, 10, 11, 17).map {
                dice
                    .atCharacterLevel(it)
                    .damage
                    .single()
                    .dice
            },
        )
        val bolt =
            CharacterSpell(
                1,
                "Bolt",
                0,
                damage = listOf(SpellDamage("1d10")),
                growth = SpellGrowth(cantrip = CantripGrowth.PROJECTILES),
            )
        assertEquals(listOf(1, 2, 3, 4), listOf(1, 5, 11, 17).map { bolt.atCharacterLevel(it).projectiles })
        assertEquals(2, cantripTier(11))
    }

    @Test
    fun `growth survives the column text with its special characters`() {
        val growth =
            SpellGrowth(
                enabled = true,
                cantrip = CantripGrowth.PROJECTILES,
                rules =
                    listOf(
                        GrowthRule(GrowthTarget.DICE, add = "1d6"),
                        GrowthRule(
                            GrowthTarget.CUSTOM,
                            GrowthMode.STEPS,
                            steps = listOf(GrowthStep(3, "a, b; c: d | e ~ 50%")),
                        ),
                        GrowthRule(GrowthTarget.DURATION, GrowthMode.EVERY_N, every = 3, add = "2"),
                    ),
            )
        assertEquals(growth, decodeGrowth(encodeGrowth(growth)))
    }

    @Test
    fun `an empty growth is an empty text and garbage decodes to nothing`() {
        assertEquals("", encodeGrowth(SpellGrowth()))
        assertEquals(SpellGrowth(), decodeGrowth(""))
        assertEquals(SpellGrowth(), decodeGrowth("nonsense"))
        assertEquals(emptyList(), decodeGrowth("1~NONE~BAD,EVERY").rules)
    }

    @Test
    fun `the rounds of a slot come from the duration it brings`() {
        val rule =
            GrowthRule(
                GrowthTarget.DURATION,
                GrowthMode.STEPS,
                steps = listOf(GrowthStep(3, durationStep(DurationUnit.MINUTE, 10))),
            )
        val s =
            CharacterSpell(
                1,
                "S",
                1,
                durationUnit = DurationUnit.MINUTE,
                durationValue = 1,
                growth = SpellGrowth(true, rules = listOf(rule)),
            )
        assertEquals(10, s.atCircle(1).combatRounds)
        assertEquals(100, s.atCircle(3).combatRounds)
    }
}
