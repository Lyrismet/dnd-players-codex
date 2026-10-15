package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus

// the one-tap choices - they never go through a typed draft, so each one just sets its own field
fun Npc.withStatus(status: NpcStatus): Npc = copy(status = status)

fun Npc.withLifeState(lifeState: NpcLifeState): Npc = copy(lifeState = lifeState)

fun Quest.withStatus(status: QuestStatus): Quest = copy(status = status)

fun PartyMember.withPresence(presence: PartyPresence): PartyMember = copy(presence = presence)
