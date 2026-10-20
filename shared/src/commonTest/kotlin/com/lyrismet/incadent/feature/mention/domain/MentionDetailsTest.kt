package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

private val valley = Location(1, "Долина Эрн", "")
private val village = Location(2, "Мирдейл", "", parentId = 1)
private val miller =
    Npc(5, "Мельник", NpcStatus.FRIEND, NpcLifeState.ALIVE, "", listOf(2), "Человек", nicknames = listOf("Дед"))
private val quest = Quest(7, "Долг", QuestStatus.ACTIVE, "", givenByNpcId = 5, locationId = null)

class MentionDetailsTest {
    private val details =
        mentionDetails(listOf(miller), listOf(valley, village), listOf(quest), emptyList()) { "от $it" }

    @Test
    fun `an npc shows race and place`() {
        assertEquals("Человек · Мирдейл", details[MentionRef(EntityKind.NPC, 5)])
    }

    @Test
    fun `a place shows the path above it`() {
        assertEquals("Долина Эрн", details[MentionRef(EntityKind.LOCATION, 2)])
        assertEquals("", details[MentionRef(EntityKind.LOCATION, 1)])
    }

    @Test
    fun `a quest shows who gave it`() {
        assertEquals("от Мельник", details[MentionRef(EntityKind.QUEST, 7)])
    }

    @Test
    fun `nicknames are listed only for records that have them`() {
        val nicknames = mentionNicknames(listOf(miller))
        assertEquals(mapOf(MentionRef(EntityKind.NPC, 5) to listOf("Дед")), nicknames)
    }
}
