package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.designsystem.component.StatusKind
import com.lyrismet.incadent.core.file.SaveFolder
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.LiveFight
import com.lyrismet.incadent.feature.campaign.domain.model.MAX_FILE_BYTES
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class ExportScreen(
    val campaignId: String,
) : Screen

/** what the finished file is handed to, a share opens the system sheet and a save opens [SavePrompt] first */
enum class ExportDelivery { SHARE, SAVE }

/** the sheet that asks where the finished file [source] goes and under which name, before it is copied */
data class SavePrompt(
    val source: String,
    val name: String,
    val folder: SaveFolder?,
    val sizeBytes: Long,
)

/** how the delivery the person asked for is going, shown as a mark inside its own button */
data class DeliveryStatus(
    val delivery: ExportDelivery,
    val kind: StatusKind,
)

data class ExportState(
    val mode: ExportMode = ExportMode.FRIEND,
    val fileName: String = "",
    val partyNotes: Boolean = false,
    val loaded: Boolean = false,
    val writing: Boolean = false,
    val campaignName: String = "",
    val whole: Boolean = true,
    val pick: ExchangePickView? = null,
    val recordCount: Int = 0,
    val sessionCount: Int = 0,
    val photos: Boolean = true,
    val estimate: ExportEstimate? = null,
    val liveFight: LiveFight? = null,
    val liveFightOn: Boolean = true,
    val canExport: Boolean = false,
    val file: String? = null,
    val savePrompt: SavePrompt? = null,
    val status: DeliveryStatus? = null,
    val eventSink: (ExportEvent) -> Unit = {},
) : CircuitUiState {
    /** the file is over what an import accepts, so the person is told to leave the photos out */
    val tooLarge: Boolean
        get() = estimate?.let { it.textBytes + if (photos) it.photoBytes else 0L }?.let { it > MAX_FILE_BYTES } ?: false

    val canGo: Boolean get() = canExport && !writing && !tooLarge
}

sealed interface ExportEvent : CircuitUiEvent {
    data class ModeSelected(
        val mode: ExportMode,
    ) : ExportEvent

    data object PartyNotesToggled : ExportEvent

    data object LiveFightToggled : ExportEvent

    data class WholeSelected(
        val whole: Boolean,
    ) : ExportEvent

    data class Pick(
        val event: ExchangePickEvent,
    ) : ExportEvent

    data object PhotosToggled : ExportEvent

    data class DeliverClicked(
        val delivery: ExportDelivery,
    ) : ExportEvent

    data class SaveNameChanged(
        val name: String,
    ) : ExportEvent

    data class SaveFolderPicked(
        val folder: SaveFolder,
    ) : ExportEvent

    data object SaveCancelled : ExportEvent

    /** the file was handed to the share sheet or copied by the save sheet, [saved] is null for a share */
    data class Delivered(
        val saved: Boolean?,
    ) : ExportEvent

    data object Closed : ExportEvent
}
