package com.lyrismet.incadent.feature.session.presentation.list.downtime

import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.model.DowntimeItem
import kotlin.math.abs

/** one thing gained, as the card and the editor show it */
data class DowntimeLine(
    val name: String,
    val quantity: Int = 1,
)

/** a downtime ready to render - the labels are the numbers of the two sessions around the gap */
data class DowntimeCard(
    val id: Long,
    val afterSessionId: Long,
    val afterLabel: String,
    // null when the earlier session is the last one and nothing follows the gap
    val nextLabel: String?,
    val shortText: String,
    val longText: String,
    val gainedText: String,
    // below zero when gold was spent
    val goldDelta: Int,
    val items: List<DowntimeLine>,
    val applied: Boolean,
) {
    val hasLoot: Boolean get() = goldDelta != 0 || items.isNotEmpty()
    val isEmpty: Boolean get() = shortText.isBlank() && longText.isBlank() && gainedText.isBlank() && !hasLoot
}

/** the open editor, [goldAmount] is what is typed and [goldSpent] says which way it goes */
data class DowntimeDraft(
    val afterSessionId: Long,
    val afterLabel: String,
    val nextLabel: String?,
    val id: Long = 0,
    val shortText: String = "",
    val longText: String = "",
    val gainedText: String = "",
    val goldAmount: Int? = null,
    val goldSpent: Boolean = false,
    val items: List<DowntimeLine> = emptyList(),
) {
    val canSave: Boolean get() = shortText.isNotBlank()
}

/** which downtime window is open over the list */
sealed interface DowntimeSheetState {
    data object Hidden : DowntimeSheetState

    /** [canApply] is false while no character is marked as mine, the sheet then says where to pick one */
    data class Detail(
        val card: DowntimeCard,
        val canApply: Boolean,
        val readOnly: Boolean,
    ) : DowntimeSheetState

    /** [canApply] is false while no character is marked as mine, the gold and things are then dimmed */
    data class Edit(
        val draft: DowntimeDraft,
        val canApply: Boolean,
    ) : DowntimeSheetState
}

sealed interface DowntimeEvent {
    /** a new downtime in the gap after the session [afterSessionId], named as the tapped tile names it */
    data class NewClicked(
        val afterSessionId: Long,
        val afterLabel: String,
        val nextLabel: String?,
    ) : DowntimeEvent

    data class CardClicked(
        val id: Long,
    ) : DowntimeEvent

    data object EditClicked : DowntimeEvent

    data object DeleteClicked : DowntimeEvent

    data object ApplyClicked : DowntimeEvent

    data object RevertClicked : DowntimeEvent

    data class DraftChanged(
        val draft: DowntimeDraft,
    ) : DowntimeEvent

    data object SaveClicked : DowntimeEvent

    data object Dismissed : DowntimeEvent
}

internal fun Downtime.toCard(
    afterLabel: String,
    nextLabel: String?,
) = DowntimeCard(
    id = id,
    afterSessionId = afterSessionId,
    afterLabel = afterLabel,
    nextLabel = nextLabel,
    shortText = shortText,
    longText = longText,
    gainedText = gainedText,
    goldDelta = goldDelta,
    items = items.map { DowntimeLine(it.name, it.quantity) },
    applied = isApplied,
)

internal fun DowntimeCard.toDraft() =
    DowntimeDraft(
        afterSessionId = afterSessionId,
        afterLabel = afterLabel,
        nextLabel = nextLabel,
        id = id,
        shortText = shortText,
        longText = longText,
        gainedText = gainedText,
        goldAmount = goldDelta.let { if (it == 0) null else abs(it) },
        goldSpent = goldDelta < 0,
        items = items,
    )

/** what the editor holds as a downtime, a thing without a name is dropped and the sheet link of [previous] is kept */
internal fun DowntimeDraft.toDowntime(previous: Downtime?): Downtime {
    val amount = goldAmount ?: 0
    return Downtime(
        id = id,
        afterSessionId = afterSessionId,
        shortText = shortText.trim(),
        longText = longText.trim(),
        gainedText = gainedText.trim(),
        goldDelta = if (goldSpent) -amount else amount,
        items = items.filter { it.name.isNotBlank() }.map { DowntimeItem(it.name.trim(), it.quantity) },
        appliedTo = previous?.appliedTo,
        appliedGold = previous?.appliedGold ?: 0,
        originId = previous?.originId.orEmpty(),
    )
}

/** the gold change with an explicit sign, "+120" or "-30" */
internal fun signedGold(delta: Int): String = (if (delta > 0) "+" else "-") + abs(delta)
