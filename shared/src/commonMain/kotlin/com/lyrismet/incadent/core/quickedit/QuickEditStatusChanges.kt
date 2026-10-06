package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus

// the one-tap choices - they never go through a typed draft, so each one just sets its own field
fun Npc.withStatus(status: NpcStatus): Npc = copy(status = status)

fun Npc.withLifeState(lifeState: NpcLifeState): Npc = copy(lifeState = lifeState)

fun Quest.withStatus(status: QuestStatus): Quest = copy(status = status)

fun PartyMember.withPresence(presence: PartyPresence): PartyMember = copy(presence = presence)
