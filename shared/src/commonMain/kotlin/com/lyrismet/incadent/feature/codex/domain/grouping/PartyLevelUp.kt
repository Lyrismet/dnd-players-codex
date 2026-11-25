package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import kotlinx.coroutines.flow.first

/** the others below the player's new level, they catch up to it - fallen ones and ones without a level stay put */
fun levelFollowers(
    party: List<PartyMember>,
    ownerId: Long,
    newLevel: Int,
): List<PartyMember> = party.filter { it.id != ownerId && !it.fallen && (it.level ?: newLevel) < newLevel }

/** the party follows only when the player's own level goes up */
suspend fun PartyRepository.followersOf(
    before: PartyMember,
    after: PartyMember,
): List<PartyMember> {
    val from = before.level
    val to = after.level
    val raised = before.isPlayerCharacter && from != null && to != null && to > from
    return if (raised) levelFollowers(observeAll().first(), before.id, checkNotNull(to)) else emptyList()
}
