package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState

/** what a line of the log tells, the words are the screen's */
sealed interface LogEntry {
    data class Damage(
        val actor: String?,
        val target: String,
        val amount: Int,
    ) : LogEntry

    data class Heal(
        val actor: String?,
        val target: String,
        val amount: Int,
    ) : LogEntry

    data class Temp(
        val actor: String?,
        val target: String,
        val amount: Int,
    ) : LogEntry

    data class Down(
        val target: String,
        val party: Boolean,
        val by: String?,
    ) : LogEntry

    data class Up(
        val target: String,
    ) : LogEntry

    data class Dead(
        val target: String,
    ) : LogEntry

    data class Mark(
        val target: String,
        val state: CombatState,
    ) : LogEntry

    data class Stance(
        val target: String,
        val stance: CombatStance,
    ) : LogEntry
}

data class LogLine(
    val round: Int,
    val entry: LogEntry,
)

/** what a log line needs to know of the fight besides its own event */
private class LogContext(
    val events: List<CombatEvent>,
    val names: Map<Long, String>,
    val parties: Set<Long>,
) {
    private val seenHits = mutableSetOf<Pair<Int, Long>>()

    fun entryOf(event: CombatEvent): LogEntry? {
        val target = names[event.targetId] ?: return null
        val actor = event.sourceId?.let(names::get)
        return when (event.kind) {
            CombatEventKind.DAMAGE -> damageEntry(events, event, seenHits, actor, target)
            CombatEventKind.HEAL -> LogEntry.Heal(actor, target, event.amount)
            CombatEventKind.TEMP -> LogEntry.Temp(actor, target, event.amount)
            CombatEventKind.DOWN ->
                LogEntry.Down(target, event.targetId in parties, actor?.takeIf { event.sourceId != event.targetId })
            CombatEventKind.UP -> LogEntry.Up(target)
            CombatEventKind.DEAD -> LogEntry.Dead(target)
            CombatEventKind.STATE -> event.state?.let { LogEntry.Mark(target, it) }
            CombatEventKind.STANCE -> stanceEntry(event, target)
            else -> null
        }
    }

    private fun stanceEntry(
        event: CombatEvent,
        target: String,
    ): LogEntry? = CombatStance.entries.firstOrNull { it.name == event.detail }?.let { LogEntry.Stance(target, it) }
}

/** the fight round by round: damage, healing, falls, marks and changes of side, conditions are left out */
fun roundLog(combat: Combat): List<LogLine> {
    val context =
        LogContext(
            events = combat.events,
            names = combat.combatants.associate { it.id to it.name },
            parties =
                combat.combatants
                    .filter { it.side == CombatSide.PARTY }
                    .map { it.id }
                    .toSet(),
        )
    return combat.events.mapNotNull { event -> context.entryOf(event)?.let { LogLine(event.round, it) } }
}

/** one line per blow on one target: the first term carries the total of its terms */
private fun damageEntry(
    all: List<CombatEvent>,
    event: CombatEvent,
    seen: MutableSet<Pair<Int, Long>>,
    actor: String?,
    target: String,
): LogEntry? {
    val key = event.hit to event.targetId
    if (!seen.add(key)) return null
    val total =
        all
            .filter {
                it.kind == CombatEventKind.DAMAGE && it.hit == event.hit && it.targetId == event.targetId
            }.sumOf { it.amount }
    return LogEntry.Damage(actor, target, total)
}
