package com.lyrismet.incadent.feature.combat.presentation.list

import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object CombatListScreen : Screen

data class CombatListState(
    val campaignName: String = "",
    val campaignId: String? = null,
    // a finished campaign is read-only - no new combat, and the banner offers to resume it
    val campaignArchived: Boolean = false,
    val campaignSheet: CampaignSheetState = CampaignSheetState.Hidden,
    val groups: List<CombatGroupItem> = emptyList(),
    val footer: CombatFooter = CombatFooter.Hidden,
    val resetConfirmVisible: Boolean = false,
    val toast: ToastMessage? = null,
    val eventSink: (CombatListEvent) -> Unit = {},
) : CircuitUiState

/** the finished combats of one session under its "СЕССИЯ XIII · ТИТУЛ" heading */
data class CombatGroupItem(
    val label: String,
    val rows: List<CombatRowItem>,
)

data class CombatRowItem(
    val id: Long,
    val foes: String,
    val meta: String,
    val outcome: CombatOutcome,
)

/** what the bar over the tab bar offers */
sealed interface CombatFooter {
    data object Hidden : CombatFooter

    data class Start(
        val target: String,
    ) : CombatFooter

    data class Prepared(
        val summary: String,
    ) : CombatFooter
}

sealed interface CombatListEvent : CircuitUiEvent {
    data object StartClicked : CombatListEvent

    data object ContinueClicked : CombatListEvent

    data object ResetClicked : CombatListEvent

    data object ResetConfirmed : CombatListEvent

    data object ResetDismissed : CombatListEvent

    data class CampaignSheet(
        val event: CampaignSheetEvent,
    ) : CombatListEvent
}
