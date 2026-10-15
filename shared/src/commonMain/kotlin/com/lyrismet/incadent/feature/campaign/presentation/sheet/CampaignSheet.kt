package com.lyrismet.incadent.feature.campaign.presentation.sheet

/** one campaign as the library card shows it - every label is already formatted */
data class CampaignCardItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val lastLabel: String,
    val isCurrent: Boolean,
    val isArchived: Boolean,
)

/** the campaign list split the way the library draws it: running ones first, finished ones after */
data class CampaignLibraryState(
    val live: List<CampaignCardItem> = emptyList(),
    val archived: List<CampaignCardItem> = emptyList(),
) {
    val isEmpty: Boolean get() = live.isEmpty() && archived.isEmpty()
}

data class CampaignSourceItem(
    val id: String,
    val name: String,
    val subtitle: String,
)

data class CampaignTransferRow(
    val id: Long,
    val name: String,
    val subtitle: String,
    val checked: Boolean,
)

enum class CampaignCreateStep { FORM, ITEMS }

enum class CampaignTransferTab { PLACES, NPCS }

/** what the sheet host shows over the screen, [Hidden] means no campaign sheet at all */
sealed interface CampaignSheetState {
    data object Hidden : CampaignSheetState

    data class Library(
        val library: CampaignLibraryState,
    ) : CampaignSheetState

    data class Rename(
        val draft: String,
    ) : CampaignSheetState

    data class Finish(
        val name: String,
    ) : CampaignSheetState

    data class DeleteData(
        val name: String,
        val typed: String,
    ) : CampaignSheetState {
        val canDelete: Boolean get() = typed.trim() == name.trim()
    }

    @Suppress("LongParameterList")
    data class Create(
        val name: String,
        val sameWorld: Boolean,
        val sources: List<CampaignSourceItem>,
        val sourceId: String?,
        val copyTags: Boolean,
        val copyPhrases: Boolean,
        val step: CampaignCreateStep,
        val sourceName: String,
        val tab: CampaignTransferTab,
        val query: String,
        val placesLabel: String,
        val npcsLabel: String,
        val rows: List<CampaignTransferRow>,
        val allChecked: Boolean,
        val transferCount: Int,
        val canProceed: Boolean,
    ) : CampaignSheetState
}

sealed interface CampaignSheetEvent {
    data object LibraryOpened : CampaignSheetEvent

    data object Dismissed : CampaignSheetEvent

    data class CampaignClicked(
        val id: String,
    ) : CampaignSheetEvent

    data class Resumed(
        val id: String,
    ) : CampaignSheetEvent

    data object NewCampaignClicked : CampaignSheetEvent

    data object RenameClicked : CampaignSheetEvent

    data class RenameDraftChanged(
        val value: String,
    ) : CampaignSheetEvent

    data object RenameSaved : CampaignSheetEvent

    data object FinishClicked : CampaignSheetEvent

    data object ArchiveConfirmed : CampaignSheetEvent

    data object DeleteDataClicked : CampaignSheetEvent

    data class DeleteTypedChanged(
        val value: String,
    ) : CampaignSheetEvent

    data object DeleteConfirmed : CampaignSheetEvent

    data class NameChanged(
        val value: String,
    ) : CampaignSheetEvent

    data class WorldSelected(
        val sameWorld: Boolean,
    ) : CampaignSheetEvent

    data class SourceSelected(
        val id: String,
    ) : CampaignSheetEvent

    data object CopyTagsToggled : CampaignSheetEvent

    data object CopyPhrasesToggled : CampaignSheetEvent

    data object FormNext : CampaignSheetEvent

    data object BackToForm : CampaignSheetEvent

    data class TabSelected(
        val tab: CampaignTransferTab,
    ) : CampaignSheetEvent

    data class QueryChanged(
        val value: String,
    ) : CampaignSheetEvent

    data class ItemToggled(
        val id: Long,
    ) : CampaignSheetEvent

    data object AllToggled : CampaignSheetEvent
}
