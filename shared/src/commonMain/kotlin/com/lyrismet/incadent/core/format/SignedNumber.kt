package com.lyrismet.incadent.core.format

private const val MINUS = '−'

/** a number with its sign, "+2" and "−1" with a true minus, a missing one is a dash */
fun Int?.signed(): String =
    when {
        this == null -> EMPTY_VALUE_DASH
        this >= 0 -> "+$this"
        else -> "$MINUS${-this}"
    }
