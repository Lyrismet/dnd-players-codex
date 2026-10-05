package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.QuestStatus

internal fun partyFilterOptions(
    searched: List<PartyMember>,
    labels: Map<PartyPresence, String>,
    allLabel: String,
): List<CodexFilterOption<PartyPresence>> =
    listOf<CodexFilterOption<PartyPresence>>(
        CodexFilterOption(value = null, label = allLabel, count = searched.size, dotColor = null),
    ) +
        PartyPresence.entries.map { presence ->
            CodexFilterOption(
                value = presence,
                label = labels.getValue(presence),
                count = searched.count { it.presence == presence },
                dotColor = presence.toStatusColor().foreground,
            )
        }

internal fun npcFilterOptions(
    searched: List<Npc>,
    statusLabels: Map<NpcStatus, String>,
    deadLabel: String,
    allLabel: String,
): List<CodexFilterOption<NpcListFilter>> {
    val relationOptions =
        NpcStatus.entries.map { status ->
            CodexFilterOption<NpcListFilter>(
                value = NpcListFilter.Relation(status),
                label = statusLabels.getValue(status),
                count = searched.count { it.status == status },
                dotColor = status.toStatusColor().foreground,
            )
        }
    val deadOption =
        CodexFilterOption<NpcListFilter>(
            value = NpcListFilter.Dead,
            label = deadLabel,
            count = searched.count { it.lifeState == NpcLifeState.DEAD },
            dotColor = NpcLifeState.DEAD.toStatusColor().foreground,
        )
    val allOption =
        CodexFilterOption<NpcListFilter>(value = null, label = allLabel, count = searched.size, dotColor = null)
    return listOf(allOption) + relationOptions + deadOption
}

internal fun questFilterOptions(
    counts: Map<QuestStatus, Int>,
    labels: Map<QuestStatus, String>,
    allLabel: String,
): List<CodexFilterOption<QuestStatus>> =
    listOf<CodexFilterOption<QuestStatus>>(
        CodexFilterOption(value = null, label = allLabel, count = counts.values.sum(), dotColor = null),
    ) +
        QuestStatus.entries.map { status ->
            CodexFilterOption(
                value = status,
                label = labels.getValue(status),
                count = counts[status] ?: 0,
                dotColor = status.toStatusColor().foreground,
            )
        }
