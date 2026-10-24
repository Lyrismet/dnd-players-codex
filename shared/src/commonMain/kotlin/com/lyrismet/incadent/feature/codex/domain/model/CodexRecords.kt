package com.lyrismet.incadent.feature.codex.domain.model

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
)
