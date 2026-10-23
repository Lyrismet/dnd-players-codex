package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.Species

// free-text fields stay blank, lists and sheets render the dash themselves
fun quickNpc(name: String) =
    Npc(
        id = 0,
        name = name,
        status = NpcStatus.NEUTRAL,
        lifeState = NpcLifeState.ALIVE,
        description = "",
        locationIds = emptyList(),
        race = "",
    )

fun quickLocation(name: String) = Location(id = 0, name = name, description = "")

fun quickSpecies(name: String) = Species(id = 0, name = name, creatureType = CreatureType.UNSPECIFIED)

private fun quickQuest(name: String) =
    Quest(id = 0, title = name, status = QuestStatus.ACTIVE, reward = "", givenByNpcId = null, locationId = null)

private fun quickPartyMember(name: String) =
    PartyMember(
        id = 0,
        name = name,
        characterClass = "",
        race = "",
        level = null,
        playerName = "",
        isPlayerCharacter = false,
        presence = PartyPresence.IN,
        hpMax = null,
        hpCurrent = null,
        description = "",
    )

/** stores a record of [kind] that has only a name and returns its id - the rest is filled in on its card later */
suspend fun CodexRepositories.createNamed(
    kind: EntityKind,
    name: String,
): Long =
    when (kind) {
        EntityKind.NPC -> npcRepository.upsert(quickNpc(name))
        EntityKind.PARTY -> partyRepository.upsert(quickPartyMember(name))
        EntityKind.QUEST -> questRepository.upsert(quickQuest(name))
        EntityKind.LOCATION -> locationRepository.upsert(quickLocation(name))
        EntityKind.SPECIES -> speciesRepository.upsert(quickSpecies(name))
    }

suspend fun CodexRepositories.deleteRecord(
    kind: EntityKind,
    id: Long,
) {
    when (kind) {
        EntityKind.NPC -> npcRepository.delete(id)
        EntityKind.PARTY -> partyRepository.delete(id)
        EntityKind.QUEST -> questRepository.delete(id)
        EntityKind.LOCATION -> locationRepository.delete(id)
        EntityKind.SPECIES -> speciesRepository.delete(id)
    }
}
