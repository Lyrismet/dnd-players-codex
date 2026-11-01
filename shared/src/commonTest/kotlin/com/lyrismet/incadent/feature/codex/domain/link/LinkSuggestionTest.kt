package com.lyrismet.incadent.feature.codex.domain.link

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSuggestionTest {
    private val npc = MentionRef(EntityKind.NPC, 1)
    private val place = MentionRef(EntityKind.LOCATION, 2)
    private val quest = MentionRef(EntityKind.QUEST, 3)
    private val other = MentionEntity.NpcMention(4, "Ann", NpcStatus.FRIEND, NpcLifeState.ALIVE)
    private val town = MentionEntity.LocationMention(2, "Town")
    private val bob = MentionEntity.NpcMention(1, "Bob", NpcStatus.FRIEND, NpcLifeState.ALIVE)

    @Test
    fun `a place in npc notes is offered for meet at`() {
        val result = suggestLinks(npc, listOf(town, other), emptySet(), emptySet())

        assertEquals(listOf(LinkSuggestion(npc, town, LinkField.MEET_AT)), result)
    }

    @Test
    fun `an npc in place notes is offered for who is here`() {
        val result = suggestLinks(place, listOf(other, town), emptySet(), emptySet())

        assertEquals(listOf(LinkSuggestion(place, other, LinkField.WHO_HERE)), result)
    }

    @Test
    fun `a quest offers only the first npc and only while it has no giver`() {
        val second = other.copy(id = 5, name = "Cid")

        assertEquals(
            listOf(LinkSuggestion(quest, other, LinkField.GIVER)),
            suggestLinks(quest, listOf(other, second), emptySet(), emptySet()),
        )
        assertEquals(emptyList(), suggestLinks(quest, listOf(other), emptySet(), emptySet(), giverTaken = true))
    }

    @Test
    fun `an existing link is not offered again`() {
        assertEquals(emptyList(), suggestLinks(npc, listOf(town), setOf(place), emptySet()))
    }

    @Test
    fun `a dismissed mention is not offered again`() {
        assertEquals(emptyList(), suggestLinks(npc, listOf(town), emptySet(), setOf(place)))
    }

    @Test
    fun `a record never offers itself`() {
        assertEquals(emptyList(), suggestLinks(place, listOf(town), emptySet(), emptySet()))
        assertEquals(emptyList(), suggestLinks(npc, listOf(bob), emptySet(), emptySet()))
    }

    @Test
    fun `the party and species notes offer nothing`() {
        val party = MentionRef(EntityKind.PARTY, 9)
        val species = MentionRef(EntityKind.SPECIES, 9)

        assertEquals(emptyList(), suggestLinks(party, listOf(town, other), emptySet(), emptySet()))
        assertEquals(emptyList(), suggestLinks(species, listOf(town, other), emptySet(), emptySet()))
    }

    @Test
    fun `records offer links read from their notes and what they already link`() {
        val bobNpc =
            Npc(1, "Bob", NpcStatus.FRIEND, NpcLifeState.ALIVE, "meets at @[Town|l:2] and @[Mill|l:6]", listOf(2), "")
        val town = Location(2, "Town", "")
        val mill = Location(6, "Mill", "")
        val records =
            CodexRecords(
                parties = emptyList(),
                npcs = listOf(bobNpc),
                quests = emptyList(),
                locations = listOf(town, mill),
                mentionIndex = MentionIndex(mentionEntitiesFrom(listOf(bobNpc), listOf(town, mill), emptyList())),
            )

        val result = records.linkSuggestionsFor(npc, dismissed = emptySet())

        assertEquals(listOf(6L), result.map { it.target.ref.id })
        assertEquals(
            emptyList(),
            records.linkSuggestionsFor(npc, dismissed = setOf(MentionRef(EntityKind.LOCATION, 6))),
        )
    }
}
