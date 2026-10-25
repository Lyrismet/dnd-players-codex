package com.lyrismet.incadent.core.portrait

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import com.lyrismet.incadent.core.logging.AppLog
import com.lyrismet.incadent.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// the working copy a crop window cuts from - big enough to zoom to 3x without going soft
private const val CROP_SOURCE_MAX_DIMENSION_PX = 1600
private const val CROP_SOURCE_TARGET_BYTES = 1000 * 1024
private const val LOG_COMPONENT = "PortraitCodec"
private const val BYTES_PER_KB = 1024

// the small copy lists and badges draw - a few kilobytes, so a list never decodes the full picture
private const val PORTRAIT_THUMB_MAX_PX = 208
private const val PORTRAIT_THUMB_BYTES = 12 * BYTES_PER_KB
private const val COVER_THUMB_MAX_PX = 360
private const val COVER_THUMB_BYTES = 16 * BYTES_PER_KB

/** which stored photo of an entity a pick is for - decides the crop window shape and the byte budgets */
enum class PhotoSlot(
    val aspect: CropAspect,
    val thumbMaxPx: Int,
    val thumbBytes: Int,
) {
    PORTRAIT(CropAspect.Portrait, PORTRAIT_THUMB_MAX_PX, PORTRAIT_THUMB_BYTES),
    COVER(CropAspect.Wide, COVER_THUMB_MAX_PX, COVER_THUMB_BYTES),
}

/** a photo waiting in the crop window - a working-size jpeg, never stored itself */
class PhotoCropDraft(
    val source: ByteArray,
    val slot: PhotoSlot,
    /** true when [source] is the picture already stored, so an untouched window has nothing to write */
    val isStored: Boolean = false,
) {
    fun keepsStored(crop: CropRect): Boolean = isStored && crop.isWholeImage
}

/** shrinks a raw pick to a working size and orients it, so the crop window decodes it quickly */
fun prepareCropDraft(
    rawBytes: ByteArray,
    slot: PhotoSlot,
    compressor: ImageCompressor,
): PhotoCropDraft =
    PhotoCropDraft(
        compressor.compressToJpeg(rawBytes, CROP_SOURCE_MAX_DIMENSION_PX, CROP_SOURCE_TARGET_BYTES),
        slot,
    )

/** [prepareCropDraft] off the main thread, for the hosts that launch it from a pick */
suspend fun prepareCropDraftAsync(
    rawBytes: ByteArray,
    slot: PhotoSlot,
    compressor: ImageCompressor,
): PhotoCropDraft = withContext(Dispatchers.Default) { prepareCropDraft(rawBytes, slot, compressor) }

/** the stored jpeg opened again in the crop window - it can only be narrowed, the cut-off sides are gone */
fun recropDraft(
    stored: ByteArray,
    slot: PhotoSlot,
): PhotoCropDraft = PhotoCropDraft(stored, slot, isStored = true)

/** cuts [crop] out of the draft into the slot's stored size and byte budget, with the thumbnail made from it */
fun PhotoCropDraft.encode(
    crop: CropRect,
    compressor: ImageCompressor,
): Photo {
    val aspect = slot.aspect
    val full = compressor.cropToJpeg(source, crop, aspect.outWidth, aspect.outHeight, aspect.targetBytes)
    AppLog.info(
        LOG_COMPONENT,
        "${slot.name} cropped to ${full.size.toKb()} KB (limit ${aspect.targetBytes.toKb()} KB)",
    )
    return Photo(thumb = makeThumb(full, slot, compressor), full = full)
}

/** [encode] off the main thread, for the hosts that launch it from "Готово" */
suspend fun PhotoCropDraft.encodeAsync(
    crop: CropRect,
    compressor: ImageCompressor,
): Photo = withContext(Dispatchers.Default) { encode(crop, compressor) }

/** the list-sized copy of an already stored jpeg */
fun makeThumb(
    full: ByteArray,
    slot: PhotoSlot,
    compressor: ImageCompressor,
): ByteArray = compressor.compressToJpeg(full, slot.thumbMaxPx, slot.thumbBytes)

/** decodes [bytes] once per distinct picture - the key is the content, so a re-emitted list does not decode again */
@Composable
fun rememberPhotoBitmap(bytes: ByteArray): ImageBitmap {
    // the hash is walked once per array instance, not on every recomposition
    val key = remember(bytes) { photoKeyOf(bytes) }
    return remember(key) { DecodedBitmaps.getOrDecode(key, bytes) }
}

/** keeps recent decodes so a list item that scrolls back or a card opened twice does not decode its jpeg again */
private object DecodedBitmaps {
    // only the main thread touches it, a decode off the main thread hands its result back before it is stored
    private val cache = LinkedHashMap<PhotoKey, ImageBitmap>()
    private var cachedPixels = 0L

    fun peek(key: PhotoKey): ImageBitmap? {
        // re-inserting on a hit keeps the iteration order oldest-use first
        val hit = cache.remove(key) ?: return null
        cache[key] = hit
        return hit
    }

    fun put(
        key: PhotoKey,
        bitmap: ImageBitmap,
    ): ImageBitmap {
        cache.remove(key)?.let { cachedPixels -= it.pixels() }
        cache[key] = bitmap
        cachedPixels += bitmap.pixels()
        val iterator = cache.entries.iterator()
        while (cachedPixels > MAX_CACHED_PIXELS && cache.size > 1 && iterator.hasNext()) {
            cachedPixels -= iterator.next().value.pixels()
            iterator.remove()
        }
        return bitmap
    }

    fun getOrDecode(
        key: PhotoKey,
        bytes: ByteArray,
    ): ImageBitmap = peek(key) ?: put(key, decodeImageBitmap(bytes))

    private fun ImageBitmap.pixels(): Long = width.toLong() * height
}

/** like [rememberPhotoBitmap] but a big picture decodes off the main thread, null until it is ready */
@Composable
fun rememberPhotoBitmapAsync(bytes: ByteArray): ImageBitmap? {
    val key = remember(bytes) { photoKeyOf(bytes) }
    // a thumbnail is cheap enough to decode at once, so a list never shows an empty frame for it
    val first =
        remember(key) {
            DecodedBitmaps.peek(key)
                ?: if (bytes.size <= SYNC_DECODE_MAX_BYTES) DecodedBitmaps.getOrDecode(key, bytes) else null
        }
    // null until a big picture decodes, the slot draws its loader meanwhile
    val bitmap by produceState(first, key) {
        value =
            DecodedBitmaps.peek(key)
                ?: DecodedBitmaps.put(key, withContext(Dispatchers.Default) { decodeImageBitmap(bytes) })
    }
    return bitmap
}

private const val SYNC_DECODE_MAX_BYTES = 20_000
private const val MAX_CACHED_PIXELS = 6_000_000L

internal data class PhotoKey(
    val size: Int,
    val hash: Long,
)

// 64-bit fnv-1a, so two different pictures of one size practically never share a cache entry
internal fun photoKeyOf(bytes: ByteArray): PhotoKey {
    var hash = FNV_OFFSET_BASIS
    for (byte in bytes) hash = (hash xor byte.toLong()) * FNV_PRIME
    return PhotoKey(bytes.size, hash)
}

private const val FNV_OFFSET_BASIS = -3750763034362895579L
private const val FNV_PRIME = 1099511628211L

private fun Int.toKb(): Int = (this + BYTES_PER_KB / 2) / BYTES_PER_KB

/** stored size of a photo in whole kilobytes, rounded, shown in the form hint */
fun photoSizeKb(bytes: ByteArray): Int = bytes.size.toKb()
