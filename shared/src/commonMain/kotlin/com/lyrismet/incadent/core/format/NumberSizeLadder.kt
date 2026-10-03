package com.lyrismet.incadent.core.format

/** font sizes in sp for a roman numeral label of 1-2, 3, 4, 5 and 6+ characters, from the design's `num()` ladder */
@Suppress("MagicNumber") // literal lookup tables from the design, naming each number would only hide it
enum class NumberSizeLadder(
    private val sizes: List<Int>,
) {
    LIVE_SESSION(listOf(46, 40, 34, 28, 24)),
    ARCHIVE_SESSION(listOf(24, 21, 18, 15, 13)),
    RELATED_NOTE(listOf(20, 18, 16, 14, 12)),
    ;

    fun sizeFor(label: String): Int =
        when {
            label.length <= 2 -> sizes[0]
            label.length == 3 -> sizes[1]
            label.length == 4 -> sizes[2]
            label.length == 5 -> sizes[3]
            else -> sizes[4]
        }
}
