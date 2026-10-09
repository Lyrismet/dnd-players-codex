package com.lyrismet.incadent.core.portrait

import androidx.compose.ui.graphics.ImageBitmap
import com.lyrismet.incadent.core.logging.AppLog
import kotlin.io.encoding.Base64

// mockup's budget for a stored portrait - Players Codex v6.dc.html readPhoto: M = 640, quality starts at 0.72
private const val PORTRAIT_MAX_DIMENSION_PX = 640
private const val PORTRAIT_TARGET_BYTES = 60 * 1024
private const val LOG_COMPONENT = "PortraitCodec"

fun encodePortraitBase64(
    rawBytes: ByteArray,
    compressor: ImageCompressor,
): String {
    val jpeg = compressor.compressToJpeg(rawBytes, PORTRAIT_MAX_DIMENSION_PX, PORTRAIT_TARGET_BYTES)
    val picked = rawBytes.size.toKb()
    val compressed = jpeg.size.toKb()
    AppLog.info(
        LOG_COMPONENT,
        "picked $picked KB, compressed to $compressed KB (limit ${PORTRAIT_TARGET_BYTES.toKb()} KB)",
    )
    return Base64.Default.encode(jpeg)
}

fun decodePortraitBitmap(base64: String): ImageBitmap = decodeImageBitmap(Base64.Default.decode(base64))

private const val BYTES_PER_KB = 1024

private fun Int.toKb(): Int = (this + BYTES_PER_KB / 2) / BYTES_PER_KB

/** stored size of a base64 portrait in whole kilobytes, rounded, shown in the form hint */
fun portraitSizeKb(base64: String): Int {
    val padding = base64.takeLastWhile { it == '=' }.length
    val bytes = base64.length * 3L / 4 - padding
    return ((bytes + BYTES_PER_KB / 2) / BYTES_PER_KB).toInt()
}
