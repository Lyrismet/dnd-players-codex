package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.file.ExchangeRequest
import com.lyrismet.incadent.core.file.ExchangeRequests
import com.lyrismet.incadent.core.logging.AppLog
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_import_campaign_untitled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val LOG_COMPONENT = "Import"

class ImportPresenter(
    private val repository: CampaignExchangeRepository,
    private val campaigns: CampaignRepository,
    private val requests: ExchangeRequests,
    private val navigator: Navigator,
) : Presenter<ImportState> {
    // one flat merge of the import's steps and events, splitting it would only move the vars around
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Composable
    override fun present(): ImportState {
        // the file to import lives in the request, a file picked inside the sheet replaces it there too
        val request by requests.pending.collectAsState()
        val origin = (request as? ExchangeRequest.Import)?.origin
        val current by campaigns.observeCurrent().collectAsState(initial = null)
        var contents by remember(origin) { mutableStateOf<ImportContents?>(null) }
        var step by remember(origin) { mutableStateOf<ImportStep>(ImportStep.Reading) }
        LaunchedEffect(origin) {
            if (origin != null) {
                step =
                    attempt({ ImportStep.Failed(it) }) {
                        contents = repository.readContents(origin)
                        ImportStep.Preview
                    }
            }
        }

        var destination by remember(origin) { mutableStateOf(ImportDestination.NEW) }
        var photos by remember(origin) { mutableStateOf(true) }
        var conflicts by remember(origin) { mutableStateOf(ConflictMode.SKIP) }
        var leavePrompt by remember(origin) { mutableStateOf(false) }
        var replacing by remember(origin) { mutableStateOf(emptySet<ExchangeRef>()) }
        var pick by remember(contents) { mutableStateOf(contents?.let { ExchangePick.handPick(it.options, it.graph) }) }
        val scope = rememberCoroutineScope()

        val currentId = current?.id
        // only names are compared, so the notes of the open campaign are not read
        val existing by produceState<ExchangeCatalog?>(null, currentId) {
            value = currentId?.let { repository.loadCatalog(it, withMentions = false) }
        }
        val untitled = stringResource(Res.string.exchange_import_campaign_untitled)

        val file = contents
        val taking = pick
        val there = existing
        val duplicates =
            remember(file, taking?.scope, there) {
                if (file != null && taking != null && there != null) {
                    findDuplicates(taking.scope, file.options, there.options)
                } else {
                    emptyList()
                }
            }
        val view = remember(taking) { taking?.toView() }
        val pickedRecords = taking?.recordCount ?: 0
        val newCount = pickedRecords - duplicates.size
        // a replace ticked for a record that is no longer picked or no longer a duplicate does not count
        val replaceNow = duplicates.map { it.ref }.filter { it in replacing }.toSet()

        fun importNow() {
            val source = origin ?: return
            val target =
                if (destination == ImportDestination.NEW) {
                    ImportTarget.NewCampaign(untitled)
                } else {
                    ImportTarget.Existing(currentId ?: return)
                }
            val partial = destination == ImportDestination.CURRENT
            val import =
                ImportRequest(
                    target = target,
                    conflicts = conflicts,
                    selection = if (partial) taking?.selection?.copy(photos = photos) else null,
                    scope = if (partial) taking?.scope else null,
                    replace = if (partial) replaceNow else emptySet(),
                )
            step = ImportStep.Working
            scope.launch {
                step =
                    attempt({ ImportStep.Failed(it) }) {
                        val result = repository.import(source, import)
                        if (target is ImportTarget.NewCampaign) {
                            // the switch rebuilds the whole app, so the sheet goes with it
                            requests.clear()
                            campaigns.open(result.campaignId)
                        }
                        ImportStep.Done(result.records)
                    }
            }
        }

        return ImportState(
            step = step,
            fileName = file?.fileName.orEmpty(),
            fileBytes = file?.bytes ?: 0L,
            campaignTitle = file?.campaignName.orEmpty(),
            recordCount = file?.options?.recordCount ?: 0,
            sessionCount = file?.options?.sessionCount ?: 0,
            fileHasPhotos = file?.withPhotos ?: false,
            currentName = current?.name,
            destination = if (current == null) ImportDestination.NEW else destination,
            pick = view,
            pickRecordCount = pickedRecords,
            pickSessionCount = taking?.sessionCount ?: 0,
            photos = photos,
            conflicts = conflicts,
            duplicates = duplicates,
            replacing = replaceNow,
            newCount = newCount,
            leavePromptVisible = leavePrompt,
            importCount = if (conflicts == ConflictMode.SKIP) newCount + replaceNow.size else pickedRecords,
        ) { event ->
            when (event) {
                is ImportEvent.FilePicked -> requests.request(ExchangeRequest.Import(event.origin))
                is ImportEvent.DestinationSelected -> destination = event.destination
                ImportEvent.PreviewNext ->
                    if (destination == ImportDestination.NEW || current == null) {
                        destination = ImportDestination.NEW
                        importNow()
                    } else {
                        step = ImportStep.Items
                    }
                is ImportEvent.Pick -> pick = pick?.reduce(event.event)
                ImportEvent.PhotosToggled -> photos = !photos
                ImportEvent.ItemsNext ->
                    if (taking?.isEmpty == false) {
                        if (duplicates.isEmpty()) importNow() else step = ImportStep.Match
                    }
                is ImportEvent.ConflictsSelected -> conflicts = event.mode
                is ImportEvent.ReplaceToggled ->
                    replacing = if (event.ref in replacing) replacing - event.ref else replacing + event.ref
                is ImportEvent.ReplaceAll ->
                    replacing =
                        if (event.replace) duplicates.map { it.ref }.toSet() else emptySet()
                ImportEvent.ImportConfirmed -> importNow()
                ImportEvent.Back -> step = if (step == ImportStep.Match) ImportStep.Items else ImportStep.Preview
                ImportEvent.CloseRequested ->
                    when (step) {
                        ImportStep.Items, ImportStep.Match -> leavePrompt = true
                        // the write cannot be taken back half way, the sheet stays until it is over
                        ImportStep.Working -> Unit
                        else -> {
                            requests.clear()
                            navigator.pop()
                        }
                    }
                ImportEvent.LeaveCancelled -> leavePrompt = false
                ImportEvent.Closed -> {
                    requests.clear()
                    navigator.pop()
                }
            }
        }
    }

    // a file the app rejects or cannot read becomes a failed step, a cancelled job is left to unwind
    private suspend fun attempt(
        failed: (ImportFailure) -> ImportStep,
        block: suspend () -> ImportStep,
    ): ImportStep =
        try {
            block()
        } catch (failure: ImportFailedException) {
            failed(ImportFailure.Rejected(failure.error))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (
            @Suppress("TooGenericExceptionCaught") failure: Exception,
        ) {
            AppLog.error(LOG_COMPONENT, "the file could not be imported", failure)
            failed(ImportFailure.Unreadable)
        }
}
