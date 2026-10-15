package com.lyrismet.incadent.presentation.campaign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.campaign.rememberCampaignSheetHost
import com.lyrismet.incadent.core.language.LanguageSheetController
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.repository.CampaignRepository
import com.lyrismet.incadent.domain.repository.LanguageRepository
import com.slack.circuit.runtime.presenter.Presenter

/** loads the campaign list and hands every tap to the shared campaign sheet flow */
class CampaignLibraryPresenter(
    private val campaignRepository: CampaignRepository,
    private val languageRepository: LanguageRepository,
) : Presenter<CampaignLibraryScreenState> {
    @Composable
    override fun present(): CampaignLibraryScreenState {
        val toast = remember { mutableStateOf<String?>(null) }
        toast.autoDismiss()
        val host = rememberCampaignSheetHost(campaignRepository) { text -> toast.value = text }
        val language by languageRepository.observeLanguage().collectAsState(initial = AppLanguage.RUSSIAN)
        val languageSheet = remember { LanguageSheetController(languageRepository) }
        return CampaignLibraryScreenState(
            library = host.library,
            isOnboarding = host.library.isEmpty,
            sheet = host.sheet,
            toast = toast.value,
            language = language,
            languageSheetOpen = languageSheet.isOpen.value,
        ) { event ->
            when (event) {
                is CampaignLibraryEvent.CampaignSheet -> host.onEvent(event.event)
                is CampaignLibraryEvent.LanguageSheet -> languageSheet.onEvent(event.event)
            }
        }
    }
}
