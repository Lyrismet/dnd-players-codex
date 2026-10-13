package com.lyrismet.incadent.core.portrait

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max

// searchJpegQuality works in integer 0-100 steps, UIImageJPEGRepresentation wants a 0.0-1.0 fraction
private const val QUALITY_PERCENT = 100.0

actual class ImageCompressor {
    @OptIn(ExperimentalForeignApi::class)
    actual fun compressToJpeg(
        bytes: ByteArray,
        maxDimensionPx: Int,
        targetBytes: Int,
    ): ByteArray {
        val original = UIImage(data = bytes.toNSData())
        val (width, height) = original.size.useContents { width to height }
        val scale = (maxDimensionPx / max(width, height)).coerceAtMost(1.0)
        // always redrawn, so the exif orientation is baked into the pixels
        val upright = original.drawn(width * scale, height * scale, 0.0, 0.0, width * scale, height * scale)
        return upright.toJpeg(targetBytes)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun cropToJpeg(
        bytes: ByteArray,
        crop: CropRect,
        outWidth: Int,
        outHeight: Int,
        targetBytes: Int,
    ): ByteArray {
        val original = UIImage(data = bytes.toNSData())
        val cropWidth = (crop.right - crop.left).toDouble()
        val cropHeight = (crop.bottom - crop.top).toDouble()
        // the whole image is drawn scaled and shifted so the crop window lands exactly on the output canvas
        val drawWidth = outWidth / cropWidth
        val drawHeight = outHeight / cropHeight
        val cropped =
            original.drawn(
                canvasWidth = outWidth.toDouble(),
                canvasHeight = outHeight.toDouble(),
                x = -crop.left * drawWidth,
                y = -crop.top * drawHeight,
                width = drawWidth,
                height = drawHeight,
            )
        return cropped.toJpeg(targetBytes)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.drawn(
    canvasWidth: Double,
    canvasHeight: Double,
    x: Double,
    y: Double,
    width: Double,
    height: Double,
): UIImage {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(canvasWidth, canvasHeight), false, 1.0)
    drawInRect(CGRectMake(x, y, width, height))
    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image ?: this
}

private fun UIImage.toJpeg(targetBytes: Int): ByteArray =
    searchJpegQuality(targetBytes) { quality ->
        UIImageJPEGRepresentation(this, quality / QUALITY_PERCENT)?.toByteArray() ?: ByteArray(0)
    }
