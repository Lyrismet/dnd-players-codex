package com.lyrismet.incadent.feature.session.domain.model

/** what the party did between two sessions, kept with the earlier one ([afterSessionId]) and not a session itself */
data class Downtime(
    val id: Long,
    val afterSessionId: Long,
    val shortText: String = "",
    val longText: String = "",
    val gainedText: String = "",
    // gold gained, below zero when it was spent
    val goldDelta: Int = 0,
    val items: List<DowntimeItem> = emptyList(),
    // the party member whose sheet took the gold and the items, null while nothing is put into a sheet
    val appliedTo: Long? = null,
    // the gold the purse really took, less than [goldDelta] when spending or earning hit the purse's limits
    val appliedGold: Int = 0,
    val originId: String = "",
) {
    val isApplied: Boolean get() = appliedTo != null

    /** there is something to put into a sheet */
    val hasLoot: Boolean get() = goldDelta != 0 || items.isNotEmpty()

    /** [other] puts the same gold and the same things into a sheet, so an applied downtime need not be redone */
    fun sameLootAs(other: Downtime): Boolean =
        goldDelta == other.goldDelta &&
            items.map { it.name to it.quantity } == other.items.map { it.name to it.quantity }
}

/** one thing gained, [itemId] points at the sheet's item it became while the downtime is applied */
data class DowntimeItem(
    val name: String,
    val quantity: Int = 1,
    val itemId: Long = 0,
)
