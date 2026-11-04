package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_ability_hint
import dndplayerscodex.shared.generated.resources.me_ability_overline
import dndplayerscodex.shared.generated.resources.me_ac_from_hint
import dndplayerscodex.shared.generated.resources.me_ac_from_label
import dndplayerscodex.shared.generated.resources.me_ac_from_placeholder
import dndplayerscodex.shared.generated.resources.me_ac_overline
import dndplayerscodex.shared.generated.resources.me_ac_title
import dndplayerscodex.shared.generated.resources.me_adjustment_hint
import dndplayerscodex.shared.generated.resources.me_calc_bonus
import dndplayerscodex.shared.generated.resources.me_calc_save
import dndplayerscodex.shared.generated.resources.me_done
import dndplayerscodex.shared.generated.resources.me_hp_apply
import dndplayerscodex.shared.generated.resources.me_hp_mode_minus
import dndplayerscodex.shared.generated.resources.me_hp_mode_plus
import dndplayerscodex.shared.generated.resources.me_hp_mode_temp
import dndplayerscodex.shared.generated.resources.me_hp_pad_overline
import dndplayerscodex.shared.generated.resources.me_hp_preview_format
import dndplayerscodex.shared.generated.resources.me_popover_adjustment
import dndplayerscodex.shared.generated.resources.me_popover_initiative
import dndplayerscodex.shared.generated.resources.me_popover_passive
import dndplayerscodex.shared.generated.resources.me_proficiency_expertise
import dndplayerscodex.shared.generated.resources.me_proficiency_none
import dndplayerscodex.shared.generated.resources.me_proficiency_proficient
import dndplayerscodex.shared.generated.resources.me_save_proficiency
import dndplayerscodex.shared.generated.resources.me_skill_adjustment
import dndplayerscodex.shared.generated.resources.me_skill_adjustment_hint
import dndplayerscodex.shared.generated.resources.me_skill_bonus
import dndplayerscodex.shared.generated.resources.me_skill_overline
import dndplayerscodex.shared.generated.resources.me_speed_hint
import dndplayerscodex.shared.generated.resources.me_speed_overline
import dndplayerscodex.shared.generated.resources.me_speed_title
import dndplayerscodex.shared.generated.resources.me_step_less
import dndplayerscodex.shared.generated.resources.me_step_more
import org.jetbrains.compose.resources.stringResource

private const val SPEED_STEP = 5

/** the sheet over the screen that belongs to the open [MeCharacter.overlay], nothing while there is none */
@Composable
internal fun MeOverlays(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val send = { event: OverlayEvent -> onEvent(MeEvent.Overlay(event)) }
    when (val overlay = character.overlay) {
        null -> Unit
        is MeOverlay.HitPoints -> HitPointsSheet(character.name, overlay) { onEvent(MeEvent.Hp(it)) }
        is MeOverlay.AbilityEdit -> AbilitySheet(overlay, send)
        is MeOverlay.SkillEdit -> SkillSheet(overlay, send)
        is MeOverlay.ArmorClass -> ArmorClassSheet(overlay, send)
        is MeOverlay.Speed -> SpeedSheet(overlay, send)
        is MeOverlay.Adjustment -> AdjustmentSheet(overlay, send)
        is MeOverlay.Fill -> MeFillSheet(overlay.draft, send)
        is MeOverlay.EditCharacter -> MeEditSheet(overlay.draft, send)
    }
}

