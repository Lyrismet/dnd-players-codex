package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.CalculatorIcon
import com.lyrismet.incadent.core.designsystem.component.sheetTopEdge
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_calc_mode_damage
import dndplayerscodex.shared.generated.resources.combat_calc_mode_heal
import dndplayerscodex.shared.generated.resources.combat_calc_mode_temp
import dndplayerscodex.shared.generated.resources.combat_panel_capture
import dndplayerscodex.shared.generated.resources.combat_panel_captured
import dndplayerscodex.shared.generated.resources.combat_panel_died
import dndplayerscodex.shared.generated.resources.combat_panel_died_note
import dndplayerscodex.shared.generated.resources.combat_panel_mark_auto
import dndplayerscodex.shared.generated.resources.combat_panel_mark_title
import dndplayerscodex.shared.generated.resources.combat_panel_saves_failure
import dndplayerscodex.shared.generated.resources.combat_panel_saves_hint
import dndplayerscodex.shared.generated.resources.combat_panel_saves_success
import dndplayerscodex.shared.generated.resources.combat_panel_saves_title
import dndplayerscodex.shared.generated.resources.combat_panel_stand_up
import dndplayerscodex.shared.generated.resources.combat_panel_tab_actions
import dndplayerscodex.shared.generated.resources.combat_panel_tab_conditions
import dndplayerscodex.shared.generated.resources.combat_panel_tab_more
import dndplayerscodex.shared.generated.resources.combat_panel_undo
import org.jetbrains.compose.resources.stringResource

private const val SAVE_PIPS = 3
private const val DAMAGE_FILL_ALPHA = 0.18f
private const val DAMAGE_BORDER_ALPHA = 0.6f
private const val SOFT_FILL_ALPHA = 0.12f
private const val SOFT_BORDER_ALPHA = 0.45f
private val PANEL_RADIUS = 20.dp
private val PanelShape = RoundedCornerShape(topStart = PANEL_RADIUS, topEnd = PANEL_RADIUS)

@Composable
internal fun RoundPanelUi(
    panel: RoundPanel,
    tab: PanelTab,
    maxHeight: Dp,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(PanelShape)
                .background(AppPalette.Surface)
                .sheetTopEdge(PANEL_RADIUS)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PanelHeader(panel, send)
        AppSegmentedTabs(
            tabs =
                listOf(
                    SegmentedTab(PanelTab.ACTIONS, stringResource(Res.string.combat_panel_tab_actions)),
                    SegmentedTab(PanelTab.CONDITIONS, stringResource(Res.string.combat_panel_tab_conditions)),
                    SegmentedTab(PanelTab.MORE, stringResource(Res.string.combat_panel_tab_more)),
                ),
            selected = tab,
            onSelected = { send(CombatRoundEvent.TabSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        when (tab) {
            PanelTab.ACTIONS -> ActionsTab(panel, send)
            PanelTab.CONDITIONS -> ConditionsTab(panel, send)
            PanelTab.MORE -> MoreTab(panel, send)
        }
    }
}

@Composable
private fun PanelHeader(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(size = 6.dp, color = RoundColors.side(panel.side))
                Text(
                    text = panel.sideLabel,
                    color = RoundColors.side(panel.side),
                    fontSize = AppFontSize.Tiny,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                )
            }
            Text(
                text = panel.name,
                style = MaterialTheme.typography.headlineMedium,
                color = AppPalette.TextHeading,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            panel.hp?.let { hp -> PanelHpBlock(hp, send) }
            panel.wound?.let { wound ->
                Text(
                    wound.label,
                    color = RoundColors.wound(wound.tone),
                    fontSize = AppFontSize.Heading,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    wound.source,
                    color = AppPalette.TextMuted,
                    fontSize = AppFontSize.Caption,
                    lineHeight = AppLineHeight.Row,
                )
            }
        }
    }
}

@Composable
private fun ActionsTab(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    panel.marks?.let { MarksBlock(it, panel.sayLine, send) }
    panel.saves?.let { SavesBlock(it, send) }
    panel.down?.let { DownButtons(it, send) }
    if (panel.died) DiedNote(send)
    CalcButtons(panel, send)
}

@Composable
private fun MarksBlock(
    marks: List<MarkOption>,
    sayLine: String,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_panel_mark_title))
        SegmentedControl(
            items = marks,
            onSelected = { mark -> send(CombatRoundEvent.MarkSelected(mark.state)) },
            itemBackground = { mark -> if (mark.selected) AppPalette.SurfacePopover else Color.Transparent },
            containerBackground = AppPalette.Background,
            itemShape = AppShapes.Small,
            itemHeight = 32.dp,
            contentPadding = 3.dp,
            itemBorder = { mark -> if (mark.selected) RoundColors.wound(mark.tone) else Color.Transparent },
        ) { mark ->
            Text(
                text = mark.label,
                color = if (mark.selected) RoundColors.wound(mark.tone) else AppPalette.TextSecondary,
                fontSize = AppFontSize.Caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        Text(sayLine, color = AppPalette.TextDescription, fontSize = AppFontSize.Small, lineHeight = AppLineHeight.Row)
        Text(
            stringResource(Res.string.combat_panel_mark_auto),
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Caption,
            lineHeight = AppLineHeight.Row,
        )
    }
}

