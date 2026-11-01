package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember

/** true while the party has someone alive to mark as the player's own and nobody carries the mark */
fun needsPlayerCharacterPick(party: List<PartyMember>): Boolean =
    party.any { !it.fallen } && party.none { it.isPlayerCharacter }
