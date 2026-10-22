package com.lyrismet.incadent.feature.combat.domain.grouping

import com.lyrismet.incadent.core.format.toRomanNumeral

private val ROMAN_SUFFIX = Regex("\\s+[IVXLC]+$")

/** "Скелет-лучник II" without its autonumber */
fun String.baseName(): String = replace(ROMAN_SUFFIX, "")

/** the names of one side's members, a name that is taken more than once gets "I", "II"... in order of appearance */
fun autonumber(baseNames: List<String>): List<String> {
    val totals = baseNames.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return baseNames.map { base ->
        if (totals.getValue(base) == 1) {
            base
        } else {
            val index = (seen[base] ?: 0) + 1
            seen[base] = index
            "$base ${index.toRomanNumeral()}"
        }
    }
}
