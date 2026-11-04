package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.RestPlan
import com.lyrismet.incadent.feature.codex.domain.model.RestUndo

interface CharacterRestRepository {
    /** what a long rest of [memberId] would do, nothing is written, null when the member is gone */
    suspend fun preview(memberId: Long): RestPlan?

    /** applies the long rest in one step and returns what it did, null when the member is gone */
    suspend fun longRest(memberId: Long): RestPlan?

    /** puts back what [undo] recorded, edits made after the rest to anything else stay */
    suspend fun undo(
        memberId: Long,
        undo: RestUndo,
    )
}
