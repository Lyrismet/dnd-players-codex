package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MentionUnlinkedTest {
    @Test
    fun `a bare mention becomes a token no record answers to`() {
        val ref = unlinkedMentionRef("Xyz")
        assertEquals("Пошли к ${mentionToken(ref, "Xyz")} ночью", markUnlinkedMentions("Пошли к @Xyz ночью"))
    }

    @Test
    fun `trailing punctuation stays outside the token`() {
        val ref = unlinkedMentionRef("Xyz")
        assertEquals("видели ${mentionToken(ref, "Xyz")}.", markUnlinkedMentions("видели @Xyz."))
    }

    @Test
    fun `an existing token and an email are left alone`() {
        val text = "@[Рагнар|n:1] и mail@site.ru"
        assertEquals(text, markUnlinkedMentions(text))
    }

    @Test
    fun `a lone at sign stays`() {
        assertEquals("цена @ рынке", markUnlinkedMentions("цена @ рынке"))
    }

    @Test
    fun `the same name always gets the same ref`() {
        assertEquals(unlinkedMentionRef("Ёжик"), unlinkedMentionRef(" ёжик "))
        assertTrue(unlinkedMentionRef("Ёжик").isUnlinked())
        assertFalse(MentionRef(EntityKind.NPC, 12).isUnlinked())
    }

    @Test
    fun `an unlinked token resolves to a deleted stand in`() {
        val token = mentionToken(unlinkedMentionRef("Xyz"), "Xyz")
        val segment = parseMentions(token, MentionIndex.EMPTY).single() as MentionSegment.Mention
        assertTrue(segment.entity.ref.isUnlinked())
        assertEquals("Xyz", segment.entity.name)
    }

    @Test
    fun `an unlinked tag turns back into typed text for editing`() {
        val marked = markUnlinkedMentions("видели @Xyz. и @[Рагнар|n:1]")
        assertEquals("видели @Xyz. и @[Рагнар|n:1]", unmarkUnlinkedMentions(marked))
    }

    @Test
    fun `text typed next to an unmarked tag joins the name`() {
        val edited = unmarkUnlinkedMentions(markUnlinkedMentions("пришёл @Xyz")) + "ab"
        assertEquals("пришёл ${mentionToken(unlinkedMentionRef("Xyzab"), "Xyzab")}", markUnlinkedMentions(edited))
    }
}
