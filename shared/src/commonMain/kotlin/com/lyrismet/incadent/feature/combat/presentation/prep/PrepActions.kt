package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.calc.CalcExpression
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.preparation.CombatPreparation
import com.lyrismet.incadent.feature.combat.domain.preparation.MemberSource
import com.lyrismet.incadent.feature.combat.domain.preparation.PartyHp
import com.lyrismet.incadent.feature.combat.domain.preparation.PartySeat
import com.lyrismet.incadent.feature.combat.domain.preparation.isMadeOf
import com.lyrismet.incadent.feature.combat.domain.preparation.setCurrentHp
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.RoundActions
import com.lyrismet.incadent.feature.combat.domain.round.StartReadiness
import com.lyrismet.incadent.feature.combat.domain.round.startReadiness
import com.lyrismet.incadent.feature.combat.domain.round.toggledTurnOrder
import com.lyrismet.incadent.feature.combat.presentation.round.CombatRoundScreen
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** what the event handlers need to know of the screen at the moment of the event */
internal class PrepSnapshot(
    val party: List<PartyMember>,
    // the hit points of the player's own character, followed by numbers once the fight starts
    val ownHp: Map<Long, PartyHp>,
    val npcs: List<Npc>,
    val species: List<Species>,
    val hasOthers: Boolean,
) {
    fun nameOf(key: String): String? =
        npcs.firstOrNull { npcKey(it.id) == key }?.name ?: species.firstOrNull { speciesKey(it.id) == key }?.name
}

/** the writes of the preparation, each reads the combat again under a lock so quick taps never number a stale list */
internal class PrepEditor(
    val combatId: Long,
    private val combatRepository: CombatRepository,
    private val codexRepositories: CodexRepositories,
    private val lock: Mutex,
    private val launch: (suspend () -> Unit) -> Unit,
) : FoeAdder {
    private val preparation = CombatPreparation(combatRepository)

    fun edit(block: suspend (Combat) -> Unit) {
        launch { lock.withLock { combatRepository.observe(combatId).first()?.let { block(it) } } }
    }

    fun togglePartyMember(member: PartyMember) =
        edit { combat ->
            val included = combat.combatants.any { it.partyMemberId == member.id }
            preparation.setPartyIncluded(combat, PartySeat(member.id, member.name), !included)
        }

    override fun add(
        side: CombatSide,
        source: MemberSource,
    ) = edit { preparation.add(it, side, source) }

    override fun removeOne(
        side: CombatSide,
        source: MemberSource,
    ) = edit { preparation.removeOne(it, side, source) }

    /** a named npc is taken out again when it is already there, a species or a summon is added once more */
    override fun toggle(
        side: CombatSide,
        source: MemberSource,
    ) = edit { combat ->
        val present = combat.combatants.any { it.side == side && it.isMadeOf(source) }
        if (source.npcId != null && present) {
            preparation.removeAll(combat, side, source)
        } else {
            preparation.add(combat, side, source)
        }
    }

    override fun addSaved(
        side: CombatSide,
        kind: EntityKind,
        id: Long,
    ) = edit { combat ->
        when (kind) {
            EntityKind.NPC ->
                codexRepositories.npcRepository.getById(id)?.let {
                    preparation.add(combat, side, MemberSource(id, null, it.name))
                }
            EntityKind.SPECIES ->
                codexRepositories.speciesRepository.getById(id)?.let {
                    preparation.add(combat, side, MemberSource(null, id, it.name))
                }
            EntityKind.PARTY ->
                codexRepositories.partyRepository.getById(id)?.let {
                    preparation.setPartyIncluded(combat, PartySeat(id, it.name), included = true)
                }
            EntityKind.QUEST, EntityKind.LOCATION -> Unit
        }
    }

    /** the character's current hit points are the codex's, the temporary ones belong to this fight */
    fun applyHp(
        member: PartyMember,
        mode: HpMode,
        value: Int,
    ) = edit { combat ->
        when (mode) {
            HpMode.HP -> {
                val hp = setCurrentHp(member.hpMax, value)
                codexRepositories.partyRepository.upsert(member.copy(hpCurrent = hp.current, hpMax = hp.max))
            }
            HpMode.TEMP -> {
                val seat = PartySeat(member.id, member.name)
                if (combat.combatants.none { it.partyMemberId == member.id }) {
                    preparation.setPartyIncluded(
                        combat,
                        seat,
                        true,
                    )
                }
                val seated =
                    combatRepository.observe(combatId).first()?.combatants?.firstOrNull {
                        it.partyMemberId ==
                            member.id
                    }
                seated?.let { combatRepository.setTempHp(it.id, value) }
            }
        }
    }

    fun tapOrder(memberId: Long) =
        edit { combat ->
            val member = combat.combatants.firstOrNull { it.id == memberId } ?: return@edit
            combatRepository.setTurnOrder(memberId, toggledTurnOrder(combat.combatants, member))
        }

    fun resetOrder() = edit { combatRepository.clearTurnOrders(combatId) }

    /** the fight begins once every participant has a place, [onStarted] runs after it was written */
    fun start(
        partyHp: Map<Long, PartyHp>,
        onStarted: () -> Unit,
    ) = edit { combat ->
        if (startReadiness(combat.combatants) is StartReadiness.Ready) {
            RoundActions(
                combatRepository,
            ).begin(combat, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), partyHp)
            onStarted()
        }
    }

    fun discard(onDone: () -> Unit) {
        launch {
            combatRepository.delete(combatId)
            onDone()
        }
    }
}

