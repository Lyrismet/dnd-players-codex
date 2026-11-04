package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordScopeResolverTest {
    private val graph =
        ExchangeGraph(
            placeParent = mapOf(1L to null, 2L to 1L, 3L to 2L),
            npcSpecies = mapOf(10L to 100L),
            npcPlaces = mapOf(10L to listOf(3L)),
            questGiver = mapOf(20L to 10L),
            questPlace = mapOf(20L to 1L),
            sessionMentions =
                mapOf(
                    30L to setOf(ExchangeRef(ExchangeKind.QUEST, 20L), ExchangeRef(ExchangeKind.PARTY, 40L)),
                ),
        )

    @Test
    fun `an npc brings its species and its places with their parents`() {
        val scope = RecordSelection(npcs = setOf(10L)).resolve(graph)
        assertEquals(setOf(100L), scope.species)
        assertEquals(setOf(1L, 2L, 3L), scope.places)
    }

    @Test
    fun `a session brings what it mentions and what those need`() {
        val scope = RecordSelection(sessions = setOf(30L)).resolve(graph)
        assertEquals(setOf(20L), scope.quests)
        assertEquals(setOf(10L), scope.npcs)
        assertEquals(setOf(40L), scope.party)
        assertEquals(setOf(100L), scope.species)
        assertEquals(setOf(1L, 2L, 3L), scope.places)
    }

    @Test
    fun `nothing ticked brings nothing`() {
        val scope = RecordSelection().resolve(graph)
        assertEquals(
            emptySet(),
            scope.npcs + scope.places + scope.quests + scope.species + scope.party + scope.sessions,
        )
    }

    @Test
    fun `a place with a parent loop does not hang`() {
        val loop = ExchangeGraph(placeParent = mapOf(1L to 2L, 2L to 1L))
        assertEquals(setOf(1L, 2L), RecordSelection(places = setOf(1L)).resolve(loop).places)
    }

    private val carried =
        ExchangeGraph(
            itemOwner =
                mapOf(
                    50L to ExchangeRef(ExchangeKind.NPC, 10L),
                    51L to ExchangeRef(ExchangeKind.PARTY, 40L),
                    52L to null,
                ),
            npcSpecies = mapOf(10L to 100L),
            sessionMentions = mapOf(30L to setOf(ExchangeRef(ExchangeKind.ITEM, 50L))),
        )

    @Test
    fun `an item brings the npc that carries it with what that npc needs`() {
        val scope = RecordSelection(items = setOf(50L)).resolve(carried)
        assertEquals(setOf(50L), scope.items)
        assertEquals(setOf(10L), scope.npcs)
        assertEquals(setOf(100L), scope.species)
    }

    @Test
    fun `a shared item brings nobody`() {
        val scope = RecordSelection(items = setOf(52L)).resolve(carried)
        assertEquals(setOf(52L), scope.items)
        assertEquals(emptySet(), scope.npcs + scope.party)
    }

    @Test
    fun `a personal copy takes what a character carries and the character with it`() {
        val scope = RecordSelection(items = setOf(51L), mode = ExportMode.SELF).resolve(carried)
        assertEquals(setOf(51L), scope.items)
        assertEquals(setOf(40L), scope.party)
    }

    @Test
    fun `a friend copy leaves out what a character carries and does not bring the character for it`() {
        val scope = RecordSelection(items = setOf(50L, 51L), mode = ExportMode.FRIEND).resolve(carried)
        assertEquals(setOf(50L), scope.items)
        assertEquals(emptySet(), scope.party)
    }

    @Test
    fun `a session brings the item it mentions`() {
        val scope = RecordSelection(sessions = setOf(30L)).resolve(carried)
        assertEquals(setOf(50L), scope.items)
        assertEquals(setOf(10L), scope.npcs)
    }
}
