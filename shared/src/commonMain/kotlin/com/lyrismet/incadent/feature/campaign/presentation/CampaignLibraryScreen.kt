package com.lyrismet.incadent.feature.campaign.presentation

import com.lyrismet.incadent.core.language.LanguageSheetEvent
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignLibraryState
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** shown instead of the tabs while no campaign is open - the first launch and the day every campaign is finished */
@Serializable
data object CampaignLibraryScreen : Screen

data class CampaignLibraryScreenState(
    val library: CampaignLibraryState = CampaignLibraryState(),
    // the very first launch has no campaigns at all and gets the short intro instead of an empty list
    val isOnboarding: Boolean = false,
    val sheet: CampaignSheetState = CampaignSheetState.Hidden,
    val toast: String? = null,
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val languageSheetOpen: Boolean = false,
    val eventSink: (CampaignLibraryEvent) -> Unit = {},
) : CircuitUiState

sealed interface CampaignLibraryEvent : CircuitUiEvent {
    data class CampaignSheet(
        val event: CampaignSheetEvent,
    ) : CampaignLibraryEvent

    data class LanguageSheet(
        val event: LanguageSheetEvent,
    ) : CampaignLibraryEvent
}
