package com.lyrismet.incadent.core.portrait

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64

// mockup's budget for a stored portrait - Players Codex v6.dc.html readPhoto: M = 640, quality starts at 0.72
private const val PORTRAIT_MAX_DIMENSION_PX = 640
private const val PORTRAIT_TARGET_BYTES = 60 * 1024

fun encodePortraitBase64(
    rawBytes: ByteArray,
    compressor: ImageCompressor,
): String {
    val jpeg = compressor.compressToJpeg(rawBytes, PORTRAIT_MAX_DIMENSION_PX, PORTRAIT_TARGET_BYTES)
    return Base64.Default.encode(jpeg)
}

fun decodePortraitBitmap(base64: String): ImageBitmap = decodeImageBitmap(Base64.Default.decode(base64))
