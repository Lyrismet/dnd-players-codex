package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest

/** the grey second line that tells same named records apart - race and place, who gave a quest, the path of a place */
@Suppress("LongParameterList")
fun mentionDetails(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
    questGiverLine: (String) -> String,
    items: List<Item> = emptyList(),
    meLabel: String = "",
): Map<MentionRef, String> {
    val tree = PlaceTree(locations)
    val npcById = npcs.associateBy { it.id }
    val details = mutableMapOf<MentionRef, String>()
    npcs.forEach { npc ->
        val place =
            npc.locationIds
                .firstNotNullOfOrNull { tree.find(it) }
                ?.name
                .orEmpty()
        details[MentionRef(EntityKind.NPC, npc.id)] = joinWithDot(npc.race, place)
    }
    parties.forEach {
        details[MentionRef(EntityKind.PARTY, it.id)] = joinWithDot(it.characterClass, it.race)
    }
    locations.forEach { place ->
        details[MentionRef(EntityKind.LOCATION, place.id)] = tree.ancestorsLabel(place)
    }
    quests.forEach { quest ->
        val giver = quest.givenByNpcId?.let { npcById[it] }?.name
        details[MentionRef(EntityKind.QUEST, quest.id)] = giver?.let(questGiverLine).orEmpty()
    }
    details += itemDetails(items, parties, npcById, tree, meLabel)
    return details
}

/** the nicknames of the npcs by ref, npcs without any left out */
fun mentionNicknames(npcs: List<Npc>): Map<MentionRef, List<String>> =
    buildMap {
        npcs.forEach { if (it.nicknames.isNotEmpty()) put(MentionRef(EntityKind.NPC, it.id), it.nicknames) }
    }

// who carries each item and how many there are, "Лира · ×2"
private fun itemDetails(
    items: List<Item>,
    parties: List<PartyMember>,
    npcById: Map<Long, Npc>,
    tree: PlaceTree,
    meLabel: String,
): Map<MentionRef, String> =
    items.associate { item ->
        val holder = item.owner as? ItemOwner.Of
        val owner =
            when (holder?.kind) {
                EntityKind.PARTY ->
                    parties.find { it.id == holder.id }?.let { if (it.isPlayerCharacter) meLabel else it.name }
                EntityKind.NPC -> npcById[holder.id]?.name
                EntityKind.LOCATION -> tree.find(holder.id)?.name
                else -> null
            }.orEmpty()
        MentionRef(EntityKind.ITEM, item.id) to joinWithDot(owner, "×${item.quantity}")
    }
