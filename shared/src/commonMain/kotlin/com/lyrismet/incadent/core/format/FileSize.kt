package com.lyrismet.incadent.core.format

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val TENTHS = 10

/** a file size as a number and its unit, the number already carries the language's decimal separator */
data class FileSize(
    val number: String,
    val isMegabytes: Boolean,
)

/** whole kilobytes under a megabyte (never less than 1), megabytes with one decimal from there */
fun fileSizeOf(
    bytes: Long,
    decimalSeparator: Char,
): FileSize =
    if (bytes < BYTES_PER_MB) {
        FileSize(((bytes + BYTES_PER_KB / 2) / BYTES_PER_KB).coerceAtLeast(1).toString(), isMegabytes = false)
    } else {
        val tenths = (bytes * TENTHS + BYTES_PER_MB / 2) / BYTES_PER_MB
        FileSize("${tenths / TENTHS}$decimalSeparator${tenths % TENTHS}", isMegabytes = true)
    }
