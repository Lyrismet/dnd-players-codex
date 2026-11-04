package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.isSpellTabVisible
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.codex.presentation.party.PlayerCharacterPicker
import com.lyrismet.incadent.feature.me.domain.abilityStrips
import com.slack.circuit.runtime.presenter.Presenter

/** loads the character marked "me" with its sheet and turns every press of the screen into a write */
internal class MePresenter(
    private val partyRepository: PartyRepository,
    private val sheetRepository: CharacterSheetRepository,
    private val resourceRepository: CharacterResourceRepository,
    private val campaignRepository: CampaignRepository,
    private val undoController: UndoController,
    appPreferencesRepository: AppPreferencesRepository,
) : Presenter<MeState> {
    private val picker = PlayerCharacterPicker(partyRepository, undoController, appPreferencesRepository)

    @Composable
    override fun present(): MeState {
        val scope = rememberCoroutineScope()
        val party by remember { partyRepository.observeAll() }.collectAsState(initial = null)
        val campaign by remember { campaignRepository.observeCurrent() }.collectAsState(initial = null)
        val actions = remember { MeActions(scope, partyRepository, sheetRepository, undoController) }
        val readOnly = campaign?.status == CampaignStatus.ARCHIVED
        val list = party
        val member = list?.firstOrNull { it.isPlayerCharacter }
        val ui = rememberMeUiState()
        val characterContent = member?.let { characterContent(it, ui, readOnly) }
        val content =
            when {
                list == null -> MeContent.Loading
                member == null -> MeContent.Pick(pickOptions(list), readOnly)
                else -> characterContent ?: MeContent.Loading
            }
        val eventScope = MeEventScope(member, list.orEmpty(), (content as? MeContent.Character)?.character, readOnly)
        val latestScope by rememberUpdatedState(eventScope)
        val handler = remember { MeEventHandler({ latestScope }, ui, actions, picker, scope) }
        val eventSink = remember(handler) { handler::handle }
        return MeState(content = content, eventSink = eventSink)
    }

    @Composable
    private fun characterContent(
        member: PartyMember,
        ui: MeUiState,
        readOnly: Boolean,
    ): MeContent.Character? {
        val sheet by remember(
            member.id,
        ) { sheetRepository.observeSheet(member.id) }.collectAsState(initial = CharacterSheet())
        val vitals by remember(member.id) { sheetRepository.observeVitals(member.id) }.collectAsState(initial = null)
        val resources by remember(
            member.id,
        ) { resourceRepository.observe(member.id) }.collectAsState(initial = emptyList())
        val shownVitals = vitals ?: return null
        val level = member.level ?: 1
        // the skill names are only read while the search is open
        val searchTexts = if (ui.searchOpen) rememberSkillSearchTexts() else emptyMap()
        val query = if (ui.searchOpen) ui.query.trim().lowercase() else ""
        val hasSpells = isSpellTabVisible(sheet, resources)
        val tabs = remember(hasSpells) { meTabs(hasSpells) }
        val figures = remember(sheet, shownVitals.armorClass, level) { MeSheetFigures(sheet, shownVitals, level) }
        val strips =
            remember(sheet, level, query, searchTexts) {
                abilityStrips(
                    sheet,
                    level,
                    if (query.isNotEmpty()) {
                        { skill ->
                            searchTexts[skill].orEmpty().contains(query)
                        }
                    } else {
                        null
                    },
                )
            }
        return MeContent.Character(buildMeCharacter(member, shownVitals, sheet, figures, strips, tabs, ui, readOnly))
    }

    private fun pickOptions(party: List<PartyMember>?): List<MePickOption> =
        party.orEmpty().filter { !it.fallen }.map { MePickOption(it.id, it.name, it.characterClass, it.level) }
}
