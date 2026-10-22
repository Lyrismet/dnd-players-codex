package com.lyrismet.incadent.feature.combat.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.file.ImportSource
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import com.lyrismet.incadent.feature.campaign.presentation.sheet.rememberCampaignSheetHost
import com.lyrismet.incadent.feature.combat.domain.grouping.EnemyChip
import com.lyrismet.incadent.feature.combat.domain.grouping.enemyChips
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.repository.SessionNoteRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_list_prep_enemies
import dndplayerscodex.shared.generated.resources.combat_list_prep_no_enemies
import dndplayerscodex.shared.generated.resources.combat_list_prep_with_allies
import dndplayerscodex.shared.generated.resources.combat_list_rounds
import dndplayerscodex.shared.generated.resources.combat_list_row_meta
import dndplayerscodex.shared.generated.resources.combat_list_session_group
import dndplayerscodex.shared.generated.resources.combat_list_target_new
import dndplayerscodex.shared.generated.resources.combat_list_target_session
import dndplayerscodex.shared.generated.resources.error_action_failed
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private const val NON_BREAKING_HYPHEN = '‑'

class CombatListPresenter(
    private val combatRepository: CombatRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val campaignRepository: CampaignRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val importSource: ImportSource,
) : Presenter<CombatListState> {
    @Composable
    override fun present(): CombatListState {
        val combats by combatRepository.observeAll().collectAsState(initial = emptyList())
        val sessions by sessionNoteRepository.observeAll().collectAsState(initial = emptyList())
        val numbering by appPreferencesRepository
            .observeSessionNumbering()
            .collectAsState(initial = SessionNumbering.ROMAN)
        val toast = remember { mutableStateOf<ToastMessage?>(null) }
        toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val scope = rememberGuardedScope { toast.value = ToastMessage(failedText) }
        val campaignHost =
            rememberCampaignSheetHost(campaignRepository, importSource) { text -> toast.value = ToastMessage(text) }
        val campaignArchived = campaignHost.current?.status == CampaignStatus.ARCHIVED

        val groups = journalGroups(combats, sessions, numbering)
        val draft = combats.firstOrNull { it.phase == CombatPhase.PREPARING && it.combatants.isNotEmpty() }
        val footer =
            when {
                campaignArchived -> CombatFooter.Hidden
                draft != null -> CombatFooter.Prepared(prepSummary(draft))
                else -> CombatFooter.Start(startTarget(sessions, numbering))
            }

        return CombatListState(
            campaignName = campaignHost.current?.name.orEmpty(),
            campaignId = campaignHost.current?.id,
            campaignArchived = campaignArchived,
            campaignSheet = campaignHost.sheet,
            groups = groups,
            footer = footer,
            toast = toast.value,
        ) { event ->
            when (event) {
                // the preparation screen opens from here once it exists, for now the draft is only kept
                CombatListEvent.StartClicked, CombatListEvent.ContinueClicked ->
                    if (!campaignArchived && combats.none { it.phase == CombatPhase.PREPARING }) {
                        scope.launch {
                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            combatRepository.create(sessionNoteId = null, startedAt = now)
                        }
                    }
                CombatListEvent.ResetClicked ->
                    scope.launch {
                        combats.filter { it.phase == CombatPhase.PREPARING }.forEach { combatRepository.delete(it.id) }
                    }
                is CombatListEvent.CampaignSheet -> campaignHost.onEvent(event.event)
            }
        }
    }

    @Composable
    private fun journalGroups(
        combats: List<Combat>,
        sessions: List<SessionNote>,
        numbering: SessionNumbering,
    ): List<CombatGroupItem> {
        val labels = sessions.sessionNumberLabels(numbering)
        val finished = combats.filter { it.phase == CombatPhase.FINISHED && it.outcome != null }
        return sessions
            .sortedByDescending { it.sessionDate }
            .mapNotNull { session ->
                val rows = finished.filter { it.sessionNoteId == session.id }.map { it.toRow() }
                if (rows.isEmpty()) return@mapNotNull null
                val label =
                    stringResource(
                        Res.string.combat_list_session_group,
                        labels.getValue(session.id),
                        session.title,
                    ).uppercase()
                CombatGroupItem(label, rows)
            }
    }

    @Composable
    private fun Combat.toRow(): CombatRowItem {
        val rounds = pluralStringResource(Res.plurals.combat_list_rounds, round, round)
        val time = startedAt.toDisplayTime()
        return CombatRowItem(
            id = id,
            foes = combatants.enemyChips().joinToString(", ") { it.label() },
            meta = stringResource(Res.string.combat_list_row_meta, rounds, time),
            outcome = checkNotNull(outcome),
        )
    }

    @Composable
    private fun startTarget(
        sessions: List<SessionNote>,
        numbering: SessionNumbering,
    ): String {
        val live = sessions.firstOrNull { it.isLive }
        return if (live == null) {
            stringResource(Res.string.combat_list_target_new)
        } else {
            stringResource(
                Res.string.combat_list_target_session,
                sessions.sessionNumberLabels(numbering).getValue(live.id),
            )
        }
    }

    @Composable
    private fun prepSummary(draft: Combat): String {
        val enemies = draft.combatants.enemyChips()
        val base =
            if (enemies.isEmpty()) {
                stringResource(Res.string.combat_list_prep_no_enemies)
            } else {
                stringResource(Res.string.combat_list_prep_enemies, enemies.joinToString(", ") { it.summary() })
            }
        val allies = draft.combatants.count { it.side == CombatSide.ALLY }
        return if (allies == 0) base else stringResource(Res.string.combat_list_prep_with_allies, base, allies)
    }
}

// a numbered group reads with the hollow dot and a lone one with the filled dot, the name never breaks at a hyphen
private fun EnemyChip.label(): String =
    (if (several) "○ " else "● ") + name.replace('-', NON_BREAKING_HYPHEN) + if (count > 1) " ×$count" else ""

private fun EnemyChip.summary(): String = name + if (count > 1) " ×$count" else ""
