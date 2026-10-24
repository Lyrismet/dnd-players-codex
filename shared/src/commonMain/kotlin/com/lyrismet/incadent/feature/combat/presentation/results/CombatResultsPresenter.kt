package com.lyrismet.incadent.feature.combat.presentation.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.domain.grouping.enemyTitle
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.repository.HitObservationRepository
import com.lyrismet.incadent.feature.combat.domain.results.CodexEntry
import com.lyrismet.incadent.feature.combat.domain.results.codexChecklist
import com.lyrismet.incadent.feature.combat.domain.results.minutesUntil
import com.lyrismet.incadent.feature.combat.domain.round.autoOutcome
import com.lyrismet.incadent.feature.combat.presentation.combatSessionTarget
import com.lyrismet.incadent.feature.combat.presentation.list.CombatListScreen
import com.lyrismet.incadent.feature.combat.presentation.summary.summaryOf
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import com.lyrismet.incadent.feature.session.presentation.route.SessionDetailScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_list_rounds
import dndplayerscodex.shared.generated.resources.combat_results_deleted
import dndplayerscodex.shared.generated.resources.combat_results_meta
import dndplayerscodex.shared.generated.resources.combat_results_minutes
import dndplayerscodex.shared.generated.resources.combat_results_saved_codex
import dndplayerscodex.shared.generated.resources.combat_results_saved_in
import dndplayerscodex.shared.generated.resources.combat_results_saved_plain
import dndplayerscodex.shared.generated.resources.combat_results_saved_title
import dndplayerscodex.shared.generated.resources.combat_results_target
import dndplayerscodex.shared.generated.resources.combat_target_session_titled
import dndplayerscodex.shared.generated.resources.error_action_failed
import dndplayerscodex.shared.generated.resources.new_session_default_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** what the screen keeps of its own while it is open: the outcome picked, the ticks changed, the open dialogs */
@Suppress("LongParameterList")
private class ResultsDrafts(
    val chosen: MutableState<CombatOutcome?>,
    val logExpanded: MutableState<Boolean>,
    val deleteConfirm: MutableState<Boolean>,
    val saving: MutableState<Boolean>,
    // the ticks the player turned away from the proposal, either way
    val unticked: MutableState<Set<String>>,
    val ticked: MutableState<Set<String>>,
    val toast: MutableState<ToastMessage?>,
)

@Composable
private fun rememberDrafts() =
    ResultsDrafts(
        chosen = remember { mutableStateOf(null) },
        logExpanded = remember { mutableStateOf(false) },
        deleteConfirm = remember { mutableStateOf(false) },
        saving = remember { mutableStateOf(false) },
        unticked = remember { mutableStateOf(emptySet()) },
        ticked = remember { mutableStateOf(emptySet()) },
        toast = remember { mutableStateOf(null) },
    )

/** the texts the saving and the deleting say, resolved once */
private class ResultsWords(
    val sessionTitle: String,
    val deleted: String,
    val savedTitle: String,
    val savedCodex: String,
    val savedPlain: String,
)

@Composable
private fun rememberWords() =
    ResultsWords(
        sessionTitle = stringResource(Res.string.new_session_default_title),
        deleted = stringResource(Res.string.combat_results_deleted),
        savedTitle = stringResource(Res.string.combat_results_saved_title),
        savedCodex = stringResource(Res.string.combat_results_saved_codex),
        savedPlain = stringResource(Res.string.combat_results_saved_plain),
    )

