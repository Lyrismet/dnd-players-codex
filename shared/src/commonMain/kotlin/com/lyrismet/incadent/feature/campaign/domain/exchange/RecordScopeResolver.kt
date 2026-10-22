package com.lyrismet.incadent.feature.campaign.domain.exchange

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection

/** the ticked records plus what they need: parents, species, givers and whatever ticked sessions mention */
fun RecordSelection.resolve(graph: ExchangeGraph): RecordScope {
    val sessions = sessions.toMutableSet()
    val places = places.toMutableSet()
    val npcs = npcs.toMutableSet()
    val quests = quests.toMutableSet()
    val species = species.toMutableSet()
    val party = party.toMutableSet()
    val mentioned =
        sessions.flatMap { graph.sessionMentions[it].orEmpty() }
    mentioned.forEach { ref ->
        when (ref.kind) {
            ExchangeKind.PLACE -> places += ref.id
            ExchangeKind.NPC -> npcs += ref.id
            ExchangeKind.QUEST -> quests += ref.id
            ExchangeKind.SPECIES -> species += ref.id
            ExchangeKind.PARTY -> party += ref.id
            ExchangeKind.SESSION -> Unit
        }
    }
    // a quest brings its giver and place, an npc its species and places, a place its parents, in this order
    quests.toList().forEach { id ->
        graph.questGiver[id]?.let(npcs::add)
        graph.questPlace[id]?.let(places::add)
    }
    npcs.toList().forEach { id ->
        graph.npcSpecies[id]?.let(species::add)
        places += graph.npcPlaces[id].orEmpty()
    }
    places.toList().forEach { id ->
        var parent = graph.placeParent[id]
        while (parent != null && places.add(parent)) parent = graph.placeParent[parent]
    }
    return RecordScope(sessions, places, npcs, quests, species, party)
}
