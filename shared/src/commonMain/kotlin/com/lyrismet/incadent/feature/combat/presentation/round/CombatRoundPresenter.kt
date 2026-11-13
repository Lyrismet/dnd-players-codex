package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.NpcMergeRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.forecast.SpeciesHitObservation
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.CustomCondition
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.domain.round.NpcPromotion
import com.lyrismet.incadent.feature.combat.domain.round.QueueStatus
import com.lyrismet.incadent.feature.combat.domain.round.TurnPosition
import com.lyrismet.incadent.feature.combat.domain.round.hostileCount
import com.lyrismet.incadent.feature.combat.domain.round.inTurnOrder
import com.lyrismet.incadent.feature.combat.domain.round.previousTurn
import com.lyrismet.incadent.feature.combat.domain.round.queueSlots
import com.lyrismet.incadent.feature.combat.domain.round.queueStatus
import com.lyrismet.incadent.feature.combat.presentation.prep.HpMode
import com.lyrismet.incadent.feature.combat.presentation.prep.HpPadState
import com.lyrismet.incadent.feature.combat.presentation.prep.SearchDraft
import com.lyrismet.incadent.feature.combat.presentation.prep.mentionedKeys
import com.lyrismet.incadent.feature.combat.presentation.prep.npcKey
import com.lyrismet.incadent.feature.combat.presentation.prep.searchState
import com.lyrismet.incadent.feature.combat.presentation.prep.speciesKey
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsBlockState
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_damage_state_immunity
import incadent.shared.generated.resources.combat_damage_state_normal
import incadent.shared.generated.resources.combat_damage_state_resistance
import incadent.shared.generated.resources.combat_damage_state_vulnerability
import incadent.shared.generated.resources.combat_hp_preview_now
import incadent.shared.generated.resources.combat_hp_preview_over
import incadent.shared.generated.resources.combat_hp_preview_unset
import incadent.shared.generated.resources.combat_queue_unnamed_line
import incadent.shared.generated.resources.combat_round_hostile
import incadent.shared.generated.resources.combat_round_no_hostile
import incadent.shared.generated.resources.error_action_failed
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.compose.resources.stringResource

/** everything the fight reads from the app, collected once */
@Suppress("LongParameterList")
private class RoundData(
    val combat: Combat?,
    val sessions: List<SessionNote>,
    val npcs: List<Npc>,
    val species: List<Species>,
    val party: List<PartyMember>,
    val liveBodies: List<String>,
    val seen: Map<Long, List<SpeciesHitObservation>>,
    val learned: List<LearnedVulnerability>,
    val customs: List<CustomCondition>,
)

