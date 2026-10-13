package com.lyrismet.incadent.core.portrait

/**
 * scales a raw image down to [maxDimensionPx] on its longer side and re-encodes it as jpeg under [targetBytes],
 * and cuts a [CropRect] out of an image into an exact output size
 */
expect class ImageCompressor() {
    fun compressToJpeg(
        bytes: ByteArray,
        maxDimensionPx: Int,
        targetBytes: Int,
    ): ByteArray

    fun cropToJpeg(
        bytes: ByteArray,
        crop: CropRect,
        outWidth: Int,
        outHeight: Int,
        targetBytes: Int,
    ): ByteArray
}
