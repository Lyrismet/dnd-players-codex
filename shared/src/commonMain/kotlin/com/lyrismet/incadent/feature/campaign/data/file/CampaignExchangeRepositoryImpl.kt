package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.makeThumb
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CampaignExchangeRepositoryImpl(
    private val database: AppDatabase,
    compressor: ImageCompressor,
) : CampaignExchangeRepository {
    private val exporter = CampaignExporter(database)
    private val importer = CampaignImporter(database) { full, slot -> makeThumb(full, slot, compressor) }

    override suspend fun loadCatalog(
        campaignId: String,
        withMentions: Boolean,
    ): ExchangeCatalog = withContext(Dispatchers.Default) { exporter.catalog(campaignId, withMentions) }

    override suspend fun estimate(
        campaignId: String,
        selection: RecordSelection,
    ): ExportEstimate = withContext(Dispatchers.Default) { exporter.estimate(campaignId, selection) }

    override suspend fun export(
        campaignId: String,
        selection: RecordSelection,
        sink: FileSink,
    ) = withContext(Dispatchers.Default) {
        val name =
            database.campaignQueries
                .selectName(campaignId)
                .executeAsOneOrNull()
                .orEmpty()
        exporter.write(campaignId, name, selection, sink)
    }

    override suspend fun readContents(origin: FileOrigin): ImportContents =
        withContext(Dispatchers.Default) { readImportContents(origin) }

    override suspend fun import(
        origin: FileOrigin,
        request: ImportRequest,
    ): ImportResult = withContext(Dispatchers.Default) { importer.import(origin, request) }
}
