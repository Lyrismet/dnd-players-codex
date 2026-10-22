package com.lyrismet.incadent.core.file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LineStreamsTest {
    // hands the bytes out a few at a time, so a line and a letter get cut in the middle
    private fun sourceOf(
        text: String,
        step: Int,
    ): LineReaderSource {
        val bytes = text.encodeToByteArray()
        var pos = 0
        return LineReaderSource(bytes.size.toLong(), { buffer ->
            if (pos >= bytes.size) {
                -1
            } else {
                val n = minOf(step, buffer.size, bytes.size - pos)
                bytes.copyInto(buffer, 0, pos, pos + n)
                pos += n
                n
            }
        })
    }

    private fun linesOf(source: LineReaderSource): List<String> = generateSequence { source.readLine(1000) }.toList()

    @Test
    fun `lines come back whole whatever the chunk size`() {
        listOf(1, 2, 3, 7, 64).forEach { step ->
            assertEquals(listOf("one", "два", "", "last"), linesOf(sourceOf("one\nдва\n\nlast", step)), "step $step")
        }
    }

    @Test
    fun `a carriage return before the newline is dropped`() {
        assertEquals(listOf("a", "b"), linesOf(sourceOf("a\r\nb\r\n", 3)))
    }

    @Test
    fun `nothing after the last newline gives no extra line`() {
        assertEquals(listOf("a"), linesOf(sourceOf("a\n", 5)))
        assertNull(sourceOf("", 5).readLine(10))
    }

    @Test
    fun `a line past the limit throws`() {
        assertFailsWith<LineTooLongException> { sourceOf("x".repeat(50) + "\nok", 4).readLine(10) }
    }

    @Test
    fun `a sink writes one line per call`() {
        val out = StringBuilder()
        val sink = LineWriterSink({ out.append(it.decodeToString()) })
        sink.writeLine("a")
        sink.writeLine("б")
        sink.close()
        assertEquals("a\nб\n", out.toString())
    }
}
