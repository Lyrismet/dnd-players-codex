package com.lyrismet.dndcodex.core.format

private val ROMAN_VALUES =
    listOf(
        1000 to "M",
        900 to "CM",
        500 to "D",
        400 to "CD",
        100 to "C",
        90 to "XC",
        50 to "L",
        40 to "XL",
        10 to "X",
        9 to "IX",
        5 to "V",
        4 to "IV",
        1 to "I",
    )

/** the design numbers sessions in roman numerals ("Сессия XII") - only meaningful for positive numbers */
fun Int.toRomanNumeral(): String {
    require(this > 0) { "roman numerals are only defined for positive numbers, got $this" }
    var remainder = this
    val builder = StringBuilder()
    for ((value, symbol) in ROMAN_VALUES) {
        while (remainder >= value) {
            builder.append(symbol)
            remainder -= value
        }
    }
    return builder.toString()
}