@Suppress("LongParameterList")
class CombatResultsPresenter(
    private val screen: CombatResultsScreen,
    private val navigator: Navigator,
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    hitObservations: HitObservationRepository,
    private val codexRepositories: CodexRepositories,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val undo: UndoController,
) : Presenter<CombatResultsState> {
    private val saver = ResultsSaver(combatRepository, sessionNoteRepository, hitObservations, codexRepositories)

    @Composable
    override fun present(): CombatResultsState {
        val combat by combatRepository.observe(screen.combatId).collectAsState(initial = null)
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val species by codexRepositories.speciesRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val drafts = rememberDrafts()
        drafts.toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val scope =
            rememberGuardedScope {
                drafts.saving.value = false
                drafts.toast.value = ToastMessage(failedText)
            }
        val words = rememberWords()

        val current =
            combat ?: return CombatResultsState(readOnly = screen.readOnly, toast = drafts.toast.value) { event ->
                if (event == CombatResultsEvent.BackClicked) navigator.pop()
            }
        val suggested = if (screen.interrupted) CombatOutcome.INTERRUPTED else autoOutcome(current.combatants)
        val outcome = current.outcome?.takeIf { screen.readOnly } ?: drafts.chosen.value ?: suggested
        val entries = remember(current, species) { codexChecklist(current, species.associateBy { it.id }) }
        val checklist =
            entries.map { entry ->
                val key = entry.item.key
                entry.copy(checked = if (entry.checked) key !in drafts.unticked.value else key in drafts.ticked.value)
            }
        val handler =
            ResultsHandler(
                ResultsContext(current, outcome, entries, checklist, words),
                drafts,
                ResultsActions(navigator, combatRepository, saver, undo, scope),
            )
        val target = resultsTarget(current, sessions, numbering)
        return CombatResultsState(
            loaded = true,
            readOnly = screen.readOnly,
            title = current.combatants.enemyTitle(),
            meta = metaOf(current),
            targetPrefix = target.first,
            target = target.second,
            suggested = suggested,
            outcome = outcome,
            summary = remember(current) { summaryOf(current) },
            logExpanded = drafts.logExpanded.value,
            checklist = if (screen.readOnly) emptyList() else checklist,
            deleteConfirmVisible = drafts.deleteConfirm.value,
            saving = drafts.saving.value,
            toast = drafts.toast.value,
            eventSink = handler::onEvent,
        )
    }

    /** "Сохранится в:" with the session, "Сохранён в:" once it is saved */
    @Composable
    private fun resultsTarget(
        combat: Combat,
        sessions: List<SessionNote>,
        numbering: SessionNumbering,
    ): Pair<String, String> {
        if (!screen.readOnly) {
            val target = combatSessionTarget(sessions, numbering, withTitle = true)
            return stringResource(Res.string.combat_results_target) to target.session
        }
        val session = sessions.firstOrNull { it.id == combat.sessionNoteId }
        val value =
            session
                ?.let {
                    stringResource(
                        Res.string.combat_target_session_titled,
                        sessions.sessionNumberLabels(numbering).getValue(it.id),
                        it.title,
                    )
                }.orEmpty()
        return stringResource(Res.string.combat_results_saved_in) to value
    }

    @Composable
    private fun metaOf(combat: Combat): String {
        val rounds = pluralStringResource(Res.plurals.combat_list_rounds, combat.round, combat.round)
        val minutes = combat.minutesUntil(combat.finishedAt ?: now())
        val time = stringResource(Res.string.combat_results_minutes, minutes)
        return stringResource(Res.string.combat_results_meta, rounds, time)
    }
}

/** the fight as it stands and what the ticks and the words say about saving it */
private class ResultsContext(
    val combat: Combat,
    val outcome: CombatOutcome,
    val entries: List<CodexEntry>,
    val checklist: List<CodexEntry>,
    val words: ResultsWords,
)

private class ResultsActions(
    val navigator: Navigator,
    val combats: CombatRepository,
    val saver: ResultsSaver,
    val undo: UndoController,
    val scope: CoroutineScope,
)

/** reacts to what the player does on the results */
private class ResultsHandler(
    private val context: ResultsContext,
    private val drafts: ResultsDrafts,
    private val actions: ResultsActions,
) {
    fun onEvent(event: CombatResultsEvent) {
        when (event) {
            is CombatResultsEvent.OutcomeSelected -> drafts.chosen.value = event.outcome
            CombatResultsEvent.LogToggled -> drafts.logExpanded.value = !drafts.logExpanded.value
            is CombatResultsEvent.ItemToggled -> toggle(event.key)
            CombatResultsEvent.SaveClicked -> save()
            CombatResultsEvent.BackClicked -> actions.navigator.pop()
            CombatResultsEvent.OpenSessionClicked ->
                context.combat.sessionNoteId?.let { actions.navigator.goTo(SessionDetailScreen(it)) }
            CombatResultsEvent.DeleteClicked -> drafts.deleteConfirm.value = true
            CombatResultsEvent.DeleteDismissed -> drafts.deleteConfirm.value = false
            CombatResultsEvent.DeleteConfirmed -> delete()
        }
    }

    private fun toggle(key: String) {
        val entry = context.entries.firstOrNull { it.item.key == key } ?: return
        if (entry.checked) {
            drafts.unticked.value = drafts.unticked.value.toggled(key)
        } else {
            drafts.ticked.value = drafts.ticked.value.toggled(key)
        }
    }

    private fun save() {
        if (drafts.saving.value) return
        drafts.saving.value = true
        actions.scope.launchSave()
    }

    private fun CoroutineScope.launchSave() {
        launch {
            val ticked = context.checklist.filter(CodexEntry::checked)
            val saved =
                actions.saver.save(
                    context.combat,
                    context.outcome,
                    ticked.map {
                        it.item
                    },
                    context.words.sessionTitle,
                    now(),
                )
            val note = if (ticked.isEmpty()) context.words.savedPlain else context.words.savedCodex
            actions.undo.show(context.words.savedTitle, note) { saved.undo() }
            actions.navigator.resetRoot(CombatListScreen)
            actions.navigator.goTo(SessionDetailScreen(saved.sessionId))
        }
    }

    private fun delete() {
        drafts.deleteConfirm.value = false
        actions.scope.launchDelete()
    }

    private fun CoroutineScope.launchDelete() {
        launch {
            actions.combats.delete(context.combat.id)
            drafts.toast.value = ToastMessage(context.words.deleted)
            delay(DELETED_TOAST_MILLIS)
            actions.navigator.resetRoot(CombatListScreen)
        }
    }
}

private const val DELETED_TOAST_MILLIS = 900L

private fun Set<String>.toggled(key: String): Set<String> = if (key in this) this - key else this + key

private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
