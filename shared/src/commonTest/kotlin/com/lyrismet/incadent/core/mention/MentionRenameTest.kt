package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

private val gromm = MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val curse = MentionEntity.QuestMention(2, "Проклятие", QuestStatus.ACTIVE)
private val goneRef = MentionRef(DeletedMentionType.NPC, 7)

class MentionRewriteTest {
    @Test
    fun `unlinking turns the tags of one record into its plain name and leaves the others`() {
        val gone = mentionToken(goneRef, "Кассиан")
        val body = "$gone встретил ${gromm.token()}, потом снова $gone"

        val plain = unlinkMentionIn(body, goneRef)

        assertEquals("Кассиан встретил ${gromm.token()}, потом снова Кассиан", plain)
    }

    @Test
    fun `relinking points the tags of a deleted record at another record`() {
        val gone = mentionToken(goneRef, "Кассиан")

        val linked = relinkMentionIn("видели $gone у моста", goneRef, gromm)

        assertEquals("видели ${gromm.token()} у моста", linked)
    }

    @Test
    fun `a record with the same id but another kind is not touched`() {
        val questToken = curse.token()

        assertEquals(questToken, unlinkMentionIn(questToken, MentionRef(DeletedMentionType.NPC, curse.id)))
    }
}

class DeletedMentionTest {
    private val index = MentionIndex(listOf(gromm))

    @Test
    fun `a record created again under its old id takes its tags back`() {
        val tag = mentionToken(MentionRef(DeletedMentionType.NPC, 5), "Кассиан")
        val revived = MentionEntity.NpcMention(5, "Кассиан", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)

        assertEquals(
            listOf<MentionSegment>(
                MentionSegment.Mention(MentionEntity.DeletedMention(5, "Кассиан", DeletedMentionType.NPC)),
            ),
            parseMentions(tag, index),
        )
        assertEquals(
            listOf<MentionSegment>(MentionSegment.Mention(revived)),
            parseMentions(tag, MentionIndex(listOf(gromm, revived))),
        )
    }

    @Test
    fun `hits count notes and distinct sessions of one record`() {
        val bodies =
            listOf(
                MentionBody(1, "${gromm.token()} был тут"),
                MentionBody(1, "и ${gromm.token()} снова"),
                MentionBody(2, "${curse.token()} снят"),
                MentionBody(3, "ничего"),
            )
        val both = MentionIndex(listOf(gromm, curse))

        assertEquals(MentionHits(notes = 2, sessions = 1), countMentionHits(gromm, bodies, both))
        assertEquals(MentionHits(notes = 1, sessions = 1), countMentionHits(curse, bodies, both))
    }
}
