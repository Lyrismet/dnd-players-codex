package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** where a session stood and where it stands now */
data class SessionDateChange(
    val id: Long,
    val from: LocalDateTime,
    val to: LocalDateTime,
)

/** the dates that put a session at place [toIndex] (0 is the oldest) among the others, none when it is there */
fun List<SessionNote>.moveChanges(
    movedId: Long,
    toIndex: Int,
): List<SessionDateChange> {
    val ordered = sortedBy { it.sessionDate }
    val moved = ordered.firstOrNull { it.id == movedId }
    val others = ordered.filter { it.id != movedId }
    val index = toIndex.coerceIn(0, others.size)
    if (moved == null || ordered.indexOf(moved) == index) return emptyList()
    val placement = placeAmong(others, index)
    val dates = placement.shifted + (movedId to placement.date)
    return (listOf(moved) + others).mapNotNull { session ->
        dates[session.id]?.takeIf { it != session.sessionDate }?.let {
            SessionDateChange(
                session.id,
                session.sessionDate,
                it,
            )
        }
    }
}

/** the date and the shifts of the later sessions that put a new session at place [index] among [others] */
data class SessionPlacement(
    val date: LocalDateTime,
    val shifted: Map<Long, LocalDateTime>,
)

/**
 * a date between the two neighbours, a day beyond the only neighbour there is, and when the neighbours are less
 * than two seconds apart the later ones move on by a second each so the order holds
 */
fun placeAmong(
    others: List<SessionNote>,
    index: Int,
): SessionPlacement {
    val earlier = others.getOrNull(index - 1)?.sessionDate
    val later = others.getOrNull(index)?.sessionDate
    val date =
        when {
            earlier != null && later != null -> between(earlier, later)
            earlier != null -> earlier.shiftedBy(1.days.inWholeSeconds)
            later != null -> later.shiftedBy(-1.days.inWholeSeconds)
            else -> LocalDateTime(1970, 1, 1, 0, 0)
        }
    val shifted = mutableMapOf<Long, LocalDateTime>()
    var last = date
    for (session in others.drop(index)) {
        if (session.sessionDate > last) break
        last = last.shiftedBy(1.seconds.inWholeSeconds)
        shifted[session.id] = last
    }
    return SessionPlacement(date, shifted)
}

private fun between(
    earlier: LocalDateTime,
    later: LocalDateTime,
): LocalDateTime {
    val from = earlier.toEpochSeconds()
    val gap = later.toEpochSeconds() - from
    return earlier.shiftedBy(if (gap >= 2) gap / 2 else 1)
}

private fun LocalDateTime.toEpochSeconds(): Long = toInstant(TimeZone.UTC).epochSeconds

private fun LocalDateTime.shiftedBy(seconds: Long): LocalDateTime =
    Instant.fromEpochSeconds(toEpochSeconds() + seconds).toLocalDateTime(TimeZone.UTC)
