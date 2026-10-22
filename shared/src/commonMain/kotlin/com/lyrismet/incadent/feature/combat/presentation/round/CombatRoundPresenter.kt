package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.autoOutcome
import com.lyrismet.incadent.feature.combat.domain.round.formatElapsed
import com.lyrismet.incadent.feature.combat.domain.round.hostileCount
import com.lyrismet.incadent.feature.combat.domain.round.inTurnOrder
import com.lyrismet.incadent.feature.combat.presentation.prep.HpMode
import com.lyrismet.incadent.feature.combat.presentation.prep.HpPadState
import com.lyrismet.incadent.feature.combat.presentation.prep.SearchDraft
import com.lyrismet.incadent.feature.combat.presentation.prep.mentionedKeys
import com.lyrismet.incadent.feature.combat.presentation.prep.npcKey
import com.lyrismet.incadent.feature.combat.presentation.prep.searchState
import com.lyrismet.incadent.feature.combat.presentation.prep.speciesKey
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_damage_state_immunity
import dndplayerscodex.shared.generated.resources.combat_damage_state_normal
import dndplayerscodex.shared.generated.resources.combat_damage_state_resistance
import dndplayerscodex.shared.generated.resources.combat_damage_state_vulnerability
import dndplayerscodex.shared.generated.resources.combat_hp_preview_now
import dndplayerscodex.shared.generated.resources.combat_hp_preview_over
import dndplayerscodex.shared.generated.resources.combat_hp_preview_unset
import dndplayerscodex.shared.generated.resources.combat_prep_ally_npc
import dndplayerscodex.shared.generated.resources.combat_prep_ally_species
import dndplayerscodex.shared.generated.resources.combat_prep_ally_temp
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_npc
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_species
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_temp
import dndplayerscodex.shared.generated.resources.combat_round_hostile
import dndplayerscodex.shared.generated.resources.combat_round_no_hostile
import dndplayerscodex.shared.generated.resources.error_action_failed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

private const val TICK_MILLIS = 1000L

/** everything the fight reads from the app, collected once */
@Suppress("LongParameterList")
private class RoundData(
    val combat: Combat?,
    val sessions: List<SessionNote>,
    val npcs: List<Npc>,
    val species: List<Species>,
    val party: List<PartyMember>,
    val liveBodies: List<String>,
    val now: Instant,
)

@Composable
private fun CombatRoundPresenter.rememberData(): RoundData {
    val combat by combatRepository.observe(combatId).collectAsState(initial = null)
    val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
    val npcs by codexRepositories.npcRepository.observeAll().collectAsState(initial = emptyList())
    val species by codexRepositories.speciesRepository.observeAll().collectAsState(initial = emptyList())
    val party by codexRepositories.partyRepository.observeAll().collectAsState(initial = emptyList())
    val live = sessions.firstOrNull { it.isLive }
    val entries by remember(live?.id) {
        live?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val now by produceState(Clock.System.now()) {
        while (true) {
            value = Clock.System.now()
            delay(TICK_MILLIS)
        }
    }
    return RoundData(combat, sessions, npcs, species, party, entries.map { it.body }, now)
}

/** the states of the sheets and the panel, they live as long as the screen does */
@Suppress("LongParameterList")
private class RoundDrafts(
    val selected: MutableState<Long?>,
    val tab: MutableState<PanelTab>,
    val menuOpen: MutableState<Boolean>,
    val endConfirm: MutableState<EndConfirm?>,
    val hpPad: MutableState<RoundHpDraft?>,
    val search: MutableState<SearchDraft?>,
    val calc: MutableState<CalcDraft?>,
    val summary: MutableState<SummaryDraft?>,
)

@Composable
private fun rememberDrafts() =
    RoundDrafts(
        selected = remember { mutableStateOf(null) },
        tab = remember { mutableStateOf(PanelTab.ACTIONS) },
        menuOpen = remember { mutableStateOf(false) },
        endConfirm = remember { mutableStateOf(null) },
        hpPad = remember { mutableStateOf(null) },
        search = remember { mutableStateOf(null) },
        calc = remember { mutableStateOf(null) },
        summary = remember { mutableStateOf(null) },
    )

/** the words the messages of the fight are made of, resolved once */
private class RoundWords(
    val typeNames: Map<DamageType, String>,
    val stateTitles: Map<DamageState, String>,
    val effectWords: Map<HitEffect, String>,
)

@Composable
private fun rememberWords(): RoundWords {
    val types = damageTypeLabels().mapValues { it.value.lowercase() }
    val resistance = stringResource(Res.string.combat_damage_state_resistance)
    val immunity = stringResource(Res.string.combat_damage_state_immunity)
    val vulnerability = stringResource(Res.string.combat_damage_state_vulnerability)
    val normal = stringResource(Res.string.combat_damage_state_normal)
    return RoundWords(
        typeNames = types,
        stateTitles =
            mapOf(
                DamageState.RESISTANCE to resistance.replaceFirstChar { it.uppercase() },
                DamageState.IMMUNITY to immunity.replaceFirstChar { it.uppercase() },
                DamageState.VULNERABILITY to vulnerability.replaceFirstChar { it.uppercase() },
                DamageState.NORMAL to normal.replaceFirstChar { it.uppercase() },
            ),
        effectWords =
            mapOf(
                HitEffect.FULL to normal,
                HitEffect.HALF to resistance,
                HitEffect.NONE to immunity,
                HitEffect.DOUBLE to vulnerability,
            ),
    )
}

/** the species each participant is made of, directly or through its npc */
private fun speciesOfMembers(
    ordered: List<Combatant>,
    npcs: List<Npc>,
    species: List<Species>,
): Map<Long, Species> =
    ordered
        .mapNotNull { member ->
            val id = member.speciesId ?: member.npcId?.let { npcId -> npcs.firstOrNull { it.id == npcId }?.speciesId }
            species.firstOrNull { it.id == id }?.let { member.id to it }
        }.toMap()

@Suppress("LongParameterList")
class CombatRoundPresenter(
    internal val combatId: Long,
    private val navigator: Navigator,
    internal val combatRepository: CombatRepository,
    internal val sessionNoteRepository: SessionNoteRepository,
    internal val sessionEntryRepository: SessionEntryRepository,
    internal val codexRepositories: CodexRepositories,
    private val undoController: UndoController,
) : Presenter<CombatRoundState> {
    private val editLock = Mutex()

    @Suppress("LongMethod")
    @Composable
    override fun present(): CombatRoundState {
        val data = rememberData()
        val drafts = rememberDrafts()
        val words = rememberWords()
        val toast = remember { mutableStateOf<ToastMessage?>(null) }
        toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val scope = rememberGuardedScope { toast.value = ToastMessage(failedText) }

        val current = data.combat
        val ordered = current?.combatants?.inTurnOrder().orEmpty()
        val mover = ordered.getOrNull(current?.turn ?: 0) ?: ordered.firstOrNull()
        // the first participant to move is chosen when the screen opens, like the mockup does
        LaunchedEffect(mover?.id) {
            if (drafts.selected.value == null &&
                mover != null
            ) {
                drafts.selected.value = mover.id
            }
        }

        val editor =
            RoundEditor(combatId, combatRepository, codexRepositories, sessionNoteRepository, editLock, {
                toast.value =
                    ToastMessage(it)
            }) { block ->
                scope.launch { block() }
            }
        val learner = SpeciesLearner(codexRepositories.speciesRepository, undoController)
        val handler =
            RoundHandler(
                editor,
                navigator,
                drafts.selected,
                drafts.tab,
                drafts.menuOpen,
                drafts.endConfirm,
                drafts.hpPad,
                drafts.search,
            )
        val calcHandler = CalcHandler(editor, drafts.calc, learner)
        val summaryHandler = SummaryHandler(editor, drafts.summary, learner)
        val ownIds =
            ordered
                .filter { m ->
                    data.party.any { it.id == m.partyMemberId && it.isPlayerCharacter }
                }.map { it.id }
                .toSet()
        val speciesOf = speciesOfMembers(ordered, data.npcs, data.species)
        val resistances = speciesOf.mapValues { it.value.damage }
        val snapshot = RoundSnapshot(drafts.selected.value, ownIds, autoOutcome(ordered))
        val calcSnapshot = CalcSnapshot(ordered, mover?.id, resistances, speciesOf, words.typeNames, words.stateTitles)
        val summarySnapshot = SummarySnapshot(speciesOf, words.typeNames, words.effectWords)
        val nameOf = { key: String ->
            data.npcs.firstOrNull { npcKey(it.id) == key }?.name
                ?: data.species.firstOrNull { speciesKey(it.id) == key }?.name
        }
        val sink: (CombatRoundEvent) -> Unit = { event ->
            when (event) {
                is CombatRoundEvent.Calc -> calcHandler.onEvent(event.event, calcSnapshot, drafts.selected.value)
                is CombatRoundEvent.Summary -> summaryHandler.onEvent(event.event, summarySnapshot)
                else -> handler.onEvent(event, snapshot, nameOf)
            }
        }

        if (current == null) return CombatRoundState(eventSink = sink)
        val view = RoundView(data, current, ordered, mover, ownIds, speciesOf, resistances)
        return view.render(drafts, toast.value, sink)
    }
}

/** what the screen shows of a fight that is on: the participants in their order and what is known of them */
@Suppress("LongParameterList")
private class RoundView(
    val data: RoundData,
    val current: Combat,
    val ordered: List<Combatant>,
    val mover: Combatant?,
    val ownIds: Set<Long>,
    val speciesOf: Map<Long, Species>,
    val resistances: Map<Long, Map<DamageType, DamageState>>,
)

@Composable
private fun RoundView.render(
    drafts: RoundDrafts,
    toast: ToastMessage?,
    sink: (CombatRoundEvent) -> Unit,
): CombatRoundState {
    val kinds = memberKinds(ordered)
    val chosen = ordered.firstOrNull { it.id == drafts.selected.value }
    val hostile = ordered.hostileCount()
    val learners = speciesOf.filterKeys { id -> ordered.any { it.id == id && it.side != CombatSide.PARTY } }.keys
    val calcContext = CalcContext(ordered, mover, ownIds, resistances, learners)
    return CombatRoundState(
        timer =
            formatElapsed(
                (data.now - current.startedAt.toInstant(TimeZone.currentSystemDefault())).inWholeSeconds,
            ),
        round = current.round,
        currentName = mover?.name.orEmpty(),
        hostile = hostileText(hostile),
        allClear = hostile == 0,
        rows = roundRows(current, ordered, mover?.id, drafts.selected.value, data.party, kinds),
        panel =
            chosen?.let {
                roundPanel(
                    current,
                    it,
                    roleOf(it, data.party, kinds.getValue(it.id)),
                    mover?.name.orEmpty(),
                )
            },
        tab = drafts.tab.value,
        menuOpen = drafts.menuOpen.value,
        search =
            drafts.search.value?.let {
                searchState(
                    it,
                    current,
                    data.npcs,
                    data.species,
                    mentionedKeys(data.liveBodies, data.npcs, data.species, data.party),
                    true,
                )
            },
        hpPad = hpPadState(drafts.hpPad.value, ordered),
        calc = drafts.calc.value?.let { calcState(it, calcContext, current.round) },
        summary = drafts.summary.value?.let { summaryState(it, current, learners) },
        roundDamage = roundDamage(current),
        endConfirm = drafts.endConfirm.value,
        toast = toast,
        eventSink = sink,
    )
}

@Composable
private fun hostileText(count: Int): String =
    if (count >
        0
    ) {
        stringResource(Res.string.combat_round_hostile, count)
    } else {
        stringResource(Res.string.combat_round_no_hostile)
    }

/** the header button of the round summary shows once anything was dealt, with the damage of this round beside it */
private fun roundDamage(combat: Combat): RoundDamage? {
    val damage = combat.events.filter { it.kind == CombatEventKind.DAMAGE }
    val now = damage.filter { it.round == combat.round }.sumOf { it.amount }
    return if (damage.isEmpty()) null else RoundDamage(if (now > 0) "−$now" else null)
}

/** what each participant is, in a word: a party member has its class instead */
@Composable
private fun memberKinds(ordered: List<Combatant>): Map<Long, String> {
    val allyNpc = stringResource(Res.string.combat_prep_ally_npc)
    val allySpecies = stringResource(Res.string.combat_prep_ally_species)
    val allyTemp = stringResource(Res.string.combat_prep_ally_temp)
    val enemyNpc = stringResource(Res.string.combat_prep_enemy_npc)
    val enemySpecies = stringResource(Res.string.combat_prep_enemy_species)
    val enemyTemp = stringResource(Res.string.combat_prep_enemy_temp)
    return ordered.associate { member ->
        val ally = member.side == CombatSide.ALLY
        member.id to
            when {
                member.side == CombatSide.PARTY -> ""
                member.npcId != null -> if (ally) allyNpc else enemyNpc
                member.speciesId != null -> if (ally) allySpecies else enemySpecies
                else -> if (ally) allyTemp else enemyTemp
            }
    }
}

@Composable
private fun hpPadState(
    draft: RoundHpDraft?,
    ordered: List<Combatant>,
): HpPadState? {
    val member = draft?.let { d -> ordered.firstOrNull { it.id == d.memberId } } ?: return null
    val value = draft.expr.value()
    val max = member.hpMax
    val now = member.hpCurrent ?: max
    val preview =
        when {
            max == null || now == null -> stringResource(Res.string.combat_hp_preview_unset)
            value > max -> stringResource(Res.string.combat_hp_preview_over, now, max)
            else -> stringResource(Res.string.combat_hp_preview_now, now, max)
        }
    return HpPadState(
        name = member.name,
        mode = HpMode.HP,
        result = if (draft.expr.raw.isEmpty()) "0" else value.toString(),
        preview = preview,
        canApply = draft.expr.raw.isNotEmpty(),
        modesVisible = false,
    )
}
