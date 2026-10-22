package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** the file waiting in [com.lyrismet.incadent.core.file.ImportSource] is what this screen imports */
@Serializable
data object ImportScreen : Screen

/** why a file did not open: the file itself is wrong, or the system could not hand its bytes over */
sealed interface ImportFailure {
    data class Rejected(
        val error: ImportError,
    ) : ImportFailure

    data object Unreadable : ImportFailure
}

sealed interface ImportStep {
    data object Reading : ImportStep

    data class Failed(
        val failure: ImportFailure,
    ) : ImportStep

    data object Preview : ImportStep

    data object Items : ImportStep

    data object Match : ImportStep

    data object Working : ImportStep

    data class Done(
        val records: Int,
    ) : ImportStep
}

enum class ImportDestination { NEW, CURRENT }

data class ImportState(
    val step: ImportStep = ImportStep.Reading,
    val fileName: String = "",
    val fileBytes: Long = 0L,
    val campaignTitle: String = "",
    val recordCount: Int = 0,
    val sessionCount: Int = 0,
    val fileHasPhotos: Boolean = false,
    val currentName: String? = null,
    val destination: ImportDestination = ImportDestination.NEW,
    val pick: ExchangePickView? = null,
    val pickRecordCount: Int = 0,
    val pickSessionCount: Int = 0,
    val photos: Boolean = true,
    val conflicts: ConflictMode = ConflictMode.SKIP,
    val duplicates: List<ImportDuplicate> = emptyList(),
    // the duplicates the file's version overwrites, the rest keep what the campaign has
    val replacing: Set<ExchangeRef> = emptySet(),
    val newCount: Int = 0,
    val importCount: Int = 0,
    val leavePromptVisible: Boolean = false,
    val eventSink: (ImportEvent) -> Unit = {},
) : CircuitUiState {
    val canTakeSelection: Boolean get() = pickRecordCount + pickSessionCount > 0

    // a pick or a replace list in the making, or a write under way: a swipe down must not throw it away
    val guardsClosing: Boolean get() =
        step == ImportStep.Items ||
            step == ImportStep.Match ||
            step == ImportStep.Working
}

sealed interface ImportEvent : CircuitUiEvent {
    data class FilePicked(
        val origin: FileOrigin,
    ) : ImportEvent

    data class DestinationSelected(
        val destination: ImportDestination,
    ) : ImportEvent

    data object PreviewNext : ImportEvent

    data class Pick(
        val event: ExchangePickEvent,
    ) : ImportEvent

    data object PhotosToggled : ImportEvent

    data object ItemsNext : ImportEvent

    data class ReplaceToggled(
        val ref: ExchangeRef,
    ) : ImportEvent

    data class ReplaceAll(
        val replace: Boolean,
    ) : ImportEvent

    data class ConflictsSelected(
        val mode: ConflictMode,
    ) : ImportEvent

    data object ImportConfirmed : ImportEvent

    data object Back : ImportEvent

    /** the cross, a swipe down, a tap on the scrim or the back key - may ask first */
    data object CloseRequested : ImportEvent

    /** "stay" in the prompt that asks before the pick is thrown away */
    data object LeaveCancelled : ImportEvent

    data object Closed : ImportEvent
}
