package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
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

internal fun List<Npc>.toCodexItems(statusLabels: Map<NpcStatus, String>): List<NpcCodexItem> =
    map { npc ->
        NpcCodexItem(
            id = npc.id,
            name = npc.name,
            initial = npc.name.take(1).uppercase(),
            isDead = npc.status == NpcStatus.DEAD,
            statusLabel = statusLabels.getValue(npc.status),
            statusColor = npc.status.toStatusColor(),
            subtitle = joinWithDot(npc.race, npc.faction),
        )
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
                        isDead = it.status == NpcStatus.DEAD,
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
