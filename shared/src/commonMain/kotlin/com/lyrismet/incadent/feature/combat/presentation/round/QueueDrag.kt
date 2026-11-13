package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** the order of the queue while a row is dragged, held after the drop until the saved order arrives */
internal class QueueDrag {
    var draggedId by mutableStateOf<Long?>(null)
        private set
    var rows: List<RoundRow> = emptyList()
    private var order by mutableStateOf<List<Long>>(emptyList())
    private var movedId by mutableStateOf<Long?>(null)
    private var startOrder = emptyList<Long>()
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
        startOrder = rows.map { it.id }
        order = startOrder
        movedId = id
        draggedId = id
        limit = rows.count { !it.pending && it.id != id }
    }

    /** the held row went over another one, the named ones are its limit */
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

    /** the row is let go, a place it did not leave is not sent */
    fun end(onDrop: (id: Long, toIndex: Int) -> Unit) {
        val id = draggedId ?: return
        if (order == startOrder) return cancel()
        draggedId = null
        onDrop(id, order.indexOf(id))
    }

    fun cancel() {
        draggedId = null
        order = emptyList()
        movedId = null
    }

    /** the saved order is the shown one so the drag is over */
    fun settle(rowIds: List<Long>) {
        if (draggedId == null && order.isNotEmpty() && order == rowIds) cancel()
    }

    val settling: Boolean get() = draggedId == null && order.isNotEmpty()
}
