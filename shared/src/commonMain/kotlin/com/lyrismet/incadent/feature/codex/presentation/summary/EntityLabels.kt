package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.codex_npc_life_alive
import incadent.shared.generated.resources.codex_npc_life_dead
import incadent.shared.generated.resources.codex_npc_status_enemy
import incadent.shared.generated.resources.codex_npc_status_friend
import incadent.shared.generated.resources.codex_npc_status_neutral
import incadent.shared.generated.resources.codex_party_presence_away
import incadent.shared.generated.resources.codex_party_presence_in
import incadent.shared.generated.resources.codex_quest_status_active
import incadent.shared.generated.resources.codex_quest_status_completed
import incadent.shared.generated.resources.codex_quest_status_failed
import org.jetbrains.compose.resources.stringResource

/** resource-backed label maps for the enum statuses - composable because the strings come from compose resources */
@Composable
fun npcStatusLabels(): Map<NpcStatus, String> =
    mapOf(
        NpcStatus.FRIEND to stringResource(Res.string.codex_npc_status_friend),
        NpcStatus.ENEMY to stringResource(Res.string.codex_npc_status_enemy),
        NpcStatus.NEUTRAL to stringResource(Res.string.codex_npc_status_neutral),
    )

@Composable
fun npcLifeLabels(): Map<NpcLifeState, String> =
    mapOf(
        NpcLifeState.ALIVE to stringResource(Res.string.codex_npc_life_alive),
        NpcLifeState.DEAD to stringResource(Res.string.codex_npc_life_dead),
    )

@Composable
fun partyPresenceLabels(): Map<PartyPresence, String> =
    mapOf(
        PartyPresence.IN to stringResource(Res.string.codex_party_presence_in),
        PartyPresence.AWAY to stringResource(Res.string.codex_party_presence_away),
    )

@Composable
fun questStatusLabels(): Map<QuestStatus, String> =
    mapOf(
        QuestStatus.ACTIVE to stringResource(Res.string.codex_quest_status_active),
        QuestStatus.COMPLETED to stringResource(Res.string.codex_quest_status_completed),
        QuestStatus.FAILED to stringResource(Res.string.codex_quest_status_failed),
    )
