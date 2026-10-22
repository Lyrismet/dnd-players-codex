package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.presentation.prep.CombatPrepEvent
import com.lyrismet.incadent.feature.combat.presentation.prep.FoeSearchSheet
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_end_confirm
import dndplayerscodex.shared.generated.resources.combat_end_keep
import dndplayerscodex.shared.generated.resources.combat_end_text
import dndplayerscodex.shared.generated.resources.combat_end_title
import dndplayerscodex.shared.generated.resources.combat_outcome_interrupted
import dndplayerscodex.shared.generated.resources.combat_outcome_peace
import dndplayerscodex.shared.generated.resources.combat_outcome_retreat
import dndplayerscodex.shared.generated.resources.combat_outcome_victory
import dndplayerscodex.shared.generated.resources.combat_round_all_clear
import dndplayerscodex.shared.generated.resources.combat_round_current
import dndplayerscodex.shared.generated.resources.combat_round_end
import dndplayerscodex.shared.generated.resources.combat_round_next
import dndplayerscodex.shared.generated.resources.combat_round_overline
import dndplayerscodex.shared.generated.resources.combat_round_stop
import dndplayerscodex.shared.generated.resources.combat_round_stop_hint
import dndplayerscodex.shared.generated.resources.combat_round_sum_up
import dndplayerscodex.shared.generated.resources.combat_round_title
import dndplayerscodex.shared.generated.resources.combat_stop_confirm
import dndplayerscodex.shared.generated.resources.combat_stop_title
import dndplayerscodex.shared.generated.resources.combat_summary_button
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val LineHeight20 = 20.sp
private const val PANEL_SHARE = 0.58f
private const val MENU_OFFSET_Y = 120

@Composable
fun CombatRoundUi(
    state: CombatRoundState,
    modifier: Modifier = Modifier,
) {
    val send = state.eventSink
    val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Scaffold(modifier = modifier, contentWindowInsets = contentWindowInsets) { contentPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                RoundHeader(state)
                if (state.allClear) AllClearBanner { send(CombatRoundEvent.EndRequested(EndKind.FINISH)) }
                RoundList(state, Modifier.weight(1f))
                state.panel?.let { RoundPanelUi(it, state.tab, this@BoxWithConstraints.maxHeight * PANEL_SHARE, send) }
            }
            AppToast(text = state.toast?.text, modifier = floatingBarAnchor())
        }
    }
    state.search?.let { search ->
        FoeSearchSheet(
            search,
        ) { event -> if (event is CombatPrepEvent.SearchEvent) send(CombatRoundEvent.Search(event)) }
    }
    state.hpPad?.let { RoundHpPad(it, send) }
    state.calc?.let { CalcSheet(it, send) }
    state.summary?.let { SummarySheet(it, send) }
    state.endConfirm?.let { EndDialog(it, send) }
}

@Composable
private fun RoundHeader(state: CombatRoundState) {
    val send = state.eventSink
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionOverline(
                text = stringResource(Res.string.combat_round_overline, state.timer),
                color = AppPalette.MaroonBright,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    text = stringResource(Res.string.combat_round_end),
                    onClick = { send(CombatRoundEvent.EndRequested(EndKind.FINISH)) },
                    height = 36.dp,
                )
                MenuButton(state.menuOpen, send)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.combat_round_title, state.round),
                style = MaterialTheme.typography.displayLarge,
                color = AppPalette.TextHeading,
            )
            state.roundDamage?.let { SummaryButton(it) { send(CombatRoundEvent.Summary(SummaryEvent.Opened)) } }
        }
        TurnCard(state, Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun MenuButton(
    open: Boolean,
    send: (CombatRoundEvent) -> Unit,
) {
    Box {
        Text(
            text = "⋯",
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .size(width = 44.dp, height = 36.dp)
                    .appCard(shape = AppShapes.Control, background = Color.Transparent, border = AppPalette.BorderHover)
                    .clickable { send(CombatRoundEvent.MenuToggled) }
                    .padding(top = 3.dp),
        )
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, MENU_OFFSET_Y),
                onDismissRequest = { send(CombatRoundEvent.MenuToggled) },
            ) {
                MenuCard { send(CombatRoundEvent.EndRequested(EndKind.STOP)) }
            }
        }
    }
}

