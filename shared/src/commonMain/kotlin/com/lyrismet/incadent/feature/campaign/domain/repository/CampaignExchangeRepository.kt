package com.lyrismet.incadent.feature.campaign.domain.repository

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import kotlinx.coroutines.flow.Flow

/** moves a campaign in and out of a file, a bad file fails with an `ImportFailedException` */
interface CampaignExchangeRepository {
    /** what [campaignId] has to offer an export */
    suspend fun loadCatalog(campaignId: String): ExchangeCatalog

    /** how big the file of [selection] would be, with and without its photos */
    suspend fun estimate(
        campaignId: String,
        selection: RecordSelection,
    ): ExportEstimate

    suspend fun export(
        campaignId: String,
        selection: RecordSelection,
        sink: FileSink,
    )

    /** checks the file and lists what is in it, nothing is written */
    suspend fun readContents(origin: FileOrigin): ImportContents

    suspend fun import(
        origin: FileOrigin,
        request: ImportRequest,
    ): ImportResult

    /** holds the file up against [campaignId] and says what each record of it would be, nothing is written */
    suspend fun plan(
        origin: FileOrigin,
        campaignId: String,
    ): ImportPlan

    /** true while the last import into [campaignId] can still be taken back */
    fun observeCanUndoLastImport(campaignId: String): Flow<Boolean>

    /** puts [campaignId] back as it was before its last import */
    suspend fun undoLastImport(campaignId: String)
}
