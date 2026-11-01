package com.lyrismet.incadent.feature.codex.domain.link

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionsIn

/** the field of a record that a mention in its notes can fill */
enum class LinkField {
    // a place mentioned by an npc is where that npc can be met
    MEET_AT,

    // an npc mentioned by a place is someone who is there
    WHO_HERE,

    // an npc mentioned by a quest is who gave it
    GIVER,
}

/** an offer to fill [field] of [owner] (or of [target] for [LinkField.WHO_HERE]) with the record [target] */
data class LinkSuggestion(
    val owner: MentionRef,
    val target: MentionEntity,
    val field: LinkField,
)

/** what the notes of [owner] mention that is not linked yet, without the [linked] and the [dismissed] ones */
fun suggestLinks(
    owner: MentionRef,
    mentioned: List<MentionEntity>,
    linked: Set<MentionRef>,
    dismissed: Set<MentionRef>,
    giverTaken: Boolean = false,
): List<LinkSuggestion> {
    val field = owner.type.linkFieldFor() ?: return emptyList()
    val candidates =
        mentioned
            .filter { it.ref != owner && it.ref.type == field.targetKind() }
            .filter { it.ref !in linked && it.ref !in dismissed }
    return when {
        field == LinkField.GIVER && giverTaken -> emptyList()
        field == LinkField.GIVER -> candidates.take(1)
        else -> candidates
    }.map { LinkSuggestion(owner, it, field) }
}

private fun EntityKind.linkFieldFor(): LinkField? =
    when (this) {
        EntityKind.NPC -> LinkField.MEET_AT
        EntityKind.LOCATION -> LinkField.WHO_HERE
        EntityKind.QUEST -> LinkField.GIVER
        EntityKind.PARTY, EntityKind.SPECIES -> null
    }

private fun LinkField.targetKind(): EntityKind =
    when (this) {
        LinkField.MEET_AT -> EntityKind.LOCATION
        LinkField.WHO_HERE, LinkField.GIVER -> EntityKind.NPC
    }

/** the offers for the notes of the record [owner] points at, read against what the codex already links */
fun CodexRecords.linkSuggestionsFor(
    owner: MentionRef,
    dismissed: Set<MentionRef>,
): List<LinkSuggestion> {
    val notes =
        when (owner.type) {
            EntityKind.NPC -> npcs.find { it.id == owner.id }?.description
            EntityKind.LOCATION -> locations.find { it.id == owner.id }?.description
            EntityKind.QUEST -> quests.find { it.id == owner.id }?.description
            EntityKind.PARTY, EntityKind.SPECIES -> null
        } ?: return emptyList()
    val mentioned = mentionsIn(listOf(notes), mentionIndex).filter { mentionIndex.has(it.ref) }
    val linked =
        when (owner.type) {
            EntityKind.NPC ->
                npcs
                    .find { it.id == owner.id }
                    ?.locationIds
                    .orEmpty()
                    .map { MentionRef(EntityKind.LOCATION, it) }
            EntityKind.LOCATION ->
                npcs.filter { owner.id in it.locationIds }.map { MentionRef(EntityKind.NPC, it.id) }
            else -> emptyList()
        }.toSet()
    val giverTaken = quests.find { it.id == owner.id }?.givenByNpcId != null
    return suggestLinks(owner, mentioned, linked, dismissed, giverTaken)
}
