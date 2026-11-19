package com.lyrismet.incadent.core.portrait

import com.lyrismet.incadent.domain.model.Photo
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CropGeometryTest {
    // a landscape photo, so the portrait window covers it by height and the horizontal slack can be dragged
    private val geometry = CropGeometry(CropAspect.Portrait, imageWidth = 1600, imageHeight = 900)

    @Test
    fun initialCropIsCenteredAndCoversWholeHeight() {
        val rect = geometry.toCropRect(geometry.initial())
        assertEquals(0f, rect.top, 0.0001f)
        assertEquals(1f, rect.bottom, 0.0001f)
        assertEquals(0.5f, (rect.left + rect.right) / 2, 0.0001f)
    }

    @Test
    fun dragIsClampedInsideTheImage() {
        val dragged = geometry.dragBy(geometry.initial(), dx = 100_000f, dy = 100_000f)
        val rect = geometry.toCropRect(dragged)
        assertEquals(0f, rect.left, 0.0001f)
        assertEquals(0f, rect.top, 0.0001f)
        val far = geometry.dragBy(geometry.initial(), dx = -100_000f, dy = -100_000f)
        assertEquals(1f, geometry.toCropRect(far).right, 0.0001f)
    }

    @Test
    fun zoomShrinksTheWindowAndKeepsItsCentre() {
        val start = geometry.initial()
        val before = geometry.toCropRect(start)
        val zoomed = geometry.zoomTo(start, 2f)
        val after = geometry.toCropRect(zoomed)
        assertEquals((before.left + before.right) / 2, (after.left + after.right) / 2, 0.0001f)
        assertEquals((before.right - before.left) / 2, after.right - after.left, 0.0001f)
    }

    @Test
    fun zoomIsLimitedToTheRange() {
        assertEquals(MAX_ZOOM, geometry.zoomTo(geometry.initial(), 10f).zoom)
        assertEquals(MIN_ZOOM, geometry.zoomTo(geometry.initial(), 0.1f).zoom)
    }

    @Test
    fun cropRectStaysInsideUnitSquare() {
        val rect = geometry.toCropRect(geometry.zoomTo(geometry.initial(), 3f))
        assertTrue(rect.left >= 0f && rect.top >= 0f && rect.right <= 1.0001f && rect.bottom <= 1.0001f)
    }

    @Test
    fun clampNeverThrowsOnFloatRoundingForAnyImageSize() {
        for (aspect in CropAspect.entries) {
            for (width in 300..1500 step 7) {
                for (height in listOf(300, 541, 777, 1000, 1333, 2048)) {
                    val g = CropGeometry(aspect, width, height)
                    val rect = g.toCropRect(g.initial())
                    assertTrue(rect.left >= 0f && rect.top >= 0f)
                }
            }
        }
    }

    @Test
    fun aWindowOverTheWholeImageIsWhole() {
        assertTrue(CropRect(0f, 0.002f, 0.998f, 1f).isWholeImage)
        assertFalse(CropRect(0.05f, 0f, 1f, 1f).isWholeImage)
    }

    @Test
    fun onlyAnUntouchedWindowOverAStoredPictureKeepsIt() {
        val whole = CropRect(0f, 0f, 1f, 1f)
        val narrowed = CropRect(0.1f, 0f, 0.9f, 1f)
        val stored = recropDraft(Photo(thumb = ByteArray(1), full = ByteArray(1)), PhotoSlot.COVER)
        val fresh = PhotoCropDraft(ByteArray(1), PhotoSlot.COVER)

        assertTrue(stored.keepsStored(whole))
        assertFalse(stored.keepsStored(narrowed))
        assertFalse(fresh.keepsStored(whole))
    }

    @Test
    fun aPhotoWithASourceReopensFromTheSourceAndMayGrowBack() {
        val source = byteArrayOf(7, 7)
        val draft =
            recropDraft(Photo(thumb = byteArrayOf(1), full = byteArrayOf(2), source = source), PhotoSlot.PORTRAIT)

        assertContentEquals(source, draft.source)
        assertTrue(draft.fromSource)
        assertFalse(draft.keepsStored(CropRect(0f, 0f, 1f, 1f)))
    }

    @Test
    fun aPhotoWithoutASourceReopensFromTheCroppedPicture() {
        val full = byteArrayOf(2, 2)
        val draft = recropDraft(Photo(thumb = byteArrayOf(1), full = full), PhotoSlot.PORTRAIT)

        assertContentEquals(full, draft.source)
        assertFalse(draft.fromSource)
        assertTrue(draft.keepsStored(CropRect(0f, 0f, 1f, 1f)))
    }
}
