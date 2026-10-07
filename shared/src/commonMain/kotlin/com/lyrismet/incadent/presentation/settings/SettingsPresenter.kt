package com.lyrismet.incadent.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.campaign.RenameSheetState
import com.lyrismet.incadent.core.campaign.savedCampaignNameOrNull
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.HoldHintState
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.default_campaign_name
import org.jetbrains.compose.resources.stringResource

class SettingsPresenter(
    private val languageRepository: LanguageRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : Presenter<SettingsState> {
    @Composable
    override fun present(): SettingsState {
        val language by languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val mentionStyle by appPreferencesRepository.observeMentionStyle().collectAsState(initial = MentionStyle.FILLED)
        val editMode by appPreferencesRepository
            .observeEntityEditMode()
            .collectAsState(initial = EntityEditMode.QUICK)
        val campaignNameOverride by appPreferencesRepository.observeCampaignName().collectAsState(initial = null)
        val defaultCampaignName = stringResource(Res.string.default_campaign_name)
        val campaignName = campaignNameOverride ?: defaultCampaignName
        val renameSheet = remember { mutableStateOf<RenameSheetState>(RenameSheetState.Hidden) }

        return SettingsState(
            language = language,
            numbering = numbering,
            mentionStyle = mentionStyle,
            editMode = editMode,
            campaignName = campaignName,
            renameSheet = renameSheet.value,
        ) { event ->
            when (event) {
                is SettingsEvent.LanguageSelected -> languageRepository.setLanguage(event.language)
                is SettingsEvent.NumberingSelected -> appPreferencesRepository.setSessionNumbering(event.numbering)
                is SettingsEvent.MentionStyleSelected -> appPreferencesRepository.setMentionStyle(event.style)
                is SettingsEvent.EditModeSelected -> appPreferencesRepository.setEntityEditMode(event.mode)
                SettingsEvent.HoldHintShowRequested -> appPreferencesRepository.setHoldHintState(HoldHintState.PENDING)
                SettingsEvent.RenameOpened -> renameSheet.value = RenameSheetState.Editing(campaignName)
                is SettingsEvent.RenameDraftChanged -> renameSheet.value = RenameSheetState.Editing(event.value)
                SettingsEvent.RenameSaved -> {
                    // a blank name is refused and the sheet stays open, matching the disabled save button
                    renameSheet.value.savedCampaignNameOrNull()?.let { name ->
                        appPreferencesRepository.setCampaignName(name)
                        renameSheet.value = RenameSheetState.Hidden
                    }
                }
                SettingsEvent.RenameDismissed -> renameSheet.value = RenameSheetState.Hidden
            }
        }
    }
}
