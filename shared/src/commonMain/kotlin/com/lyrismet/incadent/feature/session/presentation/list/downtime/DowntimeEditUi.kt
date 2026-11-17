package com.lyrismet.incadent.feature.session.presentation.list.downtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.downtime_edit_hint
import incadent.shared.generated.resources.downtime_edit_hint_no_character
import incadent.shared.generated.resources.downtime_field_gained
import incadent.shared.generated.resources.downtime_field_gold
import incadent.shared.generated.resources.downtime_field_items
import incadent.shared.generated.resources.downtime_field_long
import incadent.shared.generated.resources.downtime_field_short
import incadent.shared.generated.resources.downtime_gold_gained_description
import incadent.shared.generated.resources.downtime_gold_spent_description
import incadent.shared.generated.resources.downtime_gold_unit
import incadent.shared.generated.resources.downtime_hint_in_list
import incadent.shared.generated.resources.downtime_hint_optional
import incadent.shared.generated.resources.downtime_item_add
import incadent.shared.generated.resources.downtime_item_less
import incadent.shared.generated.resources.downtime_item_more
import incadent.shared.generated.resources.downtime_item_name_placeholder
import incadent.shared.generated.resources.downtime_placeholder_gained
import incadent.shared.generated.resources.downtime_placeholder_long
import incadent.shared.generated.resources.downtime_placeholder_short
import incadent.shared.generated.resources.downtime_save
import incadent.shared.generated.resources.downtime_title_edit
import incadent.shared.generated.resources.downtime_title_new
import org.jetbrains.compose.resources.stringResource

private const val GOLD_DIGITS = 7

