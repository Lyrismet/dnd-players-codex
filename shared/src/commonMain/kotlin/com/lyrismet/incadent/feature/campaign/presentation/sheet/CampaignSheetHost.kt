package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.format.toShortDisplayDate
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.navigation.LocalOverlayNavigator
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.feature.campaign.domain.model.Campaign
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.model.TransferCandidates
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.campaign.presentation.exchange.ExportScreen
import com.slack.circuit.runtime.GoToNavigator
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_items_tab_format
import dndplayerscodex.shared.generated.resources.campaign_last_new
import dndplayerscodex.shared.generated.resources.campaign_player_format
import dndplayerscodex.shared.generated.resources.campaign_player_level_format
import dndplayerscodex.shared.generated.resources.campaign_sessions_count
import dndplayerscodex.shared.generated.resources.campaign_tab_npcs
import dndplayerscodex.shared.generated.resources.campaign_tab_places
import dndplayerscodex.shared.generated.resources.campaign_toast_archived
import dndplayerscodex.shared.generated.resources.campaign_toast_created
import dndplayerscodex.shared.generated.resources.campaign_toast_created_transfer
import dndplayerscodex.shared.generated.resources.campaign_toast_deleted
import dndplayerscodex.shared.generated.resources.error_action_failed
import dndplayerscodex.shared.generated.resources.session_list_rename_saved_toast
import dndplayerscodex.shared.generated.resources.starter_phrase_arrived
import dndplayerscodex.shared.generated.resources.starter_phrase_met
import dndplayerscodex.shared.generated.resources.starter_phrase_quest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** everything a screen needs of the campaign switcher - the card lists, what is open and the sheet over the screen */
class CampaignSheetHost(
    val library: CampaignLibraryState,
    val current: Campaign?,
    val sheet: CampaignSheetState,
    val onEvent: (CampaignSheetEvent) -> Unit,
)

private sealed interface HostDialog {
    data object Hidden : HostDialog

    data object Library : HostDialog

    data class Create(
        val draft: CampaignCreateDraft,
    ) : HostDialog

    data class Rename(
        val draft: String,
    ) : HostDialog

    data object Finish : HostDialog

    data class DeleteData(
        val typed: String,
    ) : HostDialog
}

private val NoCandidates = TransferCandidates(emptyList(), emptyList())

/**
 * owns the campaign sheets' flow for one screen - Settings, the session list and the library all show the same ones.
 * [showToast] gets a finished, localized message.
 */
@Composable
fun rememberCampaignSheetHost(
    repository: CampaignRepository,
    importSource: ImportSource,
    showToast: (String) -> Unit,
): CampaignSheetHost {
    val campaigns by repository.observeAll().collectAsState(initial = emptyList())
    val current by repository.observeCurrent().collectAsState(initial = null)
    var dialog by remember { mutableStateOf<HostDialog>(HostDialog.Hidden) }
    val failedText = stringResource(Res.string.error_action_failed)
    val scope = rememberGuardedScope { showToast(failedText) }
    val overlay = LocalOverlayNavigator.current

    val create = dialog as? HostDialog.Create
    val sourceId = create?.draft?.sourceId?.takeIf { create.draft.sameWorld }
    val candidatesFlow = remember(sourceId) { sourceId?.let(repository::observeTransferCandidates) ?: emptyFlow() }
    val candidates by candidatesFlow.collectAsState(initial = NoCandidates)
    val effectiveCandidates = if (sourceId == null) NoCandidates else candidates

    val sessionWords = campaignSessionLabels(campaigns)
    val lastNew = stringResource(Res.string.campaign_last_new)
    val language = currentAppLanguage()
    val suffixes = campaigns.associate { it.id to playerSuffix(it) }
    // rebuilt when the campaigns change, not on every key typed into a rename or a delete confirmation
    val library =
        remember(campaigns, current?.id, sessionWords, suffixes, lastNew, language) {
            val cards =
                campaigns.map {
                    it.toCard(current?.id, sessionWords.getValue(it.id), lastNew, suffixes.getValue(it.id), language)
                }
            CampaignLibraryState(
                live = cards.filterNot { it.isArchived },
                archived = cards.filter { it.isArchived },
            )
        }

    val sheet =
        when (val open = dialog) {
            HostDialog.Hidden -> CampaignSheetState.Hidden
            HostDialog.Library -> CampaignSheetState.Library(library)
            is HostDialog.Rename -> CampaignSheetState.Rename(open.draft)
            HostDialog.Finish -> CampaignSheetState.Finish(current?.name.orEmpty())
            is HostDialog.DeleteData -> CampaignSheetState.DeleteData(current?.name.orEmpty(), open.typed)
            is HostDialog.Create -> createState(open.draft, campaigns, effectiveCandidates)
        }

    val onEvent: (CampaignSheetEvent) -> Unit = { event ->
        val next =
            reduce(
                event = event,
                dialog = dialog,
                current = current,
                candidates = effectiveCandidates,
                repository = repository,
                importSource = importSource,
                overlay = overlay,
                scope = scope,
                showToast = showToast,
            )
        if (next != null) dialog = next
    }
    return CampaignSheetHost(library, current, sheet, onEvent)
}

