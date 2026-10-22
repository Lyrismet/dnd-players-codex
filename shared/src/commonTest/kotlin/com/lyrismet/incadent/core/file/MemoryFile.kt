package com.lyrismet.incadent.core.file

/** a file that lives in memory, the text of small exports and what the tests write and read */
class MemoryFile(
    initial: List<String> = emptyList(),
) : FileSink,
    FileSource {
    private val lines = initial.toMutableList()
    private var next = 0

    val text: String get() = lines.joinToString("\n")

    override val sizeOrNull: Long get() = lines.sumOf { it.encodeToByteArray().size + 1L }

    override fun writeLine(line: String) {
        lines += line
    }

    override fun readLine(maxChars: Int): String? =
        lines.getOrNull(next)?.also {
            if (it.length > maxChars) throw LineTooLongException()
            next++
        }

    override fun close() = Unit
}
