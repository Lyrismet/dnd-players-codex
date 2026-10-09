package com.lyrismet.incadent.core.portrait

/**
 * walks jpeg quality down from [maxQuality] by [step], calling [encodeAt] for each step, and stops at the
 * first result at or under [targetBytes] - falls back to [minQuality]'s encoding if nothing fit
 */
fun searchJpegQuality(
    targetBytes: Int,
    minQuality: Int = 40,
    maxQuality: Int = 90,
    step: Int = 10,
    encodeAt: (quality: Int) -> ByteArray,
): ByteArray {
    var quality = maxQuality
    var lastEncoded = encodeAt(quality)
    while (lastEncoded.size > targetBytes && quality > minQuality) {
        quality = (quality - step).coerceAtLeast(minQuality)
        lastEncoded = encodeAt(quality)
    }
    return lastEncoded
}
