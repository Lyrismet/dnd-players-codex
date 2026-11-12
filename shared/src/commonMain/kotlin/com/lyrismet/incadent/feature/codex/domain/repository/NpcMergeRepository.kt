package com.lyrismet.incadent.feature.codex.domain.repository

import com.lyrismet.incadent.feature.codex.domain.model.NpcMergeTrace

/** two records of one name become one: everything that held the one that goes away moves to the one that stays */
interface NpcMergeRepository {
    /** null when either record is gone or they are the same one */
    suspend fun merge(
        survivorId: Long,
        loserId: Long,
    ): NpcMergeTrace?

    /** brings the loser back under its old id, with the survivor as it was and everything that moved */
    suspend fun undo(trace: NpcMergeTrace)
}
