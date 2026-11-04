package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ItemMentionTest {
    private val potion = Item(id = 7, name = "Зелье лечения")
    private val index = MentionIndex(mentionEntitiesFrom(emptyList(), emptyList(), emptyList(), items = listOf(potion)))

    @Test
    fun `an item token uses the letter i and is read back`() {
        val ref = MentionRef(EntityKind.ITEM, 7)
        val token = mentionToken(ref, potion.name)
        assertEquals("@[Зелье_лечения|i:7]", token)
        val mention = parseMentions("взяли $token", index).filterIsInstance<MentionSegment.Mention>().single()
        assertIs<MentionEntity.ItemMention>(mention.entity)
        assertEquals(1, mention.quantity)
    }

    @Test
    fun `an item is never found in plain text`() {
        val segments = parseMentions("взяли Зелье лечения", index, autoMatch = true)
        assertTrue(segments.none { it is MentionSegment.AutoMatch })
    }

    @Test
    fun `the old letters stay valid`() {
        val kinds =
            mapOf(
                'n' to EntityKind.NPC,
                'p' to EntityKind.PARTY,
                'l' to EntityKind.LOCATION,
                'q' to EntityKind.QUEST,
                's' to EntityKind.SPECIES,
            )
        kinds.forEach { (letter, kind) ->
            val ref = TOKEN_REGEX.find("@[Имя|$letter:3]")?.toRef()
            assertEquals(MentionRef(kind, 3), ref)
        }
    }

    @Test
    fun `a deleted item reads as a question mark tag`() {
        val gone = parseMentions("@[Зелье|i:99]", index).filterIsInstance<MentionSegment.Mention>().single()
        assertIs<MentionEntity.DeletedMention>(gone.entity)
        assertEquals(EntityKind.ITEM, gone.entity.ref.type)
    }

    @Test
    fun `an item sharing a name does not make a character ambiguous`() {
        val npc = Npc(1, "Лампа", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")
        val lamp = Item(2, "Лампа", owner = ItemOwner.Shared)
        val mixed = MentionIndex(mentionEntitiesFrom(listOf(npc), emptyList(), emptyList(), items = listOf(lamp)))
        assertEquals(1, linkTypedMentions("@Лампа", mixed).count { it == '|' })
        assertTrue(linkTypedMentions("@Лампа", mixed).contains("|n:1]"))
    }
}
