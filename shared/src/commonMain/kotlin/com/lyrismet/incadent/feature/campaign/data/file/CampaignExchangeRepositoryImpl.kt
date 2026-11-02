package com.lyrismet.incadent.feature.campaign.data.file

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSink
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.makeThumb
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportResult
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CampaignExchangeRepositoryImpl(
    database: AppDatabase,
    compressor: ImageCompressor,
) : CampaignExchangeRepository {
    private val exporter = CampaignExporter(database)
    private val importer = CampaignImporter(database) { full, slot -> makeThumb(full, slot, compressor) }
    private val planner = ImportPlanner(database)
    private val rollback = ImportRollback(database) { full, slot -> makeThumb(full, slot, compressor) }
    private val journalQueries = database.importJournalQueries

    override suspend fun loadCatalog(campaignId: String): ExchangeCatalog =
        withContext(DatabaseDispatcher) { exporter.catalog(campaignId) }

    override suspend fun estimate(
        campaignId: String,
        selection: RecordSelection,
    ): ExportEstimate = withContext(DatabaseDispatcher) { exporter.estimate(campaignId, selection) }

    override suspend fun export(
        campaignId: String,
        selection: RecordSelection,
        sink: FileSink,
    ) = withContext(DatabaseDispatcher) { exporter.write(campaignId, selection, sink) }

    override suspend fun readContents(origin: FileOrigin): ImportContents =
        withContext(DatabaseDispatcher) { readImportContents(origin) }

    override suspend fun import(
        origin: FileOrigin,
        request: ImportRequest,
    ): ImportResult = withContext(DatabaseDispatcher) { importer.import(origin, request) }

    override suspend fun plan(
        origin: FileOrigin,
        campaignId: String,
    ): ImportPlan = withContext(DatabaseDispatcher) { planner.plan(origin, campaignId) }

    override fun observeCanUndoLastImport(campaignId: String): Flow<Boolean> =
        journalQueries
            .hasAny(campaignId)
            .asFlow()
            .mapToOne(DatabaseDispatcher)
            .map { it > 0 }

    override suspend fun undoLastImport(campaignId: String) {
        withContext(DatabaseDispatcher) { rollback.undo(campaignId) }
    }
}
