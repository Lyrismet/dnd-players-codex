package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.designsystem.PartyMemberColor
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

// the grouping's subtitle wins over the default race and faction line when it is set
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
        portraitBase64 = npc.portraitBase64,
        isDead = isDead,
        lifeBadge =
            if (isDead) {
                NpcLifeBadge(lifeLabels.getValue(NpcLifeState.DEAD), NpcLifeState.DEAD.toStatusColor())
            } else {
                null
            },
        statusLabel = statusLabels.getValue(npc.status),
        statusColor = npc.status.toStatusColor(),
        subtitle = subtitle ?: joinWithDot(npc.race, npc.faction),
    )
}

internal fun List<PartyMember>.toPartyCodexItems(labels: PartyCardLabels): List<PartyCodexItem> =
    map { member ->
        PartyCodexItem(
            id = member.id,
            name = member.name,
            initial = member.name.take(1).uppercase(),
            portraitBase64 = member.portraitBase64,
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

// the grouping's subtitle wins over the default type and region line when it is set
internal fun Location.toCodexItem(subtitle: String? = null): LocationCodexItem =
    LocationCodexItem(
        id = id,
        name = name,
        subtitle = subtitle ?: joinWithDot(type, region),
    )