@Composable
private fun CombatRoundPresenter.rememberData(): RoundData {
    val combat by combatRepository.observe(combatId).collectAsState(initial = null)
    val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
    val loadedNpcs by codexRepositories.npcRepository.observeAll().collectAsState(initial = null)
    val loadedSpecies by codexRepositories.speciesRepository.observeAll().collectAsState(initial = null)
    val loadedParty by codexRepositories.partyRepository.observeAll().collectAsState(initial = null)
    val live = sessions.firstOrNull { it.isLive }
    val entries by remember(live?.id) {
        live?.let { sessionEntryRepository.observeForSession(it.id) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val seen by hitObservationRepository.observeAll().collectAsState(initial = emptyMap())
    val learned by combatRepository.observeLearned(combatId).collectAsState(initial = emptyList())
    val customs by combatRepository.observeCustomConditions().collectAsState(initial = emptyList())
    // the screen waits for the records the rows are named from, a first frame without them would show other names
    val loaded = loadedNpcs != null && loadedSpecies != null && loadedParty != null
    return RoundData(
        combat.takeIf { loaded },
        sessions,
        loadedNpcs.orEmpty(),
        loadedSpecies.orEmpty(),
        loadedParty.orEmpty(),
        entries.map { it.body },
        seen,
        learned,
        customs,
    )
}

/** the states of the sheets and the panel, they live as long as the screen does */
@Suppress("LongParameterList")
private class RoundDrafts(
    val selected: MutableState<Long?>,
    val tab: MutableState<PanelTab>,
    val menuOpen: MutableState<Boolean>,
    val hpPad: MutableState<RoundHpDraft?>,
    val search: MutableState<SearchDraft?>,
    val calc: MutableState<CalcDraft?>,
    val summary: MutableState<SummaryDraft?>,
    val naming: MutableState<NameDraft?>,
    val promotion: MutableState<PromotionDraft?>,
    val identity: MutableState<IdentityDraft?>,
    val queue: MutableState<QueueSnapshot?>,
    val mark: MutableState<MarkDraft?>,
    val features: MutableState<FeaturesDraft?>,
    val shieldEdit: MutableState<ShieldEditDraft?>,
)

@Composable
private fun rememberDrafts() =
    RoundDrafts(
        selected = remember { mutableStateOf(null) },
        tab = remember { mutableStateOf(PanelTab.ACTIONS) },
        menuOpen = remember { mutableStateOf(false) },
        hpPad = remember { mutableStateOf(null) },
        search = remember { mutableStateOf(null) },
        calc = remember { mutableStateOf(null) },
        summary = remember { mutableStateOf(null) },
        naming = remember { mutableStateOf(null) },
        promotion = remember { mutableStateOf(null) },
        identity = remember { mutableStateOf(null) },
        queue = remember { mutableStateOf(null) },
        mark = remember { mutableStateOf(null) },
        features = remember { mutableStateOf(null) },
        shieldEdit = remember { mutableStateOf(null) },
    )

/** the words the messages of the fight are made of, resolved once */
internal class RoundWords(
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
    private val npcMerge: NpcMergeRepository,
    internal val hitObservationRepository: HitObservationRepository,
    private val imageCompressor: ImageCompressor,
    private val undoController: UndoController,
    private val magicSources: MagicSources,
) : Presenter<CombatRoundState> {
    private val editLock = Mutex()

    @Suppress("LongMethod", "CyclomaticComplexMethod")
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
            remember(combatId, combatRepository, codexRepositories, undoController, scope) {
                RoundEditor(combatId, combatRepository, codexRepositories, editLock, undoController, {
                    toast.value =
                        ToastMessage(it)
                }) { block ->
                    scope.launch { block() }
                }
            }
        val form =
            rememberRoundForm(
                codexRepositories,
                editor,
                imageCompressor,
                data.party,
                data.npcs,
                data.species,
                undoController,
            )
        val learner =
            VulnerabilityLearner(
                codexRepositories.npcRepository,
                combatRepository,
                undoController,
            )
        val handler =
            RoundHandler(
                editor,
                navigator,
                drafts.selected,
                drafts.tab,
                drafts.menuOpen,
                drafts.hpPad,
                drafts.search,
                form,
                scope,
            )
        val namingHandler =
            NamingHandler(
                editor,
                drafts.selected,
                drafts.naming,
                drafts.promotion,
                NpcPromotion(combatRepository, codexRepositories.npcRepository, codexRepositories.speciesRepository),
                combatRepository,
                undoController,
            )
        val identityHandler =
            remember(editor, drafts, codexRepositories, npcMerge, combatRepository, undoController) {
                IdentityHandler(
                    editor,
                    drafts.selected,
                    drafts.tab,
                    drafts.identity,
                    codexRepositories.npcRepository,
                    npcMerge,
                    combatRepository,
                    undoController,
                )
            }
        val namingSnapshot = NamingSnapshot(data.species)
        val queueHandler = QueueHandler(editor, combatRepository, drafts.queue, undoController, drafts.selected)
        val markHandler = MarkHandler(editor, drafts.selected, drafts.mark, combatRepository)
        val shieldEditing = ShieldEditing(editor, learner, drafts.shieldEdit)
        val featuresHandler =
            FeaturesHandler(
                editor,
                drafts.selected,
                drafts.features,
                codexRepositories.npcRepository,
                combatRepository,
                undoController,
            )
        val calcHandler = CalcHandler(editor, drafts.calc, learner)
        val summaryHandler = SummaryHandler(editor, drafts.summary, learner)
        val own = ordered.firstOrNull { m -> data.party.any { it.id == m.partyMemberId && it.isPlayerCharacter } }
        val magic = rememberRoundMagic(magicSources, data.party, own, current, combatRepository, undoController, scope)
        val ownIds =
            ordered
                .filter { m ->
                    data.party.any { it.id == m.partyMemberId && it.isPlayerCharacter }
                }.map { it.id }
                .toSet()
        val speciesOf = speciesOfMembers(ordered, data.npcs, data.species)
        val resistances = resistancesOf(ordered, data.npcs, speciesOf)
        val snapshot = RoundSnapshot(drafts.selected.value, ownIds)
        val calcSnapshot = CalcSnapshot(ordered, mover?.id, resistances, speciesOf, words.typeNames, words.stateTitles)
        val summarySnapshot = SummarySnapshot(speciesOf, words.typeNames, words.effectWords)
        val nameOf = { key: String ->
            data.npcs.firstOrNull { npcKey(it.id) == key }?.name
                ?: data.species.firstOrNull { speciesKey(it.id) == key }?.name
        }
        val sink: (CombatRoundEvent) -> Unit = { event ->
            when (event) {
                is CombatRoundEvent.Spells -> magic.handle(event.event)
                is CombatRoundEvent.Calc -> calcHandler.onEvent(event.event, calcSnapshot, drafts.selected.value)
                is CombatRoundEvent.Summary -> summaryHandler.onEvent(event.event, summarySnapshot)
                is CombatRoundEvent.QueueEvent -> queueHandler.onEvent(event)
                is CombatRoundEvent.MarkEvent -> markHandler.onEvent(event, data.customs)
                is CombatRoundEvent.ShieldTapped ->
                    shieldEditing.tap(
                        drafts.selected.value,
                        event.type,
                        speciesOf[drafts.selected.value]?.vulnerabilities.orEmpty(),
                    )
                CombatRoundEvent.ShieldsEditStarted -> shieldEditing.start(drafts.selected.value)
                CombatRoundEvent.ShieldsEditDone -> shieldEditing.done()
                is CombatRoundEvent.FeaturesOpened -> {
                    shieldEditing.start(drafts.selected.value)
                    featuresHandler.onEvent(event)
                }
                is CombatRoundEvent.FeaturesClosed -> {
                    featuresHandler.onEvent(event)
                    shieldEditing.done()
                }
                is CombatRoundEvent.FeaturesEvent -> featuresHandler.onEvent(event)
                is CombatRoundEvent.NameChanged,
                is CombatRoundEvent.NameChosen,
                is CombatRoundEvent.PromotionScopeChanged,
                CombatRoundEvent.PromotionConfirmed,
                CombatRoundEvent.PromotionRejected,
                -> namingHandler.onEvent(event, namingSnapshot)
                CombatRoundEvent.NameTapped,
                is CombatRoundEvent.RenameChanged,
                CombatRoundEvent.RenameConfirmed,
                CombatRoundEvent.ClassOpened,
                is CombatRoundEvent.ClassChosen,
                CombatRoundEvent.ClassClosed,
                CombatRoundEvent.LinkOpened,
                CombatRoundEvent.LinkClosed,
                CombatRoundEvent.MergeConfirmed,
                -> identityHandler.onEvent(event, data.npcs)
                is CombatRoundEvent.LinkPicked -> {
                    identityHandler.onEvent(CombatRoundEvent.LinkClosed, data.npcs)
                    namingHandler.onEvent(
                        CombatRoundEvent.NameChosen(NameChoice.Existing(event.npcId, event.name)),
                        namingSnapshot,
                    )
                }
                is CombatRoundEvent.VulnerabilityChanged ->
                    shieldEditing.set(drafts.selected.value, event.type, event.state)
                else -> handler.onEvent(event, snapshot, nameOf)
            }
        }

        if (current == null) return CombatRoundState(eventSink = sink)
        val view = RoundView(data, current, ordered, mover, ownIds, speciesOf, resistances, form, magic)
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
    val form: RoundForm,
    val magic: RoundMagic,
)

@Composable
private fun RoundView.panelOf(
    member: Combatant,
    drafts: RoundDrafts,
    seenByMember: Map<Long, List<SpeciesHitObservation>>,
    typeNames: Map<DamageType, String>,
    magic: SpellsBlockState?,
): RoundPanel {
    val typed =
        drafts.naming.value
            ?.takeIf { it.memberId == member.id }
            ?.text
            .orEmpty()
    val focus = drafts.identity.value?.let { it.memberId == member.id && it.focus } == true
    val role = roleOf(member, RoleSources(data.party, data.npcs, speciesOf))
    return roundPanel(
        current,
        member,
        role,
        mover?.name.orEmpty(),
        seenByMember[member.id].orEmpty(),
        PanelKin(
            species = speciesOf[member.id],
            npc = member.npcId?.let { id -> data.npcs.firstOrNull { it.id == id } },
            vulnerabilities = resistances[member.id].orEmpty(),
            naming = namingState(current, member, typed, data.npcs, data.species, data.learned, typeNames, focus),
            identity =
                identityState(
                    member,
                    role,
                    drafts.identity.value,
                    data.npcs,
                    data.species,
                    IdentityFacts(current, data.learned, typeNames),
                ),
        ),
        data.customs,
    ).copy(magic = magic)
}

@Composable
private fun RoundView.promotionOf(
    draft: PromotionDraft,
    typeNames: Map<DamageType, String>,
): PromotionSheet? {
    val member = current.combatants.firstOrNull { it.id == draft.trace.memberId }
    val npc = data.npcs.firstOrNull { it.id == draft.trace.npcId }
    return promotionSheet(draft, current, npc, data.species.firstOrNull { it.id == member?.speciesId }, typeNames)
}

@Suppress("LongMethod")
@Composable
private fun RoundView.render(
    drafts: RoundDrafts,
    toast: ToastMessage?,
    sink: (CombatRoundEvent) -> Unit,
): CombatRoundState {
    val sources = RoleSources(data.party, data.npcs, speciesOf)
    val typeNames = damageTypeLabels().mapValues { it.value.lowercase() }
    val seenByMember = speciesOf.mapValues { (_, species) -> data.seen[species.id].orEmpty() }
    val chosen = ordered.firstOrNull { it.id == drafts.selected.value }
    val hostile = ordered.hostileCount()
    val learners = speciesOf.filterKeys { id -> ordered.any { it.id == id && it.side != CombatSide.PARTY } }.keys
    val calcContext = CalcContext(ordered, mover, ownIds, resistances, learners)
    return CombatRoundState(
        isLoading = false,
        round = current.round,
        canGoBack = previousTurn(ordered, TurnPosition(current.round, current.turn)) != null,
        currentName = mover?.name.orEmpty(),
        currentBadge = current.combatants.queueSlots()[mover?.id].badgeText(),
        currentPending = mover?.turnOrder == null,
        hostile = hostileLine(current, hostile),
        allClear = hostile == 0,
        rows = roundRows(current, ordered, mover?.id, drafts.selected.value, sources, seenByMember),
        queueUnnamed = current.combatants.queueStatus() == QueueStatus.Unnamed,
        queueEditing = drafts.queue.value != null,
        panel = chosen?.let { panelOf(it, drafts, seenByMember, typeNames, magic.stateFor(it.id)) },
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
        promotion = drafts.promotion.value?.let { promotionOf(it, typeNames) },
        classSheet =
            chosen?.let {
                drafts.identity.value.classSelect(
                    it.id,
                    partsOf(
                        it,
                        data.npcs.firstOrNull { npc ->
                            npc.id ==
                                it.npcId
                        },
                    ),
                )
            },
        markSheet = markSheetFor(drafts.mark.value, current),
        featuresSheet = featuresSheetFor(drafts.features.value, current, data.npcs, resistances),
        linkSheet =
            chosen
                ?.takeIf { drafts.identity.value?.let { d -> d.memberId == it.id && d.linkOpen } == true }
                ?.let { linkSheet(it, current, data.npcs, data.species) },
        entryForm = form.state,
        formKinds = form.kinds,
        calc = drafts.calc.value?.let { calcState(it, calcContext, current.round) },
        summary = drafts.summary.value?.let { summaryState(it, current, learners) },
        roundDamage = roundDamage(current),
        toast = toast,
        eventSink = sink,
    )
}

/** under the name of the one who moves: the hostile count, or that the turns go by the list */
@Composable
private fun hostileLine(
    combat: Combat,
    hostile: Int,
): String =
    if (combat.combatants.queueStatus() == QueueStatus.Unnamed) {
        stringResource(Res.string.combat_queue_unnamed_line)
    } else {
        hostileText(hostile)
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
