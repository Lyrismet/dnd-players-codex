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
        val scale = maxDimensionPx / max(width, height)
        val scaled = if (scale < 1.0) original.scaledBy(scale) else original
        return searchJpegQuality(targetBytes) { quality ->
            UIImageJPEGRepresentation(scaled, quality / QUALITY_PERCENT)?.toByteArray() ?: ByteArray(0)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.scaledBy(scale: Double): UIImage {
    val (width, height) = size.useContents { width to height }
    val scaledSize = CGSizeMake(width * scale, height * scale)
    UIGraphicsBeginImageContextWithOptions(scaledSize, false, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
    val scaledImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return scaledImage ?: this
}