// returns the dialog to show next, or null when the event changes nothing on screen
@Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
private fun reduce(
    event: CampaignSheetEvent,
    dialog: HostDialog,
    current: Campaign?,
    candidates: TransferCandidates,
    repository: CampaignRepository,
    importSource: ImportSource,
    overlay: GoToNavigator,
    scope: CoroutineScope,
    showToast: (String) -> Unit,
): HostDialog? {
    val draft = (dialog as? HostDialog.Create)?.draft
    return when (event) {
        CampaignSheetEvent.LibraryOpened -> HostDialog.Library
        CampaignSheetEvent.Dismissed -> HostDialog.Hidden
        is CampaignSheetEvent.CampaignClicked -> {
            scope.launch { repository.open(event.id) }
            HostDialog.Hidden
        }
        is CampaignSheetEvent.Resumed -> {
            scope.launch { repository.resume(event.id) }
            null
        }
        CampaignSheetEvent.NewCampaignClicked -> HostDialog.Create(CampaignCreateDraft())
        CampaignSheetEvent.RenameClicked -> current?.let { HostDialog.Rename(it.name) }
        is CampaignSheetEvent.RenameDraftChanged -> HostDialog.Rename(event.value)
        CampaignSheetEvent.RenameSaved -> {
            val name = (dialog as? HostDialog.Rename)?.draft?.let(::savableCampaignName)
            if (name == null || current == null) {
                null
            } else {
                scope.launch {
                    repository.rename(current.id, name)
                    showToast(getString(Res.string.session_list_rename_saved_toast))
                }
                HostDialog.Hidden
            }
        }
        is CampaignSheetEvent.ExportClicked -> {
            overlay.goTo(ExportScreen(event.id))
            HostDialog.Hidden
        }
        CampaignSheetEvent.ExportCurrentClicked -> {
            current?.let { overlay.goTo(ExportScreen(it.id)) }
            HostDialog.Hidden
        }
        is CampaignSheetEvent.FilePicked -> {
            importSource.offer(event.origin)
            HostDialog.Hidden
        }
        CampaignSheetEvent.FinishClicked -> HostDialog.Finish
        CampaignSheetEvent.ArchiveConfirmed -> {
            current?.let { campaign ->
                scope.launch {
                    repository.archive(campaign.id)
                    showToast(getString(Res.string.campaign_toast_archived))
                }
            }
            HostDialog.Hidden
        }
        CampaignSheetEvent.DeleteDataClicked -> HostDialog.DeleteData("")
        is CampaignSheetEvent.DeleteTypedChanged -> HostDialog.DeleteData(event.value)
        CampaignSheetEvent.DeleteConfirmed -> {
            val typed = (dialog as? HostDialog.DeleteData)?.typed
            if (current == null || typed == null || typed.trim() != current.name.trim()) {
                null
            } else {
                scope.launch {
                    repository.deleteWithData(current.id)
                    showToast(getString(Res.string.campaign_toast_deleted))
                }
                HostDialog.Hidden
            }
        }
        else -> draft?.let { HostDialog.Create(reduceCreate(event, it, candidates, repository, scope, showToast)) }
    }
}

@Suppress("LongParameterList")
private fun reduceCreate(
    event: CampaignSheetEvent,
    draft: CampaignCreateDraft,
    candidates: TransferCandidates,
    repository: CampaignRepository,
    scope: CoroutineScope,
    showToast: (String) -> Unit,
): CampaignCreateDraft =
    when (event) {
        is CampaignSheetEvent.NameChanged -> draft.copy(name = event.value)
        is CampaignSheetEvent.WorldSelected -> draft.withWorld(event.sameWorld)
        is CampaignSheetEvent.SourceSelected -> draft.withSource(event.id)
        CampaignSheetEvent.CopyTagsToggled -> draft.copy(copyTags = !draft.copyTags)
        CampaignSheetEvent.CopyPhrasesToggled -> draft.copy(copyPhrases = !draft.copyPhrases)
        CampaignSheetEvent.BackToForm -> draft.copy(step = CampaignCreateStep.FORM)
        is CampaignSheetEvent.TabSelected -> draft.copy(tab = event.tab, query = "")
        is CampaignSheetEvent.QueryChanged -> draft.copy(query = event.value)
        is CampaignSheetEvent.ItemToggled -> draft.withItemToggled(event.id, candidates)
        CampaignSheetEvent.AllToggled -> draft.withAllToggled(candidates)
        CampaignSheetEvent.FormNext -> submitForm(draft, candidates, repository, scope, showToast)
        else -> draft
    }

