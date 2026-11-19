package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals

/** everything a party member owns besides the record itself, which a delete takes along */
data class PartySheetSnapshot(
    val sheet: CharacterSheet,
    val vitals: CharacterVitals?,
    val resources: List<CharacterResource>,
    val spells: List<CharacterSpell>,
    val feats: List<CharacterFeat>,
)

/** keeps the sheet of a party member through a delete, so its undo brings the whole character back */
interface PartySheetBackup {
    suspend fun capture(memberId: Long): PartySheetSnapshot

    /** writes [snapshot] under [memberId], the member record has to be stored again before */
    suspend fun restore(
        memberId: Long,
        snapshot: PartySheetSnapshot,
    )
}
