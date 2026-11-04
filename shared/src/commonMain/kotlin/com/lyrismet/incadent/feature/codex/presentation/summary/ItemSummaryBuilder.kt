package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.designsystem.component.PortraitMark
import com.lyrismet.incadent.core.designsystem.component.icons.entity.EntityIcons
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.carriedBy
import com.lyrismet.incadent.feature.codex.domain.grouping.view
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.presentation.style.ItemMentionColor
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.mention.presentation.toMentionChip

internal fun buildItemSummary(
    item: Item,
    lookup: EntityLookup,
): EntitySummaryItem.ItemSummary {
    val records = lookup.asRecords()
    return EntitySummaryItem.ItemSummary(
        ref = EntityRef.Item(item.id),
        name = item.name,
        quantity = item.quantity,
        price = item.price,
        coin = item.coin,
        owner = item.owner.view(records),
        ownerChip = ownerChipOf(item.owner, lookup),
        notes = item.notes,
        descriptionSegments = noteSegments(item.notes, lookup),
        emblem =
            EntityEmblem(
                mark = PortraitMark.Icon(EntityIcons.Item),
                shape = EntityEmblemShape.ROUNDED,
                color = ItemMentionColor,
                portrait = item.photo,
            ),
        relatedNotes = buildRelatedNotes(EntityRef.Item(item.id), lookup),
    )
}

// the card of whoever carries the item, so the card of the item can open it
private fun ownerChipOf(
    owner: ItemOwner,
    lookup: EntityLookup,
): MentionChipItem? =
    (owner as? ItemOwner.Of)?.let { held ->
        when (held.kind) {
            EntityKind.PARTY ->
                lookup.parties
                    .find {
                        it.id == held.id
                    }?.let { MentionEntity.PartyMention(it.id, it.name).toChipItem() }
            EntityKind.NPC -> lookup.npcs.find { it.id == held.id }?.toMentionChip()
            EntityKind.LOCATION -> lookup.locations.find { it.id == held.id }?.toMentionChip()
            else -> null
        }
    }

/** what an owner carries, as chips that open the item cards */
internal fun gearOf(
    kind: EntityKind,
    id: Long,
    lookup: EntityLookup,
): List<MentionChipItem> =
    lookup.items.carriedBy(ItemOwner.Of(kind, id)).map { MentionEntity.ItemMention(it.id, it.name).toChipItem() }
