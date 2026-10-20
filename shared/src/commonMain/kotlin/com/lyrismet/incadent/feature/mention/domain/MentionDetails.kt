package com.lyrismet.incadent.feature.mention.domain

import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest

private const val PATH_SEPARATOR = " › "

/** the grey second line that tells same named records apart - race and place, who gave a quest, the path of a place */
fun mentionDetails(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
    questGiverLine: (String) -> String,
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
        val path = tree.pathOf(place).dropLast(1).joinToString(PATH_SEPARATOR) { it.name }
        details[MentionRef(EntityKind.LOCATION, place.id)] = path
    }
    quests.forEach { quest ->
        val giver = quest.givenByNpcId?.let { npcById[it] }?.name
        details[MentionRef(EntityKind.QUEST, quest.id)] = giver?.let(questGiverLine).orEmpty()
    }
    return details
}

/** every record's nicknames by ref, records without any left out */
fun mentionNicknames(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
): Map<MentionRef, List<String>> =
    buildMap {
        npcs.forEach { if (it.nicknames.isNotEmpty()) put(MentionRef(EntityKind.NPC, it.id), it.nicknames) }
        parties.forEach { if (it.nicknames.isNotEmpty()) put(MentionRef(EntityKind.PARTY, it.id), it.nicknames) }
        locations.forEach { if (it.nicknames.isNotEmpty()) put(MentionRef(EntityKind.LOCATION, it.id), it.nicknames) }
        quests.forEach { if (it.nicknames.isNotEmpty()) put(MentionRef(EntityKind.QUEST, it.id), it.nicknames) }
    }
