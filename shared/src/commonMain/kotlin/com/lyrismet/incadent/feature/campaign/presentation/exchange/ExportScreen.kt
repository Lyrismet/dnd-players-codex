package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.designsystem.component.StatusKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.model.MAX_FILE_BYTES
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class ExportScreen(
    val campaignId: String,
) : Screen

/** what the finished file is handed to, the view opens the system dialog and answers with [ExportEvent.Delivered] */
enum class ExportDelivery { SHARE, SAVE }

data class ExportedFile(
    val fileName: String,
    val delivery: ExportDelivery,
)

/** how the delivery the person asked for is going, shown as a mark inside its own button */
data class DeliveryStatus(
    val delivery: ExportDelivery,
    val kind: StatusKind,
)

data class ExportState(
    val loaded: Boolean = false,
    val writing: Boolean = false,
    val campaignName: String = "",
    val whole: Boolean = true,
    val pick: ExchangePickView? = null,
    val recordCount: Int = 0,
    val sessionCount: Int = 0,
    val photos: Boolean = true,
    val estimate: ExportEstimate? = null,
    val canExport: Boolean = false,
    val file: ExportedFile? = null,
    val status: DeliveryStatus? = null,
    val eventSink: (ExportEvent) -> Unit = {},
) : CircuitUiState {
    /** the file is over what an import accepts, so the person is told to leave the photos out */
    val tooLarge: Boolean
        get() = estimate?.let { it.textBytes + if (photos) it.photoBytes else 0L }?.let { it > MAX_FILE_BYTES } ?: false

    val canGo: Boolean get() = canExport && !writing && !tooLarge
}

sealed interface ExportEvent : CircuitUiEvent {
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

    /** the file was handed to the share sheet or the save dialog, [saved] is null for a share */
    data class Delivered(
        val saved: Boolean?,
    ) : ExportEvent

    data object Closed : ExportEvent
}
