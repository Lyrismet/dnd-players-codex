package com.lyrismet.incadent.core.portrait

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val ROTATE_90 = 90f
private const val ROTATE_180 = 180f
private const val ROTATE_270 = 270f

actual class ImageCompressor {
    actual fun compressToJpeg(
        bytes: ByteArray,
        maxDimensionPx: Int,
        targetBytes: Int,
    ): ByteArray {
        val original = decodeUpright(bytes) { width, height -> max(width, height) >= maxDimensionPx }
        val scale = maxDimensionPx.toFloat() / max(original.width, original.height)
        val scaled = if (scale < 1f) original.scaledTo(original.width * scale, original.height * scale) else original
        return scaled.toJpeg(targetBytes)
    }

    actual fun cropToJpeg(
        bytes: ByteArray,
        crop: CropRect,
        outWidth: Int,
        outHeight: Int,
        targetBytes: Int,
    ): ByteArray {
        // sampled so the cut-out region itself still has at least the output's pixels on both axes
        val neededWidth = outWidth / (crop.right - crop.left)
        val neededHeight = outHeight / (crop.bottom - crop.top)
        val source = decodeUpright(bytes) { width, height -> width >= neededWidth && height >= neededHeight }
        val left = (crop.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (crop.top * source.height).toInt().coerceIn(0, source.height - 1)
        val width = ((crop.right - crop.left) * source.width).toInt().coerceIn(1, source.width - left)
        val height = ((crop.bottom - crop.top) * source.height).toInt().coerceIn(1, source.height - top)
        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        return cropped.scaledTo(outWidth.toFloat(), outHeight.toFloat()).toJpeg(targetBytes)
    }
}

/** decodes at the coarsest sample size whose upright dimensions still satisfy [isLargeEnough], then applies exif */
private fun decodeUpright(
    bytes: ByteArray,
    isLargeEnough: (width: Int, height: Int) -> Boolean,
): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val swapsAxes = exifOrientation(bytes) in ORIENTATIONS_SWAPPING_AXES
    val width = if (swapsAxes) bounds.outHeight else bounds.outWidth
    val height = if (swapsAxes) bounds.outWidth else bounds.outHeight
    var sample = 1
    while (isLargeEnough(width / (sample * 2), height / (sample * 2))) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    val matrix = exifMatrix(bytes) ?: return decoded
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

private val ORIENTATIONS_SWAPPING_AXES =
    setOf(
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE,
    )

private fun exifOrientation(bytes: ByteArray): Int =
    runCatching {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun exifMatrix(bytes: ByteArray): Matrix? {
    val matrix = Matrix()
    when (exifOrientation(bytes)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(ROTATE_90)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(ROTATE_180)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(ROTATE_270)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(ROTATE_90)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(ROTATE_270)
            matrix.postScale(-1f, 1f)
        }
        else -> return null
    }
    return matrix
}

private fun Bitmap.scaledTo(
    width: Float,
    height: Float,
): Bitmap = Bitmap.createScaledBitmap(this, width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), true)

private fun Bitmap.toJpeg(targetBytes: Int): ByteArray =
    searchJpegQuality(targetBytes) { quality ->
        ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }
