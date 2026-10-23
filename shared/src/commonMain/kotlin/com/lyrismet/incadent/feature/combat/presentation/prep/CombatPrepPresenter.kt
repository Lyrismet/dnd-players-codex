package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.preparation.PartyHp
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.presentation.combatSessionTarget
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_prep_intro_foe_done
import dndplayerscodex.shared.generated.resources.combat_prep_intro_foe_todo
import dndplayerscodex.shared.generated.resources.combat_prep_intro_party_todo
import dndplayerscodex.shared.generated.resources.error_action_failed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.compose.resources.stringResource

@Suppress("LongParameterList")
class CombatPrepPresenter(
    private val combatId: Long,
    private val navigator: Navigator,
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionEntryRepository: SessionEntryRepository,
    private val codexRepositories: CodexRepositories,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val imageCompressor: ImageCompressor,
) : Presenter<CombatPrepState> {
    private val editLock = Mutex()

    @Suppress("LongMethod")
    @Composable
    override fun present(): CombatPrepState {
        val combat by combatRepository.observe(combatId).collectAsState(initial = null)
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val npcs by codexRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
        val species by codexRepositories.speciesRepository.observeAll().collectAsState(initial = emptyList())
        val everyone by codexRepositories.partyRepository.observeAll().collectAsState(initial = emptyList())
        // a character who fell is out of the party until brought back
        val party = everyone.filterNot { it.fallen }
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val live = sessions.firstOrNull { it.isLive }
        val liveEntries by remember(live?.id) {
            live?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
        }.collectAsState(initial = emptyList())
        val search = remember { mutableStateOf<SearchDraft?>(null) }
        val locations by codexRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
        val formTarget = remember { mutableStateOf<CombatSide?>(null) }
        val formController =
            CodexEntryFormController.rememberController(
                codexRepositories.npcRepository,
                codexRepositories.partyRepository,
                codexRepositories.questRepository,
                codexRepositories.locationRepository,
                codexRepositories.speciesRepository,
                remember { mutableStateOf(emptyList()) },
                party.firstOrNull { it.isPlayerCharacter }?.level,
            )
        val hpPad = remember { mutableStateOf<HpPadDraft?>(null) }
        val exitConfirmVisible = remember { mutableStateOf(false) }
        val toast = remember { mutableStateOf<ToastMessage?>(null) }
        toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val scope = rememberGuardedScope { toast.value = ToastMessage(failedText) }

        val current = combat
        val rows = prepRows(current, party)
        val mentioned = mentionedKeys(liveEntries.map { it.body }, npcs, species, party)
        val searchState = search.value?.let { searchState(it, current, npcs, species, mentioned) }
        val target = combatSessionTarget(sessions, numbering, withTitle = true)
        val foeCount = npcs.count { it.lifeState == NpcLifeState.ALIVE && it.status == NpcStatus.ENEMY } + species.size
        val intro = prepIntro(party.map { it.name }, foeCount)
        val editor =
            PrepEditor(combatId, combatRepository, codexRepositories, editLock) { block -> scope.launch { block() } }
        val formHost = FoeFormHost(formController, formTarget, editor, imageCompressor)
        val actions = PrepActions(editor, navigator, search, hpPad, exitConfirmVisible, onCreate = formHost::open)
        val ownHp = ownCharacterHp(party)
        val snapshot =
            PrepSnapshot(
                party,
                ownHp,
                npcs,
                species,
                current?.combatants?.any { it.side != CombatSide.PARTY } == true,
            )

        return CombatPrepState(
            targetPrefix = target.prefix,
            target = target.session,
            intro = intro,
            partyIncluded = rows.party.count { it.included },
            party = rows.party,
            allies = rows.allies,
            enemies = rows.enemies,
            order = prepOrder(current, party),
            start = if (intro == null) prepStart(current) else null,
            search = searchState,
            entryForm = formController.buildState(npcs, locations, species),
            formKinds = formHost.kinds(),
            hpPad = hpPadState(hpPad.value, party, current),
            exitConfirmVisible = exitConfirmVisible.value,
            toast = toast.value,
        ) { event ->
            when (event) {
                is CombatPrepEvent.MemberEvent -> actions.onMember(event, snapshot)
                is CombatPrepEvent.SearchEvent -> actions.onSearch(event, snapshot)
                is CombatPrepEvent.FormEvent -> onForm(event, formHost, scope)
                is CombatPrepEvent.HpEvent -> actions.onHp(event, snapshot)
                is CombatPrepEvent.ExitEvent -> actions.onExit(event, snapshot)
                is CombatPrepEvent.StartEvent -> actions.onStart(snapshot)
            }
        }
    }
}

private fun onForm(
    event: CombatPrepEvent.FormEvent,
    host: FoeFormHost,
    scope: CoroutineScope,
) {
    when (event) {
        is CombatPrepEvent.CreateOpened -> host.open(event.side)
        is CombatPrepEvent.EntryForm -> host.onEvent(event.event, scope)
    }
}

/** the intro stands in for the lists until the codex has someone to put on each side */
@Composable
private fun prepIntro(
    partyNames: List<String>,
    foeCount: Int,
): PrepIntro? {
    if (partyNames.isNotEmpty() && foeCount > 0) return null
    val partyTodo = stringResource(Res.string.combat_prep_intro_party_todo)
    val foeTodo = stringResource(Res.string.combat_prep_intro_foe_todo)
    return PrepIntro(
        partyDone = partyNames.isNotEmpty(),
        partySub = if (partyNames.isNotEmpty()) partyNames.joinToString(", ") else partyTodo,
        foeDone = foeCount > 0,
        foeSub = if (foeCount > 0) stringResource(Res.string.combat_prep_intro_foe_done, foeCount) else foeTodo,
    )
}

/** the hit points of the player's own character, followed by numbers once the fight starts */
private fun ownCharacterHp(party: List<PartyMember>): Map<Long, PartyHp> =
    party
        .filter { it.isPlayerCharacter }
        .mapNotNull { member -> member.hpMax?.let { max -> member.id to PartyHp(member.hpCurrent ?: max, max) } }
        .toMap()
