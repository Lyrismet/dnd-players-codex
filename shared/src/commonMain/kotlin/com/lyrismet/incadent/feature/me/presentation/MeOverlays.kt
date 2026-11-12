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
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
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
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.help_ac_body
import incadent.shared.generated.resources.help_ac_title
import incadent.shared.generated.resources.me_ability_hint
import incadent.shared.generated.resources.me_ability_overline
import incadent.shared.generated.resources.me_ac_from_hint
import incadent.shared.generated.resources.me_ac_from_label
import incadent.shared.generated.resources.me_ac_from_placeholder
import incadent.shared.generated.resources.me_ac_title
import incadent.shared.generated.resources.me_calc_bonus
import incadent.shared.generated.resources.me_calc_save
import incadent.shared.generated.resources.me_done
import incadent.shared.generated.resources.me_hp_apply
import incadent.shared.generated.resources.me_hp_mode_minus
import incadent.shared.generated.resources.me_hp_mode_plus
import incadent.shared.generated.resources.me_hp_mode_temp
import incadent.shared.generated.resources.me_hp_pad_overline
import incadent.shared.generated.resources.me_hp_preview_format
import incadent.shared.generated.resources.me_proficiency_expertise
import incadent.shared.generated.resources.me_proficiency_none
import incadent.shared.generated.resources.me_proficiency_proficient
import incadent.shared.generated.resources.me_save_proficiency
import incadent.shared.generated.resources.me_sheet_overline
import incadent.shared.generated.resources.me_skill_adjustment
import incadent.shared.generated.resources.me_skill_adjustment_hint
import incadent.shared.generated.resources.me_skill_bonus
import incadent.shared.generated.resources.me_skill_overline
import incadent.shared.generated.resources.me_speed_hint
import incadent.shared.generated.resources.me_speed_title
import incadent.shared.generated.resources.me_step_less
import incadent.shared.generated.resources.me_step_more
import org.jetbrains.compose.resources.stringResource

private const val SPEED_STEP = 5

/** the sheet over the screen that belongs to the open [MeCharacter.overlay], nothing while there is none */
@Suppress("CyclomaticComplexMethod")
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
        is MeOverlay.Slots -> SlotsSheet(overlay, send)
        is MeOverlay.Resource ->
            ResourceSheet(overlay, send) { onEvent(MeEvent.Counters(CounterEvent.ResourceDeleted)) }
        is MeOverlay.SpellAbility ->
            SpellAbilitySheet(
                overlay,
                onChoose = { onEvent(MeEvent.Counters(CounterEvent.SpellAbilityChosen(it))) },
                onClose = { send(OverlayEvent.Closed) },
            )
        is MeOverlay.Rest -> MeRestSheet(character.name, overlay) { onEvent(MeEvent.Rest(it)) }
        is MeOverlay.Wallet -> MeWalletSheet(character.name, overlay) { onEvent(MeEvent.Wallet(it)) }
        MeOverlay.Templates ->
            TemplatesSheet(
                character.resources + character.spells.columns.map { it.resource },
                onChoose = { template, name -> onEvent(MeEvent.Counters(CounterEvent.TemplateChosen(template, name))) },
                onClose = { send(OverlayEvent.Closed) },
            )
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
            result = pad.result.withSign(pad.mode),
            accent = pad.mode.accent(),
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
                            SegmentedTab(
                                HitPointMode.HEAL,
                                stringResource(Res.string.me_hp_mode_plus),
                                accent = HitPointMode.HEAL.accent(),
                            ),
                            SegmentedTab(
                                HitPointMode.DAMAGE,
                                stringResource(Res.string.me_hp_mode_minus),
                                accent = HitPointMode.DAMAGE.accent(),
                            ),
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
        NumberRow(edit.score, send, spread = true)
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
            NumberRow(edit.adjustment, send, signed = true)
        }
        Text(
            stringResource(Res.string.me_skill_adjustment_hint),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        AdjustmentNote(edit.sheet.note(edit.skill), send)
        CalcCard(stringResource(Res.string.me_skill_bonus), edit.bonus.signed(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArmorClassSheet(
    edit: MeOverlay.ArmorClass,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_sheet_overline),
        title = stringResource(Res.string.me_ac_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
        help = stringResource(Res.string.help_ac_title) to stringResource(Res.string.help_ac_body),
    ) {
        NumberRow(edit.value, send, spread = true)
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
        overline = stringResource(Res.string.me_sheet_overline),
        title = stringResource(Res.string.me_speed_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        NumberRow(edit.value, send, step = SPEED_STEP, spread = true)
        Text(stringResource(Res.string.me_speed_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
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
    help: Pair<String, String>? = null,
    doneLabel: String = stringResource(Res.string.me_done),
    doneEnabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = title,
                    onClose = onClose,
                    overline = overline,
                    subtitle = subtitle,
                    overlineTrailing = help?.let { (hintTitle, hintBody) -> { AppHelpHint(hintTitle, hintBody) } },
                )
            },
        ) {
            content()
            FormPrimaryButton(text = doneLabel, onClick = onDone, enabled = doneEnabled)
        }
    }
}

@Composable
internal fun NumberRow(
    value: Int,
    send: (OverlayEvent) -> Unit,
    step: Int = 1,
    spread: Boolean = false,
    // a value that is added to a number reads "+1", a number that is set reads as it is
    signed: Boolean = false,
) {
    AppStepper(
        count = value,
        countText = if (signed) value.signed() else value.toString(),
        onDecrease = { send(OverlayEvent.NumberStepped(-step)) },
        onIncrease = { send(OverlayEvent.NumberStepped(step)) },
        decreaseDescription = stringResource(Res.string.me_step_less),
        increaseDescription = stringResource(Res.string.me_step_more),
        tiles = true,
        removesAtOne = false,
        spread = spread,
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
