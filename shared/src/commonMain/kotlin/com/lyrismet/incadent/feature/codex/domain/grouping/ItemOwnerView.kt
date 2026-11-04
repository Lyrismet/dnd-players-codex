package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner

/** who carries an item as the screens tell it - [kind] is null for a shared item, [isMe] marks the player's own */
data class ItemOwnerView(
    val kind: EntityKind?,
    val name: String,
    val isMe: Boolean = false,
) {
    companion object {
        val SHARED = ItemOwnerView(null, "")
    }
}

/** the owner as words, a record that is gone reads as shared so the item is never lost from view */
fun ItemOwner.view(records: CodexRecords): ItemOwnerView =
    when (this) {
        ItemOwner.Shared -> ItemOwnerView.SHARED
        is ItemOwner.Of ->
            when (kind) {
                EntityKind.PARTY ->
                    records.parties.find { it.id == id }?.let { ItemOwnerView(kind, it.name, it.isPlayerCharacter) }
                EntityKind.NPC -> records.npcs.find { it.id == id }?.let { ItemOwnerView(kind, it.name) }
                EntityKind.LOCATION -> records.locations.find { it.id == id }?.let { ItemOwnerView(kind, it.name) }
                else -> null
            } ?: ItemOwnerView.SHARED
    }

/** the owner a new item starts with - the player's own character, shared while the party has none */
fun CodexRecords.defaultItemOwner(): ItemOwner =
    parties.firstOrNull { it.isPlayerCharacter }?.let { ItemOwner.Of(EntityKind.PARTY, it.id) } ?: ItemOwner.Shared
