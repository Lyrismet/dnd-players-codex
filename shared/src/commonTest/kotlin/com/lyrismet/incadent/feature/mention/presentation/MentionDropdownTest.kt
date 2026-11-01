package com.lyrismet.incadent.feature.mention.presentation

import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val labels =
    MentionTypeLabels(npc = "NPC", party = "Отряд", location = "Место", quest = "Квест", species = "Вид")
private val texts =
    MentionDropdownTexts(
        search = "ПОИСК",
        thisSession = "В ЭТОЙ СЕССИИ",
        previousSession = "В ПРОШЛОЙ СЕССИИ",
        recent = "НЕДАВНО СОЗДАНЫ",
        codex = "КОДЕКС",
    )
private val npcs = (1L..9L).map { MentionEntity.NpcMention(it, "Герой $it", NpcStatus.NEUTRAL, NpcLifeState.ALIVE) }

private fun input(
    entities: List<MentionEntity>,
    current: List<String> = emptyList(),
    nicknames: Map<com.lyrismet.incadent.feature.mention.domain.MentionRef, List<String>> = emptyMap(),
) = MentionDropdownInput(
    entities,
    MentionIndex(entities),
    nicknames,
    emptyMap(),
    MentionSessionBodies(current, emptyList()),
)

class MentionDropdownTest {
    @Test
    fun `the latest mentions of a session are the ones kept`() {
        val notes = npcs.map { it.token() }
        val items = mentionDropdown(input(npcs, notes), "", labels, texts)

        val names = items.filterIsInstance<MentionDropdownItem.Entry>().map { it.suggestion.name }
        assertEquals(listOf("Герой 9", "Герой 8", "Герой 7", "Герой 6", "Герой 5", "Герой 4", "Герой 3"), names)
        assertEquals("В ЭТОЙ СЕССИИ", (items.first() as MentionDropdownItem.Header).title)
    }

    @Test
    fun `a nickname hit travels as its own field`() {
        val items =
            mentionDropdown(input(npcs, nicknames = mapOf(npcs[0].ref to listOf("Рыжий"))), "рыж", labels, texts)

        val entry = items.filterIsInstance<MentionDropdownItem.Entry>().single()
        assertEquals("Рыжий", entry.suggestion.nickname)
    }

    @Test
    fun `a typed name no record has offers to make one`() {
        val items = mentionDropdown(input(npcs), "Zzz", labels, texts)

        assertIs<MentionDropdownItem.Create>(items.last())
    }

    @Test
    fun `a codex of three records or less ends with the hinted create row`() {
        val few = npcs.take(3)

        assertEquals(
            MentionDropdownItem.CreateRecord(withHint = true),
            mentionDropdown(input(few), "", labels, texts).last(),
        )
        assertEquals(
            MentionDropdownItem.CreateRecord(withHint = true),
            mentionDropdown(input(few), "Герой 1", labels, texts).last(),
        )
    }

    @Test
    fun `a bigger codex ends with the compact create row`() {
        assertEquals(
            MentionDropdownItem.CreateRecord(withHint = false),
            mentionDropdown(input(npcs), "", labels, texts).last(),
        )
    }

    @Test
    fun `the create row is split off to stay pinned`() {
        val rows = mentionDropdown(input(npcs), "", labels, texts)
        val (list, pinned) = rows.withPinnedCreate()

        assertEquals(MentionDropdownItem.CreateRecord(withHint = false), pinned)
        assertEquals(rows.dropLast(1), list)
    }

    @Test
    fun `rows without a create row pin nothing`() {
        val rows = listOf(MentionDropdownItem.EmptyCodex)

        assertEquals(rows to null, rows.withPinnedCreate())
    }
}
