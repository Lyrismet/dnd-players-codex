package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MentionQuantityTest {
    private val goblin = MentionRef(EntityKind.SPECIES, 7)
    private val picked = "Нашли @[Гоблин|s:7] "

    @Test
    fun `a picked species waits for its count`() {
        assertEquals("Гоблин", pendingQuantityName(picked, picked.length))
    }

    @Test
    fun `the hint goes once the caret leaves the end or a count is set`() {
        assertNull(pendingQuantityName(picked, picked.length - 1))
        assertNull(pendingQuantityName("Нашли @[Гоблин|s:7|4] ", 22))
        assertNull(pendingQuantityName("Нашли @[Маг|n:7] ", 17))
    }

    @Test
    fun `the first digit goes into the token`() {
        val edit = typedQuantityEdit(picked, picked.length, picked + "4")
        assertEquals("Нашли @[Гоблин|s:7|4] ", edit?.text)
        assertEquals(edit?.text?.length?.minus(1), edit?.caret)
    }

    @Test
    fun `a letter after the token is not a count`() {
        assertNull(typedQuantityEdit(picked, picked.length, picked + "и"))
        assertNull(typedQuantityEdit(picked, picked.length, picked + "0"))
    }

    @Test
    fun `the next digit extends the count`() {
        val first = typedQuantityEdit(picked, picked.length, picked + "1")!!
        val typed = first.text.dropLast(1) + "2 "
        val second = typedQuantityEdit(first.text, first.caret, typed)
        assertEquals("Нашли @[Гоблин|s:7|12] ", second?.text)
    }

    @Test
    fun `a count past the cap keeps the draft`() {
        val full = "@[Гоблин|s:7|999] "
        val edit = typedQuantityEdit(full, full.length - 1, "@[Гоблин|s:7|999]1 ")
        assertEquals(full, edit?.text)
    }

    @Test
    fun `quantities are listed in the order of the tokens`() {
        val body = "@[Гоблин|s:7|4] и @[Волк|s:8] и @[Гоблин|s:7]"
        assertEquals(listOf(4, 1), tokenQuantities(body, goblin))
    }

    @Test
    fun `one token gets a new count and the others stay`() {
        val body = "@[Гоблин|s:7|4] и @[Волк|s:8] и @[Гоблин|s:7]"
        assertEquals(
            "@[Гоблин|s:7|4] и @[Волк|s:8] и @[Гоблин|s:7|3]",
            withTokenQuantity(body, goblin, 1, 3),
        )
    }

    @Test
    fun `a count of one is written without a number`() {
        assertEquals("@[Гоблин|s:7]", withTokenQuantity("@[Гоблин|s:7|4]", goblin, 0, 1))
    }

    @Test
    fun `a count is kept between one and the cap`() {
        assertEquals("@[Гоблин|s:7|999]", withTokenQuantity("@[Гоблин|s:7]", goblin, 0, 5000))
        assertEquals("@[Гоблин|s:7]", withTokenQuantity("@[Гоблин|s:7|3]", goblin, 0, -2))
    }
}
