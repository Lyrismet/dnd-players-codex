package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppCheckBox
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.PillFill
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_mark_confirm
import incadent.shared.generated.resources.combat_mark_field_label
import incadent.shared.generated.resources.combat_mark_preset_ac
import incadent.shared.generated.resources.combat_mark_preset_save
import incadent.shared.generated.resources.combat_mark_preset_speed
import incadent.shared.generated.resources.combat_mark_rounds_custom
import incadent.shared.generated.resources.combat_mark_rounds_minute
import incadent.shared.generated.resources.combat_mark_rounds_none
import incadent.shared.generated.resources.combat_mark_rounds_ten
import incadent.shared.generated.resources.combat_mark_rounds_title
import incadent.shared.generated.resources.combat_mark_toggle_concentration
import org.jetbrains.compose.resources.stringResource

private val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

/** a short text, an optional round counter and the switch of a concentration, on one sheet without tabs */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun MarkSheetUi(
    sheet: MarkSheet,
    send: (CombatRoundEvent) -> Unit,
) {
    val close = { send(CombatRoundEvent.MarkSheetClosed) }
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    overline =
                        stringResource(Res.string.combat_mark_field_label).uppercase() + " · " + sheet.name.uppercase(),
                    title = sheet.title,
                    onClose = close,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormFieldLabel(sheet.fieldLabel)
                AppTextInput(
                    value = sheet.text,
                    onValueChange = { send(CombatRoundEvent.MarkTextChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTextInputStyle(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
                    autoFocus = true,
                )
            }
            if (sheet.showPresets) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        stringResource(Res.string.combat_mark_preset_save),
                        stringResource(Res.string.combat_mark_preset_ac),
                        stringResource(Res.string.combat_mark_preset_speed),
                    ).forEach { preset ->
                        SheetChip(preset, false) { send(CombatRoundEvent.MarkTextChanged(preset)) }
                    }
                }
            }
            RoundsPicker(sheet, send)
            if (!sheet.custom) {
                ConcentrationRow(sheet.concentration) { send(CombatRoundEvent.MarkConcentrationToggled) }
            }
            FormPrimaryButton(
                stringResource(Res.string.combat_mark_confirm),
                { send(CombatRoundEvent.MarkConfirmed) },
                enabled = sheet.canAdd,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RoundsPicker(
    sheet: MarkSheet,
    send: (CombatRoundEvent) -> Unit,
) {
    val minute = MARK_MINUTE_ROUNDS.toString()
    val ten = MARK_TEN_MINUTES_ROUNDS.toString()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_mark_rounds_title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetChip(stringResource(Res.string.combat_mark_rounds_none), sheet.roundsText.isEmpty()) {
                send(CombatRoundEvent.MarkRoundsChanged(""))
            }
            SheetChip(stringResource(Res.string.combat_mark_rounds_minute), sheet.roundsText == minute) {
                send(CombatRoundEvent.MarkRoundsChanged(minute))
            }
            SheetChip(stringResource(Res.string.combat_mark_rounds_ten), sheet.roundsText == ten) {
                send(CombatRoundEvent.MarkRoundsChanged(ten))
            }
        }
        AppTextInput(
            value = sheet.roundsText,
            onValueChange = { send(CombatRoundEvent.MarkRoundsChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.combat_mark_rounds_custom),
            style = AppTextInputStyle(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
            keyboardOptions = NumberKeyboard,
        )
    }
}

@Composable
private fun ConcentrationRow(
    on: Boolean,
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
                ).defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckBox(on)
        Text(
            stringResource(Res.string.combat_mark_toggle_concentration),
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.Body,
        )
    }
}

@Composable
private fun SheetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) = SelectablePill(
    text = text,
    selected = selected,
    onClick = onClick,
    size = PillSize.Small,
    selectedFill = PillFill.GoldTint,
    unselectedContent = AppPalette.TextSecondary,
)
