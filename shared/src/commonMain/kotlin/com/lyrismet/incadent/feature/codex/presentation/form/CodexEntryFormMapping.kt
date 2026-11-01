package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.SPECIES_HIT_POINTS_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.joinNicknames
import com.lyrismet.incadent.feature.codex.domain.model.parseNicknames
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef

internal fun CodexEntryFormFields.toPartyMember(): PartyMember {
    val party = party
    return PartyMember(
        id = (editingRef as? EntityRef.Party)?.id ?: reviveId,
        name = name.trim().capitalizeFirst(),
        characterClass = party.characterClass.trim().capitalizeFirst(),
        race = race.trim().capitalizeFirst(),
        level = party.level,
        // the "вы" flag replaces the player name, so a player character never keeps one
        playerName = if (party.isPlayerCharacter) "" else party.player.trim().capitalizeFirst(),
        isPlayerCharacter = party.isPlayerCharacter,
        presence = party.presence,
        hpMax = party.hpMax,
        hpCurrent = party.hpCurrent ?: party.hpMax,
        description = description.trim(),
        portrait = portrait,
    )
}

internal fun Npc.toFields(ref: EntityRef.Npc) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = EntityKind.NPC,
        name = name,
        description = description,
        portrait = portrait,
        race = race,
        nicknames = nicknames.joinNicknames(),
        npcStatus = status,
        npcLife = lifeState,
        npcLocationIds = locationIds,
        npcSpeciesId = speciesId,
        vulnerabilities = vulnerabilities,
    )

internal fun PartyMember.toFields(ref: EntityRef.Party) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = EntityKind.PARTY,
        name = name,
        description = description,
        portrait = portrait,
        race = race,
        party =
            PartyFormFields(
                isPlayerCharacter = isPlayerCharacter,
                player = playerName,
                characterClass = characterClass,
                presence = presence,
                level = level,
                hpMax = hpMax,
                hpCurrent = hpCurrent,
            ),
    )

internal fun Quest.toFields(ref: EntityRef.Quest) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = title,
        type = EntityKind.QUEST,
        name = title,
        description = description,
        reward = reward,
        questGiverId = givenByNpcId,
        questLocationId = locationId,
        questStatus = status,
    )

internal fun Location.toFields(ref: EntityRef.Location) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = EntityKind.LOCATION,
        name = name,
        description = description,
        locationParentId = parentId,
        cover = cover,
    )

internal fun CodexEntryFormFields.toNpc() =
    Npc(
        id = (editingRef as? EntityRef.Npc)?.id ?: reviveId,
        name = name.trim().capitalizeFirst(),
        status = npcStatus,
        lifeState = npcLife,
        description = description.trim(),
        locationIds = npcLocationIds,
        race = race.trim().capitalizeFirst(),
        portrait = portrait,
        nicknames = parseNicknames(nicknames),
        speciesId = npcSpeciesId,
        vulnerabilities = vulnerabilities,
    )

internal fun CodexEntryFormFields.toQuest() =
    Quest(
        id = (editingRef as? EntityRef.Quest)?.id ?: reviveId,
        title = name.trim().capitalizeFirst(),
        status = questStatus,
        reward = reward.trim(),
        givenByNpcId = questGiverId,
        locationId = questLocationId,
        description = description.trim(),
    )

internal fun CodexEntryFormFields.toLocation() =
    Location(
        id = (editingRef as? EntityRef.Location)?.id ?: reviveId,
        name = name.trim().capitalizeFirst(),
        description = description.trim(),
        parentId = locationParentId,
        cover = cover,
    )

internal fun Species.toFields(ref: EntityRef.Species) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = EntityKind.SPECIES,
        name = name,
        description = notes,
        portrait = photo,
        speciesType = type,
        vulnerabilities = vulnerabilities,
        speciesHitPoints = hitPoints,
    )

internal fun CodexEntryFormFields.toSpecies() =
    Species(
        id = (editingRef as? EntityRef.Species)?.id ?: reviveId,
        name = name.trim().capitalizeFirst(),
        creatureType = CreatureType.UNSPECIFIED,
        vulnerabilities = vulnerabilities,
        notes = description.trim(),
        photo = portrait,
        hitPoints = speciesHitPoints?.coerceIn(SPECIES_HIT_POINTS_RANGE),
    ).withType(speciesType)