@Composable
private fun MenuCard(onStop: () -> Unit) {
    Column(
        modifier =
            Modifier
                .size(width = 220.dp, height = 64.dp)
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfacePopover,
                    border = AppPalette.BorderPopover,
                ).clickable(onClick = onStop)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(Res.string.combat_round_stop),
            color = AppPalette.MaroonBright,
            fontSize = AppFontSize.Medium,
            lineHeight = LineHeight20,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.combat_round_stop_hint),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Caption,
            lineHeight = LineHeight20,
        )
    }
}

@Composable
private fun TurnCard(
    state: CombatRoundState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Card, background = AppPalette.SurfaceVariant)
                .padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.combat_round_current, state.currentName),
                color = AppPalette.GoldBright,
                fontSize = AppFontSize.MediumPlus,
                lineHeight = LineHeight20,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.hostile,
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                lineHeight = LineHeight20,
            )
        }
        AppButton(
            text = stringResource(Res.string.combat_round_next),
            onClick = { state.eventSink(CombatRoundEvent.NextTurn) },
            variant = ButtonVariant.Solid,
            size = ButtonSize.Medium,
            height = 44.dp,
        )
    }
}

@Composable
private fun AllClearBanner(onSumUp: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Emerald.copy(alpha = 0.12f),
                    border = AppPalette.Emerald.copy(alpha = 0.45f),
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.combat_round_all_clear),
            color = AppPalette.EmeraldBright,
            fontSize = AppFontSize.Body,
            lineHeight = LineHeight20,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.combat_round_sum_up),
            onClick = onSumUp,
            tone = ButtonTone.Gold,
            variant = ButtonVariant.Solid,
        )
    }
}

@Composable
private fun EndDialog(
    confirm: EndConfirm,
    send: (CombatRoundEvent) -> Unit,
) {
    val stop = confirm.kind == EndKind.STOP
    ConfirmationDialog(
        title = stringResource(if (stop) Res.string.combat_stop_title else Res.string.combat_end_title),
        text = stringResource(Res.string.combat_end_text, stringResource(confirm.outcome.label())),
        onConfirm = { send(CombatRoundEvent.EndConfirmed) },
        onDismiss = { send(CombatRoundEvent.EndDismissed) },
        confirmLabel = stringResource(if (stop) Res.string.combat_stop_confirm else Res.string.combat_end_confirm),
        dismissLabel = stringResource(Res.string.combat_end_keep),
    )
}

private fun CombatOutcome.label(): StringResource =
    when (this) {
        CombatOutcome.VICTORY -> Res.string.combat_outcome_victory
        CombatOutcome.RETREAT -> Res.string.combat_outcome_retreat
        CombatOutcome.PEACE -> Res.string.combat_outcome_peace
        CombatOutcome.INTERRUPTED -> Res.string.combat_outcome_interrupted
    }

@Composable
private fun SummaryButton(
    damage: RoundDamage,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .defaultMinSize(minHeight = 32.dp)
                .appCard(
                    shape = AppShapes.Tile,
                    background = Color.Transparent,
                    border = AppPalette.BorderHover,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.combat_summary_button),
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.SmallPlus,
            fontWeight = FontWeight.SemiBold,
        )
        damage.current?.let {
            Text(
                it,
                color = AppPalette.MaroonBright,
                fontSize = AppFontSize.Small,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .appCard(
                            AppShapes.Badge,
                            AppPalette.Maroon.copy(alpha = 0.16f),
                            null,
                        ).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}
