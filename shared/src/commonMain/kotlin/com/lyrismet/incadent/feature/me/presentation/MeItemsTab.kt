package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.formAreaInputStyle
import com.lyrismet.incadent.core.designsystem.component.icons.entity.icon
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.codex.presentation.item.ItemEvent
import com.lyrismet.incadent.feature.codex.presentation.item.ItemSheet
import com.lyrismet.incadent.feature.codex.presentation.item.itemPriceEachText
import com.lyrismet.incadent.feature.codex.presentation.item.itemQuantityText
import com.lyrismet.incadent.feature.me.domain.isDown
import com.lyrismet.incadent.feature.mention.presentation.MentionTextField
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.items_quantity_less
import dndplayerscodex.shared.generated.resources.items_quantity_more
import dndplayerscodex.shared.generated.resources.me_items_add
import dndplayerscodex.shared.generated.resources.me_items_title_format
import dndplayerscodex.shared.generated.resources.me_notes_placeholder
import dndplayerscodex.shared.generated.resources.me_notes_title
import org.jetbrains.compose.resources.stringResource

private const val NOTES_MIN_LINES = 4

/** the purse first so a long list of items does not bury it, then the items and the notes */
@Composable
internal fun MeItemsTab(
    character: MeCharacter,
    listState: LazyListState,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val things = character.items
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (character.vitals.isDown) item { MeDeathSaves(character, onEvent) }
        item { MeWalletTitle() }
        item {
            MeWalletCard(things.purse, character.readOnly) { onEvent(MeEvent.Wallet(WalletEvent.Opened)) }
        }
        if (things.rows.isNotEmpty()) {
            item {
                MeSectionTitle(stringResource(Res.string.me_items_title_format, things.rows.size))
            }
        }
        item { ItemLines(things.rows, character.readOnly, onEvent) }
        item { MeSectionTitle(stringResource(Res.string.me_notes_title)) }
        item { NotesField(things, character.readOnly, onEvent) }
    }
    things.sheet?.let { sheet ->
        ItemSheet(
            state = sheet,
            onEvent = { onEvent(MeEvent.Items(it)) },
            onDelete = {
                onEvent(MeEvent.ItemDeleted(sheet.draft.id))
                onEvent(MeEvent.Items(ItemEvent.SheetDismissed))
            },
        )
    }
}

@Composable
private fun ItemLines(
    rows: List<MeItemRow>,
    readOnly: Boolean,
    onEvent: (MeEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().appCard().padding(start = 14.dp, end = 8.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) AppDivider()
            ItemLine(row, readOnly, onEvent)
        }
        if (!readOnly) {
            if (rows.isNotEmpty()) AppDivider()
            Text(
                stringResource(Res.string.me_items_add),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onEvent(MeEvent.Items(ItemEvent.AddClicked)) }
                        .padding(vertical = 13.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = AppPalette.GoldBright,
            )
        }
    }
}

@Composable
private fun ItemLine(
    row: MeItemRow,
    readOnly: Boolean,
    onEvent: (MeEvent) -> Unit,
) {
    AppListRow(
        title = row.name,
        subtitle = row.price?.let { itemPriceEachText(it, row.coin) },
        onClick = { onEvent(MeEvent.Items(ItemEvent.Opened(row.id))) },
        chevron = false,
        minHeight = 60.dp,
        leading = {
            Icon(
                EntityKind.ITEM.icon(),
                contentDescription = null,
                tint = AppPalette.TextDescription,
                modifier = Modifier.size(18.dp),
            )
        },
        trailing = {
            if (readOnly) {
                Text(
                    itemQuantityText(row.quantity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextPrimary,
                )
            } else {
                AppStepper(
                    count = row.quantity,
                    onDecrease = { onEvent(MeEvent.ItemStepped(row.id, -1)) },
                    onIncrease = { onEvent(MeEvent.ItemStepped(row.id, 1)) },
                    decreaseDescription = stringResource(Res.string.items_quantity_less),
                    increaseDescription = stringResource(Res.string.items_quantity_more),
                    canIncrease = row.quantity < ITEM_QUANTITY_RANGE.last,
                    tiles = true,
                    removesAtOne = false,
                )
            }
        },
    )
}

@Composable
private fun NotesField(
    things: MeItems,
    readOnly: Boolean,
    onEvent: (MeEvent) -> Unit,
) {
    if (readOnly) {
        if (things.notes.isNotBlank()) {
            Text(things.notes, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
        }
        return
    }
    MentionTextField(
        text = things.notes,
        onTextChange = { onEvent(MeEvent.Sheet(SheetEvent.TextChanged(MeText.NOTES, it))) },
        source = things.mention.source,
        self = things.mention.self,
        placeholder = stringResource(Res.string.me_notes_placeholder),
        style = formAreaInputStyle(),
        minLines = NOTES_MIN_LINES,
    )
}
