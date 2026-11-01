package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember

/** the others below the player's new level, they catch up to it - fallen ones and ones without a level stay put */
fun levelFollowers(
    party: List<PartyMember>,
    ownerId: Long,
    newLevel: Int,
): List<PartyMember> = party.filter { it.id != ownerId && !it.fallen && (it.level ?: newLevel) < newLevel }
