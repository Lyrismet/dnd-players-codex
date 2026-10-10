package com.lyrismet.incadent.core.mention

import kotlin.test.Test
import kotlin.test.assertEquals

class MentionRenameTest {
    @Test
    fun `renames every mention of the old key`() {
        val result = renameMentionIn("@Рагнар пришёл, потом @Рагнар ушёл", "Рагнар", "Ларс", emptyList())
        assertEquals("@Ларс пришёл, потом @Ларс ушёл", result)
    }

    @Test
    fun `leaves a longer mention that merely starts with the old key`() {
        val result = renameMentionIn("@Рагнар Каменный и @Рагнар", "Рагнар", "Ларс", listOf("Рагнар Каменный"))
        assertEquals("@Рагнар Каменный и @Ларс", result)
    }

    @Test
    fun `leaves text without an at sign alone`() {
        assertEquals("Рагнар пришёл", renameMentionIn("Рагнар пришёл", "Рагнар", "Ларс", emptyList()))
    }

    @Test
    fun `renames a quest mention with its prefix`() {
        val result = renameMentionIn("взяли @Квест: Меч", "Квест: Меч", "Квест: Щит", emptyList())
        assertEquals("взяли @Квест: Щит", result)
    }
}
