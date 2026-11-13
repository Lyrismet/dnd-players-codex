package com.lyrismet.incadent.feature.combat.domain.preparation

import kotlin.math.roundToInt

private const val TENTH = 10

/** the mean of the levels that were given in tenths (43 is 4.3), none when no one has a level */
fun averageLevelTenths(levels: List<Int?>): Int? {
    val known = levels.filterNotNull()
    return if (known.isEmpty()) null else (known.sum() * TENTH.toDouble() / known.size).roundToInt()
}

/** tenths as a number with one decimal, a whole one has none: 43 is "4,3" and 40 is "4" */
fun tenthsText(
    tenths: Int,
    separator: Char,
): String = if (tenths % TENTH == 0) (tenths / TENTH).toString() else "${tenths / TENTH}$separator${tenths % TENTH}"
