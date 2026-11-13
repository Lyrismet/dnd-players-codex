package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * the order of the queue while a row is dragged. The gesture and the movement of the rows belong to the reorderable
 * library, this keeps the order it makes (the held row cannot go past the named ones, the numbers follow it on the fly)
 * and holds it after the drop until the saved order arrives, so the list does not jump back
 */
internal class QueueDrag {
    var draggedId by mutableStateOf<Long?>(null)
        private set
    var rows: List<RoundRow> = emptyList()
    private var order by mutableStateOf<List<Long>>(emptyList())
    private var movedId by mutableStateOf<Long?>(null)
    private var limit = 0

    /** the rows in the order they are shown, numbered by the place they stand on while one is held or settling */
    fun shown(shownRows: List<RoundRow>): List<RoundRow> {
        if (order.isEmpty() || order.toSet() != shownRows.map { it.id }.toSet()) return shownRows
        val byId = shownRows.associateBy { it.id }
        return order.mapIndexed { index, id ->
            val row = checkNotNull(byId[id])
            val pending = row.pending && id != movedId
            row.copy(badge = if (pending) "?${index + 1}" else (index + 1).toString(), pending = pending)
        }
    }

    fun start(id: Long) {
        order = rows.map { it.id }
        movedId = id
        draggedId = id
        limit = rows.count { !it.pending && it.id != id }
    }

    /** the held row went over another one, [from] and [to] are places in the queue and the named ones are its limit */
    fun move(
        from: Int,
        to: Int,
    ) {
        val id = draggedId ?: return
        val at = order.indexOf(id)
        val target = to.coerceIn(0, limit)
        if (at < 0 || at != from.coerceIn(0, order.lastIndex) || target == at) return
        order = order.toMutableList().apply { add(target, removeAt(at)) }
    }

    /** the row is let go, the place it got is sent and the shown order stays until it comes back */
    fun end(onDrop: (id: Long, toIndex: Int) -> Unit) {
        val id = draggedId ?: return
        draggedId = null
        onDrop(id, order.indexOf(id))
    }

    fun cancel() {
        draggedId = null
        order = emptyList()
        movedId = null
    }

    /** the saved order is the shown one: the drag is over */
    fun settle(rowIds: List<Long>) {
        if (draggedId == null && order.isNotEmpty() && order == rowIds) cancel()
    }

    val settling: Boolean get() = draggedId == null && order.isNotEmpty()
}
