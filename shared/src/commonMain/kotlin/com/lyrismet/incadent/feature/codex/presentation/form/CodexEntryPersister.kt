package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.clampedToRanges
import com.lyrismet.incadent.feature.codex.domain.repository.LocationRepository
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.codex.domain.repository.QuestRepository

/** turns the form's fields into the domain record of its type and writes it, one repository per kind */
internal class CodexEntryPersister(
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) {
    /** returns the id the record is stored under */
    suspend fun save(form: CodexEntryFormFields): Long =
        when (form.type) {
            EntityKind.PARTY -> partyRepository.upsert(form.toPartyMember().clampedToRanges())
            EntityKind.NPC -> npcRepository.upsert(form.toNpc())
            EntityKind.QUEST -> questRepository.upsert(form.toQuest())
            EntityKind.LOCATION -> saveLocation(form)
        }

    // the places the form lists as inside move under the saved place, the ones it dropped go to the top level
    private suspend fun saveLocation(form: CodexEntryFormFields): Long {
        val id = locationRepository.upsert(form.toLocation())
        locationRepository.setInside(id, form.locationChildIds)
        return id
    }
}
