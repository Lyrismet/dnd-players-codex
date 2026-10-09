package com.lyrismet.incadent.core.portrait

/** scales a raw image down to [maxDimensionPx] on its longer side and re-encodes it as jpeg under [targetBytes] */
expect class ImageCompressor() {
    fun compressToJpeg(
        bytes: ByteArray,
        maxDimensionPx: Int,
        targetBytes: Int,
    ): ByteArray
}
