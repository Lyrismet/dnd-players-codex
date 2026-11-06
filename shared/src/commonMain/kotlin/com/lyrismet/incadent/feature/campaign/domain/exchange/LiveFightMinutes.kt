package com.lyrismet.incadent.feature.campaign.domain.exchange

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** whole minutes a fight has been going, both times are on the same clock so the zone cancels out */
fun minutesSince(
    startedAt: LocalDateTime,
    now: LocalDateTime,
): Int =
    (now.toInstant(TimeZone.UTC) - startedAt.toInstant(TimeZone.UTC))
        .inWholeMinutes
        .coerceAtLeast(0)
        .toInt()