@Composable
private fun SavesBlock(
    saves: SavesState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = RoundColors.Failure.copy(alpha = 0.07f),
                    border = RoundColors.Failure.copy(alpha = 0.4f),
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(Res.string.combat_panel_saves_title),
                color = AppPalette.MaroonBright,
                fontSize = AppFontSize.Tiny,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            )
            Text(
                stringResource(Res.string.combat_panel_saves_hint),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Caption,
            )
        }
        SaveRow(stringResource(Res.string.combat_panel_saves_success), saves.successes, RoundColors.Success) {
            send(CombatRoundEvent.SaveToggled(SaveKind.SUCCESS, it))
        }
        SaveRow(stringResource(Res.string.combat_panel_saves_failure), saves.failures, RoundColors.Failure) {
            send(CombatRoundEvent.SaveToggled(SaveKind.FAILURE, it))
        }
    }
}

@Composable
private fun SaveRow(
    label: String,
    filled: Int,
    color: Color,
    onToggle: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.Body,
            modifier = Modifier.size(width = 70.dp, height = 20.dp),
        )
        repeat(SAVE_PIPS) { index ->
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .border(2.dp, color, CircleShape)
                        .appCard(
                            CircleShape,
                            if (index <
                                filled
                            ) {
                                color
                            } else {
                                Color.Transparent
                            },
                            null,
                            onClick = { onToggle(index) },
                        ),
            )
        }
    }
}

@Composable
private fun DownButtons(
    down: DownState,
    send: (CombatRoundEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (down.canStandUp) {
            DownButton(
                text = stringResource(Res.string.combat_panel_stand_up),
                color = AppPalette.EmeraldBright,
                fill = AppPalette.Emerald.copy(alpha = 0.12f),
                border = AppPalette.Emerald.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            ) { send(CombatRoundEvent.StandUp) }
        }
        if (down.canCapture) {
            DownButton(
                text =
                    stringResource(
                        if (down.captured) Res.string.combat_panel_captured else Res.string.combat_panel_capture,
                    ),
                color = if (down.captured) AppPalette.Azure else AppPalette.TextDescription,
                fill = if (down.captured) AppPalette.Azure.copy(alpha = 0.14f) else Color.Transparent,
                border = if (down.captured) AppPalette.Azure.copy(alpha = 0.5f) else AppPalette.BorderHover,
                modifier = Modifier.weight(1f),
            ) { send(CombatRoundEvent.CaptureToggled) }
        }
        if (down.canDie) {
            DownButton(
                text = stringResource(Res.string.combat_panel_died),
                color = AppPalette.TextMuted,
                fill = Color.Transparent,
                border = AppPalette.BorderHover,
                modifier = Modifier.weight(1f),
            ) { send(CombatRoundEvent.DiedToggled) }
        }
    }
}

@Composable
private fun DownButton(
    text: String,
    color: Color,
    fill: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .appCard(shape = AppShapes.Field, background = fill, border = border, onClick = onClick)
                .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = AppFontSize.Medium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun DiedNote(send: (CombatRoundEvent) -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Field, background = AppPalette.Background, border = AppPalette.Border)
                .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.combat_panel_died_note),
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.BodyPlus,
            lineHeight = AppLineHeight.Row,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.combat_panel_undo),
            onClick = { send(CombatRoundEvent.DiedToggled) },
            variant = ButtonVariant.Ghost,
        )
    }
}

/** the three ways into the calculator: damage, healing and, for those with counted hit points, temporary ones */
@Composable
private fun CalcButtons(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        panel.calcModes.forEach { mode ->
            val look = calcLook(mode)
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 50.dp)
                        .appCard(
                            shape = AppShapes.Field,
                            background = look.base.copy(alpha = look.fillAlpha),
                            border = look.base.copy(alpha = look.borderAlpha),
                            onClick = { send(CombatRoundEvent.Calc(CalcEvent.Opened(mode))) },
                        ).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalculatorIcon(tint = look.text, width = 13.dp, height = 16.dp)
                Text(
                    look.label,
                    color = look.text,
                    fontSize = AppFontSize.Medium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
    Text(
        panel.sourceNote,
        color = AppPalette.TextTertiary,
        fontSize = AppFontSize.Caption,
        lineHeight = AppLineHeight.Row,
    )
}

@Composable
private fun calcLook(mode: CalcMode): CalcLook =
    when (mode) {
        CalcMode.DAMAGE ->
            CalcLook(
                stringResource(Res.string.combat_calc_mode_damage),
                AppPalette.MaroonBright,
                AppPalette.Maroon,
                DAMAGE_FILL_ALPHA,
                DAMAGE_BORDER_ALPHA,
            )
        CalcMode.HEAL ->
            CalcLook(
                stringResource(Res.string.combat_calc_mode_heal),
                AppPalette.EmeraldBright,
                AppPalette.Emerald,
                SOFT_FILL_ALPHA,
                SOFT_BORDER_ALPHA,
            )
        CalcMode.TEMP ->
            CalcLook(
                stringResource(Res.string.combat_calc_mode_temp),
                AppPalette.Azure,
                AppPalette.Party,
                SOFT_FILL_ALPHA,
                SOFT_BORDER_ALPHA,
            )
    }

/** one calculator button: its words, the text tint and the base colour its fill and edge come from */
private class CalcLook(
    val label: String,
    val text: Color,
    val base: Color,
    val fillAlpha: Float,
    val borderAlpha: Float,
)
