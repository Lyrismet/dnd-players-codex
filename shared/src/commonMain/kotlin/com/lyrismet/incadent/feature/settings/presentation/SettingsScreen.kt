package com.lyrismet.incadent.feature.settings.presentation

import com.lyrismet.incadent.core.appinfo.UpdateStatus
import com.lyrismet.incadent.core.language.LanguageSheetEvent
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.preferences.MentionStyle
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetEvent
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSheetState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsScreen : Screen

data class SettingsState(
    val language: AppLanguage,
    val languageSheetOpen: Boolean,
    val numbering: SessionNumbering,
    val mentionStyle: MentionStyle,
    val followPartyLevel: Boolean,
    val campaignName: String,
    val campaignSheet: CampaignSheetState,
    val version: String,
    val updateStatus: UpdateStatus = UpdateStatus.Unknown,
    val toast: String?,
    val pinnedPhraseCount: Int,
    val frequentPhraseCount: Int,
    val phraseSheet: PhraseSheetState?,
    val canUndoImport: Boolean = false,
    val undoImportPrompt: Boolean = false,
    val eventSink: (SettingsEvent) -> Unit = {},
) : CircuitUiState

sealed interface SettingsEvent : CircuitUiEvent {
    data object UndoImportRequested : SettingsEvent

    data object UndoImportConfirmed : SettingsEvent

    data object UndoImportDismissed : SettingsEvent

    data class LanguageSheet(
        val event: LanguageSheetEvent,
    ) : SettingsEvent

    data class NumberingSelected(
        val numbering: SessionNumbering,
    ) : SettingsEvent

    data class MentionStyleSelected(
        val style: MentionStyle,
    ) : SettingsEvent

    data class FollowPartyLevelToggled(
        val follow: Boolean,
    ) : SettingsEvent

    data object UpdateRequested : SettingsEvent

    data object SwipeHintsShowRequested : SettingsEvent

    data object PhrasesOpened : SettingsEvent

    data class PhraseSheet(
        val event: PhraseSheetEvent,
    ) : SettingsEvent

    data class CampaignSheet(
        val event: CampaignSheetEvent,
    ) : SettingsEvent
}
