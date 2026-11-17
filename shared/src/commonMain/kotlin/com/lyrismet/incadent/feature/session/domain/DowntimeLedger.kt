package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.ItemRepository
import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.model.DowntimeItem

/** puts the gold and the things of a downtime into a character's sheet and takes exactly that back out */
class DowntimeLedger(
    private val sheets: CharacterSheetRepository,
    private val items: ItemRepository,
) {
    /** [downtime] as it stands once its gold and items are in the sheet of [memberId] */
    suspend fun apply(
        downtime: Downtime,
        memberId: Long,
    ): Downtime {
        if (downtime.goldDelta != 0) {
            sheets.updateSheet(memberId) { it.copy(purse = it.purse.moved(Coin.GOLD, downtime.goldDelta)) }
        }
        val owner = ItemOwner.Of(EntityKind.PARTY, memberId)
        val placed =
            downtime.items.map { gained ->
                val id = items.upsert(Item(id = 0, name = gained.name, quantity = gained.quantity, owner = owner))
                gained.copy(itemId = id)
            }
        return downtime.copy(items = placed, appliedTo = memberId)
    }

    /** [downtime] with its gold and items taken back out of the sheet, a thing already used up is left alone */
    suspend fun revert(downtime: Downtime): Downtime {
        val memberId = downtime.appliedTo ?: return downtime
        if (downtime.goldDelta != 0) {
            sheets.updateSheet(memberId) { it.copy(purse = it.purse.moved(Coin.GOLD, -downtime.goldDelta)) }
        }
        downtime.items.forEach { gained -> takeBack(gained) }
        return downtime.copy(items = downtime.items.map { it.copy(itemId = 0) }, appliedTo = null)
    }

    private suspend fun takeBack(gained: DowntimeItem) {
        val item = items.getById(gained.itemId) ?: return
        val left = item.quantity - gained.quantity
        if (left <= 0) items.delete(item.id) else items.upsert(item.copy(quantity = left))
    }
}
