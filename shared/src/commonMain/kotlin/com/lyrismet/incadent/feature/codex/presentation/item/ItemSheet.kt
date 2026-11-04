package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSelectSheet
import com.lyrismet.incadent.core.designsystem.component.AppSelectTexts
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_PRICE_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.mention.presentation.MentionTextField
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_search_empty_title
import dndplayerscodex.shared.generated.resources.codex_search_placeholder
import dndplayerscodex.shared.generated.resources.items_delete
import dndplayerscodex.shared.generated.resources.items_name_label
import dndplayerscodex.shared.generated.resources.items_name_placeholder
import dndplayerscodex.shared.generated.resources.items_notes_label
import dndplayerscodex.shared.generated.resources.items_notes_placeholder
import dndplayerscodex.shared.generated.resources.items_owner_label
import dndplayerscodex.shared.generated.resources.items_owner_pick_title
import dndplayerscodex.shared.generated.resources.items_owner_shared
import dndplayerscodex.shared.generated.resources.items_price_label
import dndplayerscodex.shared.generated.resources.items_price_placeholder
import dndplayerscodex.shared.generated.resources.items_quantity_label
import dndplayerscodex.shared.generated.resources.items_quantity_less
import dndplayerscodex.shared.generated.resources.items_quantity_more
import dndplayerscodex.shared.generated.resources.items_save
import dndplayerscodex.shared.generated.resources.items_sheet_title_edit
import dndplayerscodex.shared.generated.resources.items_sheet_title_new
import org.jetbrains.compose.resources.stringResource

private const val PRICE_DIGITS = 7

/** the one sheet that adds an item and edits it - [onDelete] is asked for an item that already exists */
@Composable
internal fun ItemSheet(
    state: ItemSheetState,
    onEvent: (ItemEvent) -> Unit,
    onDelete: () -> Unit,
) {
    val draft = state.draft
    AppBottomSheet(onDismissRequest = { onEvent(ItemEvent.SheetDismissed) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppSheetHeader(
                title =
                    stringResource(
                        if (draft.isNew) Res.string.items_sheet_title_new else Res.string.items_sheet_title_edit,
                    ),
                onClose = { onEvent(ItemEvent.SheetDismissed) },
            )
            FormTextField(
                label = stringResource(Res.string.items_name_label),
                value = draft.name,
                onChange = { onEvent(ItemEvent.NameChanged(it)) },
                placeholder = stringResource(Res.string.items_name_placeholder),
                required = true,
            )
            ItemQuantityField(draft.quantity, draft.isNew, onEvent, onDelete)
            ItemPriceFields(draft.price, draft.coin, onEvent)
            FormPickField(
                label = stringResource(Res.string.items_owner_label),
                placeholder = stringResource(Res.string.items_owner_shared),
                onClick = { onEvent(ItemEvent.OwnerPickerOpened) },
                title = state.owner.text(),
            )
            ItemNotesField(state, onEvent)
            FormPrimaryButton(
                text = stringResource(Res.string.items_save),
                onClick = { onEvent(ItemEvent.SaveClicked) },
                enabled = draft.canSave,
            )
            if (!draft.isNew) {
                AppButton(
                    text = stringResource(Res.string.items_delete),
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    tone = ButtonTone.Maroon,
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Large,
                )
            }
        }
    }
    if (state.ownerPickerOpen) ItemOwnerPicker(state, onEvent)
}

@Composable
private fun ItemQuantityField(
    quantity: Int,
    isNew: Boolean,
    onEvent: (ItemEvent) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormFieldLabel(text = stringResource(Res.string.items_quantity_label))
        AppStepper(
            count = quantity,
            // the last piece taken away removes the item, a new one has nothing to remove yet
            onDecrease = {
                when {
                    quantity > ITEM_QUANTITY_RANGE.first -> onEvent(ItemEvent.QuantityStepped(-1))
                    !isNew -> onDelete()
                }
            },
            onIncrease = { onEvent(ItemEvent.QuantityStepped(1)) },
            decreaseDescription = stringResource(Res.string.items_quantity_less),
            increaseDescription = stringResource(Res.string.items_quantity_more),
            canIncrease = quantity < ITEM_QUANTITY_RANGE.last,
        )
    }
}

@Composable
private fun ItemPriceFields(
    price: Int?,
    coin: Coin,
    onEvent: (ItemEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormFieldLabel(text = stringResource(Res.string.items_price_label))
        AppTextInput(
            value = price?.toString().orEmpty(),
            onValueChange = { typed ->
                val digits = typed.filter { it.isDigit() }.take(PRICE_DIGITS)
                onEvent(ItemEvent.PriceChanged(digits.toIntOrNull()?.coerceAtMost(ITEM_PRICE_RANGE.last)))
            },
            placeholder = stringResource(Res.string.items_price_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (price != null) {
            FormChipPicker(
                label = EMPTY_VALUE_DASH,
                options = Coin.entries.map { FormChipOption(it, coinLabel(it), it == coin) },
                onClick = { onEvent(ItemEvent.CoinChanged(it)) },
            )
        }
    }
}

@Composable
private fun ItemNotesField(
    state: ItemSheetState,
    onEvent: (ItemEvent) -> Unit,
) {
    val mention = state.mention
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(text = stringResource(Res.string.items_notes_label))
        if (mention != null) {
            MentionTextField(
                text = state.draft.notes,
                onTextChange = { onEvent(ItemEvent.NotesChanged(it)) },
                source = mention.source,
                self = mention.self,
                placeholder = stringResource(Res.string.items_notes_placeholder),
                minLines = 3,
            )
        }
    }
}

@Composable
private fun ItemOwnerPicker(
    state: ItemSheetState,
    onEvent: (ItemEvent) -> Unit,
) {
    val selected = if (state.ownerSelectedId == 0L) emptySet() else setOf(state.ownerSelectedId)
    AppSelectSheet(
        texts =
            AppSelectTexts(
                title = stringResource(Res.string.items_owner_pick_title),
                searchPlaceholder = stringResource(Res.string.codex_search_placeholder),
                emptyText = stringResource(Res.string.codex_search_empty_title),
                multiHint = "",
                clearLabel = stringResource(Res.string.items_owner_shared),
                doneLabel = { "" },
                createLabel = null,
            ),
        options = state.ownerOptions,
        selectedIds = selected,
        multi = false,
        onConfirm = { ids -> onEvent(ItemEvent.OwnerPicked(ids.firstOrNull() ?: 0L)) },
        onDismiss = { onEvent(ItemEvent.OwnerPickerClosed) },
    )
}
