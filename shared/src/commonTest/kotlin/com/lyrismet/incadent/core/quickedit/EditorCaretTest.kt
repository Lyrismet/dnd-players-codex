package com.lyrismet.incadent.core.quickedit

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorCaretTest {
    @Test
    fun `the caret of an opened editor sits at the end of the text`() {
        assertEquals(TextRange(7), editorCaretAtEnd("Кассиан"))
    }

    @Test
    fun `an empty draft puts the caret at zero`() {
        assertEquals(TextRange(0), editorCaretAtEnd(""))
    }

    @Test
    fun `nothing is selected when the editor opens`() {
        assertEquals(true, editorCaretAtEnd("abc").collapsed)
    }
}
