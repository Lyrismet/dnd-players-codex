package com.lyrismet.incadent.presentation.settings

import com.lyrismet.incadent.core.campaign.CampaignSheetEvent
import com.lyrismet.incadent.core.campaign.CampaignSheetState
import com.lyrismet.incadent.core.language.LanguageSheetEvent
import com.lyrismet.incadent.core.phrases.PhraseSheetEvent
import com.lyrismet.incadent.core.phrases.PhraseSheetState
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
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
    val editMode: EntityEditMode,
    val campaignName: String,
    val campaignSheet: CampaignSheetState,
    val toast: String?,
    val pinnedPhraseCount: Int,
    val frequentPhraseCount: Int,
    val phraseSheet: PhraseSheetState?,
    val eventSink: (SettingsEvent) -> Unit = {},
) : CircuitUiState

sealed interface SettingsEvent : CircuitUiEvent {
    data class LanguageSheet(
        val event: LanguageSheetEvent,
    ) : SettingsEvent

    data class NumberingSelected(
        val numbering: SessionNumbering,
    ) : SettingsEvent

    data class MentionStyleSelected(
        val style: MentionStyle,
    ) : SettingsEvent

    data class EditModeSelected(
        val mode: EntityEditMode,
    ) : SettingsEvent

    data object SwipeHintsShowRequested : SettingsEvent

    data object PhrasesOpened : SettingsEvent

    data class PhraseSheet(
        val event: PhraseSheetEvent,
    ) : SettingsEvent

    data class CampaignSheet(
        val event: CampaignSheetEvent,
    ) : SettingsEvent
}
