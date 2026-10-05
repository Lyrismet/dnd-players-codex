package com.lyrismet.incadent.presentation.codex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CodexEmptySearchTest {
    @Test
    fun `a trimmed query shorter than the minimum offers no create`() {
        assertNull(codexSearchCreate(CodexTab.NPC, "ab"))
        assertNull(codexSearchCreate(CodexTab.NPC, "  ab  "))
    }

    @Test
    fun `a trimmed query of the minimum length offers a create with the trimmed name`() {
        assertEquals(CodexSearchCreate(CodexEntryType.NPC, "abc"), codexSearchCreate(CodexTab.NPC, "  abc "))
    }

    @Test
    fun `the create type follows the tab and the all tab creates an npc`() {
        assertEquals(CodexEntryType.NPC, codexSearchCreate(CodexTab.ALL, "Рагнар")?.type)
        assertEquals(CodexEntryType.NPC, codexSearchCreate(CodexTab.NPC, "Рагнар")?.type)
        assertEquals(CodexEntryType.QUEST, codexSearchCreate(CodexTab.QUEST, "Ключ")?.type)
        assertEquals(CodexEntryType.LOCATION, codexSearchCreate(CodexTab.LOCATION, "Башня")?.type)
        assertEquals(CodexEntryType.PARTY, codexSearchCreate(CodexTab.PARTY, "Бран")?.type)
    }

    @Test
    fun `a tab with entries shows no empty state whatever the query`() {
        assertEquals(CodexEmptyKind.None, codexEmptyKind(CodexTab.NPC, "", hasEntries = true))
        assertEquals(CodexEmptyKind.None, codexEmptyKind(CodexTab.NPC, "Рагнар", hasEntries = true))
    }

    @Test
    fun `a blank tab without a search is a blank state a whitespace search counts as no search`() {
        assertEquals(CodexEmptyKind.Blank(CodexTab.QUEST), codexEmptyKind(CodexTab.QUEST, "", hasEntries = false))
        assertEquals(CodexEmptyKind.Blank(CodexTab.QUEST), codexEmptyKind(CodexTab.QUEST, "   ", hasEntries = false))
    }

    @Test
    fun `a search that finds nothing is a no-match state with the trimmed query`() {
        assertEquals(
            CodexEmptyKind.NoMatch("Рагнар", CodexSearchCreate(CodexEntryType.NPC, "Рагнар")),
            codexEmptyKind(CodexTab.ALL, "  Рагнар ", hasEntries = false),
        )
    }

    @Test
    fun `a short search that finds nothing is a no-match state without a create`() {
        assertEquals(CodexEmptyKind.NoMatch("ab", null), codexEmptyKind(CodexTab.PARTY, " ab ", hasEntries = false))
    }
}
