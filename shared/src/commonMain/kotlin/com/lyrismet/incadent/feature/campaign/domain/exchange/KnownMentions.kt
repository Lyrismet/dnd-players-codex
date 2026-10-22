package com.lyrismet.incadent.feature.campaign.domain.exchange

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef

/** drops the mentions of records that are not on offer, a note can still hold the tag of a deleted record */
fun ExchangeGraph.withKnownMentionsOnly(options: ExchangeOptions): ExchangeGraph {
    val known =
        buildSet {
            options.places.forEach { add(ExchangeRef(ExchangeKind.PLACE, it.id)) }
            options.npcs.forEach { add(ExchangeRef(ExchangeKind.NPC, it.id)) }
            options.quests.forEach { add(ExchangeRef(ExchangeKind.QUEST, it.id)) }
            options.species.forEach { add(ExchangeRef(ExchangeKind.SPECIES, it.id)) }
            options.party.forEach { add(ExchangeRef(ExchangeKind.PARTY, it.id)) }
        }
    return copy(
        sessionMentions =
            sessionMentions.mapValues { (_, refs) ->
                refs.filterTo(mutableSetOf()) { it in known }
            },
    )
}
