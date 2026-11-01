package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.feature.mention.domain.withoutBrokenTokens

/** a typed edit with every token it cut into dropped whole, the caret landing where the same text position now is */
fun cleanedEdit(
    text: String,
    selection: TextRange,
    composition: TextRange? = null,
): TextFieldValue {
    val clean = withoutBrokenTokens(text)
    if (clean == text) return TextFieldValue(text, selection, composition)
    val caret = withoutBrokenTokens(text.take(selection.end)).length
    return TextFieldValue(clean, TextRange(caret.coerceAtMost(clean.length)))
}
