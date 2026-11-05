package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import kotlinx.coroutines.flow.Flow

/** the sheet and the vitals of one party member, a member without a saved sheet reads as an empty one */
interface CharacterSheetRepository {
    fun observeSheet(memberId: Long): Flow<CharacterSheet>

    suspend fun getSheet(memberId: Long): CharacterSheet

    suspend fun saveSheet(
        memberId: Long,
        sheet: CharacterSheet,
    )

    /** changes the stored sheet from what is stored now, in one transaction so quick edits do not undo each other */
    suspend fun updateSheet(
        memberId: Long,
        transform: (CharacterSheet) -> CharacterSheet,
    )

    fun observeVitals(memberId: Long): Flow<CharacterVitals?>

    suspend fun saveVitals(
        memberId: Long,
        vitals: CharacterVitals,
    )

    /** like [updateSheet] for the vitals, a member without vitals is left as it is */
    suspend fun updateVitals(
        memberId: Long,
        transform: (CharacterVitals) -> CharacterVitals,
    )
}