@Composable
private fun HitPointsSheet(
    name: String,
    pad: MeOverlay.HitPoints,
    onEvent: (HitPointsEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(HitPointsEvent.Closed) }) {
        NumberPadSheet(
            overline = stringResource(Res.string.me_hp_pad_overline),
            title = name,
            expr = "",
            result = pad.result,
            rangeHint = stringResource(Res.string.me_hp_preview_format, pad.before, pad.after),
            applyLabel = stringResource(Res.string.me_hp_apply),
            applyEnabled = pad.canApply,
            onDigit = { onEvent(HitPointsEvent.Digit(it)) },
            onBackspace = { onEvent(HitPointsEvent.Backspace) },
            onClear = { onEvent(HitPointsEvent.Cleared) },
            onApply = { onEvent(HitPointsEvent.Applied) },
            onClose = { onEvent(HitPointsEvent.Closed) },
            modes = {
                AppSegmentedTabs(
                    tabs =
                        listOf(
                            SegmentedTab(HitPointMode.DAMAGE, stringResource(Res.string.me_hp_mode_minus)),
                            SegmentedTab(HitPointMode.HEAL, stringResource(Res.string.me_hp_mode_plus)),
                            SegmentedTab(HitPointMode.TEMP, stringResource(Res.string.me_hp_mode_temp)),
                        ),
                    selected = pad.mode,
                    onSelected = { onEvent(HitPointsEvent.ModeChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun AbilitySheet(
    edit: MeOverlay.AbilityEdit,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_ability_overline),
        title = stringResource(edit.ability.nameRes()),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.score, send)
        Text(stringResource(Res.string.me_ability_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcCard(stringResource(Res.string.me_calc_bonus), edit.bonus.signed(), Modifier.weight(1f))
            CalcCard(stringResource(Res.string.me_calc_save), edit.saveBonus.signed(), Modifier.weight(1f))
        }
        SwitchRow(stringResource(Res.string.me_save_proficiency), edit.save) { send(OverlayEvent.SaveToggled) }
    }
}

@Composable
private fun SkillSheet(
    edit: MeOverlay.SkillEdit,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_skill_overline),
        title = stringResource(edit.skill.nameRes()),
        subtitle = edit.skill.altRes()?.let { stringResource(it) },
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        AppSegmentedTabs(
            tabs =
                listOf(
                    SegmentedTab(Proficiency.NONE, stringResource(Res.string.me_proficiency_none)),
                    SegmentedTab(Proficiency.PROFICIENT, stringResource(Res.string.me_proficiency_proficient)),
                    SegmentedTab(Proficiency.EXPERTISE, stringResource(Res.string.me_proficiency_expertise)),
                ),
            selected = edit.proficiency,
            onSelected = { send(OverlayEvent.ProficiencyChosen(it)) },
            modifier = Modifier.fillMaxWidth(),
            itemHeight = 42.dp,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.me_skill_adjustment),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = AppPalette.TextPrimary,
            )
            NumberRow(edit.adjustment, send)
        }
        Text(
            stringResource(Res.string.me_skill_adjustment_hint),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        CalcCard(stringResource(Res.string.me_skill_bonus), edit.bonus.signed(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArmorClassSheet(
    edit: MeOverlay.ArmorClass,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_ac_overline),
        title = stringResource(Res.string.me_ac_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.value, send)
        FormTextField(
            label = stringResource(Res.string.me_ac_from_label),
            value = edit.from,
            onChange = { send(OverlayEvent.TextChanged(OverlayField.ARMOR_CLASS_FROM, it)) },
            placeholder = stringResource(Res.string.me_ac_from_placeholder),
        )
        Text(stringResource(Res.string.me_ac_from_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun SpeedSheet(
    edit: MeOverlay.Speed,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_speed_overline),
        title = stringResource(Res.string.me_speed_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.value, send, step = SPEED_STEP)
        Text(stringResource(Res.string.me_speed_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun AdjustmentSheet(
    edit: MeOverlay.Adjustment,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_popover_adjustment),
        title =
            stringResource(
                if (edit.tile == MeTile.INITIATIVE) Res.string.me_popover_initiative else Res.string.me_popover_passive,
            ),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.value, send)
        Text(stringResource(Res.string.me_adjustment_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

/** the shell of a small edit sheet: a header, what it edits and the one button that saves it */
@Composable
internal fun EditSheet(
    overline: String,
    title: String,
    onClose: () -> Unit,
    onDone: () -> Unit,
    subtitle: String? = null,
    doneLabel: String = stringResource(Res.string.me_done),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            scroll = true,
            header = { AppSheetHeader(title = title, onClose = onClose, overline = overline, subtitle = subtitle) },
        ) {
            content()
            FormPrimaryButton(text = doneLabel, onClick = onDone)
        }
    }
}

@Composable
internal fun NumberRow(
    value: Int,
    send: (OverlayEvent) -> Unit,
    step: Int = 1,
) {
    AppStepper(
        count = value,
        onDecrease = { send(OverlayEvent.NumberStepped(-step)) },
        onIncrease = { send(OverlayEvent.NumberStepped(step)) },
        decreaseDescription = stringResource(Res.string.me_step_less),
        increaseDescription = stringResource(Res.string.me_step_more),
        tiles = true,
        removesAtOne = false,
    )
}

@Composable
private fun CalcCard(
    label: String,
    value: String,
    modifier: Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier =
            modifier
                .appCard(
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                ).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = AppText.caption, color = AppPalette.TextSecondary)
        Text(value, style = AppText.numeral(24.sp), color = AppPalette.TextHeading)
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
        AppSwitch(checked = checked, onCheckedChange = { onToggle() })
    }
}
