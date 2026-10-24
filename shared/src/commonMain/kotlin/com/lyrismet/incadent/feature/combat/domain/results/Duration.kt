package com.lyrismet.incadent.feature.combat.domain.results

import com.lyrismet.incadent.feature.combat.domain.model.Combat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** whole minutes from the start to [end], never less than one so a quick fight does not read "0 мин" */
fun Combat.minutesUntil(end: LocalDateTime): Int =
    (end.toInstant(TimeZone.UTC) - startedAt.toInstant(TimeZone.UTC)).inWholeMinutes.toInt().coerceAtLeast(1)
