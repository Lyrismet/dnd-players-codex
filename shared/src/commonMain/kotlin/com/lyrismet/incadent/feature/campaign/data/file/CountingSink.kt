package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileSink

private const val UTF8_ONE_LIMIT = 0x80
private const val UTF8_TWO_LIMIT = 0x800
private const val UTF8_TWO = 2
private const val UTF8_THREE = 3
private const val UTF8_FOUR = 4

/** counts what is written and keeps none of it */
internal class CountingSink : FileSink {
    var bytes = 0L

    override fun writeLine(line: String) {
        bytes += line.utf8Length() + 1
    }

    override fun close() = Unit
}

// utf-8 size without building the byte array, a surrogate pair is 4 bytes
private fun String.utf8Length(): Int {
    var length = 0
    var index = 0
    while (index < this.length) {
        val char = this[index]
        val paired = char.isHighSurrogate() && index + 1 < this.length && this[index + 1].isLowSurrogate()
        if (paired) index++
        length +=
            when {
                paired -> UTF8_FOUR
                char.code < UTF8_ONE_LIMIT -> 1
                char.code < UTF8_TWO_LIMIT -> UTF8_TWO
                else -> UTF8_THREE
            }
        index++
    }
    return length
}
