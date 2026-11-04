package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.ItemOwnerView
import com.lyrismet.incadent.feature.codex.domain.grouping.optionId
import com.lyrismet.incadent.feature.codex.domain.grouping.view
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.presentation.MentionField
import com.lyrismet.incadent.feature.mention.presentation.MentionSource
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.items_owner_me
import org.jetbrains.compose.resources.stringResource

/** the rows of the items list for what is typed in the search, name and notes are looked at */
internal fun itemRows(
    records: CodexRecords,
    query: String,
): List<ItemCodexItem> {
    val needle = query.trim().foldForSearch()
    return records.items
        .filter {
            needle.isEmpty() ||
                it.name.foldForSearch().contains(needle) ||
                it.notes.foldForSearch().contains(needle)
        }.map {
            ItemCodexItem(it.id, it.name, it.quantity, it.price, it.coin, it.owner.view(records), it.photo)
        }
}

/** the owners an item can be given, grouped by the kind of record - the player's own character is marked */
internal fun itemOwnerOptions(
    records: CodexRecords,
    kindLabels: Map<EntityKind, String>,
    meLabel: String,
): List<SelectOption> {
    fun option(
        kind: EntityKind,
        id: Long,
        name: String,
        note: String? = null,
    ) = SelectOption(ItemOwner.Of(kind, id).optionId(), name, note, group = kindLabels[kind])
    return records.parties.map {
        option(
            EntityKind.PARTY,
            it.id,
            it.name,
            meLabel.takeIf { _ ->
                it.isPlayerCharacter
            },
        )
    } +
        records.npcs.map { option(EntityKind.NPC, it.id, it.name) } +
        records.locations.map { option(EntityKind.LOCATION, it.id, it.name) }
}

/** the rows of the gear pick of [pick]'s owner - what is ticked, then everything else */
internal fun equipmentSheetState(
    records: CodexRecords,
    pick: EquipmentDraft,
): EquipmentSheetState {
    fun row(item: Item): EquipmentRow {
        val holder = item.owner.view(records)
        return EquipmentRow(
            item.id,
            item.name,
            item.quantity,
            holder.takeIf {
                item.owner != pick.owner &&
                    it.kind != null
            },
        )
    }
    val (selected, others) = records.items.sortedBy { it.name.lowercase() }.partition { it.id in pick.selected }
    return EquipmentSheetState(
        ownerName = pick.owner.view(records).name,
        selected = selected.map(::row),
        others = others.map(::row),
    )
}

/** everything the items part of the screen shows, built from the controller's drafts and the records */
@Composable
internal fun rememberItemsState(
    controller: ItemsController,
    records: CodexRecords,
    query: String,
    kindLabels: Map<EntityKind, String>,
    mention: MentionSource,
): ItemsState {
    val meLabel = stringResource(Res.string.items_owner_me)
    val rows =
        remember(records.items, records.parties, records.npcs, records.locations, query) { itemRows(records, query) }
    val draft = controller.draftValue
    val pick = controller.equipmentValue
    return ItemsState(
        rows = rows,
        total = records.items.size,
        sheet =
            draft?.let {
                ItemSheetState(
                    draft = it,
                    owner = it.owner.view(records),
                    ownerPickerOpen = controller.ownerPickerOpen,
                    ownerOptions = itemOwnerOptions(records, kindLabels, meLabel),
                    ownerSelectedId = it.owner.optionId(),
                    mention =
                        MentionField(
                            mention,
                            it.id
                                .takeIf { id ->
                                    id != 0L
                                }?.let { id -> MentionRef(EntityKind.ITEM, id) },
                        ),
                )
            },
        equipment = pick?.let { equipmentSheetState(records, it) },
    )
}

internal val ItemOwnerView.isShared: Boolean get() = kind == null
