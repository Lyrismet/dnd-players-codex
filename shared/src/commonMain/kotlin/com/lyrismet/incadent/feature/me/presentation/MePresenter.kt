package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.isSpellTabVisible
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterRestRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.codex.presentation.item.ItemsController
import com.lyrismet.incadent.feature.codex.presentation.item.rememberItemsController
import com.lyrismet.incadent.feature.codex.presentation.party.PlayerCharacterPicker
import com.lyrismet.incadent.feature.me.domain.abilityStrips
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.presenter.Presenter

/** loads the character marked "me" with its sheet and turns every press of the screen into a write */
@Suppress("LongParameterList")
internal class MePresenter(
    private val partyRepository: PartyRepository,
    private val sheetRepository: CharacterSheetRepository,
    private val resourceRepository: CharacterResourceRepository,
    private val campaignRepository: CampaignRepository,
    private val codexRepositories: CodexRepositories,
    private val restRepository: CharacterRestRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
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
        // the records and the notes are only read once a character is picked
        val records =
            if (member != null) {
                rememberMeRecords(codexRepositories, list)
            } else {
                CodexRecords(emptyList(), emptyList(), emptyList(), emptyList())
            }
        val itemsController = rememberItemsController(codexRepositories.itemRepository, undoController)
        val live = if (member != null) rememberLiveSession() else null
        // the open level belongs to one character, the next one starts closed
        LaunchedEffect(member?.id) { ui.slotLevel = null }
        val characterContent =
            member?.let {
                characterContent(it, ui, readOnly, MeThings(records, itemsController, live?.title))
            }
        val content =
            when {
                list == null -> MeContent.Loading
                member == null -> MeContent.Pick(pickOptions(list), readOnly)
                else -> characterContent ?: MeContent.Loading
            }
        val eventScope =
            MeEventScope(
                member,
                list.orEmpty(),
                (content as? MeContent.Character)?.character,
                readOnly,
                records,
                live?.id,
            )
        val latestScope by rememberUpdatedState(eventScope)
        val counters = remember { MeCounterActions(scope, resourceRepository, sheetRepository, undoController) }
        val things =
            remember {
                MeThingsHandler(
                    ui,
                    actions,
                    MeRestActions(scope, restRepository, sessionEntryRepository, undoController),
                    MeItemActions(scope, codexRepositories.itemRepository, undoController),
                    itemsController,
                    scope,
                )
            }
        val handler = remember { MeEventHandler({ latestScope }, ui, actions, counters, picker, things, scope) }
        val eventSink = remember(handler) { handler::handle }
        return MeState(content = content, eventSink = eventSink)
    }

    @Composable
    private fun rememberLiveSession(): SessionNote? {
        val sessionNotes by remember { sessionNoteRepository.observeAll() }.collectAsState(initial = emptyList())
        return sessionNotes.firstOrNull { it.isLive }
    }

    @Composable
    private fun characterContent(
        member: PartyMember,
        ui: MeUiState,
        readOnly: Boolean,
        things: MeThings,
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
        val hasSpells = isSpellTabVisible(sheet)
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
        val items =
            rememberMeItems(member, sheet, ui, things.records, things.controller, codexRepositories, undoController)
        return MeContent.Character(
            buildMeCharacter(
                member,
                shownVitals,
                sheet,
                figures,
                strips,
                resources,
                tabs,
                ui,
                readOnly,
                items,
                things.liveSessionTitle,
            ),
        )
    }

    private fun pickOptions(party: List<PartyMember>?): List<MePickOption> =
        party.orEmpty().filter { !it.fallen }.map { MePickOption(it.id, it.name, it.characterClass, it.level) }
}

/** what the things tab and the rest sheet read besides the sheet itself */
private class MeThings(
    val records: CodexRecords,
    val controller: ItemsController,
    val liveSessionTitle: String?,
)
