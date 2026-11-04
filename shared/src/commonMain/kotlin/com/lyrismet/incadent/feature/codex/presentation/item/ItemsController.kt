package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.defaultItemOwner
import com.lyrismet.incadent.feature.codex.domain.grouping.equipmentChanges
import com.lyrismet.incadent.feature.codex.domain.grouping.ownerOfOptionId
import com.lyrismet.incadent.feature.codex.domain.grouping.view
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_OWNER_KINDS
import com.lyrismet.incadent.feature.codex.domain.model.ITEM_QUANTITY_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.repository.ItemRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** the words the undo bar says after the items part of the screen changed something */
internal class ItemTexts(
    val gearChangedTitle: String,
)

/** owns the item sheet and the gear pick of the codex screen and writes what they decide */
internal class ItemsController(
    private val draft: MutableState<ItemDraft?>,
    private val ownerPicker: MutableState<Boolean>,
    private val equipment: MutableState<EquipmentDraft?>,
    private val repository: ItemRepository,
    private val undo: UndoController,
    private val texts: ItemTexts,
) {
    val draftValue: ItemDraft? get() = draft.value
    val ownerPickerOpen: Boolean get() = ownerPicker.value
    val equipmentValue: EquipmentDraft? get() = equipment.value

    fun handle(
        event: ItemEvent,
        scope: CoroutineScope,
        records: CodexRecords,
    ) {
        when (event) {
            ItemEvent.AddClicked -> draft.value = ItemDraft(owner = records.defaultItemOwner())
            is ItemEvent.Opened -> open(event.id, records)
            ItemEvent.SheetDismissed -> close()
            ItemEvent.SaveClicked -> save(scope)
            is ItemEvent.EquipmentRequested -> requestEquipment(event.ref, records)
            is ItemEvent.EquipmentToggled -> toggle(event.itemId)
            ItemEvent.EquipmentConfirmed -> confirmEquipment(scope, records)
            ItemEvent.EquipmentDismissed -> equipment.value = null
            else -> edit(event)
        }
    }

    // the events that change a field of the open sheet
    private fun edit(event: ItemEvent) {
        when (event) {
            is ItemEvent.NameChanged -> edit { it.copy(name = event.name) }
            is ItemEvent.QuantityStepped ->
                edit { it.copy(quantity = (it.quantity + event.delta).coerceIn(ITEM_QUANTITY_RANGE)) }
            is ItemEvent.PriceChanged -> edit { it.copy(price = event.price) }
            is ItemEvent.CoinChanged -> edit { it.copy(coin = event.coin) }
            ItemEvent.OwnerPickerOpened -> ownerPicker.value = true
            ItemEvent.OwnerPickerClosed -> ownerPicker.value = false
            is ItemEvent.OwnerPicked -> {
                ownerPicker.value = false
                edit { it.copy(owner = ownerOfOptionId(event.optionId)) }
            }
            is ItemEvent.NotesChanged -> edit { it.copy(notes = event.notes) }
            else -> Unit
        }
    }

    /** the card's stepper - a plus or a minus on a stack of more than one, a lone piece is deleted by the host */
    fun stepQuantity(
        scope: CoroutineScope,
        itemId: Long,
        delta: Int,
    ) {
        scope.launch { repository.stepQuantity(itemId, delta) }
    }

    /** opens the sheet of an item, an unknown id opens nothing */
    fun open(
        id: Long,
        records: CodexRecords,
    ) {
        records.items.find { it.id == id }?.let { draft.value = it.toDraft() }
    }

    private fun close() {
        draft.value = null
        ownerPicker.value = false
    }

    private fun edit(change: (ItemDraft) -> ItemDraft) {
        draft.value = draft.value?.let(change)
    }

    private fun save(scope: CoroutineScope) {
        val current = draft.value?.takeIf { it.canSave } ?: return
        close()
        scope.launch { repository.upsert(current.toItem()) }
    }

    private fun requestEquipment(
        ref: EntityRef,
        records: CodexRecords,
    ) {
        val kind = records.kindOf(ref) ?: return
        if (kind !in ITEM_OWNER_KINDS) return
        val owner = ItemOwner.Of(kind, ref.id)
        val carried =
            records.items
                .filter { it.owner == owner }
                .map { it.id }
                .toSet()
        equipment.value = EquipmentDraft(owner, carried)
    }

    private fun CodexRecords.kindOf(ref: EntityRef): EntityKind? =
        when (ref) {
            is EntityRef.Party -> EntityKind.PARTY.takeIf { parties.any { p -> p.id == ref.id } }
            is EntityRef.Npc -> EntityKind.NPC.takeIf { npcs.any { n -> n.id == ref.id } }
            is EntityRef.Location -> EntityKind.LOCATION.takeIf { locations.any { l -> l.id == ref.id } }
            else -> null
        }

    private fun toggle(itemId: Long) {
        equipment.value =
            equipment.value?.let {
                it.copy(selected = if (itemId in it.selected) it.selected - itemId else it.selected + itemId)
            }
    }

    private fun confirmEquipment(
        scope: CoroutineScope,
        records: CodexRecords,
    ) {
        val pick = equipment.value ?: return
        equipment.value = null
        val changes = equipmentChanges(records.items, pick.owner, pick.selected)
        if (changes.isEmpty()) return
        val before = changes.keys.associateWith { id -> records.items.first { it.id == id }.owner }
        scope.launch {
            changes.entries.groupBy({ it.value }, { it.key }).forEach { (owner, ids) ->
                repository.setOwner(ids, owner)
            }
            val name = pick.owner.view(records).name
            undo.show(texts.gearChangedTitle, name) {
                before.entries.groupBy({ it.value }, { it.key }).forEach { (owner, ids) ->
                    repository.setOwner(ids, owner)
                }
            }
        }
    }
}
