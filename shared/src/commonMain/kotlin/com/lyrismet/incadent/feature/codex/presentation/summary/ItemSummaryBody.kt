package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.codex.presentation.item.coinAmountText
import com.lyrismet.incadent.feature.codex.presentation.item.itemQuantityText
import com.lyrismet.incadent.feature.codex.presentation.item.text
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.items_card_owner
import incadent.shared.generated.resources.items_card_price
import incadent.shared.generated.resources.items_card_quantity
import incadent.shared.generated.resources.items_equipment_empty
import incadent.shared.generated.resources.items_equipment_hint
import incadent.shared.generated.resources.items_equipment_title
import incadent.shared.generated.resources.items_quantity_less
import incadent.shared.generated.resources.items_quantity_more
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ItemSummaryBody(
    item: EntitySummaryItem.ItemSummary,
    actions: EntitySummarySheetActions,
) {
    EntityHeader(
        emblem = item.emblem,
        title = item.name,
        subtitle = "",
        showEditHint = false,
    )
    Column(
        modifier = Modifier.fillMaxWidth().appCard(shape = AppShapes.Card, background = AppPalette.Background),
    ) {
        ItemFactLine(stringResource(Res.string.items_card_quantity)) { ItemQuantityValue(item, actions) }
        item.price?.let { price ->
            ItemFactLine(stringResource(Res.string.items_card_price)) { ItemFactText(coinAmountText(price, item.coin)) }
        }
        ItemFactLine(stringResource(Res.string.items_card_owner)) {
            val chip = item.ownerChip
            if (chip != null) {
                MentionChip(item = chip, onClick = { chip.entityRef?.let(actions.onEntityRefClicked) })
            } else {
                ItemFactText(item.owner.text())
            }
        }
    }
    EntityDescription(item.notes, item.descriptionSegments, actions)
    EntityRelatedNotesSection(item.name, item.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(item.ref, actions)
}

@Composable
private fun ItemFactLine(
    label: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormFieldLabel(text = label)
        value()
    }
}

@Composable
private fun ItemFactText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
}

// a card that cannot change the stack shows the count, one that can shows the stepper whose last piece deletes
@Composable
private fun ItemQuantityValue(
    item: EntitySummaryItem.ItemSummary,
    actions: EntitySummarySheetActions,
) {
    val step = actions.onItemQuantityStep
    if (step == null) {
        ItemFactText(itemQuantityText(item.quantity))
        return
    }
    AppStepper(
        count = item.quantity,
        onDecrease = {
            if (item.quantity > ITEM_QUANTITY_RANGE.first) {
                step(item.ref.id, -1)
            } else {
                actions.onDeleteClicked?.invoke(item.ref)
            }
        },
        onIncrease = { step(item.ref.id, 1) },
        decreaseDescription = stringResource(Res.string.items_quantity_less),
        increaseDescription = stringResource(Res.string.items_quantity_more),
        canIncrease = item.quantity < ITEM_QUANTITY_RANGE.last,
    )
}

/** what a character, an npc or a place carries - held, it opens the gear pick */
@Composable
internal fun EntityGearSection(
    ref: EntityRef,
    gear: List<MentionChipItem>,
    actions: EntitySummarySheetActions,
) {
    val held = actions.onGearHeld
    if (gear.isEmpty() && held == null) return
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = held?.let { { it(ref) } }),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormFieldLabel(text = stringResource(Res.string.items_equipment_title))
        if (gear.isEmpty()) {
            Text(
                stringResource(Res.string.items_equipment_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextTertiary,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                gear.forEach { chip ->
                    MentionChip(item = chip, onClick = { chip.entityRef?.let(actions.onEntityRefClicked) })
                }
            }
        }
        if (held != null) {
            Text(
                stringResource(Res.string.items_equipment_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
            )
        }
    }
}
