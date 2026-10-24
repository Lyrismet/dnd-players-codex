package com.lyrismet.incadent.feature.combat.presentation.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppCheckRow
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppLoader
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.presentation.style.DamageTypeShield
import com.lyrismet.incadent.feature.codex.presentation.summary.damageTypeLabels
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.results.CodexEntry
import com.lyrismet.incadent.feature.combat.domain.results.CodexItem
import com.lyrismet.incadent.feature.combat.presentation.summary.CombatSummaryBody
import com.lyrismet.incadent.feature.combat.presentation.summary.label
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_damage_state_immunity
import dndplayerscodex.shared.generated.resources.combat_damage_state_normal
import dndplayerscodex.shared.generated.resources.combat_damage_state_resistance
import dndplayerscodex.shared.generated.resources.combat_damage_state_vulnerability
import dndplayerscodex.shared.generated.resources.combat_results_back
import dndplayerscodex.shared.generated.resources.combat_results_codex
import dndplayerscodex.shared.generated.resources.combat_results_delete
import dndplayerscodex.shared.generated.resources.combat_results_delete_confirm
import dndplayerscodex.shared.generated.resources.combat_results_delete_keep
import dndplayerscodex.shared.generated.resources.combat_results_delete_text
import dndplayerscodex.shared.generated.resources.combat_results_delete_title
import dndplayerscodex.shared.generated.resources.combat_results_item_alive
import dndplayerscodex.shared.generated.resources.combat_results_item_dead
import dndplayerscodex.shared.generated.resources.combat_results_item_dead_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_fallen
import dndplayerscodex.shared.generated.resources.combat_results_item_fallen_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_fell
import dndplayerscodex.shared.generated.resources.combat_results_item_hits_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_keep
import dndplayerscodex.shared.generated.resources.combat_results_item_keep_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_party_fallen
import dndplayerscodex.shared.generated.resources.combat_results_item_party_fallen_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_resistance
import dndplayerscodex.shared.generated.resources.combat_results_item_resistance_sub
import dndplayerscodex.shared.generated.resources.combat_results_item_resistance_weak
import dndplayerscodex.shared.generated.resources.combat_results_note
import dndplayerscodex.shared.generated.resources.combat_results_open_session
import dndplayerscodex.shared.generated.resources.combat_results_outcome
import dndplayerscodex.shared.generated.resources.combat_results_overline
import dndplayerscodex.shared.generated.resources.combat_results_save
import dndplayerscodex.shared.generated.resources.combat_results_suggested
import org.jetbrains.compose.resources.stringResource

private val OutcomeOrder =
    listOf(CombatOutcome.VICTORY, CombatOutcome.RETREAT, CombatOutcome.PEACE, CombatOutcome.INTERRUPTED)

@Composable
fun CombatResultsUi(
    state: CombatResultsState,
    modifier: Modifier = Modifier,
) {
    val send = state.eventSink
    val insets =
        WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
        )
    Scaffold(modifier = modifier, contentWindowInsets = insets) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (state.loaded && state.summary != null) {
                ResultsBody(state)
            } else {
                AppLoader(modifier = Modifier.align(Alignment.Center))
            }
            AppToast(text = state.toast?.text, modifier = floatingBarAnchor())
        }
    }
    if (state.deleteConfirmVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.combat_results_delete_title),
            text = stringResource(Res.string.combat_results_delete_text),
            onConfirm = { send(CombatResultsEvent.DeleteConfirmed) },
            onDismiss = { send(CombatResultsEvent.DeleteDismissed) },
            confirmLabel = stringResource(Res.string.combat_results_delete_confirm),
            dismissLabel = stringResource(Res.string.combat_results_delete_keep),
        )
    }
}

@Composable
private fun ResultsBody(state: CombatResultsState) {
    val send = state.eventSink
    val summary = state.summary ?: return
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResultsHeader(state)
        OutcomeRow(state)
        if (state.checklist.isNotEmpty()) {
            CodexChecklistCard(
                state.checklist,
            ) { send(CombatResultsEvent.ItemToggled(it)) }
        }
        CombatSummaryBody(summary, state.logExpanded, { send(CombatResultsEvent.LogToggled) })
        if (!state.readOnly) {
            Text(
                text = stringResource(Res.string.combat_results_note),
                color = AppPalette.TextTertiary,
                style = AppText.footnote,
            )
        }
        ResultsActions(state)
    }
}

@Composable
private fun ResultsHeader(state: CombatResultsState) {
    val send = state.eventSink
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            SectionOverline(
                text = stringResource(Res.string.combat_results_overline) + " · " + state.meta.uppercase(),
                color = AppPalette.MaroonBright,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = AppPalette.TextHeading,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = state.targetPrefix, color = AppPalette.TextSecondary, style = AppText.rowSubtitle)
                Text(text = state.target, color = AppPalette.GoldBright, style = AppText.rowSubtitle)
            }
        }
        SheetCloseButton(
            onClick = { send(CombatResultsEvent.BackClicked) },
            background = AppPalette.SurfaceElevated,
        )
    }
}

