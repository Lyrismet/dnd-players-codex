package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val gromm = MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val grommFull = MentionEntity.NpcMention(2, "Громм Железный", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val artyom = MentionEntity.NpcMention(3, "Артём", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
private val temple = MentionEntity.LocationMention(4, "Храм Рассвета")
private val ox = MentionEntity.NpcMention(5, "Ох", NpcStatus.NEUTRAL, NpcLifeState.ALIVE)
private val blade = MentionEntity.QuestMention(6, "Клинок", QuestStatus.ACTIVE)

private val index = MentionIndex(listOf(gromm, grommFull, artyom, temple, ox, blade))

private fun autoMatches(text: String) = autoMatchedEntities(parseMentions(text, index, autoMatch = true))

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
    fun `a name two records share is never an auto match`() {
        val twin = MentionEntity.LocationMention(9, "Артём")

        val shared = MentionIndex(listOf(artyom, twin))

        assertTrue(autoMatchedEntities(parseMentions("пришёл артём", shared, autoMatch = true)).isEmpty())
    }

    @Test
    fun `a token is kept and a plain name next to it is found`() {
        val segments = parseMentions("${gromm.token()} и Артём", index, autoMatch = true)

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
        val gone = mentionToken(MentionRef(com.lyrismet.incadent.domain.model.EntityKind.NPC, 50), "Кассиан")

        assertTrue(autoMatches("видели $gone и Кассиан").isEmpty())
    }

    @Test
    fun `linking turns every auto matched word into the token of its record`() {
        val linked = linkAutoMentions("громм и артем ушли", index)

        assertEquals("${gromm.token()} и ${artyom.token()} ушли", linked)
    }

    @Test
    fun `linking one record leaves the other words alone`() {
        val linked = linkAutoMentions("Громм и Артём", index, only = artyom)

        assertEquals("Громм и ${artyom.token()}", linked)
    }

    @Test
    fun `a quest is linked by its id`() {
        assertEquals("взяли ${blade.token()}", linkAutoMentions("взяли Клинок", index))
    }

    @Test
    fun `linking keeps existing tokens and text`() {
        val body = "${gromm.token()}\nи артём!"

        assertEquals("${gromm.token()}\nи ${artyom.token()}!", linkAutoMentions(body, index))
    }
}

class PartyMentionTest {
    private val bran = MentionEntity.PartyMention(7, "Бран")
    private val withParty = MentionIndex(listOf(bran))

    @Test
    fun `a party member is mentioned by token like any other record`() {
        val segments = parseMentions("${bran.token()} кивнул", withParty)

        assertEquals(listOf(MentionSegment.Mention(bran), MentionSegment.Text(" кивнул")), segments)
    }

    @Test
    fun `a party member written without at is an auto match and links to its token`() {
        assertEquals(listOf<MentionEntity>(bran), autoMatchedEntities(parseMentions("бран ушёл", withParty, true)))
        assertEquals("${bran.token()} ушёл", linkAutoMentions("бран ушёл", withParty))
    }

    @Test
    fun `entities from the codex lists include the party`() {
        val member =
            com.lyrismet.incadent.feature.codex.domain.model.PartyMember(
                id = 7,
                name = "Бран",
                characterClass = "",
                race = "",
                level = null,
                playerName = "",
                isPlayerCharacter = false,
                presence = com.lyrismet.incadent.feature.codex.domain.model.PartyPresence.IN,
                hpMax = null,
                hpCurrent = null,
                description = "",
                portrait = null,
            )

        assertEquals(
            listOf<MentionEntity>(bran),
            mentionEntitiesFrom(emptyList(), emptyList(), emptyList(), listOf(member)),
        )
    }
}
