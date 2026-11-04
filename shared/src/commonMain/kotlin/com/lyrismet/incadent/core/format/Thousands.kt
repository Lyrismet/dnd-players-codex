package com.lyrismet.incadent.core.format

private const val GROUP_SIZE = 3
private const val NO_BREAK_SPACE = ' '

/** the number with a no-break space between the thousands, "1 234 567" */
fun Int.withThousands(): String {
    val digits = toString()
    val sign = if (digits.startsWith("-")) "-" else ""
    val body = digits.removePrefix("-")
    val grouped =
        body
            .reversed()
            .chunked(GROUP_SIZE)
            .joinToString(NO_BREAK_SPACE.toString())
            .reversed()
    return sign + grouped
}
