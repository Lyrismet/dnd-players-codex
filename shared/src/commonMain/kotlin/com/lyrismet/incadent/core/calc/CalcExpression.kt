package com.lyrismet.incadent.core.calc

import kotlin.math.floor

private const val MAX_TERM_LENGTH = 3

/**
 * a sum-of-terms numeric expression typed on a number-pad keypad, e.g. "12+3" sums to 15 - shared by the
 * codex form calculator (P2.9, the only caller today, whose multiplier is always 1 and never reaches [plus])
 * and the future combat damage/heal calculator (P3), which needs [plus] and a fractional [value] multiplier
 */
data class CalcExpression(
    val raw: String = "",
) {
    /** appends [digit] unless the current term (the text after the last `+`) would grow past 3 characters */
    fun digit(digit: Int): CalcExpression {
        val currentTerm = raw.substringAfterLast('+')
        return if (currentTerm.length < MAX_TERM_LENGTH) copy(raw = raw + digit) else this
    }

    /** starts a new term - a no-op on an empty expression or one that already ends with `+` */
    fun plus(): CalcExpression = if (raw.isNotEmpty() && !raw.endsWith('+')) copy(raw = raw + '+') else this

    /** drops the last character, a no-op on an empty expression */
    fun backspace(): CalcExpression = copy(raw = raw.dropLast(1))

    /** resets to a blank expression */
    fun clear(): CalcExpression = CalcExpression()

    /** the terms' sum times [multiplier], floored */
    fun value(multiplier: Double = 1.0): Int {
        val sum = raw.split('+').filter { it.isNotBlank() }.sumOf { it.toInt() }
        return floor(sum * multiplier).toInt()
    }

    /** the terms joined with " + " for display, blank unless there is more than one (i.e. [plus] was used) */
    fun display(): String {
        val terms = raw.split('+').filter { it.isNotBlank() }
        return if (terms.size > 1) terms.joinToString(" + ") else ""
    }
}