@Composable
private fun OutcomeRow(state: CombatResultsState) {
    val send = state.eventSink
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionOverline(
                text = stringResource(Res.string.combat_results_outcome),
                color = AppPalette.TextTertiary,
                modifier = Modifier.weight(1f),
            )
            if (!state.readOnly) {
                Text(
                    text =
                        stringResource(
                            Res.string.combat_results_suggested,
                            stringResource(state.suggested.label()).lowercase(),
                        ),
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Caption,
                )
            }
        }
        val tabs = OutcomeOrder.map { SegmentedTab(it, stringResource(it.label())) }
        AppSegmentedTabs(
            tabs = tabs,
            selected = state.outcome,
            onSelected = { if (!state.readOnly) send(CombatResultsEvent.OutcomeSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
            containerBackground = AppPalette.Background,
            itemHeight = 32.dp,
        )
    }
}

@Composable
private fun CodexChecklistCard(
    entries: List<CodexEntry>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = stringResource(Res.string.combat_results_codex),
            color = AppPalette.TextTertiary,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier.fillMaxWidth().appCard(background = AppPalette.Background).padding(horizontal = 12.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) AppDivider(color = AppPalette.SurfacePopover)
                ChecklistRow(entry) { onToggle(entry.item.key) }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    entry: CodexEntry,
    onClick: () -> Unit,
) {
    val words = entry.item.words()
    AppCheckRow(checked = entry.checked, onClick = onClick) {
        (entry.item as? CodexItem.Resistance)?.guess?.let { guess ->
            DamageTypeShield(guess.type, guess.state, width = 20.dp)
        }
        AppTitleSubtitle(title = words.first, subtitle = words.second, modifier = Modifier.weight(1f), maxLines = 2)
    }
}

/** the title and the line under it of one proposal */
@Composable
private fun CodexItem.words(): Pair<String, String> =
    when (this) {
        is CodexItem.HitObserved ->
            stringResource(
                if (draft.fell) Res.string.combat_results_item_fell else Res.string.combat_results_item_alive,
                draft.memberName,
                draft.damage,
            ) to stringResource(Res.string.combat_results_item_hits_sub)
        is CodexItem.MarkDead ->
            stringResource(Res.string.combat_results_item_dead, name) to
                stringResource(Res.string.combat_results_item_dead_sub)
        is CodexItem.MarkPartyFallen ->
            stringResource(Res.string.combat_results_item_party_fallen, name) to
                stringResource(Res.string.combat_results_item_party_fallen_sub)
        is CodexItem.MarkFallen ->
            stringResource(Res.string.combat_results_item_fallen, name) to
                stringResource(Res.string.combat_results_item_fallen_sub)
        is CodexItem.Resistance -> {
            val type = damageTypeLabels().getValue(guess.type)
            val state = stringResource(guess.state.word())
            val sub = stringResource(Res.string.combat_results_item_resistance_sub, state)
            stringResource(Res.string.combat_results_item_resistance, guess.speciesName, type) to
                if (guess.confident) sub else stringResource(Res.string.combat_results_item_resistance_weak, sub)
        }
        is CodexItem.KeepAlly ->
            stringResource(Res.string.combat_results_item_keep, name) to
                stringResource(Res.string.combat_results_item_keep_sub)
    }

private fun DamageState.word() =
    when (this) {
        DamageState.RESISTANCE -> Res.string.combat_damage_state_resistance
        DamageState.IMMUNITY -> Res.string.combat_damage_state_immunity
        DamageState.VULNERABILITY -> Res.string.combat_damage_state_vulnerability
        DamageState.NORMAL, DamageState.UNKNOWN -> Res.string.combat_damage_state_normal
    }

@Composable
private fun ResultsActions(state: CombatResultsState) {
    val send = state.eventSink
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.readOnly) {
            ResultsButton(Res.string.combat_results_open_session, ButtonTone.Gold, ButtonVariant.Solid) {
                send(CombatResultsEvent.OpenSessionClicked)
            }
        } else {
            ResultsButton(Res.string.combat_results_save, ButtonTone.Gold, ButtonVariant.Solid) {
                send(CombatResultsEvent.SaveClicked)
            }
            ResultsButton(Res.string.combat_results_back, ButtonTone.Neutral, ButtonVariant.Ghost) {
                send(CombatResultsEvent.BackClicked)
            }
        }
        ResultsButton(Res.string.combat_results_delete, ButtonTone.Maroon, ButtonVariant.Ghost) {
            send(CombatResultsEvent.DeleteClicked)
        }
    }
}

@Composable
private fun ResultsButton(
    label: org.jetbrains.compose.resources.StringResource,
    tone: ButtonTone,
    variant: ButtonVariant,
    onClick: () -> Unit,
) {
    AppButton(
        text = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        tone = tone,
        variant = variant,
        size = ButtonSize.Primary,
    )
}
