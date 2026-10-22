package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
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
}
