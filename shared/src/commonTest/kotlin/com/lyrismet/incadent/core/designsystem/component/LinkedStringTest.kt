package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkedStringTest {
    private val text =
        "See https://dnd.wizards.com/resources/systems-reference-document. " +
            "Then https://creativecommons.org/licenses/by/4.0/legalcode."

    @Test
    fun `both addresses become links without the closing dot`() {
        val links = linkedString(text, Color.Red).getLinkAnnotations(0, text.length)
        val urls = links.map { (it.item as LinkAnnotation.Url).url }
        assertEquals(
            listOf(
                "https://dnd.wizards.com/resources/systems-reference-document",
                "https://creativecommons.org/licenses/by/4.0/legalcode",
            ),
            urls,
        )
    }

    @Test
    fun `the visible text stays the same`() {
        assertEquals(text, linkedString(text, Color.Red).text)
    }
}
