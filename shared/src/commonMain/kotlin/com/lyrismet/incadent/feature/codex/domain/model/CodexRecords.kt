package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry

/** every record the codex lists plus the note texts and mention candidates a delete is checked against */
@Suppress("LongParameterList")
class CodexRecords(
    val parties: List<PartyMember>,
    val npcs: List<Npc>,
    val quests: List<Quest>,
    val locations: List<Location>,
    val entries: List<SessionEntry> = emptyList(),
    val mentionIndex: MentionIndex = MentionIndex.EMPTY,
    val species: List<Species> = emptyList(),
    // what the saved fights say of the species and the party, for the rows and the cards
    val facts: CombatFacts = CombatFacts.EMPTY,
    items: List<Item> = emptyList(),
) {
    // an item whose owner is gone reads as shared everywhere, the stored owner stays so an undone delete re-links it
    val items: List<Item> = items.map { it.copy(owner = liveOwner(it.owner)) }

    private fun liveOwner(owner: ItemOwner): ItemOwner =
        when {
            owner !is ItemOwner.Of -> owner
            when (owner.kind) {
                EntityKind.PARTY -> parties.any { it.id == owner.id }
                EntityKind.NPC -> npcs.any { it.id == owner.id }
                EntityKind.LOCATION -> locations.any { it.id == owner.id }
                else -> false
            } -> owner
            else -> ItemOwner.Shared
        }
}
