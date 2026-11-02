package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection

/** a friend gets the character without hit points, notes and the "я" mark: those are the player's own */
internal fun FileRecord.Party.forMode(selection: RecordSelection): FileRecord.Party =
    if (selection.mode == ExportMode.SELF) {
        this
    } else {
        copy(
            isPlayerCharacter = false,
            hpMax = null,
            hpCurrent = null,
            description = if (selection.partyNotes) description else "",
        )
    }

/** the hit points of a party member in a saved fight stay home with the rest of the vitals */
internal fun FileRecord.Fighter.forMode(selection: RecordSelection): FileRecord.Fighter =
    if (selection.mode == ExportMode.SELF || partyId == null) this else copy(hpCurrent = null, hpMax = null)
