package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val goblin = MentionEntity.SpeciesMention(7, "Гоблин")
private val index = MentionIndex(listOf(goblin))

class SpeciesMentionTest {
    @Test
    fun `a species token without a number stands for one`() {
        val segments = parseMentions("Напали ${goblin.token()} ночью", index)
        assertEquals(
            listOf(MentionSegment.Text("Напали "), MentionSegment.Mention(goblin, 1), MentionSegment.Text(" ночью")),
            segments,
        )
        assertEquals("@[Гоблин|s:7]", goblin.token())
    }

    @Test
    fun `a species token carries its number`() {
        val token = mentionToken(goblin.ref, goblin.name, quantity = 4)
        assertEquals("@[Гоблин|s:7|4]", token)
        assertEquals(listOf(MentionSegment.Mention(goblin, 4)), parseMentions(token, index))
    }

    @Test
    fun `a number of one is not written and a huge one is capped`() {
        assertEquals("@[Гоблин|s:7]", mentionToken(goblin.ref, goblin.name, quantity = 1))
        assertEquals("@[Гоблин|s:7|$MAX_MENTION_QUANTITY]", mentionToken(goblin.ref, goblin.name, quantity = 100_000))
        assertEquals(
            listOf(MentionSegment.Mention(goblin, MAX_MENTION_QUANTITY)),
            parseMentions("@[Гоблин|s:7|99999]", index),
        )
    }

    @Test
    fun `a gone species reads as a deleted tag that keeps its number`() {
        val segment = parseMentions("@[Орк|s:9|3]", index).single() as MentionSegment.Mention
        assertEquals(MentionEntity.DeletedMention(9, "Орк", EntityKind.SPECIES), segment.entity)
        assertEquals(3, segment.quantity)
    }

    @Test
    fun `relinking a token keeps its number`() {
        val relinked = relinkMentionIn("@[Орк|s:9|3]", goblin.ref.copy(id = 9), goblin.ref, goblin.name)
        assertEquals("@[Гоблин|s:7|3]", relinked)
    }

    @Test
    fun `the plain text of a token drops the number`() {
        assertEquals("Напали Гоблин ночью", plainMentionText("Напали @[Гоблин|s:7|4] ночью"))
    }

    @Test
    fun `a species name in plain text is never an auto mention`() {
        val segments = parseMentions("Гоблин спит", index, autoMatch = true)
        assertEquals(listOf(MentionSegment.Text("Гоблин спит")), segments)
        assertTrue(autoNamesOf(index).isEmpty())
    }

    @Test
    fun `a typed at name links to the species`() {
        assertEquals("Смотри @[Гоблин|s:7] идёт", linkTypedMentions("Смотри @Гоблин идёт", index))
    }

    @Test
    fun `a whole species token with a number survives the broken token cleanup`() {
        assertEquals("a @[Гоблин|s:7|4] b", withoutBrokenTokens("a @[Гоблин|s:7|4] b"))
    }

    @Test
    fun `a species sharing a name does not hide the npc from auto matching`() {
        val npc = MentionEntity.NpcMention(1, "Гоблин", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
        val shared = MentionIndex(listOf(npc, goblin))
        assertEquals(listOf<MentionEntity>(npc), shared.uniqueByName)
    }

    @Test
    fun `a count is dropped when a species token is relinked to another kind`() {
        val npc = MentionEntity.NpcMention(1, "Гоблин", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
        assertEquals("@[Гоблин|n:1]", relinkMentionIn("@[Гоблин|s:7|4]", goblin.ref, npc))
        assertEquals("@[Гоблин|s:7|4]", relinkMentionIn("@[Гоблин|s:7|4]", goblin.ref, goblin))
    }

    @Test
    fun `a count on a token that is not a species is ignored`() {
        val segment = parseMentions("@[Рагнар|n:1|9]", MentionIndex.EMPTY).single()
        assertEquals(1, (segment as MentionSegment.Mention).quantity)
    }

    @Test
    fun `display text keeps the count and plain text drops it`() {
        assertEquals("Встретили Гоблин ×4 тут", displayMentionText("Встретили @[Гоблин|s:7|4] тут"))
        assertEquals("Встретили Гоблин тут", plainMentionText("Встретили @[Гоблин|s:7|4] тут"))
    }
}

class MentionSessionCountsTest {
    @Test
    fun `a record counts once per session however many notes mention it`() {
        val bodies =
            listOf(
                MentionBody(1, "${goblin.token()} и ${goblin.token()}"),
                MentionBody(1, "ещё ${goblin.token()}"),
                MentionBody(2, "@[Гоблин|s:7|4]"),
                MentionBody(3, "ничего"),
            )
        assertEquals(mapOf(goblin.ref to 2), mentionSessionCounts(bodies, index))
    }

    @Test
    fun `a gone record is still counted by its ref`() {
        val gone = MentionRef(EntityKind.SPECIES, 9)
        assertEquals(mapOf(gone to 1), mentionSessionCounts(listOf(MentionBody(5, "@[Орк|s:9]")), index))
    }
}
