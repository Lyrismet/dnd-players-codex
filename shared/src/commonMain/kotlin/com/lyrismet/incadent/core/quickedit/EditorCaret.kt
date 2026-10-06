package com.lyrismet.incadent.core.quickedit

import androidx.compose.ui.text.TextRange

/** where the caret of a freshly opened editor sits - the end of the text, nothing selected */
fun editorCaretAtEnd(text: String): TextRange = TextRange(text.length)
