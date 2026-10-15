package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.domain.model.EntityKind
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
        assertEquals(CodexSearchCreate(EntityKind.NPC, "abc"), codexSearchCreate(CodexTab.NPC, "  abc "))
    }

    @Test
    fun `the create type follows the tab and the all tab creates an npc`() {
        assertEquals(EntityKind.NPC, codexSearchCreate(CodexTab.ALL, "Рагнар")?.type)
        assertEquals(EntityKind.NPC, codexSearchCreate(CodexTab.NPC, "Рагнар")?.type)
        assertEquals(EntityKind.QUEST, codexSearchCreate(CodexTab.QUEST, "Ключ")?.type)
        assertEquals(EntityKind.LOCATION, codexSearchCreate(CodexTab.LOCATION, "Башня")?.type)
        assertEquals(EntityKind.PARTY, codexSearchCreate(CodexTab.PARTY, "Бран")?.type)
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
            CodexEmptyKind.NoMatch("Рагнар", CodexSearchCreate(EntityKind.NPC, "Рагнар")),
            codexEmptyKind(CodexTab.ALL, "  Рагнар ", hasEntries = false),
        )
    }

    @Test
    fun `a short search that finds nothing is a no-match state without a create`() {
        assertEquals(CodexEmptyKind.NoMatch("ab", null), codexEmptyKind(CodexTab.PARTY, " ab ", hasEntries = false))
    }

    @Test
    fun `a no-match state carries the matches of the other tabs`() {
        assertEquals(
            CodexEmptyKind.NoMatch("Рагнар", CodexSearchCreate(EntityKind.QUEST, "Рагнар"), otherMatches = 3),
            codexEmptyKind(CodexTab.QUEST, "Рагнар", hasEntries = false, otherMatches = 3),
        )
    }

    @Test
    fun `other section matches skip the active tab and the all tab has none`() {
        assertEquals(0, otherSectionMatches(CodexTab.ALL, party = 1, npcs = 2, quests = 3, locations = 4))
        assertEquals(9, otherSectionMatches(CodexTab.PARTY, party = 1, npcs = 2, quests = 3, locations = 4))
        assertEquals(8, otherSectionMatches(CodexTab.NPC, party = 1, npcs = 2, quests = 3, locations = 4))
        assertEquals(7, otherSectionMatches(CodexTab.QUEST, party = 1, npcs = 2, quests = 3, locations = 4))
        assertEquals(6, otherSectionMatches(CodexTab.LOCATION, party = 1, npcs = 2, quests = 3, locations = 4))
    }
}
