package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.designsystem.PartyMemberColor
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

internal fun Npc.matchesQuery(query: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true) || description.contains(query, ignoreCase = true)

internal fun Quest.matchesQuery(
    query: String,
    npcsById: Map<Long, Npc>,
): Boolean {
    if (query.isEmpty()) return true
    val giverName = npcsById[givenByNpcId]?.name.orEmpty()
    return title.contains(query, ignoreCase = true) ||
        reward.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        giverName.contains(query, ignoreCase = true)
}

internal fun Location.matchesQuery(query: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true) || type.contains(query, ignoreCase = true)

internal fun PartyMember.matchesQuery(query: String): Boolean =
    query.isEmpty() ||
        name.contains(query, ignoreCase = true) ||
        characterClass.contains(query, ignoreCase = true) ||
        playerName.contains(query, ignoreCase = true)

internal fun NpcListFilter?.matches(npc: Npc): Boolean =
    when (this) {
        null -> true
        is NpcListFilter.Relation -> npc.status == status
        NpcListFilter.Dead -> npc.lifeState == NpcLifeState.DEAD
    }

internal fun PartyPresence?.matches(member: PartyMember): Boolean = this == null || member.presence == this

internal fun List<Npc>.toNpcCodexItems(
    statusLabels: Map<NpcStatus, String>,
    lifeLabels: Map<NpcLifeState, String>,
): List<NpcCodexItem> =
    map { npc ->
        val isDead = npc.lifeState == NpcLifeState.DEAD
        NpcCodexItem(
            id = npc.id,
            name = npc.name,
            initial = npc.name.take(1).uppercase(),
            isDead = isDead,
            lifeBadge =
                if (isDead) {
                    NpcLifeBadge(lifeLabels.getValue(NpcLifeState.DEAD), NpcLifeState.DEAD.toStatusColor())
                } else {
                    null
                },
            statusLabel = statusLabels.getValue(npc.status),
            statusColor = npc.status.toStatusColor(),
            subtitle = joinWithDot(npc.race, npc.faction),
        )
    }

internal fun List<PartyMember>.toPartyCodexItems(labels: PartyCardLabels): List<PartyCodexItem> =
    map { member ->
        PartyCodexItem(
            id = member.id,
            name = member.name,
            initial = member.name.take(1).uppercase(),
            color = PartyMemberColor,
            presenceLabel = labels.presence.getValue(member.presence),
            presenceColor = member.presence.toStatusColor(),
            subtitle = member.ownerLine(labels),
            meta =
                joinWithDot(
                    "${labels.armorClassShort} ${member.armorClass}",
                    "${labels.hpShort} ${member.hpCurrent}/${member.hpMax}",
                ),
        )
    }

private fun PartyMember.ownerLine(labels: PartyCardLabels): String =
    when {
        isPlayerCharacter -> labels.you
        playerName.isBlank() -> ""
        else -> "${labels.playerPrefix} $playerName"
    }

internal fun List<Quest>.toCodexItems(
    npcsById: Map<Long, Npc>,
    statusLabels: Map<QuestStatus, String>,
): List<QuestCodexItem> =
    map { quest ->
        val giverNpc = npcsById[quest.givenByNpcId]
        QuestCodexItem(
            id = quest.id,
            title = quest.title,
            statusLabel = statusLabels.getValue(quest.status),
            statusColor = quest.status.toStatusColor(),
            giver =
                giverNpc?.let {
                    QuestGiverItem(
                        npcId = it.id,
                        name = it.name,
                        isDead = it.lifeState == NpcLifeState.DEAD,
                        color = it.status.toStatusColor(),
                    )
                },
            reward = quest.reward,
        )
    }

internal fun List<Location>.toCodexItems(): List<LocationCodexItem> =
    map { location ->
        LocationCodexItem(
            id = location.id,
            name = location.name,
            subtitle = joinWithDot(location.type, location.region),
        )
    }