@Composable
internal fun DowntimeEditSheet(
    draft: DowntimeDraft,
    canApply: Boolean,
    onEvent: (DowntimeEvent) -> Unit,
) {
    val dismiss = { onEvent(DowntimeEvent.Dismissed) }
    val change = { next: DowntimeDraft -> onEvent(DowntimeEvent.DraftChanged(next)) }
    val isNew = draft.id == 0L
    AppBottomSheet(onDismissRequest = dismiss) {
        AppSheetColumn(spacing = 14.dp, scroll = true, header = {
            AppSheetHeader(
                overline = gapText(draft.afterLabel, draft.nextLabel).uppercase(),
                overlineColor = AppPalette.Parchment,
                title = stringResource(if (isNew) Res.string.downtime_title_new else Res.string.downtime_title_edit),
                onClose = dismiss,
            )
        }) {
            FormTextArea(
                label = stringResource(Res.string.downtime_field_short),
                value = draft.shortText,
                onChange = { change(draft.copy(shortText = it)) },
                placeholder = stringResource(Res.string.downtime_placeholder_short),
                maxLength = TextLimits.PHRASE,
                labelHint = stringResource(Res.string.downtime_hint_in_list),
            )
            FormTextArea(
                label = stringResource(Res.string.downtime_field_long),
                value = draft.longText,
                onChange = { change(draft.copy(longText = it)) },
                placeholder = stringResource(Res.string.downtime_placeholder_long),
                labelHint = stringResource(Res.string.downtime_hint_optional),
            )
            FormTextArea(
                label = stringResource(Res.string.downtime_field_gained),
                value = draft.gainedText,
                onChange = { change(draft.copy(gainedText = it)) },
                placeholder = stringResource(Res.string.downtime_placeholder_gained),
                maxLength = TextLimits.PHRASE,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldRow(draft, change)
                ItemsField(draft, change)
            }
            Text(
                stringResource(
                    if (canApply) Res.string.downtime_edit_hint else Res.string.downtime_edit_hint_no_character,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (canApply) AppPalette.TextTertiary else AppPalette.Amber,
            )
            EditActions(isNew, draft.canSave, onEvent)
        }
    }
}

@Composable
private fun EditActions(
    isNew: Boolean,
    canSave: Boolean,
    onEvent: (DowntimeEvent) -> Unit,
) {
    DowntimeActionRow(
        primaryText = stringResource(Res.string.downtime_save),
        onPrimary = { onEvent(DowntimeEvent.SaveClicked) },
        onDelete = if (isNew) null else ({ onEvent(DowntimeEvent.DeleteClicked) }),
        enabled = canSave,
    )
}

/** the "gold" label, the minus and plus switch, the number and the "gp" on one line */
@Composable
private fun GoldRow(
    draft: DowntimeDraft,
    change: (DowntimeDraft) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FormFieldLabel(text = stringResource(Res.string.downtime_field_gold), modifier = Modifier.weight(1f))
        SignSwitch(spent = draft.goldSpent, onChange = { change(draft.copy(goldSpent = it)) })
        AppTextInput(
            value = draft.goldAmount?.toString().orEmpty(),
            onValueChange = { typed ->
                change(draft.copy(goldAmount = typed.filter { it.isDigit() }.take(GOLD_DIGITS).toIntOrNull()))
            },
            modifier = Modifier.width(90.dp),
            placeholder = "0",
            style =
                AppTextInputStyle(
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                        ),
                    textColor = AppPalette.GoldBright,
                    height = 40.dp,
                ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text(
            stringResource(Res.string.downtime_gold_unit),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
        )
    }
}

/** the pair of "-" and "+" buttons in one bordered box, the chosen one raised */
@Composable
private fun SignSwitch(
    spent: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val spentDescription = stringResource(Res.string.downtime_gold_spent_description)
    val gainedDescription = stringResource(Res.string.downtime_gold_gained_description)
    SegmentedControl(
        items = listOf(true, false),
        onSelected = onChange,
        itemBackground = { if (it == spent) AppPalette.SurfaceElevated else AppPalette.Background },
        modifier = Modifier.width(SignSwitchWidth),
        containerShape = AppShapes.Control,
        containerBackground = AppPalette.Background,
        containerBorder = AppPalette.Border,
        itemShape = AppShapes.Small,
        itemHeight = 32.dp,
        itemSpacing = 3.dp,
        contentPadding = 3.dp,
    ) { isSpent ->
        val chosen = isSpent == spent
        val chosenColor = if (isSpent) AppPalette.MaroonBright else AppPalette.GoldBright
        Text(
            if (isSpent) "−" else "+",
            modifier = Modifier.semantics { contentDescription = if (isSpent) spentDescription else gainedDescription },
            color = if (chosen) chosenColor else AppPalette.TextTertiary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private val SignSwitchWidth = 88.dp

@Composable
private fun ItemsField(
    draft: DowntimeDraft,
    change: (DowntimeDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(text = stringResource(Res.string.downtime_field_items))
        draft.items.forEachIndexed { index, line ->
            ItemRow(
                line = line,
                onName = { change(draft.copy(items = draft.items.replaced(index, line.copy(name = it)))) },
                onQuantity = { change(draft.copy(items = draft.items.replaced(index, line.copy(quantity = it)))) },
                onRemove = { change(draft.copy(items = draft.items.filterIndexed { at, _ -> at != index })) },
            )
        }
        AppDashedTile(
            onClick = { change(draft.copy(items = draft.items + DowntimeLine(""))) },
            modifier = Modifier.align(Alignment.Start),
            minHeight = 36.dp,
        ) {
            Text(
                stringResource(Res.string.downtime_item_add),
                style = MaterialTheme.typography.labelLarge,
                color = AppPalette.TextDescription,
            )
        }
    }
}

@Composable
private fun ItemRow(
    line: DowntimeLine,
    onName: (String) -> Unit,
    onQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppTextInput(
            value = line.name,
            onValueChange = onName,
            modifier = Modifier.weight(1f),
            placeholder = stringResource(Res.string.downtime_item_name_placeholder),
            style = AppTextInputStyle(height = 44.dp),
            maxLength = TextLimits.PHRASE,
        )
        AppStepper(
            count = line.quantity,
            onDecrease = { if (line.quantity > 1) onQuantity(line.quantity - 1) else onRemove() },
            onIncrease = { onQuantity(line.quantity + 1) },
            decreaseDescription = stringResource(Res.string.downtime_item_less),
            increaseDescription = stringResource(Res.string.downtime_item_more),
        )
    }
}

internal fun List<DowntimeLine>.replaced(
    index: Int,
    line: DowntimeLine,
): List<DowntimeLine> = mapIndexed { at, old -> if (at == index) line else old }
