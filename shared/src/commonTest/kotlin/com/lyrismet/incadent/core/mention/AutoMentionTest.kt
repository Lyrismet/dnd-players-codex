package com.lyrismet.incadent.core.mention

import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PREFIX = "Квест: "

private val gromm = MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val grommFull = MentionEntity.NpcMention(2, "Громм Железный", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val artyom = MentionEntity.NpcMention(3, "Артём", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
private val temple = MentionEntity.LocationMention(4, "Храм Рассвета")
private val ox = MentionEntity.NpcMention(5, "Ох", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
private val blade = MentionEntity.QuestMention(6, "Клинок", QuestStatus.ACTIVE)

private val candidates = mentionCandidates(listOf(gromm, grommFull, artyom, temple, ox, blade), PREFIX)

private fun autoMatches(text: String) = autoMatchedEntities(parseMentions(text, candidates, autoMatch = true))

class AutoMentionTest {
    @Test
    fun `a name written as a whole word is found without the at sign`() {
        assertEquals(listOf<MentionEntity>(gromm), autoMatches("Вчера Громм пришёл в таверну"))
    }

    @Test
    fun `case does not matter and yo equals ye`() {
        assertEquals(listOf<MentionEntity>(gromm), autoMatches("громм молчал"))
        assertEquals(listOf<MentionEntity>(artyom), autoMatches("пришёл артем"))
        assertEquals(listOf<MentionEntity>(artyom), autoMatches("пришёл АРТЁМ"))
    }

    @Test
    fun `a name of several words is found as a whole and beats the shorter one`() {
        assertEquals(listOf<MentionEntity>(temple), autoMatches("вошли в храм рассвета"))
        assertEquals(listOf<MentionEntity>(grommFull), autoMatches("Громм Железный кивнул"))
    }

    @Test
    fun `a name inside a longer word or in another case form is not found`() {
        assertTrue(autoMatches("к Громму подошли").isEmpty())
        assertTrue(autoMatches("Громмовой молот").isEmpty())
    }

    @Test
    fun `a name glued to a digit is not found`() {
        assertTrue(autoMatches("Громм2 и 7Артём").isEmpty())
    }

    @Test
    fun `a name shorter than three letters is never suggested`() {
        assertTrue(autoMatches("Ох и тяжко").isEmpty())
    }

    @Test
    fun `a name already marked with at is skipped and a plain one next to it is kept`() {
        val segments = parseMentions("@Громм и Артём", candidates, autoMatch = true)

        assertEquals(
            listOf(
                MentionSegment.Mention(gromm),
                MentionSegment.Text(" и "),
                MentionSegment.AutoMatch("Артём", artyom),
            ),
            segments,
        )
    }

    @Test
    fun `a deleted record is never an auto match`() {
        val withDeleted =
            mentionCandidates(listOf(gromm), PREFIX, listOf(DeletedMention("Кассиан", DeletedMentionType.NPC)))

        assertTrue(autoMatchedEntities(parseMentions("Кассиан ушёл", withDeleted, autoMatch = true)).isEmpty())
    }

    @Test
    fun `linking puts the at sign in front of the record's own spelling`() {
        val linked = linkAutoMentions("громм и артем ушли", candidates, PREFIX)

        assertEquals("@Громм и @Артём ушли", linked)
    }

    @Test
    fun `linking one entity leaves the other matches alone`() {
        val linked = linkAutoMentions("Громм и Артём", candidates, PREFIX, only = artyom)

        assertEquals("Громм и @Артём", linked)
    }

    @Test
    fun `a quest is linked with its prefix`() {
        assertEquals("взяли @Квест: Клинок", linkAutoMentions("взяли Клинок", candidates, PREFIX))
    }

    @Test
    fun `linking keeps existing mentions and text byte for byte`() {
        assertEquals("@Громм\nи @Артём!", linkAutoMentions("@Громм\nи артём!", candidates, PREFIX))
    }
}

class DeletedMentionTest {
    private val deleted = DeletedMention("Кассиан", DeletedMentionType.NPC)
    private val deletedQuest = DeletedMention("Проклятие", DeletedMentionType.QUEST)

    @Test
    fun `a deleted record becomes a candidate under its key and quests keep the prefix`() {
        val all = mentionCandidates(emptyList(), PREFIX, listOf(deleted, deletedQuest))

        assertEquals(setOf("Кассиан", "Квест: Проклятие"), all.map { it.key }.toSet())
    }

    @Test
    fun `a live record with the same name wins over the deleted one`() {
        val recreated = MentionEntity.NpcMention(9, "Кассиан", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
        val all = mentionCandidates(listOf(recreated), PREFIX, listOf(deleted))

        assertEquals(listOf<MentionEntity>(recreated), all.map { it.entity })
    }

    @Test
    fun `a mention of a deleted record parses into a deleted entity`() {
        val all = mentionCandidates(emptyList(), PREFIX, listOf(deleted))
        val segments = parseMentions("видели @Кассиан у моста", all)

        assertEquals(
            listOf(
                MentionSegment.Text("видели "),
                MentionSegment.Mention(MentionEntity.DeletedMention("Кассиан", DeletedMentionType.NPC)),
                MentionSegment.Text(" у моста"),
            ),
            segments,
        )
    }

    @Test
    fun `hits count notes and distinct sessions of one record`() {
        val bodies =
            listOf(
                MentionBody(1, "@Громм был тут"),
                MentionBody(1, "и @Громм снова"),
                MentionBody(2, "@Громм Железный ушёл"),
                MentionBody(3, "ничего"),
            )

        // "@Громм Железный" is the longer record's mention, so it is not a hit of the bare name
        assertEquals(MentionHits(notes = 2, sessions = 1), countMentionHits(gromm, bodies, candidates))
        assertEquals(MentionHits(notes = 1, sessions = 1), countMentionHits(grommFull, bodies, candidates))
    }

    @Test
    fun `unlinking turns the tag into plain words and leaves a longer name alone`() {
        val body = "@Громм Железный и @Громм"

        val plain = unlinkMentionIn(body, "Громм", "Громм", otherKeys = listOf("Громм Железный"))

        assertEquals("@Громм Железный и Громм", plain)
    }

    @Test
    fun `unlinking a quest drops the prefix`() {
        assertEquals(
            "взяли Проклятие",
            unlinkMentionIn("взяли @Квест: Проклятие", "Квест: Проклятие", "Проклятие", emptyList()),
        )
    }
}

class PartyMentionTest {
    private val bran = MentionEntity.PartyMention(7, "Бран")
    private val withParty = mentionCandidates(listOf(bran), PREFIX)

    @Test
    fun `a party member is mentioned by name like any other record`() {
        val segments = parseMentions("@Бран кивнул", withParty)

        assertEquals(listOf(MentionSegment.Mention(bran), MentionSegment.Text(" кивнул")), segments)
    }

    @Test
    fun `a party member written without at is an auto match and links to its own spelling`() {
        assertEquals(listOf<MentionEntity>(bran), autoMatchedEntities(parseMentions("бран ушёл", withParty, true)))
        assertEquals("@Бран ушёл", linkAutoMentions("бран ушёл", withParty, PREFIX))
    }

    @Test
    fun `entities from the codex lists include the party`() {
        val member =
            com.lyrismet.incadent.domain.model.PartyMember(
                id = 7,
                name = "Бран",
                characterClass = "",
                race = "",
                level = null,
                playerName = "",
                isPlayerCharacter = false,
                presence = com.lyrismet.incadent.domain.model.PartyPresence.IN,
                hpMax = null,
                hpCurrent = null,
                description = "",
                portraitBase64 = null,
            )

        assertEquals(
            listOf<MentionEntity>(bran),
            mentionEntitiesFrom(emptyList(), emptyList(), emptyList(), listOf(member)),
        )
    }

    @Test
    fun `a deleted party member keeps its type for the create action`() {
        val gone = mentionCandidates(emptyList(), PREFIX, listOf(DeletedMention("Бран", DeletedMentionType.PARTY)))

        assertEquals(
            listOf(MentionSegment.Mention(MentionEntity.DeletedMention("Бран", DeletedMentionType.PARTY))),
            parseMentions("@Бран", gone),
        )
    }
}
