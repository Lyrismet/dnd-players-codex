package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.NewerFile
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
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

    data object Compare : ImportStep

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
    // null while the file is still being held up against the campaign
    val compare: CompareView? = null,
    val leavePromptVisible: Boolean = false,
    // set while the prompt about a file of a newer version waits for an answer
    val newerPrompt: NewerFile? = null,
    val eventSink: (ImportEvent) -> Unit = {},
) : CircuitUiState {
    val canTakeSelection: Boolean get() = pickRecordCount + pickSessionCount > 0

    // a pick or a replace list in the making, or a write under way: a swipe down must not throw it away
    val guardsClosing: Boolean get() =
        step == ImportStep.Items ||
            step == ImportStep.Compare ||
            step == ImportStep.Working
}

/** the answers a person gives on the comparison step, they only change [CompareAnswers] */
sealed interface CompareEvent : ImportEvent

sealed interface ImportEvent : CircuitUiEvent {
    data class FilePicked(
        val origin: FileOrigin,
    ) : ImportEvent

    data class DestinationSelected(
        val destination: ImportDestination,
    ) : ImportEvent

    data object PreviewNext : ImportEvent

    /** "continue" or "back" in the prompt about a file of a newer version */
    data class NewerAnswered(
        val proceed: Boolean,
    ) : ImportEvent

    data class Pick(
        val event: ExchangePickEvent,
    ) : ImportEvent

    data object PhotosToggled : ImportEvent

    data object ItemsNext : ImportEvent

    /** "same", "different" or "decide later" for a similar record, a second tap takes it back */
    data class SimilarAnswered(
        val ref: ExchangeRef,
        val resolution: Resolution,
    ) : CompareEvent

    data class FieldChosen(
        val ref: ExchangeRef,
        val field: RecordField,
        val choice: FieldChoice,
    ) : CompareEvent

    /** a field the file fills in is taken or left alone */
    data class FillToggled(
        val ref: ExchangeRef,
        val field: RecordField,
    ) : CompareEvent

    /** "take everything from the file" or "keep mine" for every conflicting field of every record */
    data class AllFieldsChosen(
        val choice: FieldChoice,
    ) : CompareEvent

    data class RecordOpened(
        val ref: ExchangeRef?,
    ) : CompareEvent

    data object NameTwinsToggled : CompareEvent

    data class QueryChanged(
        val text: String,
    ) : CompareEvent

    data class SessionMerged(
        val fileId: Long,
        val merge: Boolean,
    ) : CompareEvent

    data class BroughtBack(
        val ref: ExchangeRef,
    ) : CompareEvent

    data object TakeAsMineToggled : CompareEvent

    data object TakeVitalsToggled : CompareEvent

    /** an own copy: "keep from the device" (false) or "take from the file" (true) */
    data class OwnCopyChosen(
        val fromFile: Boolean,
    ) : CompareEvent

    data object ImportConfirmed : ImportEvent

    data object Back : ImportEvent

    /** the cross, a swipe down, a tap on the scrim or the back key - may ask first */
    data object CloseRequested : ImportEvent

    /** "stay" in the prompt that asks before the pick is thrown away */
    data object LeaveCancelled : ImportEvent

    data object Closed : ImportEvent
}
