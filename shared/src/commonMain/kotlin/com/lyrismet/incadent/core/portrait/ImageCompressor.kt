package com.lyrismet.incadent.core.portrait

/** shrinks a raw image to a jpeg under a byte budget and cuts a [CropRect] out of one into an exact output size */
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
