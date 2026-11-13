package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.inTurnOrder
import com.lyrismet.incadent.feature.combat.domain.round.movedInQueue
import com.lyrismet.incadent.feature.combat.domain.round.pinnedInQueue
import com.lyrismet.incadent.feature.combat.domain.round.queueOrders
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_queue_edit_changed
import incadent.shared.generated.resources.combat_queue_edit_undo
import org.jetbrains.compose.resources.getString

/** the queue as it was when the edit began, what the undo of the whole edit writes back */
internal data class QueueSnapshot(
    val orders: Map<Long, Int?>,
    val turn: Int,
)

/** the edit of the queue in a fight: the drags, the pins, the pass of the turn and the undo of all of them */
internal class QueueHandler(
    private val editor: RoundEditor,
    private val repository: CombatRepository,
    private val editing: MutableState<QueueSnapshot?>,
    private val undo: UndoController,
    private val selected: MutableState<Long?>,
) {
    fun onEvent(event: CombatRoundEvent.QueueEvent) {
        when (event) {
            CombatRoundEvent.QueueEditStarted -> editor.edit { begin(it) }
            CombatRoundEvent.QueueEditFinished -> finish()
            is CombatRoundEvent.QueueMoved ->
                editor.edit { combat ->
                    combat.combatants
                        .movedInQueue(
                            event.id,
                            event.toIndex,
                            combat.turn,
                        )?.let { write(combat, it.orders, it.turn) }
                }
            is CombatRoundEvent.QueueTileTapped -> editor.edit { tapped(it, event.id) }
        }
    }

    private fun begin(combat: Combat) {
        editing.value = QueueSnapshot(combat.combatants.queueOrders(), combat.turn)
    }

    private suspend fun tapped(
        combat: Combat,
        id: Long,
    ) {
        val member = combat.combatants.firstOrNull { it.id == id } ?: return
        when {
            editing.value == null -> if (member.turnOrder == null) begin(combat)
            member.turnOrder == null -> write(combat, combat.combatants.pinnedInQueue(id), combat.turn)
            else -> {
                // the turn passes to the participant, the round and the time stay
                val turn = combat.combatants.inTurnOrder().indexOfFirst { it.id == id }
                repository.setTurn(combat.id, combat.round, turn)
                selected.value = id
            }
        }
    }

    private suspend fun write(
        combat: Combat,
        orders: Map<Long, Int?>,
        turn: Int,
    ) {
        repository.setQueue(combat.id, combat.round, orders, turn)
    }

    private fun finish() =
        editor.edit { combat ->
            val before = editing.value
            editing.value = null
            if (before != null && before.orders != combat.combatants.queueOrders()) {
                undo.show(
                    getString(Res.string.combat_queue_edit_changed),
                    getString(Res.string.combat_queue_edit_undo),
                ) {
                    editor.edit { now -> write(now, before.orders, before.turn) }
                }
            }
        }
}
