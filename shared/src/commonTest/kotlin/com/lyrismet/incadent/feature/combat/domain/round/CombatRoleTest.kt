package com.lyrismet.incadent.feature.combat.domain.round

import kotlin.test.Test
import kotlin.test.assertEquals

class CombatRoleTest {
    private val parts = RoleParts("Эльф", "Воин", "Мастер боевых искусств")

    @Test
    fun `a role that fits is told in full`() {
        assertEquals("Эльф · Воин · Мастер боевых искусств", fitRole(parts) { true })
    }

    @Test
    fun `the kind goes first when the line is too narrow`() {
        assertEquals("Воин · Мастер боевых искусств", fitRole(parts) { it.length <= 30 })
    }

    @Test
    fun `the subclass is cut with an ellipsis after the kind is gone`() {
        assertEquals("Воин · Мастер бое…", fitRole(parts) { it.length <= 18 })
    }

    @Test
    fun `the class is never cut`() {
        assertEquals("Воин", fitRole(parts) { false })
    }

    @Test
    fun `a kind alone is left for the text to clip`() {
        assertEquals("Волк", fitRole(RoleParts(kind = "Волк")) { false })
    }

    @Test
    fun `a stored role splits into class and subclass`() {
        assertEquals(RoleParts(characterClass = "Воин", subclass = "Чемпион"), RoleParts.ofStored("Воин · Чемпион"))
        assertEquals(RoleParts(characterClass = "Бард"), RoleParts.ofStored(" Бард "))
        assertEquals("Воин · Чемпион", RoleParts.toStored("Воин", " Чемпион "))
        assertEquals("", RoleParts.toStored("", ""))
    }
}
