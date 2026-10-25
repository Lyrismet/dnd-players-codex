package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.mention.domain.MentionRef

/** the file's own number for every exported record, counted from 1 per kind in database order */
internal class FileIds(
    scope: RecordScope,
    private val graph: ExchangeGraph,
) {
    private val species = scope.species.numbered()
    private val places = scope.places.numbered()
    private val npcs = scope.npcs.numbered()
    private val quests = scope.quests.numbered()
    private val party = scope.party.numbered()
    private val sessions = scope.sessions.numbered()

    fun species(id: Long) = species.getValue(id)

    fun place(id: Long) = places.getValue(id)

    fun npc(id: Long) = npcs.getValue(id)

    fun quest(id: Long) = quests.getValue(id)

    fun party(id: Long) = party.getValue(id)

    fun session(id: Long) = sessions.getValue(id)

    fun placeDepth(id: Long): Int =
        generateSequence(graph.placeParent[id]) {
            graph.placeParent[it]
        }.take(places.size).count()

    private val byKind =
        mapOf(
            EntityKind.NPC to npcs,
            EntityKind.PARTY to party,
            EntityKind.LOCATION to places,
            EntityKind.QUEST to quests,
            EntityKind.SPECIES to species,
        )

    /** the file number a note token moves to, null for a record that is not in the file */
    fun mapped(ref: MentionRef): Long? = byKind[ref.type]?.get(ref.id)
}

private fun Set<Long>.numbered(): Map<Long, Long> = sorted().withIndex().associate { (index, id) -> id to index + 1L }
