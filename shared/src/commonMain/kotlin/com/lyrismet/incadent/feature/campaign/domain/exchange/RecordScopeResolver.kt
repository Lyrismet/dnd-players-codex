package com.lyrismet.incadent.feature.campaign.domain.exchange

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection

/**
 * the ticked records plus what they need: whoever carries an item, parents, species, givers and whatever ticked
 * sessions mention
 */
fun RecordSelection.resolve(graph: ExchangeGraph): RecordScope {
    val scope = ScopeBuilder(this, graph)
    scope.addMentioned()
    // an item brings whoever carries it, then a quest its giver and place, an npc its species and places, a place its
    // parents, in this order
    scope.addCarriers()
    scope.addQuestLinks()
    scope.addNpcLinks()
    scope.addPlaceParents()
    return scope.build()
}

private class ScopeBuilder(
    selection: RecordSelection,
    private val graph: ExchangeGraph,
) {
    private val mode = selection.mode
    private val sessions = selection.sessions.toMutableSet()
    private val places = selection.places.toMutableSet()
    private val npcs = selection.npcs.toMutableSet()
    private val quests = selection.quests.toMutableSet()
    private val species = selection.species.toMutableSet()
    private val party = selection.party.toMutableSet()
    private val items = selection.items.filterTo(mutableSetOf(), ::carriedHere)

    // a friend copy never carries what a party member carries, that goes only in the personal copy
    private fun carriedHere(item: Long): Boolean =
        mode == ExportMode.SELF || graph.itemOwner[item]?.kind != ExchangeKind.PARTY

    fun addMentioned() {
        sessions.flatMap { graph.sessionMentions[it].orEmpty() }.forEach(::add)
    }

    private fun add(ref: ExchangeRef) {
        when (ref.kind) {
            ExchangeKind.PLACE -> places += ref.id
            ExchangeKind.NPC -> npcs += ref.id
            ExchangeKind.QUEST -> quests += ref.id
            ExchangeKind.SPECIES -> species += ref.id
            ExchangeKind.PARTY -> party += ref.id
            ExchangeKind.ITEM -> if (carriedHere(ref.id)) items += ref.id
            ExchangeKind.SESSION -> Unit
        }
    }

    fun addCarriers() {
        items.forEach { id -> graph.itemOwner[id]?.takeIf { it.kind != ExchangeKind.ITEM }?.let(::add) }
    }

    fun addQuestLinks() {
        quests.toList().forEach { id ->
            graph.questGiver[id]?.let(npcs::add)
            graph.questPlace[id]?.let(places::add)
        }
    }

    fun addNpcLinks() {
        npcs.toList().forEach { id ->
            graph.npcSpecies[id]?.let(species::add)
            places += graph.npcPlaces[id].orEmpty()
        }
    }

    fun addPlaceParents() {
        places.toList().forEach { id ->
            var parent = graph.placeParent[id]
            while (parent != null && places.add(parent)) parent = graph.placeParent[parent]
        }
    }

    fun build() = RecordScope(sessions, places, npcs, quests, species, party, items)
}
