package com.lyrismet.incadent.feature.codex.presentation.item

import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.grouping.ItemOwnerView
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_PRICE_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.presentation.MentionField
import com.slack.circuit.runtime.CircuitUiEvent

/** what the party tab lists - the characters or the things they carry */
enum class PartyListMode {
    CHARACTERS,
    ITEMS,
}

/** one row of the items list, ready to draw */
data class ItemCodexItem(
    val id: Long,
    val name: String,
    val quantity: Int,
    val price: Int?,
    val coin: Coin,
    val owner: ItemOwnerView,
    val photo: Photo?,
)

/** the item the sheet is adding or editing, [id] 0 while it is new */
data class ItemDraft(
    val id: Long = 0,
    val originId: String = "",
    val name: String = "",
    val quantity: Int = ITEM_QUANTITY_RANGE.first,
    val price: Int? = null,
    val coin: Coin = Coin.GOLD,
    val owner: ItemOwner = ItemOwner.Shared,
    val notes: String = "",
    val photo: Photo? = null,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()

    fun toItem(): Item =
        Item(
            id = id,
            name = name.trim(),
            quantity = quantity.coerceIn(ITEM_QUANTITY_RANGE),
            price = price?.coerceIn(ITEM_PRICE_RANGE),
            coin = coin,
            owner = owner,
            notes = notes,
            photo = photo,
            originId = originId,
        )
}

internal fun Item.toDraft() = ItemDraft(id, originId, name, quantity, price, coin, owner, notes, photo)

/** the sheet to add or edit one item - everything it shows beyond the [draft] is already worded */
data class ItemSheetState(
    val draft: ItemDraft,
    val owner: ItemOwnerView,
    val ownerPickerOpen: Boolean,
    val ownerOptions: List<SelectOption>,
    val ownerSelectedId: Long,
    val mention: MentionField?,
)

/** one item of the gear pick, [from] names the owner it would leave when that is somebody else */
data class EquipmentRow(
    val id: Long,
    val name: String,
    val quantity: Int,
    val from: ItemOwnerView?,
)

/** the "what is in the gear" sheet of one owner */
data class EquipmentSheetState(
    val ownerName: String,
    val selected: List<EquipmentRow>,
    val others: List<EquipmentRow>,
)

/** the items part of the codex screen - the rows and the two sheets */
data class ItemsState(
    val rows: List<ItemCodexItem> = emptyList(),
    // every item of the campaign, so an empty search is told from an empty list
    val total: Int = 0,
    val sheet: ItemSheetState? = null,
    val equipment: EquipmentSheetState? = null,
)

/** the gear pick under way, [selected] being the ticked ids */
internal data class EquipmentDraft(
    val owner: ItemOwner.Of,
    val selected: Set<Long>,
)

sealed interface ItemEvent : CircuitUiEvent {
    data object AddClicked : ItemEvent

    data class Opened(
        val id: Long,
    ) : ItemEvent

    data object SheetDismissed : ItemEvent

    data class NameChanged(
        val name: String,
    ) : ItemEvent

    data class QuantityStepped(
        val delta: Int,
    ) : ItemEvent

    data class PriceChanged(
        val price: Int?,
    ) : ItemEvent

    data class CoinChanged(
        val coin: Coin,
    ) : ItemEvent

    data object OwnerPickerOpened : ItemEvent

    data object OwnerPickerClosed : ItemEvent

    data class OwnerPicked(
        val optionId: Long,
    ) : ItemEvent

    data class NotesChanged(
        val notes: String,
    ) : ItemEvent

    data object SaveClicked : ItemEvent

    /** the owner's card was held - the gear pick opens for that record */
    data class EquipmentRequested(
        val ref: EntityRef,
    ) : ItemEvent

    data class EquipmentToggled(
        val itemId: Long,
    ) : ItemEvent

    data object EquipmentConfirmed : ItemEvent

    data object EquipmentDismissed : ItemEvent
}
