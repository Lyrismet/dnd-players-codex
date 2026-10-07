package com.lyrismet.incadent.presentation.settings

import com.lyrismet.incadent.core.campaign.RenameSheetState
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
    val numbering: SessionNumbering,
    val mentionStyle: MentionStyle,
    val editMode: EntityEditMode,
    val campaignName: String,
    val renameSheet: RenameSheetState,
    val eventSink: (SettingsEvent) -> Unit = {},
) : CircuitUiState

sealed interface SettingsEvent : CircuitUiEvent {
    data class LanguageSelected(
        val language: AppLanguage,
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

    data object RenameOpened : SettingsEvent

    data class RenameDraftChanged(
        val value: String,
    ) : SettingsEvent

    data object RenameSaved : SettingsEvent

    data object RenameDismissed : SettingsEvent
}
