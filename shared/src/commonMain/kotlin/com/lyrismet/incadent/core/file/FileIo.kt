package com.lyrismet.incadent.core.file

/** where a file is written to, one line at a time so a big file is never held in memory */
interface FileSink {
    fun writeLine(line: String)

    fun close()
}

/** where a file is read from, one line at a time so a big file is never held in memory */
interface FileSource {
    /** the byte size the system reports before anything is read, null when it does not know */
    val sizeOrNull: Long?

    /** the next line or null at the end, a line longer than [maxChars] throws [LineTooLongException] */
    fun readLine(maxChars: Int): String?

    fun close()
}

class LineTooLongException : Exception("a line is longer than the limit")
