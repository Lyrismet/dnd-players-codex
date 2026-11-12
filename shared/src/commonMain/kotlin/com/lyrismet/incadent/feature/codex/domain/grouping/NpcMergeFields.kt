package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Npc

/** the survivor with its empty fields filled from the one that goes away, a filled field is never overwritten */
fun Npc.filledFrom(other: Npc): Npc =
    copy(
        description = description.ifBlank { other.description },
        race = race.ifBlank { other.race },
        characterClass = characterClass.ifBlank { other.characterClass },
        subclass = subclass.ifBlank { other.subclass },
        portrait = portrait ?: other.portrait,
        nicknames = nicknames.ifEmpty { other.nicknames },
        speciesId = speciesId ?: other.speciesId,
        vulnerabilities = vulnerabilities.ifEmpty { other.vulnerabilities },
        locationIds = (locationIds + other.locationIds).distinct(),
    )
