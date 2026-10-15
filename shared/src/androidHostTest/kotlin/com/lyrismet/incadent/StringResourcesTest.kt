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
}
