package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val ragnar = MentionEntity.NpcMention(1, "Рагнар", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val ragnarWithSurname = MentionEntity.NpcMention(2, "Рагнар Каменный", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val forge = MentionEntity.LocationMention(3, "Кузница")
private val sword = MentionEntity.QuestMention(4, "Меч", QuestStatus.ACTIVE)

private val index = MentionIndex(listOf(ragnar, ragnarWithSurname, forge, sword))

class MentionTest {
    @Test
    fun `parseMentions returns a single text segment for plain text`() {
        val segments = parseMentions("просто текст без упоминаний", index)
        assertEquals(listOf(MentionSegment.Text("просто текст без упоминаний")), segments)
    }

    @Test
    fun `parseMentions returns nothing for empty text`() {
        assertEquals(emptyList(), parseMentions("", index))
    }

    @Test
    fun `parseMentions splits text around a token`() {
        val segments = parseMentions("Поговорили с ${ragnar.token()} про меч", index)
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
    fun `a token resolves by id so a longer name starting with the same words is another record`() {
        val segments = parseMentions("${ragnarWithSurname.token()} идёт", index)
        assertEquals(
            listOf(MentionSegment.Mention(ragnarWithSurname), MentionSegment.Text(" идёт")),
            segments,
        )
    }

    @Test
    fun `a typed at sign with a name is plain text until it is turned into a token`() {
        val segments = parseMentions("пишу code@example.com и @Рагнар", index)
        assertEquals(listOf(MentionSegment.Text("пишу code@example.com и @Рагнар")), segments)
    }

    @Test
    fun `records of different kinds with one name stay separate`() {
        val npc = MentionEntity.NpcMention(10, "Рагнар", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
        val place = MentionEntity.LocationMention(10, "Рагнар")
        val quest = MentionEntity.QuestMention(10, "Рагнар", QuestStatus.ACTIVE)
        val shared = MentionIndex(listOf(npc, place, quest))

        val segments = parseMentions("${quest.token()} ${place.token()} ${npc.token()}", shared)

        assertEquals(
            listOf(
                MentionSegment.Mention(quest),
                MentionSegment.Text(" "),
                MentionSegment.Mention(place),
                MentionSegment.Text(" "),
                MentionSegment.Mention(npc),
            ),
            segments,
        )
    }

    @Test
    fun `a renamed record shows its current name while the token keeps the old one`() {
        val stale = "@[Старое имя|n:1]"

        val segments = parseMentions(stale, index)

        assertEquals(listOf(MentionSegment.Mention(ragnar)), segments)
    }

    @Test
    fun `a token of a record that is gone becomes a deleted mention carrying its name and kind`() {
        val segments = parseMentions("видели ${mentionToken(MentionRef(EntityKind.NPC, 99), "Кассиан")}", index)

        assertEquals(
            listOf(
                MentionSegment.Text("видели "),
                MentionSegment.Mention(MentionEntity.DeletedMention(99, "Кассиан", EntityKind.NPC)),
            ),
            segments,
        )
    }

    @Test
    fun `a token name keeps no brackets or bars so it always parses back`() {
        val odd = MentionEntity.LocationMention(5, "Зал [восточный] | северный")

        val segments = parseMentions(odd.token(), MentionIndex(listOf(odd)))

        assertEquals(listOf(MentionSegment.Mention(odd)), segments)
    }

    @Test
    fun `a token holds no whitespace so phrases cut from a note never split it`() {
        val spaced = MentionEntity.LocationMention(6, "Зал  с колоннами")

        assertEquals(false, spaced.token().any { it.isWhitespace() })
        assertEquals(
            listOf(MentionSegment.Mention(spaced)),
            parseMentions(spaced.token(), MentionIndex(listOf(spaced))),
        )
        assertEquals("Зал с колоннами", plainMentionText(spaced.token()))
    }

    @Test
    fun `typed names turn into tokens when they belong to one record`() {
        val typed = linkTypedMentions("встретили @Рагнар Каменный и @Меч", index)

        assertEquals("встретили ${ragnarWithSurname.token()} и ${sword.token()}", typed)
    }

    @Test
    fun `a typed name two records share is left as text`() {
        val twin = MentionEntity.LocationMention(8, "Рагнар")
        val shared = MentionIndex(listOf(ragnar, twin))

        assertEquals("@Рагнар", linkTypedMentions("@Рагнар", shared))
    }

    @Test
    fun `insertMention replaces the trailing query with the token and a space`() {
        val result = insertMention("Поговорили с @Ра", ragnar.token())
        assertEquals("Поговорили с ${ragnar.token()} ", result)
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
    fun `trailingMentionQuery ignores a finished token at the end of the draft`() {
        assertNull(trailingMentionQuery("Поговорили с ${ragnar.token()}"))
    }

    @Test
    fun `trailingMentionQuery reads the query that ends at the caret`() {
        assertEquals("Ра", trailingMentionQuery("Поговорили с @Ра и ушли", caret = 16))
    }

    @Test
    fun `insertMentionAt keeps the text after the caret and moves the caret past the token`() {
        val result = insertMentionAt("с @Ра и ушли", ragnar.token(), caret = 5)
        assertEquals("с ${ragnar.token()} и ушли", result.text)
        assertEquals("с ${ragnar.token()} ".length, result.caret)
    }

    @Test
    fun `trailingMentionQuery returns null when there is no at-sign`() {
        assertNull(trailingMentionQuery("просто текст"))
    }

    @Test
    fun `mentionsIn dedupes repeated mentions keeping the first-seen order`() {
        val bodies = listOf("${ragnar.token()} и снова ${ragnar.token()}", "теперь заходит ${forge.token()}")
        assertEquals(listOf(ragnar, forge), mentionsIn(bodies, index))
    }

    @Test
    fun `plain text swaps every token for its name`() {
        assertEquals("Рагнар и Меч", plainMentionText("${ragnar.token()} и ${sword.token()}"))
    }

    @Test
    fun `an edit that cut into a token removes the token whole`() {
        val whole = "увидели ${ragnar.token()} там"
        val cut = whole.replace("]", "")

        assertEquals("увидели  там", withoutBrokenTokens(cut))
        assertEquals(whole, withoutBrokenTokens(whole))
    }

    @Test
    fun `a stray token opener loses just its two characters`() {
        assertEquals("почта", withoutBrokenTokens("по@[чта"))
    }

    @Test
    fun `a typed name links whatever its case`() {
        assertEquals("${forge.token()} горит", linkTypedMentions("@кузница горит", index))
    }

    @Test
    fun `a name shared by two records is left as typed`() {
        val twins =
            MentionIndex(listOf(ragnar, MentionEntity.NpcMention(9, "Рагнар", NpcStatus.ENEMY, NpcLifeState.ALIVE)))
        assertEquals("@Рагнар шёл", linkTypedMentions("@Рагнар шёл", twins))
    }

    @Test
    fun `relinking by name and ref points the tokens at the new record`() {
        val from = MentionRef(EntityKind.NPC, 77)
        val body = "см. ${mentionToken(from, "Старик")} и ${ragnar.token()}"
        assertEquals("см. ${forge.token()} и ${ragnar.token()}", relinkMentionIn(body, from, forge.ref, forge.name))
    }
}
