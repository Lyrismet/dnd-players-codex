package com.lyrismet.incadent.core.portrait

private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8
private const val MARKER_PREFIX = 0xFF
private const val START_OF_IMAGE = 0xFFD8
private const val START_OF_SCAN = 0xDA
private const val END_OF_IMAGE = 0xD9
private const val FRAME_FIRST = 0xC0
private const val FRAME_LAST = 0xCF
private val NOT_FRAME_MARKERS = setOf(0xC4, 0xC8, 0xCC)
private const val SEGMENT_HEADER_BYTES = 4
private const val SEGMENT_MIN_LENGTH = 2
private const val FIRST_SEGMENT = 2
private const val HEIGHT_OFFSET = 5
private const val WIDTH_OFFSET = 7

/** width and height from the frame header of a jpeg, null when [bytes] is no readable jpeg - nothing is decoded */
fun jpegDimensions(bytes: ByteArray): Pair<Int, Int>? {
    val frame = if (bytes.u16(0) == START_OF_IMAGE) frameOffset(bytes) else null
    if (frame == null) return null
    val height = bytes.u16(frame + HEIGHT_OFFSET)
    val width = bytes.u16(frame + WIDTH_OFFSET)
    return if (width > 0 && height > 0) width to height else null
}

// walks the segments after the start marker until the frame header, a scan or a broken length ends the search
private fun frameOffset(bytes: ByteArray): Int? {
    var at = FIRST_SEGMENT
    var found: Int? = null
    while (found == null && at + SEGMENT_HEADER_BYTES < bytes.size && bytes.u8(at) == MARKER_PREFIX) {
        val marker = bytes.u8(at + 1)
        val length = bytes.u16(at + 2)
        when {
            marker == MARKER_PREFIX -> at++
            marker in FRAME_FIRST..FRAME_LAST && marker !in NOT_FRAME_MARKERS -> found = at
            marker == START_OF_SCAN || marker == END_OF_IMAGE || length < SEGMENT_MIN_LENGTH -> at = bytes.size
            else -> at += 2 + length
        }
    }
    return found
}

private fun ByteArray.u8(index: Int): Int = if (index in indices) this[index].toInt() and BYTE_MASK else -1

private fun ByteArray.u16(index: Int): Int = (u8(index) shl BITS_PER_BYTE) or u8(index + 1)
