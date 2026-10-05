package com.lyrismet.incadent.core.codexgroup

import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.Quest

/** living npcs first, then by name - the dead sink to the end of the list and of every group */
fun sortNpcs(npcs: List<Npc>): List<Npc> =
    npcs.sortedWith(
        compareBy<Npc>(
            { it.lifeState == NpcLifeState.DEAD },
            { it.name.collationKey() },
            { it.name },
            { it.id },
        ),
    )

/** active, then completed, then failed (the [com.lyrismet.incadent.domain.model.QuestStatus] order), then by name */
fun sortQuests(quests: List<Quest>): List<Quest> =
    quests.sortedWith(
        compareBy<Quest>(
            { it.status.ordinal },
            { it.title.collationKey() },
            { it.title },
            { it.id },
        ),
    )

fun sortLocations(locations: List<Location>): List<Location> =
    locations.sortedWith(
        compareBy<Location>(
            { it.name.collationKey() },
            { it.name },
            { it.id },
        ),
    )
