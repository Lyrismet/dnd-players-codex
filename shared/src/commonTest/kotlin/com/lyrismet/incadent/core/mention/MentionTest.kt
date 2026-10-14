package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val ragnar = MentionEntity.NpcMention(1, "Рагнар", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val ragnarWithSurname = MentionEntity.NpcMention(2, "Рагнар Каменный", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val forge = MentionEntity.LocationMention(3, "Кузница")
private val sword = MentionEntity.QuestMention(4, "Меч", QuestStatus.ACTIVE)

private val candidates = mentionCandidates(listOf(ragnar, ragnarWithSurname, forge, sword))

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
    fun `parseMentions resolves a quest mention by its title`() {
        val segments = parseMentions("выполнили @Меч", candidates)
        assertEquals(
            listOf(MentionSegment.Text("выполнили "), MentionSegment.Mention(sword)),
            segments,
        )
    }

    @Test
    fun `matchingMentionEntities matches by name prefix case-insensitively`() {
        val matches = matchingMentionEntities(listOf(ragnar, forge, sword), "рагн")
        assertEquals(listOf(ragnar), matches)
    }

    @Test
    fun `matchingMentionEntities finds a quest by its title`() {
        val matches = matchingMentionEntities(listOf(ragnar, forge, sword), "ме")
        assertEquals(listOf(sword), matches)
    }

    @Test
    fun `records of different kinds with one name resolve to the first listed one`() {
        val questNamedLikeNpc = MentionEntity.QuestMention(8, "Рагнар", QuestStatus.ACTIVE)
        val both = mentionCandidates(listOf(ragnar, questNamedLikeNpc))

        val segments = parseMentions("@Рагнар", both)

        assertEquals(listOf<MentionSegment>(MentionSegment.Mention(ragnar)), segments)
    }

    @Test
    fun `matchingMentionEntities respects the limit`() {
        val many =
            (1..10).map {
                MentionEntity.NpcMention(
                    it.toLong(),
                    "Стражник $it",
                    NpcStatus.NEUTRAL,
                    NpcLifeState.ALIVE,
                )
            }
        val matches = matchingMentionEntities(many, "страж", limit = 3)
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
