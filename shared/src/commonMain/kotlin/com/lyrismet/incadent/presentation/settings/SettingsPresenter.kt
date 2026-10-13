package com.lyrismet.incadent.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.lyrismet.incadent.core.campaign.RenameSheetState
import com.lyrismet.incadent.core.campaign.savedCampaignNameOrNull
import com.lyrismet.incadent.core.phrases.PhraseSheetController
import com.lyrismet.incadent.core.phrases.phrasePool
import com.lyrismet.incadent.core.phrases.rememberFrequentPhrases
import com.lyrismet.incadent.core.phrases.sheetState
import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.model.EntityEditMode
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.lyrismet.incadent.domain.model.SwipeHintState
import com.lyrismet.incadent.domain.repository.AppPreferencesRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.lyrismet.incadent.domain.repository.PhraseSnippetRepository
import com.lyrismet.incadent.domain.repository.SessionEntryRepository
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.default_campaign_name
import org.jetbrains.compose.resources.stringResource

class SettingsPresenter(
    private val languageRepository: LanguageRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val swipeHintReplayController: SwipeHintReplayController,
    private val phraseSnippetRepository: PhraseSnippetRepository,
    private val sessionEntryRepository: SessionEntryRepository,
) : Presenter<SettingsState> {
    @Suppress("CyclomaticComplexMethod")
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
        val languageSheetOpen = remember { mutableStateOf(false) }
        val pinnedPhrases by phraseSnippetRepository.observeAll().collectAsState(initial = emptyList())
        val allEntries by sessionEntryRepository.observeAll().collectAsState(initial = emptyList())
        val frequentPhrases = rememberFrequentPhrases(allEntries)
        val phrases = remember { PhraseSheetController(phraseSnippetRepository) }
        val scope = rememberCoroutineScope()

        return SettingsState(
            language = language,
            languageSheetOpen = languageSheetOpen.value,
            numbering = numbering,
            mentionStyle = mentionStyle,
            editMode = editMode,
            campaignName = campaignName,
            renameSheet = renameSheet.value,
            pinnedPhraseCount = pinnedPhrases.size,
            frequentPhraseCount = phrasePool(pinnedPhrases, frequentPhrases).count { !it.isPinned },
            phraseSheet = phrases.sheetState(pinnedPhrases, frequentPhrases),
        ) { event ->
            when (event) {
                SettingsEvent.LanguageSheetOpened -> languageSheetOpen.value = true
                SettingsEvent.LanguageSheetDismissed -> languageSheetOpen.value = false
                is SettingsEvent.LanguageSelected -> {
                    languageSheetOpen.value = false
                    languageRepository.setLanguage(event.language)
                }
                is SettingsEvent.NumberingSelected -> appPreferencesRepository.setSessionNumbering(event.numbering)
                is SettingsEvent.MentionStyleSelected -> appPreferencesRepository.setMentionStyle(event.style)
                is SettingsEvent.EditModeSelected -> appPreferencesRepository.setEntityEditMode(event.mode)
                SettingsEvent.SwipeHintsShowRequested -> {
                    // the row teaches the swipe-to-delete/edit peek, not the unrelated hold-to-edit pin card
                    appPreferencesRepository.setSessionListSwipeHintState(SwipeHintState.PENDING)
                    appPreferencesRepository.setCodexSwipeHintState(SwipeHintState.PENDING)
                    // mirrors the mockup's replayHints - jump to the sessions tab so the hint actually plays
                    swipeHintReplayController.requestSessionsReplay()
                }
                SettingsEvent.PhrasesOpened -> phrases.open()
                is SettingsEvent.PhraseSheet -> phrases.onEvent(event.event, scope)
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
