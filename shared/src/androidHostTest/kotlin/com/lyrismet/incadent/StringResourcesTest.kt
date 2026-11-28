package com.lyrismet.incadent

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class StringResourcesTest {
    private fun keysOf(folder: String): Set<String> {
        val file = File("src/commonMain/composeResources/$folder/strings.xml")
        return Regex("""<(?:string|plurals|string-array) name="([^"]+)"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun `every russian string has an english one and the other way round`() {
        val russian = keysOf("values")
        val english = keysOf("values-en")
        assertEquals(emptySet(), russian - english, "keys missing in values-en")
        assertEquals(emptySet(), english - russian, "keys missing in values")
    }

    // a line break inside a value shows up in the app as a gap and an indent
    @Test
    fun `no string wraps its text across lines`() {
        val broken =
            listOf("values", "values-en").flatMap { folder ->
                Regex("""<(string|item)\b[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
                    .map { it.groupValues[2] }
                    .filter { it.contains('\n') || it != it.trim() }
                    .map { it.take(40) }
                    .toList()
            }
        assertEquals(emptyList(), broken)
    }
}
