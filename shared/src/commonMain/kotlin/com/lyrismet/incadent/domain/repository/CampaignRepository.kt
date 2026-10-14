package com.lyrismet.incadent.domain.repository

import com.lyrismet.incadent.domain.model.Campaign
import com.lyrismet.incadent.domain.model.CampaignTransfer
import com.lyrismet.incadent.domain.model.TransferCandidates
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    /** every campaign, the most recently opened first */
    fun observeAll(): Flow<List<Campaign>>

    /** the campaign the app is open on, null before the first one is created or while none is open */
    fun observeCurrent(): Flow<Campaign?>

    /** makes [id] the open campaign, it is also where the app opens next launch */
    suspend fun open(id: String)

    /** creates a campaign, opens it and returns the number of copied places and npcs */
    suspend fun create(
        name: String,
        transfer: CampaignTransfer? = null,
    ): Int

    suspend fun rename(
        id: String,
        name: String,
    )

    suspend fun archive(id: String)

    suspend fun resume(id: String)

    suspend fun deleteWithData(id: String)

    fun observeTransferCandidates(sourceId: String): Flow<TransferCandidates>
}
