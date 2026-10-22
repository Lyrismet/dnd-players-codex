package com.lyrismet.incadent.feature.combat.domain.grouping

import kotlin.test.Test
import kotlin.test.assertEquals

class CombatNamesTest {
    @Test
    fun `a name taken once stays as it is`() {
        assertEquals(listOf("Гоблин", "Орк"), autonumber(listOf("Гоблин", "Орк")))
    }

    @Test
    fun `a repeated name is numbered in order`() {
        assertEquals(
            listOf("Скелет-лучник I", "Орк", "Скелет-лучник II"),
            autonumber(listOf("Скелет-лучник", "Орк", "Скелет-лучник")),
        )
    }

    @Test
    fun `numbering continues past ten with roman digits`() {
        val names = autonumber(List(10) { "Крыса" })
        assertEquals("Крыса X", names.last())
    }

    @Test
    fun `the base name drops the autonumber`() {
        assertEquals("Скелет-лучник", "Скелет-лучник II".baseName())
        assertEquals("Скелет-лучник", "Скелет-лучник".baseName())
    }
}