/** what the keypad of the hit points has typed so far */
internal data class HpPadDraft(
    val partyMemberId: Long,
    val mode: HpMode = HpMode.HP,
    val expr: CalcExpression = CalcExpression(),
)

internal class PrepActions(
    private val editor: PrepEditor,
    private val navigator: Navigator,
    search: MutableState<SearchDraft?>,
    private val hpPad: MutableState<HpPadDraft?>,
    private val exitConfirmVisible: MutableState<Boolean>,
    onCreate: (CombatSide, String) -> Unit,
) {
    private val searchController = FoeSearchController(search, editor, onCreate)

    fun onMember(
        event: CombatPrepEvent.MemberEvent,
        snapshot: PrepSnapshot,
    ) {
        when (event) {
            is CombatPrepEvent.PartyToggled ->
                snapshot.party.firstOrNull { it.id == event.partyMemberId }?.let(editor::togglePartyMember)
            is CombatPrepEvent.MemberIncreased ->
                sourceOf(event.key, snapshot::nameOf)?.let { editor.add(event.side.toCombatSide(), it) }
            is CombatPrepEvent.MemberDecreased ->
                sourceOf(event.key, snapshot::nameOf)?.let { editor.removeOne(event.side.toCombatSide(), it) }
            is CombatPrepEvent.OrderTapped -> editor.tapOrder(event.memberId)
            CombatPrepEvent.OrderReset -> editor.resetOrder()
        }
    }

    fun onStart(snapshot: PrepSnapshot) {
        editor.start(snapshot.ownHp) { navigator.resetRoot(CombatRoundScreen(editor.combatId)) }
    }

    fun onSearch(
        event: CombatPrepEvent.SearchEvent,
        snapshot: PrepSnapshot,
    ) = searchController.onEvent(event, snapshot::nameOf)

    fun onHp(
        event: CombatPrepEvent.HpEvent,
        snapshot: PrepSnapshot,
    ) {
        val draft = hpPad.value
        when (event) {
            is CombatPrepEvent.HpPadOpened -> hpPad.value = HpPadDraft(event.partyMemberId)
            is CombatPrepEvent.HpModeChanged -> hpPad.value = draft?.copy(mode = event.mode, expr = CalcExpression())
            is CombatPrepEvent.HpDigit -> hpPad.value = draft?.copy(expr = draft.expr.digit(event.digit))
            CombatPrepEvent.HpBackspace -> hpPad.value = draft?.copy(expr = draft.expr.backspace())
            CombatPrepEvent.HpCleared -> hpPad.value = draft?.copy(expr = CalcExpression())
            CombatPrepEvent.HpPadClosed -> hpPad.value = null
            CombatPrepEvent.HpApplied ->
                draft?.takeIf { it.expr.raw.isNotEmpty() }?.let { d ->
                    snapshot.party.firstOrNull { it.id == d.partyMemberId }?.let {
                        editor.applyHp(
                            it,
                            d.mode,
                            d.expr.value(),
                        )
                    }
                    hpPad.value = null
                }
        }
    }

    fun onExit(
        event: CombatPrepEvent.ExitEvent,
        snapshot: PrepSnapshot,
    ) {
        when (event) {
            CombatPrepEvent.CloseClicked -> if (snapshot.hasOthers) exitConfirmVisible.value = true else navigator.pop()
            CombatPrepEvent.ExitConfirmed -> {
                exitConfirmVisible.value = false
                editor.discard { navigator.pop() }
            }
            CombatPrepEvent.ExitDismissed -> exitConfirmVisible.value = false
        }
    }
}
