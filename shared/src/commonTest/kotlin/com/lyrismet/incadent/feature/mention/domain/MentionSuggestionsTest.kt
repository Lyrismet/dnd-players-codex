package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val miller = MentionEntity.NpcMention(1, "Старый мельник", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val yolka = MentionEntity.NpcMention(2, "Ёлка", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
private val mill = MentionEntity.LocationMention(3, "Мельница")
private val quest = MentionEntity.QuestMention(4, "Мельничный долг", QuestStatus.ACTIVE)
private val all = listOf(miller, yolka, mill, quest)

class MentionSuggestionsTest {
    @Test
    fun `a name start comes before a nickname and a name inside`() {
        val search = searchMentions(all, mapOf(yolka.ref to listOf("Мелкая")), "мел")
        assertEquals(listOf(mill.ref, quest.ref, yolka.ref, miller.ref), search.candidates.map { it.entity.ref })
    }

    @Test
    fun `a nickname match carries the nickname`() {
        val search = searchMentions(all, mapOf(miller.ref to listOf("Дед Мих")), "дед")
        assertEquals(listOf(miller), search.candidates.map { it.entity })
        assertEquals("Дед Мих", search.candidates.single().nickname)
    }

    @Test
    fun `yo and ye are the same letter`() {
        assertEquals(listOf(yolka), searchMentions(all, emptyMap(), "елк").candidates.map { it.entity })
        assertEquals(listOf(yolka), searchMentions(all, emptyMap(), "ёлк").candidates.map { it.entity })
    }

    @Test
    fun `the preferred kind comes first`() {
        val search = searchMentions(all, emptyMap(), "мел", preferred = EntityKind.QUEST)
        assertEquals(
            quest.ref,
            search.candidates
                .first()
                .entity.ref,
        )
    }

    @Test
    fun `create is offered unless the exact name exists`() {
        assertEquals("Стар", searchMentions(all, emptyMap(), "Стар").createName)
        assertNull(searchMentions(all, emptyMap(), "Ёлка").createName)
        assertNull(searchMentions(all, emptyMap(), "елка").createName)
    }

    @Test
    fun `create is offered alone when nothing matches`() {
        val search = searchMentions(all, emptyMap(), "Xyz")
        assertEquals(emptyList(), search.candidates)
        assertEquals("Xyz", search.createName)
    }

    @Test
    fun `an empty query offers no create`() {
        assertNull(searchMentions(all, emptyMap(), "").createName)
    }

    @Test
    fun `empty rows go by section without repeats`() {
        val rows = emptyMentionRows(listOf(miller), listOf(miller, mill), listOf(mill, quest), all)
        assertEquals(
            listOf(
                MentionSection.THIS_SESSION to miller.ref,
                MentionSection.PREVIOUS_SESSION to mill.ref,
                MentionSection.RECENT to quest.ref,
            ),
            rows.map { it.section to it.candidate.entity.ref },
        )
    }

    @Test
    fun `empty rows fall back to the codex by name`() {
        val rows = emptyMentionRows(emptyList(), emptyList(), emptyList(), all)
        assertEquals(setOf(MentionSection.CODEX), rows.map { it.section }.toSet())
        assertEquals(all.size, rows.size)
    }

    @Test
    fun `empty rows stop at seven`() {
        val many = (1L..20L).map { MentionEntity.LocationMention(it, "Место $it") }
        assertEquals(7, emptyMentionRows(many, emptyList(), emptyList(), many).size)
    }

    @Test
    fun `records sharing a name are all listed`() {
        val twin = MentionEntity.LocationMention(9, "Мельница")
        val refs = searchMentions(listOf(mill, twin), emptyMap(), "мельн").candidates.map { it.entity.ref }
        assertEquals(listOf(mill.ref, twin.ref), refs)
    }

    @Test
    fun `search stops at five rows`() {
        val many = (1L..9L).map { MentionEntity.LocationMention(it, "Страж $it") }
        assertEquals(5, searchMentions(many, emptyMap(), "страж").candidates.size)
    }
}
