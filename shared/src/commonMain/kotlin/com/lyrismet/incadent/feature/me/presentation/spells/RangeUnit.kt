package com.lyrismet.incadent.feature.me.presentation.spells

import kotlin.math.roundToInt

private val FeetPresets = listOf(30, 60)
private val MeterPresets = listOf(9, 18)

/** the unit a range is written in, [presets] are its ready-made distances, 30 and 60 feet are 9 and 18 meters */
internal enum class RangeUnit(
    val presets: List<Int>,
) {
    FEET(FeetPresets),
    METERS(MeterPresets),
}

private val RangeFormat = Regex("^(\\d+)\\s*(фт|ft|м|m)\\.?$", RegexOption.IGNORE_CASE)
private const val METERS_PER_FOOT = 0.3

private fun parseRange(text: String): Pair<Int, RangeUnit>? {
    val groups = RangeFormat.matchEntire(text.trim())?.groupValues
    val number = groups?.get(1)?.toIntOrNull()
    val unit = if (groups?.get(2)?.lowercase() in setOf("м", "m")) RangeUnit.METERS else RangeUnit.FEET
    return number?.let { it to unit }
}

/** the unit of a range written as a number with its unit, null for words like "Касание" or an empty text */
internal fun rangeUnitOf(text: String): RangeUnit? = parseRange(text)?.second

/** the number of a range written as a number with its unit, null for words */
internal fun rangeNumberOf(text: String): Int? = parseRange(text)?.first

/** a range text in [to], one that is not a number with a unit stays as it is, [suffix] names the unit */
internal fun convertRange(
    text: String,
    to: RangeUnit,
    suffix: (RangeUnit) -> String,
): String {
    val (number, unit) = parseRange(text) ?: return text
    val factor = if (to == RangeUnit.METERS) METERS_PER_FOOT else 1 / METERS_PER_FOOT
    return if (unit == to) text else "${(number * factor).roundToInt()} ${suffix(to)}"
}
