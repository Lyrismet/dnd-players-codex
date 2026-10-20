package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor

// the grouping's subtitle wins over the default race line when it is set
internal fun Npc.toCodexItem(
    statusLabels: Map<NpcStatus, String>,
    lifeLabels: Map<NpcLifeState, String>,
    subtitle: String? = null,
): NpcCodexItem {
    val npc = this
    val isDead = npc.lifeState == NpcLifeState.DEAD
    return NpcCodexItem(
        id = npc.id,
        name = npc.name,
        initial = npc.name.take(1).uppercase(),
        portrait = npc.portrait,
        isDead = isDead,
        lifeBadge =
            if (isDead) {
                NpcLifeBadge(lifeLabels.getValue(NpcLifeState.DEAD), NpcLifeState.DEAD.toStatusColor())
            } else {
                null
            },
        statusLabel = statusLabels.getValue(npc.status),
        statusColor = npc.status.toStatusColor(),
        subtitle = subtitle ?: npc.race,
    )
}

internal fun List<PartyMember>.toPartyCodexItems(labels: PartyCardLabels): List<PartyCodexItem> =
    map { member ->
        PartyCodexItem(
            id = member.id,
            name = member.name,
            initial = member.name.take(1).uppercase(),
            portrait = member.portrait,
            color = PartyMemberColor,
            presenceLabel = labels.presence.getValue(member.presence),
            presenceColor = member.presence.toStatusColor(),
            subtitle = member.ownerLine(labels),
            meta = "${labels.hpShort} ${member.hpLine()}",
        )
    }

// an unset max shows a dash alone, an unset current falls back to the max
private fun PartyMember.hpLine(): String =
    when {
        hpMax == null -> EMPTY_VALUE_DASH
        else -> "${hpCurrent ?: hpMax}/$hpMax"
    }

private fun PartyMember.ownerLine(labels: PartyCardLabels): String =
    when {
        isPlayerCharacter -> labels.you
        playerName.isBlank() -> ""
        else -> "${labels.playerPrefix} $playerName"
    }

internal fun Quest.toCodexItem(
    npcsById: Map<Long, Npc>,
    statusLabels: Map<QuestStatus, String>,
): QuestCodexItem {
    val giverNpc = npcsById[givenByNpcId]
    return QuestCodexItem(
        id = id,
        title = title,
        statusLabel = statusLabels.getValue(status),
        statusColor = status.toStatusColor(),
        giver =
            giverNpc?.let {
                QuestGiverItem(
                    npcId = it.id,
                    name = it.name,
                    isDead = it.lifeState == NpcLifeState.DEAD,
                    color = it.status.toStatusColor(),
                )
            },
        reward = reward,
    )
}

internal fun Location.toCodexItem(subtitle: String? = null): LocationCodexItem =
    LocationCodexItem(id = id, name = name, subtitle = subtitle, cover = cover)
