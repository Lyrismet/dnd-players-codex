package com.lyrismet.incadent.core.portrait

import kotlin.math.max
import kotlin.math.min

private const val BYTES_PER_KB = 1024

// the mockup's CROP table - window in dp, stored pixels, and the budget a stored photo has to fit
private const val PORTRAIT_VIEW_WIDTH = 240f
private const val PORTRAIT_VIEW_HEIGHT = 312f
private const val PORTRAIT_OUT_WIDTH = 480
private const val PORTRAIT_OUT_HEIGHT = 624
private const val PORTRAIT_BYTES = 50 * BYTES_PER_KB
private const val WIDE_VIEW_WIDTH = 336f
private const val WIDE_VIEW_HEIGHT = 189f
private const val WIDE_OUT_WIDTH = 960
private const val WIDE_OUT_HEIGHT = 540
private const val WIDE_BYTES = 90 * BYTES_PER_KB

/** the two photo shapes of the mockup's `CROP` table - window size in dp, stored size in px and the byte budget */
enum class CropAspect(
    val viewWidth: Float,
    val viewHeight: Float,
    val outWidth: Int,
    val outHeight: Int,
    val targetBytes: Int,
) {
    Portrait(PORTRAIT_VIEW_WIDTH, PORTRAIT_VIEW_HEIGHT, PORTRAIT_OUT_WIDTH, PORTRAIT_OUT_HEIGHT, PORTRAIT_BYTES),
    Wide(WIDE_VIEW_WIDTH, WIDE_VIEW_HEIGHT, WIDE_OUT_WIDTH, WIDE_OUT_HEIGHT, WIDE_BYTES),
}

/** a crop window as fractions (0..1) of the source image, so it does not depend on the decoded pixel size */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** zoom (1..[MAX_ZOOM]) and the image's top-left offset inside the crop window, in window units (never positive) */
data class CropState(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

const val MIN_ZOOM = 1f
const val MAX_ZOOM = 3f

/** pure drag/zoom math of the crop window, ported from the mockup's `cropClamp` / `cropZoom` / `applyCrop` */
class CropGeometry(
    private val aspect: CropAspect,
    private val imageWidth: Int,
    private val imageHeight: Int,
) {
    // scale at zoom 1: the image just covers the window
    private val baseScale = max(aspect.viewWidth / imageWidth, aspect.viewHeight / imageHeight)

    private fun scaleOf(zoom: Float) = baseScale * zoom

    fun initial(): CropState =
        CropState(
            zoom = MIN_ZOOM,
            offsetX = (aspect.viewWidth - imageWidth * baseScale) / 2,
            offsetY = (aspect.viewHeight - imageHeight * baseScale) / 2,
        )

    /** keeps the window covered by the image - the offset can only go from "far right/bottom" to 0 */
    fun clamp(state: CropState): CropState {
        val zoom = state.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val scale = scaleOf(zoom)
        // float rounding can push the covering side a hair past 0 and make the range empty
        val minX = min(0f, aspect.viewWidth - imageWidth * scale)
        val minY = min(0f, aspect.viewHeight - imageHeight * scale)
        return CropState(zoom, state.offsetX.coerceIn(minX, 0f), state.offsetY.coerceIn(minY, 0f))
    }

    fun dragBy(
        state: CropState,
        dx: Float,
        dy: Float,
    ): CropState = clamp(state.copy(offsetX = state.offsetX + dx, offsetY = state.offsetY + dy))

    /** changes zoom keeping the image point under the window centre where it is */
    fun zoomTo(
        state: CropState,
        zoom: Float,
    ): CropState {
        val current = clamp(state)
        val oldScale = scaleOf(current.zoom)
        val centerX = (aspect.viewWidth / 2 - current.offsetX) / oldScale
        val centerY = (aspect.viewHeight / 2 - current.offsetY) / oldScale
        val newZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val newScale = scaleOf(newZoom)
        return clamp(
            CropState(
                zoom = newZoom,
                offsetX = aspect.viewWidth / 2 - centerX * newScale,
                offsetY = aspect.viewHeight / 2 - centerY * newScale,
            ),
        )
    }

    fun displayWidth(state: CropState): Float = imageWidth * scaleOf(state.zoom)

    fun displayHeight(state: CropState): Float = imageHeight * scaleOf(state.zoom)

    fun toCropRect(state: CropState): CropRect {
        val clamped = clamp(state)
        val width = displayWidth(clamped)
        val height = displayHeight(clamped)
        val left = -clamped.offsetX / width
        val top = -clamped.offsetY / height
        return CropRect(left, top, left + aspect.viewWidth / width, top + aspect.viewHeight / height)
    }
}
