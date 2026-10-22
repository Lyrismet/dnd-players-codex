package com.lyrismet.incadent.core.file

private const val NEWLINE = '\n'.code.toByte()
private const val CARRIAGE_RETURN = '\r'.code.toByte()
private const val CHUNK_BYTES = 64 * 1024
private const val INITIAL_LINE_BYTES = 1024

/** a [FileSource] over any byte reader, [read] fills the buffer and returns how many bytes it got, -1 at the end */
class LineReaderSource(
    override val sizeOrNull: Long?,
    private val read: (ByteArray) -> Int,
    private val onClose: () -> Unit = {},
) : FileSource {
    private val chunk = ByteArray(CHUNK_BYTES)
    private var chunkSize = 0
    private var chunkPos = 0
    private var ended = false

    private var line = ByteArray(INITIAL_LINE_BYTES)

    // the limit counts bytes, which are never fewer than the characters they spell
    override fun readLine(maxChars: Int): String? {
        var length = 0
        while (true) {
            if (chunkPos == chunkSize && !fill()) return if (length == 0) null else decodeLine(length)
            val start = chunkPos
            while (chunkPos < chunkSize && chunk[chunkPos] != NEWLINE) chunkPos++
            val part = chunkPos - start
            if (length + part > maxChars) throw LineTooLongException()
            if (length + part > line.size) line = line.copyOf(maxOf(line.size * 2, length + part))
            chunk.copyInto(line, length, start, chunkPos)
            length += part
            if (chunkPos < chunkSize) {
                chunkPos++
                return decodeLine(length)
            }
        }
    }

    private fun fill(): Boolean {
        if (ended) return false
        chunkSize = read(chunk).coerceAtLeast(0)
        chunkPos = 0
        if (chunkSize == 0) ended = true
        return chunkSize > 0
    }

    private fun decodeLine(length: Int): String {
        val end = if (length > 0 && line[length - 1] == CARRIAGE_RETURN) length - 1 else length
        return line.decodeToString(0, end)
    }

    override fun close() = onClose()
}

/** a [FileSink] over any byte writer, one line per call with a newline after it */
class LineWriterSink(
    private val write: (ByteArray) -> Unit,
    private val onClose: () -> Unit = {},
) : FileSink {
    override fun writeLine(line: String) = write("$line\n".encodeToByteArray())

    override fun close() = onClose()
}
