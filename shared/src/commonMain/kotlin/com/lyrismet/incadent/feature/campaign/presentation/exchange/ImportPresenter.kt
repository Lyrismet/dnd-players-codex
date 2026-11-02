package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.logging.AppLog
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportPlan
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_from_file_label
import dndplayerscodex.shared.generated.resources.exchange_import_campaign_untitled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val LOG_COMPONENT = "Import"

class ImportPresenter(
    private val repository: CampaignExchangeRepository,
    private val campaigns: CampaignRepository,
    private val source: ImportSource,
    private val navigator: Navigator,
    private val preferences: AppPreferencesRepository,
) : Presenter<ImportState> {
    // one flat merge of the import's steps and events, splitting it would only move the vars around
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Composable
    override fun present(): ImportState {
        // a file picked inside the sheet replaces the one waiting in the source
        val origin = source.file.collectAsState().value
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
        var leavePrompt by remember(origin) { mutableStateOf(false) }
        var newerPrompt by remember(origin) { mutableStateOf(false) }
        var answers by remember(origin) { mutableStateOf(CompareAnswers()) }
        var plan by remember(origin) { mutableStateOf<ImportPlan?>(null) }
        var pick by remember(contents) { mutableStateOf(contents?.let { ExchangePick.handPick(it.options, it.graph) }) }
        val scope = rememberCoroutineScope()

        val currentId = current?.id
        val untitled = stringResource(Res.string.exchange_import_campaign_untitled)
        val fromFile = stringResource(Res.string.exchange_from_file_label)

        val file = contents
        val taking = pick
        val view = remember(taking) { taking?.toView() }
        val compare = remember(plan, taking?.scope, answers) { plan?.let { buildCompare(it, taking?.scope, answers) } }

        fun importNow() {
            val picked = origin ?: return
            val target =
                if (destination == ImportDestination.NEW) {
                    ImportTarget.NewCampaign(untitled)
                } else {
                    ImportTarget.Existing(currentId ?: return)
                }
            val partial = destination == ImportDestination.CURRENT
            // a personal copy restored as a campaign of its own is the person's, an own backup brings its vitals too
            val whole = !partial && file?.mode == ExportMode.SELF
            val own = compare?.personal?.ownCopy == true
            val import =
                ImportRequest(
                    target = target,
                    conflicts = ConflictMode.SKIP,
                    selection = if (partial) taking?.selection?.copy(photos = photos) else null,
                    scope = if (partial) taking?.scope else null,
                    resolutions = answers.similar,
                    fields = answers.fields,
                    mergeNameTwins = answers.mergeNameTwins,
                    restore = if (partial) answers.restore else emptySet(),
                    separateSessions =
                        compare
                            ?.sessions
                            ?.filter { !it.merge }
                            ?.map { it.fileId }
                            ?.toSet()
                            .orEmpty(),
                    fromFileLabel = fromFile,
                    takeAsMine = whole || answers.takeAsMine,
                    takeVitals = whole || own || answers.takeVitals,
                )
            step = ImportStep.Working
            scope.launch {
                step =
                    attempt({ ImportStep.Failed(it) }) {
                        val result = repository.import(picked, import)
                        // new characters may have come in, so the banner asking for the player's own can return
                        if (result.partyAdded > 0) preferences.setMeBannerHidden(result.campaignId, false)
                        if (target is ImportTarget.NewCampaign) {
                            // the switch rebuilds the whole app, so the sheet goes with it
                            source.clear()
                            campaigns.open(result.campaignId)
                        }
                        ImportStep.Done(result.records)
                    }
            }
        }

        fun proceedFromPreview() {
            if (destination == ImportDestination.NEW || current == null) {
                destination = ImportDestination.NEW
                importNow()
            } else {
                step = ImportStep.Items
            }
        }

        fun compareNow() {
            val picked = origin ?: return
            val id = currentId ?: return
            step = ImportStep.Compare
            if (plan == null) {
                scope.launch {
                    plan =
                        attempt<ImportPlan?>({
                            step = ImportStep.Failed(it)
                            null
                        }) { repository.plan(picked, id) }
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
            pickRecordCount = taking?.recordCount ?: 0,
            pickSessionCount = taking?.sessionCount ?: 0,
            photos = photos,
            compare = compare,
            leavePromptVisible = leavePrompt,
            newerPrompt = file?.newer.takeIf { newerPrompt },
        ) { event ->
            when (event) {
                is ImportEvent.FilePicked -> source.offer(event.origin)
                is ImportEvent.DestinationSelected -> destination = event.destination
                ImportEvent.PreviewNext -> if (file?.newer != null) newerPrompt = true else proceedFromPreview()
                is ImportEvent.NewerAnswered -> {
                    newerPrompt = false
                    if (event.proceed) proceedFromPreview()
                }
                is ImportEvent.Pick -> {
                    pick = pick?.reduce(event.event)
                    plan = null
                }
                ImportEvent.PhotosToggled -> photos = !photos
                ImportEvent.ItemsNext -> if (taking?.isEmpty == false) compareNow()
                is CompareEvent -> answers = answers.reduce(event, plan)
                ImportEvent.ImportConfirmed -> importNow()
                ImportEvent.Back -> step = if (step == ImportStep.Compare) ImportStep.Items else ImportStep.Preview
                ImportEvent.CloseRequested ->
                    when (step) {
                        ImportStep.Items, ImportStep.Compare -> leavePrompt = true
                        // the write cannot be taken back half way, the sheet stays until it is over
                        ImportStep.Working -> Unit
                        else -> {
                            source.clear()
                            navigator.pop()
                        }
                    }
                ImportEvent.LeaveCancelled -> leavePrompt = false
                ImportEvent.Closed -> {
                    source.clear()
                    navigator.pop()
                }
            }
        }
    }

    // a file the app rejects or cannot read becomes a failed step, a cancelled job is left to unwind
    private suspend fun <T> attempt(
        failed: (ImportFailure) -> T,
        block: suspend () -> T,
    ): T =
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
