package com.lyrismet.dndcodex.core.mention

import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val QUEST_PREFIX = "Квест: "

private val ragnar = MentionEntity.NpcMention(1, "Рагнар", NpcStatus.FRIEND)
private val ragnarWithSurname = MentionEntity.NpcMention(2, "Рагнар Каменный", NpcStatus.FRIEND)
private val forge = MentionEntity.LocationMention(3, "Кузница")
private val sword = MentionEntity.QuestMention(4, "Меч", QuestStatus.ACTIVE)

private val candidates = mentionCandidates(listOf(ragnar, ragnarWithSurname, forge, sword), QUEST_PREFIX)

class MentionTest {
    @Test
    fun `parseMentions returns a single text segment for plain text`() {
        val segments = parseMentions("просто текст без упоминаний", candidates)
        assertEquals(listOf(MentionSegment.Text("просто текст без упоминаний")), segments)
    }

    @Test
    fun `parseMentions returns nothing for empty text`() {
        assertEquals(emptyList(), parseMentions("", candidates))
    }

    @Test
    fun `parseMentions splits text around a mention`() {
        val segments = parseMentions("Поговорили с @Рагнар про меч", candidates)
        assertEquals(
            listOf(
                MentionSegment.Text("Поговорили с "),
                MentionSegment.Mention(ragnar),
                MentionSegment.Text(" про меч"),
            ),
            segments,
        )
    }

    @Test
    fun `parseMentions prefers the longest matching candidate key`() {
        val segments = parseMentions("@Рагнар Каменный идёт", candidates)
        assertEquals(
            listOf(MentionSegment.Mention(ragnarWithSurname), MentionSegment.Text(" идёт")),
            segments,
        )
    }

    @Test
    fun `parseMentions keeps an unmatched at-sign as plain text`() {
        val segments = parseMentions("пишу code@example.com для примера", candidates)
        assertEquals(listOf(MentionSegment.Text("пишу code@example.com для примера")), segments)
    }

    @Test
    fun `parseMentions resolves a quest mention using its prefix`() {
        val segments = parseMentions("выполнили @Квест: Меч", candidates)
        assertEquals(
            listOf(MentionSegment.Text("выполнили "), MentionSegment.Mention(sword)),
            segments,
        )
    }

    @Test
    fun `matchingMentionEntities matches by name prefix case-insensitively`() {
        val matches = matchingMentionEntities(listOf(ragnar, forge, sword), "рагн", QUEST_PREFIX)
        assertEquals(listOf(ragnar), matches)
    }

    @Test
    fun `matchingMentionEntities strips the quest prefix before matching`() {
        val matches = matchingMentionEntities(listOf(ragnar, forge, sword), "Квест: ме", QUEST_PREFIX)
        assertEquals(listOf(sword), matches)
    }

    @Test
    fun `matchingMentionEntities respects the limit`() {
        val many = (1..10).map { MentionEntity.NpcMention(it.toLong(), "Стражник $it", NpcStatus.NEUTRAL) }
        val matches = matchingMentionEntities(many, "страж", QUEST_PREFIX, limit = 3)
        assertEquals(3, matches.size)
    }

    @Test
    fun `insertMention replaces the trailing query with the full candidate key`() {
        val result = insertMention("Поговорили с @Ра", "Рагнар")
        assertEquals("Поговорили с @Рагнар ", result)
    }

    @Test
    fun `trailingMentionQuery finds the in-progress mention at the end of the draft`() {
        assertEquals("Ра", trailingMentionQuery("Поговорили с @Ра"))
    }

    @Test
    fun `trailingMentionQuery returns null once the mention is closed by a space`() {
        assertNull(trailingMentionQuery("Поговорили с @Рагнар "))
    }

    @Test
    fun `trailingMentionQuery returns null when there is no at-sign`() {
        assertNull(trailingMentionQuery("просто текст"))
    }

    @Test
    fun `mentionsIn dedupes repeated mentions keeping the first-seen order`() {
        val bodies = listOf("@Рагнар и снова @Рагнар", "теперь заходит @Кузница")
        val mentions = mentionsIn(bodies, candidates)
        assertEquals(listOf(ragnar, forge), mentions)
    }
}
