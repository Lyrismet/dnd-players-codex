package com.lyrismet.incadent.core.portrait

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

actual class ImageCompressor {
    actual fun compressToJpeg(
        bytes: ByteArray,
        maxDimensionPx: Int,
        targetBytes: Int,
    ): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val scale = maxDimensionPx.toFloat() / max(original.width, original.height)
        val scaled =
            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                original
            }
        return searchJpegQuality(targetBytes) { quality ->
            ByteArrayOutputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        }
    }
}
