package com.lyrismet.incadent.core.quickedit

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorShieldTest {
    @Test
    fun `without an open editor the shield covers the whole sheet`() {
        val regions = editorShieldRegions(hole = null, width = 300f, height = 500f)

        assertEquals(Rect(0f, 0f, 300f, 500f), regions[0])
        assertEquals(Rect.Zero, regions[1])
    }

    @Test
    fun `the four regions surround the editor and leave it uncovered`() {
        val hole = Rect(20f, 100f, 280f, 180f)
        val regions = editorShieldRegions(hole, width = 300f, height = 500f)

        assertEquals(Rect(0f, 0f, 300f, 100f), regions[0])
        assertEquals(Rect(0f, 180f, 300f, 500f), regions[1])
        assertEquals(Rect(0f, 100f, 20f, 180f), regions[2])
        assertEquals(Rect(280f, 100f, 300f, 180f), regions[3])
    }

    @Test
    fun `a hole that runs past the sheet edge is clamped to it`() {
        val hole = Rect(-10f, 400f, 320f, 600f)
        val regions = editorShieldRegions(hole, width = 300f, height = 500f)

        assertEquals(Rect(0f, 0f, 300f, 400f), regions[0])
        assertEquals(Rect(0f, 500f, 300f, 500f), regions[1])
        assertEquals(Rect(300f, 400f, 300f, 500f), regions[3])
    }
}
