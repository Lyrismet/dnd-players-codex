package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef

internal class SessionRow(
    val id: Long,
    val title: String,
    val date: String,
)

internal class PlaceRow(
    val id: Long,
    val name: String,
    val parentId: Long?,
)

internal class NpcRow(
    val id: Long,
    val name: String,
    val speciesId: Long?,
    val placeIds: List<Long>,
)

internal class QuestRow(
    val id: Long,
    val title: String,
    val giverNpcId: Long?,
    val placeId: Long?,
)

internal class ItemRow(
    val id: Long,
    val name: String,
    val quantity: Int,
    val owner: ExchangeRef?,
)

/** what the picker lists and how the records link, read from the database for an export or from a file for an import */
@Suppress("LongParameterList")
internal class ExchangeRows(
    val sessions: List<SessionRow>,
    val places: List<PlaceRow>,
    val npcs: List<NpcRow>,
    val quests: List<QuestRow>,
    val species: List<ExchangeItem>,
    val party: List<ExchangeItem>,
    val items: List<ItemRow>,
    val mentions: Map<Long, Set<ExchangeRef>>,
) {
    fun options(): ExchangeOptions {
        val placesById = places.associateBy { it.id }

        fun path(parentId: Long?): String =
            generateSequence(parentId?.let(placesById::get)) { it.parentId?.let(placesById::get) }
                .toList()
                .reversed()
                .joinToString(" › ") { it.name }
        return ExchangeOptions(
            sessions = sessions.map { ExchangeItem(it.id, it.title, it.date) },
            places = places.map { ExchangeItem(it.id, it.name, path(it.parentId)) },
            npcs =
                npcs.map { npc ->
                    ExchangeItem(
                        npc.id,
                        npc.name,
                        npc.placeIds.mapNotNull(placesById::get).joinToString(", ") { it.name },
                    )
                },
            quests = quests.map { ExchangeItem(it.id, it.title) },
            species = species,
            party = party,
            items = items.map { ExchangeItem(it.id, it.name, itemSubtitle(it, placesById)) },
        )
    }

    // "×3 · Lira" - the amount and who carries it, which is what tells two stacks of the same name apart
    private fun itemSubtitle(
        item: ItemRow,
        placesById: Map<Long, PlaceRow>,
    ): String {
        val owner =
            item.owner?.let { ref ->
                when (ref.kind) {
                    ExchangeKind.NPC -> npcs.firstOrNull { it.id == ref.id }?.name
                    ExchangeKind.PLACE -> placesById[ref.id]?.name
                    ExchangeKind.PARTY -> party.firstOrNull { it.id == ref.id }?.name
                    else -> null
                }
            }
        return listOfNotNull("×${item.quantity}", owner).joinToString(" · ")
    }

    fun graph() =
        ExchangeGraph(
            placeParent = places.associate { it.id to it.parentId },
            npcSpecies = npcs.associate { it.id to it.speciesId },
            npcPlaces = npcs.associate { it.id to it.placeIds },
            questGiver = quests.associate { it.id to it.giverNpcId },
            questPlace = quests.associate { it.id to it.placeId },
            itemOwner = items.associate { it.id to it.owner },
            sessionMentions = sessions.associate { it.id to mentions[it.id].orEmpty() },
        )
}
