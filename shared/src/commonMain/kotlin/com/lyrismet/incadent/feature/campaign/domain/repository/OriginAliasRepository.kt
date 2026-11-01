package com.lyrismet.incadent.feature.campaign.domain.repository

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind

/** the origin ids of records merged into another one, so a file that still carries an old id finds the survivor */
interface OriginAliasRepository {
    /** [mergedOriginId] now means the record with [survivorOriginId] in the open campaign */
    suspend fun add(
        kind: ExchangeKind,
        survivorOriginId: String,
        mergedOriginId: String,
    )

    /** the origin id a file's [originId] stands for now, the same id when it was never merged away */
    suspend fun resolve(
        kind: ExchangeKind,
        originId: String,
    ): String
}
