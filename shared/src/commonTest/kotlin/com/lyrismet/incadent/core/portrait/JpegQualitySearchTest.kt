package com.lyrismet.incadent.core.portrait

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class JpegQualitySearchTest {
    @Test
    fun `searchJpegQuality keeps the first quality that already fits`() {
        val seen = mutableListOf<Int>()
        val result =
            searchJpegQuality(targetBytes = 100, maxQuality = 90, minQuality = 40, step = 10) { quality ->
                seen.add(quality)
                ByteArray(50)
            }
        assertEquals(listOf(90), seen)
        assertContentEquals(ByteArray(50), result)
    }

    @Test
    fun `searchJpegQuality steps down until a result fits`() {
        val seen = mutableListOf<Int>()
        val result =
            searchJpegQuality(targetBytes = 100, maxQuality = 90, minQuality = 40, step = 10) { quality ->
                seen.add(quality)
                if (quality > 60) ByteArray(200) else ByteArray(50)
            }
        assertEquals(listOf(90, 80, 70, 60), seen)
        assertContentEquals(ByteArray(50), result)
    }

    @Test
    fun `searchJpegQuality falls back to minQuality when nothing fits`() {
        val seen = mutableListOf<Int>()
        val result =
            searchJpegQuality(targetBytes = 10, maxQuality = 90, minQuality = 40, step = 10) { quality ->
                seen.add(quality)
                ByteArray(200)
            }
        assertEquals(listOf(90, 80, 70, 60, 50, 40), seen)
        assertContentEquals(ByteArray(200), result)
    }

    @Test
    fun `searchJpegQuality clamps an uneven step to minQuality instead of overshooting`() {
        val seen = mutableListOf<Int>()
        searchJpegQuality(targetBytes = 10, maxQuality = 45, minQuality = 40, step = 10) { quality ->
            seen.add(quality)
            ByteArray(200)
        }
        assertEquals(listOf(45, 40), seen)
    }
}