// the form goes on to the picker for a same-world campaign, otherwise the campaign is created right away
private fun submitForm(
    draft: CampaignCreateDraft,
    candidates: TransferCandidates,
    repository: CampaignRepository,
    scope: CoroutineScope,
    showToast: (String) -> Unit,
): CampaignCreateDraft {
    val name = savableCampaignName(draft.name)
    return when {
        name == null || !draft.canProceed || draft.submitting -> draft
        draft.sameWorld && draft.step == CampaignCreateStep.FORM -> draft.copy(step = CampaignCreateStep.ITEMS)
        else -> {
            scope.launch {
                val copied = repository.create(name, draft.toTransfer(candidates), starterPhrases())
                showToast(createdToast(name, copied))
            }
            draft.copy(submitting = true)
        }
    }
}

// a new campaign starts with phrases that open the "@" dropdown
private suspend fun starterPhrases(): List<String> =
    listOf(
        getString(Res.string.starter_phrase_arrived),
        getString(Res.string.starter_phrase_met),
        getString(Res.string.starter_phrase_quest),
    )

private suspend fun createdToast(
    name: String,
    copied: Int,
): String =
    if (copied > 0) {
        getString(Res.string.campaign_toast_created_transfer, name, copied)
    } else {
        getString(Res.string.campaign_toast_created, name)
    }

@Composable
private fun createState(
    draft: CampaignCreateDraft,
    campaigns: List<Campaign>,
    candidates: TransferCandidates,
): CampaignSheetState.Create {
    val sessionWords = campaignSessionLabels(campaigns)
    val sources =
        campaigns.map { CampaignSourceItem(it.id, it.name, "${sessionWords.getValue(it.id)}${playerSuffix(it)}") }
    val placesTab = stringResource(Res.string.campaign_tab_places)
    val npcsTab = stringResource(Res.string.campaign_tab_npcs)
    val placesLabel =
        stringResource(
            Res.string.campaign_items_tab_format,
            placesTab,
            draft.checkedCount(CampaignTransferTab.PLACES, candidates),
            candidates.places.size,
        )
    val npcsLabel =
        stringResource(
            Res.string.campaign_items_tab_format,
            npcsTab,
            draft.checkedCount(CampaignTransferTab.NPCS, candidates),
            candidates.npcs.size,
        )
    val rows = draft.visibleRows(candidates)
    return CampaignSheetState.Create(
        name = draft.name,
        sameWorld = draft.sameWorld,
        sources = sources,
        sourceId = draft.sourceId,
        copyTags = draft.copyTags,
        copyPhrases = draft.copyPhrases,
        step = draft.step,
        sourceName = campaigns.firstOrNull { it.id == draft.sourceId }?.name.orEmpty(),
        tab = draft.tab,
        query = draft.query,
        placesLabel = placesLabel,
        npcsLabel = npcsLabel,
        rows = rows,
        allChecked = rows.isNotEmpty() && rows.all { it.checked },
        transferCount = draft.transferCount(candidates),
        canProceed = draft.canProceed,
    )
}

@Composable
private fun campaignSessionLabels(campaigns: List<Campaign>): Map<String, String> =
    campaigns.associate { campaign ->
        val n = campaign.sessionCount
        campaign.id to pluralStringResource(Res.plurals.campaign_sessions_count, n, n)
    }

@Composable
private fun playerSuffix(campaign: Campaign): String {
    val player = campaign.player ?: return ""
    val level = player.level?.let { stringResource(Res.string.campaign_player_level_format, it) }
    val detail = listOfNotNull(player.characterClass.lowercase().takeIf { it.isNotBlank() }, level).joinToString(" ")
    val text =
        if (detail.isEmpty()) player.name else stringResource(Res.string.campaign_player_format, player.name, detail)
    return " · $text"
}

private fun Campaign.toCard(
    currentId: String?,
    sessions: String,
    lastNew: String,
    playerSuffix: String,
    language: AppLanguage,
) = CampaignCardItem(
    id = id,
    name = name,
    subtitle = sessions + playerSuffix,
    lastLabel = lastSessionDate?.toShortDisplayDate(language) ?: lastNew,
    isCurrent = id == currentId,
    isArchived = status == CampaignStatus.ARCHIVED,
)
