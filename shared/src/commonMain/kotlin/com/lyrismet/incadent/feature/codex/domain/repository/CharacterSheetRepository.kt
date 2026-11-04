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

    fun observeVitals(memberId: Long): Flow<CharacterVitals?>

    suspend fun saveVitals(
        memberId: Long,
        vitals: CharacterVitals,
    )
}
