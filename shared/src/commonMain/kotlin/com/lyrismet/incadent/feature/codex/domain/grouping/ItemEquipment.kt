package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner

private const val OPTION_KINDS = 4L
private const val PARTY_CODE = 1L
private const val NPC_CODE = 2L
private const val LOCATION_CODE = 3L

/** the new owner of every item a gear pick changes - ticked ones move to [owner], unticked ones it carried go shared */
fun equipmentChanges(
    items: List<Item>,
    owner: ItemOwner.Of,
    selected: Set<Long>,
): Map<Long, ItemOwner> =
    buildMap {
        items.forEach { item ->
            val wanted =
                when {
                    item.id in selected -> owner
                    item.owner == owner -> ItemOwner.Shared
                    else -> item.owner
                }
            if (wanted != item.owner) put(item.id, wanted)
        }
    }

/** what an item owner is worth as an id of a select option - 0 is shared, the rest keep the record id and its kind */
fun ItemOwner.optionId(): Long =
    when (this) {
        ItemOwner.Shared -> 0L
        is ItemOwner.Of -> id * OPTION_KINDS + kind.optionKindCode()
    }

/** the owner an option id stands for, shared for 0 and for anything unknown */
fun ownerOfOptionId(optionId: Long): ItemOwner {
    val kind =
        when (optionId % OPTION_KINDS) {
            PARTY_CODE -> EntityKind.PARTY
            NPC_CODE -> EntityKind.NPC
            LOCATION_CODE -> EntityKind.LOCATION
            else -> null
        }
    return if (kind == null || optionId <= 0L) ItemOwner.Shared else ItemOwner.Of(kind, optionId / OPTION_KINDS)
}

private fun EntityKind.optionKindCode(): Long =
    when (this) {
        EntityKind.PARTY -> PARTY_CODE
        EntityKind.NPC -> NPC_CODE
        EntityKind.LOCATION -> LOCATION_CODE
        else -> 0L
    }

/** the items [owner] carries, by name */
fun List<Item>.carriedBy(owner: ItemOwner): List<Item> = filter { it.owner == owner }.sortedBy { it.name.lowercase() }
