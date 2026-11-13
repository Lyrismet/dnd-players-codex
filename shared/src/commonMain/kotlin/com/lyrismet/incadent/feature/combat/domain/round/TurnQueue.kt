package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.combat.domain.model.Combatant

/** a place in the queue: named by the master, or a temporary one ("?N") behind the named ones */
sealed interface QueueSlot {
    val number: Int

    data class Named(
        override val number: Int,
    ) : QueueSlot

    data class Pending(
        override val number: Int,
    ) : QueueSlot
}

/** how complete the queue is, it is only a hint on the way into the fight */
sealed interface QueueStatus {
    /** nobody was named, the fight goes by the list */
    data object Unnamed : QueueStatus

    data class Partial(
        val unplaced: Int,
    ) : QueueStatus

    data object Complete : QueueStatus
}

/** the dense places of the queue: the named ones first, then the rest as ?N in the order they were added */
fun List<Combatant>.queueSlots(): Map<Long, QueueSlot> =
    inTurnOrder()
        .mapIndexed { index, member ->
            member.id to if (member.turnOrder != null) QueueSlot.Named(index + 1) else QueueSlot.Pending(index + 1)
        }.toMap()

fun List<Combatant>.queueStatus(): QueueStatus {
    val unplaced = count { it.turnOrder == null }
    return when {
        unplaced == 0 -> QueueStatus.Complete
        unplaced == size -> QueueStatus.Unnamed
        else -> QueueStatus.Partial(unplaced)
    }
}

/** the places after an edit of the queue: the new place of each participant and the turn of the one who moves */
data class QueueChange(
    val orders: Map<Long, Int?>,
    val turn: Int,
)

/** the places as they are now, what an undo of the edits writes back */
fun List<Combatant>.queueOrders(): Map<Long, Int?> = associate { it.id to it.turnOrder }

/**
 * [id] dragged to the place [toIndex] of the list, it gets a number and the named ones stay in front of the rest. The
 * one who moves keeps moving, [turn] follows the person and not the number
 */
fun List<Combatant>.movedInQueue(
    id: Long,
    toIndex: Int,
    turn: Int,
): QueueChange? {
    val ordered = inTurnOrder()
    val moved = ordered.firstOrNull { it.id == id } ?: return null
    val rest = ordered - moved
    val named = rest.filter { it.turnOrder != null }.toMutableList()
    named.add(toIndex.coerceIn(0, named.size), moved)
    val unplaced = rest.filter { it.turnOrder == null }
    val orders: Map<Long, Int?> =
        named.mapIndexed { index, member -> member.id to (index + 1) }.toMap() +
            unplaced.associate { it.id to null }
    return QueueChange(
        orders,
        (named + unplaced).indexOfFirst { it.id == ordered.getOrNull(turn)?.id }.coerceAtLeast(0),
    )
}

/** a temporary place becomes the participant's own, the ones before it in the queue are named as they stand */
fun List<Combatant>.pinnedInQueue(id: Long): Map<Long, Int?> {
    val ordered = inTurnOrder()
    val index = ordered.indexOfFirst { it.id == id }
    if (index < 0) return queueOrders()
    return ordered
        .mapIndexed { place, member ->
            member.id to (
                if (place <=
                    index
                ) {
                    place + 1
                } else {
                    member.turnOrder
                }
            )
        }.toMap()
}
