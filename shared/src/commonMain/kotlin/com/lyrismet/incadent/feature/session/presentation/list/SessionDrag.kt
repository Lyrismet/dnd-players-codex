package com.lyrismet.incadent.feature.session.presentation.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** the order of the past sessions while one is held, kept after the drop until the saved dates arrive */
internal class SessionDrag {
    var draggedId by mutableStateOf<Long?>(null)
        private set
    private var order by mutableStateOf<List<Long>>(emptyList())
    private var startAt = -1

    /** [rows] as they are laid out now */
    fun shown(rows: List<SessionListItem>): List<SessionListItem> {
        if (order.isEmpty() || order.toSet() != rows.map { it.id }.toSet()) return rows
        val byId = rows.associateBy { it.id }
        return order.mapNotNull { byId[it] }
    }

    fun start(
        id: Long,
        rows: List<SessionListItem>,
    ) {
        order = rows.map { it.id }
        startAt = order.indexOf(id)
        draggedId = id
    }

    /** the held session went over another one */
    fun move(
        from: Any?,
        to: Any?,
    ) {
        val at = order.indexOf(from as? Long)
        val target = order.indexOf(to as? Long)
        if (at < 0 || target < 0 || at == target) return
        order = order.toMutableList().apply { add(target, removeAt(at)) }
    }

    /** the session is let go, [onMoved] hears where it landed unless it came back to its own place */
    fun end(onMoved: (id: Long, toIndex: Int) -> Unit) {
        val id = draggedId ?: return
        draggedId = null
        val at = order.indexOf(id)
        if (at == startAt) order = emptyList() else onMoved(id, at)
    }

    /** the saved dates have had their time to arrive, what is listed is what is shown */
    fun settle() {
        if (draggedId == null) order = emptyList()
    }
}
