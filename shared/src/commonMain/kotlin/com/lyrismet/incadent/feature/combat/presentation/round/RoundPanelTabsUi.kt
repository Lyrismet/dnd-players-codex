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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
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
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup
import com.lyrismet.incadent.feature.combat.presentation.prep.HpPadState
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_hp_apply
import incadent.shared.generated.resources.combat_hp_mode_hp
import incadent.shared.generated.resources.combat_mark_add
import incadent.shared.generated.resources.combat_mark_concentration_quick
import incadent.shared.generated.resources.combat_mark_custom_add
import incadent.shared.generated.resources.combat_mark_group_custom
import incadent.shared.generated.resources.combat_mark_head
import incadent.shared.generated.resources.combat_mark_none
import incadent.shared.generated.resources.combat_mark_remove
import incadent.shared.generated.resources.combat_panel_full_title
import incadent.shared.generated.resources.combat_panel_remove
import incadent.shared.generated.resources.combat_panel_set_hp
import incadent.shared.generated.resources.combat_panel_side_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ConditionsTab(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    panel.features?.let { FeaturesBlockUi(it, send) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.combat_mark_head) + if (panel.tags.isEmpty()) "" else " · ${panel.tags.size}",
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Tiny,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.combat_mark_add),
            onClick = { send(CombatRoundEvent.MarkSheetOpened()) },
            variant = ButtonVariant.Ghost,
        )
    }
    if (panel.tags.isEmpty()) {
        Text(
            stringResource(Res.string.combat_mark_none),
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Small,
        )
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            panel.tags.forEach { tag ->
                MarkTag(tag) {
                    send(
                        tag.condition?.let { CombatRoundEvent.ConditionToggled(it) }
                            ?: CombatRoundEvent.MarkRemoved(tag.id),
                    )
                }
            }
        }
    }
    panel.conditions.forEach { group ->
        ConditionGroupBlock(
            group,
            onToggle = { item -> send(CombatRoundEvent.ConditionToggled(item.condition)) },
        ) {
            if (group.group == ConditionGroup.EFFECT) {
                ConditionChip(
                    stringResource(Res.string.combat_mark_concentration_quick),
                    false,
                    ConditionGroup.EFFECT,
                ) {
                    send(CombatRoundEvent.MarkSheetOpened(concentration = true))
                }
            }
        }
    }
    CustomGroup(panel.customs, send)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CustomGroup(
    customs: List<CustomItem>,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(Res.string.combat_mark_group_custom),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Tiny,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            customs.forEach { item ->
                ConditionChip(
                    item.name,
                    item.on,
                    ConditionGroup.WEAKENING,
                ) { send(CombatRoundEvent.CustomToggled(item.name)) }
            }
            ConditionChip(stringResource(Res.string.combat_mark_custom_add), false, ConditionGroup.WEAKENING) {
                send(CombatRoundEvent.MarkSheetOpened(custom = true))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ConditionGroupBlock(
    group: ConditionGroupItem,
    onToggle: (ConditionItem) -> Unit,
    extra: @Composable () -> Unit,
) {
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            group.items.forEach { item -> ConditionChip(item.label, item.on, group.group) { onToggle(item) } }
            extra()
        }
    }
}

/** a mark that is on the participant, a tap takes it off */
@Composable
private fun MarkTag(
    mark: PanelMark,
    onRemove: () -> Unit,
) {
    val (fg, bg) = RoundColors.chip(mark.chip)
    val removeLabel = stringResource(Res.string.combat_mark_remove)
    Text(
        text = mark.label + " ×",
        color = if (mark.tone == MarkTone.NORMAL) fg else RoundColors.chip(markChip(mark.tone)).first,
        fontSize = AppFontSize.Small,
        modifier =
            Modifier
                .defaultMinSize(minHeight = 28.dp)
                .semantics { contentDescription = removeLabel }
                .appCard(
                    shape = AppShapes.Card,
                    background = bg,
                    border = RoundColors.markBorder(mark.tone),
                    onClick = onRemove,
                ).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun markChip(tone: MarkTone): ChipKind =
    if (tone ==
        MarkTone.LAST
    ) {
        ChipKind.MARK_LAST
    } else {
        ChipKind.MARK_EXPIRED
    }

@Composable
private fun ConditionChip(
    label: String,
    on: Boolean,
    group: ConditionGroup,
    onClick: () -> Unit,
) {
    val (fg, bg, border) =
        if (on) {
            RoundColors.groupOn(
                group,
            )
        } else {
            Triple(AppPalette.TextSecondary, Color.Transparent, AppPalette.Border)
        }
    Text(
        text = label,
        color = fg,
        fontSize = AppFontSize.Small,
        modifier =
            Modifier
                .defaultMinSize(minHeight = 28.dp)
                .appCard(shape = AppShapes.Card, background = bg, border = border, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun MoreTab(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    val more = panel.more
    more.identity?.let { IdentityBlock(it, send) }
    more.naming?.let { NamingField(it, send) }
    more.stances?.let { stances ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormFieldLabel(stringResource(Res.string.combat_panel_side_title))
            AppSegmentedTabs(
                tabs = stances.map { SegmentedTab(it.stance, it.label) },
                selected = stances.first { it.selected }.stance,
                onSelected = { send(CombatRoundEvent.StanceSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
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
    NoteField(panel.id, more.note, more.notePlaceholder) { send(CombatRoundEvent.NoteChanged(panel.id, it)) }
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
            subtitle = "",
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
    }
}

private const val NOTE_SAVE_DELAY_MS = 500L

@Composable
private fun NoteField(
    memberId: Long,
    note: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    // the text is kept here while it is typed, the stored one only comes back when another participant is chosen
    var text by remember(memberId) { mutableStateOf(note) }
    val latestText by rememberUpdatedState(text)
    val save by rememberUpdatedState(onChange)
    val saved = remember(memberId) { arrayOf(note) }
    // every key used to write the database and rebuild the fight, now the text is stored once the typing pauses
    LaunchedEffect(memberId, text) {
        if (text != saved[0]) {
            delay(NOTE_SAVE_DELAY_MS)
            saved[0] = text
            save(text)
        }
    }
    // leaving the field or the participant keeps what was typed in the last moment
    DisposableEffect(memberId) {
        onDispose {
            if (latestText != saved[0]) save(latestText)
        }
    }
    AppTextInput(
        value = text,
        onValueChange = { text = it },
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
