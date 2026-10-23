package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppCheckBox
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.summary.DamageGridEditor
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import com.lyrismet.incadent.feature.combat.presentation.prep.HpPadState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_hp_apply
import dndplayerscodex.shared.generated.resources.combat_hp_mode_hp
import dndplayerscodex.shared.generated.resources.combat_panel_full_hint
import dndplayerscodex.shared.generated.resources.combat_panel_full_title
import dndplayerscodex.shared.generated.resources.combat_panel_remove
import dndplayerscodex.shared.generated.resources.combat_panel_set_hp
import dndplayerscodex.shared.generated.resources.combat_panel_side_title
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_title
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ConditionsTab(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    panel.conditions.forEach { group ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(
                                6.dp,
                            ).appCard(RoundedCornerShape(2.dp), RoundColors.groupTitle(group.group), null),
                )
                Text(
                    group.title,
                    color = RoundColors.groupTitle(group.group),
                    fontSize = AppFontSize.Tiny,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                group.items.forEach { item ->
                    ConditionChip(item, group.group) { send(CombatRoundEvent.ConditionToggled(item.condition)) }
                }
            }
        }
    }
}

@Composable
private fun ConditionChip(
    item: ConditionItem,
    group: ConditionGroup,
    onClick: () -> Unit,
) {
    val (fg, bg, border) =
        if (item.on) {
            RoundColors.groupOn(
                group,
            )
        } else {
            Triple(AppPalette.TextSecondary, Color.Transparent, AppPalette.Border)
        }
    Text(
        text = item.label,
        color = fg,
        fontSize = AppFontSize.Small,
        modifier =
            Modifier
                .defaultMinSize(minHeight = 28.dp)
                .appCard(shape = RoundedCornerShape(14.dp), background = bg, border = border, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun MoreTab(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    val more = panel.more
    more.stances?.let { stances ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormFieldLabel(stringResource(Res.string.combat_panel_side_title))
            AppSegmentedTabs(
                tabs = stances.map { SegmentedTab(it.stance, it.label) },
                selected = stances.first { it.selected }.stance,
                onSelected = { send(CombatRoundEvent.StanceSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                more.stanceHint,
                color = AppPalette.TextTertiary,
                fontSize = AppFontSize.Caption,
                lineHeight = AppLineHeight.Row,
            )
        }
    }
    if (more.fullShown) FullRow(more.full) { send(CombatRoundEvent.FullToggled) }
    if (more.canSetHp) {
        AppButton(
            text = stringResource(Res.string.combat_panel_set_hp),
            onClick = { send(CombatRoundEvent.SetHpOpened) },
            variant = ButtonVariant.Ghost,
        )
    }
    more.damage?.let { damage ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormFieldLabel(stringResource(Res.string.entity_sheet_species_damage_title).uppercase())
            DamageGridEditor(damage, { type, state -> send(CombatRoundEvent.DamageChanged(type, state)) })
        }
    }
    NoteField(panel.id, more.note, more.notePlaceholder) { send(CombatRoundEvent.NoteChanged(it)) }
    AppButton(
        text = stringResource(Res.string.combat_panel_remove),
        onClick = { send(CombatRoundEvent.RemoveClicked) },
        tone = ButtonTone.Neutral,
        variant = ButtonVariant.Ghost,
    )
}

@Composable
private fun FullRow(
    full: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                    onClick = onClick,
                ).defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckBox(full)
        AppTitleSubtitle(
            title = stringResource(Res.string.combat_panel_full_title),
            subtitle = stringResource(Res.string.combat_panel_full_hint),
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
    }
}

@Composable
private fun NoteField(
    memberId: Long,
    note: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    // the text is kept here while it is typed, the stored one only comes back when another participant is chosen
    var text by remember(memberId) { mutableStateOf(note) }
    AppTextInput(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = placeholder,
        style = AppTextInputStyle(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
        singleLine = false,
        minLines = 2,
    )
}

@Composable
internal fun RoundHpPad(
    pad: HpPadState,
    send: (CombatRoundEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { send(CombatRoundEvent.HpPadClosed) }) {
        NumberPadSheet(
            overline = stringResource(Res.string.combat_hp_mode_hp),
            title = pad.name,
            expr = "",
            result = pad.result,
            rangeHint = pad.preview,
            applyLabel = stringResource(Res.string.combat_hp_apply),
            applyEnabled = pad.canApply,
            onDigit = { send(CombatRoundEvent.HpDigit(it)) },
            onBackspace = { send(CombatRoundEvent.HpBackspace) },
            onClear = { send(CombatRoundEvent.HpCleared) },
            onApply = { send(CombatRoundEvent.HpApplied) },
            onClose = { send(CombatRoundEvent.HpPadClosed) },
        )
    }
}
