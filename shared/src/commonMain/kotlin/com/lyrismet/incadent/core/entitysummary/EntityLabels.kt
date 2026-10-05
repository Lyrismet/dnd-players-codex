package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.QuestStatus
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_npc_life_alive
import dndplayerscodex.shared.generated.resources.codex_npc_life_dead
import dndplayerscodex.shared.generated.resources.codex_npc_status_enemy
import dndplayerscodex.shared.generated.resources.codex_npc_status_friend
import dndplayerscodex.shared.generated.resources.codex_npc_status_neutral
import dndplayerscodex.shared.generated.resources.codex_party_presence_away
import dndplayerscodex.shared.generated.resources.codex_party_presence_in
import dndplayerscodex.shared.generated.resources.codex_quest_status_active
import dndplayerscodex.shared.generated.resources.codex_quest_status_completed
import dndplayerscodex.shared.generated.resources.codex_quest_status_failed
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
