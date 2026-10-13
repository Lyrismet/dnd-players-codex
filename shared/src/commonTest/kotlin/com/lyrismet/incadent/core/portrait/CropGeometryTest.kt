package com.lyrismet.incadent.core.portrait

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
